import json
import logging
import socket
import statistics
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from typing import Callable, Dict, List, Optional
import ntplib
@dataclass
class TimeServerStatus:
    is_running: bool = False
    is_synchronized: bool = False
    reference_source: str = "system"
    last_ntp_sync: Optional[float] = None
    time_accuracy_ms: float = 0.0
    client_count: int = 0
    requests_served: int = 0
    average_response_time_ms: float = 0.0
@dataclass
class TimeSyncRequest:
    client_id: str
    request_timestamp: float
    sequence_number: int = 0
@dataclass
class TimeSyncResponse:
    server_timestamp: float
    request_timestamp: float
    response_timestamp: float
    server_precision_ms: float
    sequence_number: int = 0
class NTPTimeServer:
    def __init__(self, logger=None, port=8889):
        self.logger = logger or logging.getLogger(__name__)
        self.port = port
        self.is_running = False
        self.server_socket: Optional[socket.socket] = None
        self.server_thread: Optional[threading.Thread] = None
        self.thread_pool = ThreadPoolExecutor(max_workers=10)
        self.ntp_client = ntplib.NTPClient()
        self.reference_time_offset = 0.0
        self.last_ntp_sync_time = 0.0
        self.time_precision_ms = 1.0
        self.status = TimeServerStatus()
        self.connected_clients: Dict[str, float] = {}
        self.response_times: List[float] = []
        self.sync_callbacks: List[Callable[[TimeSyncResponse], None]] = []
        self.ntp_servers = ["pool.ntp.org", "time.google.com", "time.cloudflare.com"]
        self.ntp_sync_interval = 300.0
        self.max_response_time_history = 100
        self.stop_event = threading.Event()
        self.stats_lock = threading.Lock()
        self.logger.info("NTPTimeServer initialized on port %d", self.port)
    def start_server(self) -> bool:
        try:
            if self.is_running:
                self.logger.warning("NTP time server already running")
                return True
            self.logger.info("Starting NTP time server...")
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(("0.0.0.0", self.port))
            self.server_socket.listen(10)
            self.stop_event.clear()
            self.server_thread = threading.Thread(
                target=self._server_loop, name="NTPTimeServer"
            )
            self.server_thread.daemon = True
            self.server_thread.start()
            self._start_ntp_sync_thread()
            self.status.is_running = True
            self.is_running = True
            self.logger.info(
                "NTP time server started successfully on port %d", self.port
            )
            return True
        except Exception as e:
            self.logger.error("Failed to start NTP time server: %s", e)
            return False
    def stop_server(self) -> None:
        try:
            if not self.is_running:
                return
            self.logger.info("Stopping NTP time server...")
            self.stop_event.set()
            self.is_running = False
            if self.server_socket:
                self.server_socket.close()
                self.server_socket = None
            if self.server_thread and self.server_thread.is_alive():
                self.server_thread.join(timeout=5.0)
            self.thread_pool.shutdown(wait=True)
            self.status.is_running = False
            self.status.client_count = 0
            self.connected_clients.clear()
            self.logger.info("NTP time server stopped")
        except Exception as e:
            self.logger.error("Error stopping NTP time server: %s", e)
    def get_precise_timestamp(self) -> float:
        try:
            current_time = time.time()
            if self.status.is_synchronized:
                corrected_time = current_time + self.reference_time_offset
                return corrected_time
            else:
                return current_time
        except Exception as e:
            self.logger.error("Error getting precise timestamp: %s", e)
            return time.time()
    def get_timestamp_milliseconds(self) -> int:
        return int(self.get_precise_timestamp() * 1000)
    def synchronize_with_ntp(self) -> bool:
        try:
            self.logger.info("Synchronising with NTP servers...")
            successful_syncs = []
            for ntp_server in self.ntp_servers:
                try:
                    self.logger.debug("Querying NTP server: %s", ntp_server)
                    response = self.ntp_client.request(ntp_server, version=3, timeout=5)
                    ntp_time = response.tx_time
                    local_time = time.time()
                    offset = ntp_time - local_time
                    successful_syncs.append(
                        {
                            "server": ntp_server,
                            "offset": offset,
                            "delay": response.delay,
                            "precision": response.precision,
                        }
                    )
                    self.logger.debug(
                        "NTP sync with %s: offset=%.3fms, delay=%.3fms",
                        ntp_server,
                        offset * 1000,
                        response.delay * 1000,
                    )
                except Exception as e:
                    self.logger.warning(
                        "Failed to sync with NTP server %s: %s", ntp_server, e
                    )
                    continue
            if successful_syncs:
                offsets = [sync["offset"] for sync in successful_syncs]
                self.reference_time_offset = statistics.median(offsets)
                delays = [sync["delay"] for sync in successful_syncs]
                self.time_precision_ms = statistics.median(delays) * 1000 / 2
                self.status.is_synchronized = True
                self.status.reference_source = "ntp"
                self.status.last_ntp_sync = time.time()
                self.status.time_accuracy_ms = self.time_precision_ms
                self.last_ntp_sync_time = time.time()
                self.logger.info(
                    "NTP synchronisation successful: offset=%.3fms, precision=%.3fms",
                    self.reference_time_offset * 1000,
                    self.time_precision_ms,
                )
                return True
            else:
                self.logger.error("All NTP synchronisation attempts failed")
                self.status.is_synchronized = False
                self.status.reference_source = "system"
                self.time_precision_ms = 10.0
                return False
        except Exception as e:
            self.logger.error("Error during NTP synchronisation: %s", e)
            return False
    def _parse_sync_request(self, data: bytes, client_addr: str) -> Optional[Dict]:
        try:
            request_data = json.loads(data.decode("utf-8"))
        except json.JSONDecodeError:
            self.logger.error("Invalid JSON in sync request from %s", client_addr)
            return None
        if request_data.get("type") != "time_sync_request":
            return None
        return request_data
    def _create_sync_response(self, request_data: Dict, request_receive_time: float) -> Dict:
        client_id = request_data.get("client_id")
        request_timestamp = request_data.get("timestamp", 0)
        sequence_number = request_data.get("sequence", 0)
        response_send_time = self.get_precise_timestamp()
        return {
            "type": "time_sync_response",
            "server_timestamp": response_send_time,
            "request_timestamp": request_timestamp,
            "receive_timestamp": request_receive_time,
            "response_timestamp": response_send_time,
            "server_precision_ms": self.time_precision_ms,
            "sequence": sequence_number,
            "server_time_ms": self.get_timestamp_milliseconds(),
        }, client_id, sequence_number, response_send_time
    def _update_server_statistics(self, client_id: str, request_receive_time: float, response_send_time: float) -> None:
        with self.stats_lock:
            self.status.requests_served += 1
            self.connected_clients[client_id] = time.time()
            self.status.client_count = len(self.connected_clients)
            response_time = (response_send_time - request_receive_time) * 1000
            self.response_times.append(response_time)
            if len(self.response_times) > self.max_response_time_history:
                self.response_times.pop(0)
            if self.response_times:
                self.status.average_response_time_ms = statistics.mean(self.response_times)
    def _trigger_sync_callbacks(self, response_data: Dict, sequence_number: int) -> None:
        sync_response = TimeSyncResponse(
            server_timestamp=response_data["server_timestamp"],
            request_timestamp=response_data["request_timestamp"],
            response_timestamp=response_data["response_timestamp"],
            server_precision_ms=self.time_precision_ms,
            sequence_number=sequence_number,
        )
        for callback in self.sync_callbacks:
            try:
                callback(sync_response)
            except Exception as e:
                self.logger.error("Error in sync callback: %s", e)
    def handle_sync_request(self, client_socket: socket.socket, client_addr: str) -> None:
        try:
            data = client_socket.recv(4096)
            if not data:
                return
            request_receive_time = self.get_precise_timestamp()
            request_data = self._parse_sync_request(data, client_addr)
            if not request_data:
                return
            response_data, client_id, sequence_number, response_send_time = (
                self._create_sync_response(request_data, request_receive_time)
            )
            response_json = json.dumps(response_data)
            client_socket.send(response_json.encode("utf-8"))
            self._update_server_statistics(client_id, request_receive_time, response_send_time)
            self._trigger_sync_callbacks(response_data, sequence_number)
            self.logger.debug("Served time sync request from %s (seq=%d)", client_id, sequence_number)
        except Exception as e:
            self.logger.error("Error handling sync request from %s: %s", client_addr, e)
        finally:
            try:
                client_socket.close()
            except:
                pass
    def get_server_status(self) -> TimeServerStatus:
        with self.stats_lock:
            current_time = time.time()
            active_clients = {
                client_id: last_seen
                for client_id, last_seen in self.connected_clients.items()
                if current_time - last_seen < 60.0
            }
            self.connected_clients = active_clients
            self.status.client_count = len(active_clients)
            return TimeServerStatus(
                is_running=self.status.is_running,
                is_synchronized=self.status.is_synchronized,
                reference_source=self.status.reference_source,
                last_ntp_sync=self.status.last_ntp_sync,
                time_accuracy_ms=self.status.time_accuracy_ms,
                client_count=self.status.client_count,
                requests_served=self.status.requests_served,
                average_response_time_ms=self.status.average_response_time_ms,
            )
    def add_sync_callback(self, callback: Callable[[TimeSyncResponse], None]) -> None:
        self.sync_callbacks.append(callback)
    def _server_loop(self) -> None:
        self.logger.info("NTP time server loop started")
        try:
            while not self.stop_event.is_set():
                try:
                    self.server_socket.settimeout(1.0)
                    client_socket, client_addr = self.server_socket.accept()
                    self.thread_pool.submit(
                        self.handle_sync_request,
                        client_socket,
                        f"{client_addr[0]}:{client_addr[1]}",
                    )
                except socket.timeout:
                    continue
                except socket.error as e:
                    if not self.stop_event.is_set():
                        self.logger.error("Socket error in server loop: %s", e)
                    break
                except Exception as e:
                    self.logger.error("Error in server loop: %s", e)
                    break
        except Exception as e:
            self.logger.error("Fatal error in server loop: %s", e)
        finally:
            self.logger.info("NTP time server loop ended")
    def _start_ntp_sync_thread(self) -> None:
        def ntp_sync_loop():
            self.synchronize_with_ntp()
            while not self.stop_event.is_set():
                try:
                    if self.stop_event.wait(self.ntp_sync_interval):
                        break
                    self.synchronize_with_ntp()
                except Exception as e:
                    self.logger.error("Error in NTP sync loop: %s", e)
                    if self.stop_event.wait(60.0):
                        break
        sync_thread = threading.Thread(target=ntp_sync_loop, name="NTPSync")
        sync_thread.daemon = True
        sync_thread.start()
