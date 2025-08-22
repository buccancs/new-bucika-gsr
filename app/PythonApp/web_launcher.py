import argparse
import os
import sys
import time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
try:
    from PythonApp.utils.logging_config import get_logger
    from PythonApp.web_ui.integration import WebDashboardIntegration
    from PythonApp.network.pc_server import PCServer
    logger = get_logger(__name__)
except ImportError as e:
    print(f"Import error: {e}")
    print("Installing required dependencies...")
    import subprocess
    try:
        subprocess.check_call(
            [
                sys.executable,
                "-m",
                "pip",
                "install",
                "flask",
                "flask-socketio",
                "eventlet",
            ]
        )
        print("Dependencies installed. Please run the script again.")
        sys.exit(0)
    except Exception as install_error:
        print(f"Failed to install dependencies: {install_error}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(
        description="Multi-Sensor Recording System Web Dashboard",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python web_launcher.py
  python web_launcher.py --port 8080
  python web_launcher.py --host 0.0.0.0
  python web_launcher.py --debug
  python web_launcher.py --android-port 9000
        """
    )
    
    parser.add_argument(
        "--port", 
        type=int, 
        default=5000, 
        help="Web dashboard port (default: 5000)"
    )
    parser.add_argument(
        "--android-port", 
        type=int, 
        default=9000, 
        help="Android device connection port (default: 9000)"
    )
    parser.add_argument(
        "--host", 
        default="0.0.0.0", 
        help="Host to bind to (default: 0.0.0.0)"
    )
    parser.add_argument(
        "--debug", 
        action="store_true", 
        help="Enable debug mode"
    )
    parser.add_argument(
        "--no-pc-server", 
        action="store_true", 
        help="Don't start PC server for Android devices"
    )
    
    args = parser.parse_args()
    
    try:
        logger.info("=== Multi-Sensor Recording System Web Launcher ===")
        logger.info(f"Web UI Port: {args.port}")
        logger.info(f"Android Port: {args.android_port}")
        logger.info(f"Host: {args.host}")
        logger.info(f"Debug Mode: {args.debug}")
        
        # Start PC server for Android devices if enabled
        pc_server = None
        if not args.no_pc_server:
            logger.info(f"Starting PC Server for Android devices on port {args.android_port}...")
            pc_server = PCServer(port=args.android_port)
            if pc_server.start():
                logger.info(f"✅ PC Server started on port {args.android_port}")
            else:
                logger.error(f"❌ Failed to start PC Server on port {args.android_port}")
                sys.exit(1)
        
        # Start web dashboard integration
        logger.info(f"Starting Web Dashboard on {args.host}:{args.port}...")
        
        web_integration = WebDashboardIntegration(
            enable_web_ui=True,
            web_port=args.port,
            main_controller=None  # Will create WebController internally
        )
        
        if web_integration.start_web_dashboard():
            url = f"http://{args.host}:{args.port}"
            if args.host == "0.0.0.0":
                url = f"http://localhost:{args.port}"
            
            logger.info(f"✅ Web Dashboard started successfully!")
            logger.info(f"🌐 Access the dashboard at: {url}")
            if not args.no_pc_server:
                logger.info(f"📱 Android devices can connect to: {args.android_port}")
            
            print(f"\n{'='*60}")
            print(f"🚀 Multi-Sensor Recording System Started!")
            print(f"🌐 Web Dashboard: {url}")
            if not args.no_pc_server:
                print(f"📱 Android Connection Port: {args.android_port}")
            print(f"{'='*60}")
            print("Press Ctrl+C to stop...")
            
            # Keep running
            try:
                while True:
                    time.sleep(1)
            except KeyboardInterrupt:
                logger.info("Shutdown requested by user")
        else:
            logger.error("Failed to start web dashboard")
            sys.exit(1)
    
    except KeyboardInterrupt:
        logger.info("Shutdown requested")
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        sys.exit(1)
    finally:
        # Cleanup
        logger.info("Shutting down...")
        try:
            if pc_server:
                pc_server.stop()
                logger.info("PC Server stopped")
        except:
            pass

if __name__ == "__main__":
    main()
