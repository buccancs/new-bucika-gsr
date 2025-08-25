"""
GUI application for the Bucika GSR PC Orchestrator using tkinter.
Provides visual interface for monitoring devices and sessions.
"""

import tkinter as tk
from tkinter import ttk, scrolledtext
import threading
import asyncio
from datetime import datetime
from typing import Optional
from loguru import logger

from .session_manager import SessionManager
from .websocket_server import WebSocketServer
from .discovery_service import DiscoveryService


class MainWindow:
    """Main GUI window for the orchestrator"""
    
    def __init__(self, session_manager: SessionManager, 
                 websocket_server: WebSocketServer,
                 discovery_service: DiscoveryService):
        self.session_manager = session_manager
        self.websocket_server = websocket_server
        self.discovery_service = discovery_service
        
        self.root: Optional[tk.Tk] = None
        self.running = False
        
        # GUI components
        self.devices_tree: Optional[ttk.Treeview] = None
        self.sessions_tree: Optional[ttk.Treeview] = None
        self.log_text: Optional[scrolledtext.ScrolledText] = None
        
        # Status variables
        self.status_var = tk.StringVar(value="Starting...")
        self.websocket_status_var = tk.StringVar(value="Offline")
        self.discovery_status_var = tk.StringVar(value="Offline")
    
    def start(self):
        """Start the GUI in a separate thread"""
        if not self.running:
            self.running = True
            gui_thread = threading.Thread(target=self._run_gui, daemon=True)
            gui_thread.start()
    
    def _run_gui(self):
        """Run the GUI main loop"""
        try:
            self.root = tk.Tk()
            self.root.title("Bucika GSR PC Orchestrator")
            self.root.geometry("1200x800")
            
            self._create_widgets()
            self._setup_layout()
            
            # Start periodic updates
            self.root.after(1000, self._update_display)
            
            # Handle window close
            self.root.protocol("WM_DELETE_WINDOW", self._on_closing)
            
            # Set status to ready
            self.status_var.set("Ready")
            self.websocket_status_var.set("Online")
            self.discovery_status_var.set("Broadcasting")
            
            logger.info("GUI started successfully")
            
            # Start main loop
            self.root.mainloop()
            
        except Exception as e:
            logger.error(f"GUI error: {e}")
    
    def _create_widgets(self):
        """Create all GUI widgets"""
        # Main frame
        main_frame = ttk.Frame(self.root)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Status bar
        status_frame = ttk.Frame(main_frame)
        status_frame.pack(fill=tk.X, pady=(0, 10))
        
        ttk.Label(status_frame, text="Status:").pack(side=tk.LEFT)
        ttk.Label(status_frame, textvariable=self.status_var).pack(side=tk.LEFT, padx=(5, 20))
        
        ttk.Label(status_frame, text="WebSocket:").pack(side=tk.LEFT)
        ttk.Label(status_frame, textvariable=self.websocket_status_var).pack(side=tk.LEFT, padx=(5, 20))
        
        ttk.Label(status_frame, text="Discovery:").pack(side=tk.LEFT)
        ttk.Label(status_frame, textvariable=self.discovery_status_var).pack(side=tk.LEFT, padx=(5, 0))
        
        # Create notebook for tabs
        notebook = ttk.Notebook(main_frame)
        notebook.pack(fill=tk.BOTH, expand=True)
        
        # Devices tab
        devices_frame = ttk.Frame(notebook)
        notebook.add(devices_frame, text="Connected Devices")
        
        # Devices tree
        devices_columns = ("Device ID", "Device Name", "Version", "Battery", "Connected At")
        self.devices_tree = ttk.Treeview(devices_frame, columns=devices_columns, show="headings", height=10)
        
        for col in devices_columns:
            self.devices_tree.heading(col, text=col)
            self.devices_tree.column(col, width=150)
        
        devices_scrollbar = ttk.Scrollbar(devices_frame, orient=tk.VERTICAL, command=self.devices_tree.yview)
        self.devices_tree.configure(yscrollcommand=devices_scrollbar.set)
        
        self.devices_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        devices_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        # Sessions tab
        sessions_frame = ttk.Frame(notebook)
        notebook.add(sessions_frame, text="Sessions")
        
        # Sessions tree
        sessions_columns = ("Session ID", "Device ID", "Name", "State", "Started", "Samples")
        self.sessions_tree = ttk.Treeview(sessions_frame, columns=sessions_columns, show="headings", height=10)
        
        for col in sessions_columns:
            self.sessions_tree.heading(col, text=col)
            self.sessions_tree.column(col, width=150)
        
        sessions_scrollbar = ttk.Scrollbar(sessions_frame, orient=tk.VERTICAL, command=self.sessions_tree.yview)
        self.sessions_tree.configure(yscrollcommand=sessions_scrollbar.set)
        
        self.sessions_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        sessions_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        # Log tab
        log_frame = ttk.Frame(notebook)
        notebook.add(log_frame, text="Logs")
        
        self.log_text = scrolledtext.ScrolledText(log_frame, height=20, width=80)
        self.log_text.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        # Add initial log message
        self._add_log_message("Bucika GSR PC Orchestrator started")
    
    def _setup_layout(self):
        """Setup the window layout"""
        # Window icon (if available)
        try:
            # You could add an icon here if you have one
            pass
        except:
            pass
    
    def _update_display(self):
        """Update the display with current data"""
        if not self.running:
            return
        
        try:
            # Update devices table
            self._update_devices_tree()
            
            # Update sessions table
            self._update_sessions_tree()
            
            # Schedule next update
            if self.root:
                self.root.after(2000, self._update_display)  # Update every 2 seconds
                
        except Exception as e:
            logger.error(f"Error updating display: {e}")
    
    def _update_devices_tree(self):
        """Update the devices tree view"""
        if not self.devices_tree:
            return
        
        # Clear existing items
        for item in self.devices_tree.get_children():
            self.devices_tree.delete(item)
        
        # Add connected devices
        devices = self.websocket_server.get_connected_devices()
        for device_id, device in devices.items():
            connected_time = device.connected_at.strftime("%H:%M:%S")
            
            self.devices_tree.insert("", tk.END, values=(
                device_id,
                device.device_name,
                device.version,
                f"{device.battery_level}%",
                connected_time
            ))
    
    def _update_sessions_tree(self):
        """Update the sessions tree view"""
        if not self.sessions_tree:
            return
        
        # Clear existing items
        for item in self.sessions_tree.get_children():
            self.sessions_tree.delete(item)
        
        # Add all sessions
        sessions = self.session_manager.get_all_sessions()
        for session_id, session in sessions.items():
            started_time = ""
            if session.started_at:
                started_time = session.started_at.strftime("%H:%M:%S")
            
            self.sessions_tree.insert("", tk.END, values=(
                session_id,
                session.device_id,
                session.session_name,
                session.state.value,
                started_time,
                len(session.gsr_samples)
            ))
    
    def _add_log_message(self, message: str):
        """Add a message to the log text widget"""
        if self.log_text:
            timestamp = datetime.now().strftime("%H:%M:%S")
            log_entry = f"[{timestamp}] {message}\n"
            
            self.log_text.insert(tk.END, log_entry)
            self.log_text.see(tk.END)  # Scroll to bottom
    
    def _on_closing(self):
        """Handle window closing"""
        logger.info("GUI shutdown requested")
        self.running = False
        if self.root:
            self.root.quit()
            self.root.destroy()


# Custom log handler to display logs in GUI
class GUILogHandler:
    """Log handler that forwards messages to the GUI"""
    
    def __init__(self, main_window: Optional[MainWindow] = None):
        self.main_window = main_window
    
    def write(self, message: str):
        """Write log message to GUI"""
        if self.main_window and message.strip():
            try:
                # Schedule GUI update in main thread
                if self.main_window.root:
                    self.main_window.root.after(0, lambda: self._safe_add_log(message.strip()))
            except:
                pass  # Ignore errors when GUI is not ready
    
    def _safe_add_log(self, message: str):
        """Safely add log message to GUI"""
        try:
            self.main_window._add_log_message(message)
        except:
            pass  # Ignore errors if GUI components are not ready
    
    def flush(self):
        """Required for file-like interface"""
        pass