class TimeServerManager:
    def __init__(self, logger=None):
        self.logger = logger or logging.getLogger(__name__)
        self.time_server: Optional[NTPTimeServer] = None
    def initialize(self, port=8889) -> bool:
        try:
            self.time_server = NTPTimeServer(logger=self.logger, port=port)
            return True
        except Exception as e:
            self.logger.error("Failed to initialize time server: %s", e)
            return False
    def start(self) -> bool:
        if self.time_server:
            return self.time_server.start_server()
        return False
    def stop(self) -> None:
        if self.time_server:
            self.time_server.stop_server()
    def get_status(self) -> Optional[TimeServerStatus]:
        if self.time_server:
            return self.time_server.get_server_status()
        return None
    def get_timestamp_ms(self) -> int:
        if self.time_server:
            return self.time_server.get_timestamp_milliseconds()
        return int(time.time() * 1000)
if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    server = NTPTimeServer()
    try:
        if server.start_server():
            logging.info("NTP time server started successfully")
            def sync_callback(response):
                logging.info(
                    f"Sync request served: precision={response.server_precision_ms:.2f}ms"
                )
            server.add_sync_callback(sync_callback)
            logging.info("Server running... Press Ctrl+C to stop")
            while True:
                time.sleep(5)
                status = server.get_server_status()
                logging.info(
                    f"Status: clients={status.client_count}, requests={status.requests_served}, sync={status.is_synchronized}, accuracy={status.time_accuracy_ms:.2f}ms"
                )
    except KeyboardInterrupt:
        logging.info("Shutting down server...")
    finally:
        server.stop_server()
        logging.info("Server stopped")