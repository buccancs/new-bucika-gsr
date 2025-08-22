import logging
from pathlib import Path
def _import_shimmer():
    try:
        from pyshimmer import DEFAULT_BAUDRATE, DataPacket, ShimmerBluetooth
        from serial import Serial
        return Serial, ShimmerBluetooth, DataPacket, DEFAULT_BAUDRATE, True
    except ImportError:
        android_libs_path = (
            Path(__file__).parent.parent.parent / "AndroidApp" / "libs" / "pyshimmer"
        )
        if android_libs_path.exists():
            import sys
            original_path = sys.path.copy()
            try:
                sys.path.insert(0, str(android_libs_path))
                from pyshimmer import DEFAULT_BAUDRATE, DataPacket, ShimmerBluetooth
                from serial import Serial
                return Serial, ShimmerBluetooth, DataPacket, DEFAULT_BAUDRATE, True
            except ImportError as e:
                logging.warning(
                    f"PyShimmer library not available even with Android libs path: {e}"
                )
            finally:
                sys.path = original_path
        else:
            logging.warning(
                "PyShimmer library not available and Android libs path not found"
            )
        class Serial:
            def __init__(self, *args, **kwargs):
                pass
        class ShimmerBluetooth:
            def __init__(self, *args, **kwargs):
                pass
        class DataPacket:
            def __init__(self, *args, **kwargs):
                pass
        return Serial, ShimmerBluetooth, DataPacket, 115200, False
Serial, ShimmerBluetooth, DataPacket, DEFAULT_BAUDRATE, PYSHIMMER_AVAILABLE = (
    _import_shimmer()
)