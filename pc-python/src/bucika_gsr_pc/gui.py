"""
GUI application for the Bucika GSR PC Orchestrator using tkinter.
Provides visual interface for monitoring devices and sessions.
"""

import tkinter as tk
from tkinter import ttk, scrolledtext, filedialog, messagebox
import threading
import asyncio
import os
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List
from loguru import logger

from .session_manager import SessionManager
from .websocket_server import WebSocketServer
from .discovery_service import DiscoveryService

# Try to import video playback dependencies
try:
    import cv2
    from PIL import Image, ImageTk
    VIDEO_SUPPORT = True
except ImportError:
    VIDEO_SUPPORT = False


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
        
        # Video playback components
        self.video_listbox: Optional[tk.Listbox] = None
        self.video_canvas: Optional[tk.Canvas] = None
        self.video_player: Optional['VideoPlayer'] = None
        self.video_control_frame: Optional[ttk.Frame] = None
        self.video_file_paths: List[str] = []  # Store full paths for video files
        
        # Status variables
        self.status_var = tk.StringVar(value="Starting...")
        self.websocket_status_var = tk.StringVar(value="Offline")
        self.discovery_status_var = tk.StringVar(value="Offline")
        self.video_status_var = tk.StringVar(value="No video loaded")
    
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
        
        # Video Playback tab
        video_frame = ttk.Frame(notebook)
        notebook.add(video_frame, text="Video Playback")
        self._create_video_tab(video_frame)
        
        # Add initial log message
        self._add_log_message("Bucika GSR PC Orchestrator started")
    
    def _create_video_tab(self, parent_frame):
        """Create the video playback tab"""
        # Create horizontal split
        paned_window = ttk.PanedWindow(parent_frame, orient=tk.HORIZONTAL)
        paned_window.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        # Left panel - Video list
        left_frame = ttk.Frame(paned_window)
        paned_window.add(left_frame, weight=1)
        
        ttk.Label(left_frame, text="Video Files", font=("Arial", 10, "bold")).pack(anchor=tk.W, pady=(0, 5))
        
        # Video list with scrollbar
        list_frame = ttk.Frame(left_frame)
        list_frame.pack(fill=tk.BOTH, expand=True)
        
        self.video_listbox = tk.Listbox(list_frame, selectmode=tk.SINGLE)
        video_scrollbar = ttk.Scrollbar(list_frame, orient=tk.VERTICAL, command=self.video_listbox.yview)
        self.video_listbox.configure(yscrollcommand=video_scrollbar.set)
        
        self.video_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        video_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        # Video list controls
        list_controls = ttk.Frame(left_frame)
        list_controls.pack(fill=tk.X, pady=(5, 0))
        
        ttk.Button(list_controls, text="Refresh", command=self._refresh_video_list).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(list_controls, text="Browse...", command=self._browse_video_file).pack(side=tk.LEFT)
        
        # Bind selection event
        self.video_listbox.bind('<<ListboxSelect>>', self._on_video_select)
        
        # Right panel - Video player
        right_frame = ttk.Frame(paned_window)
        paned_window.add(right_frame, weight=3)
        
        # Video status
        video_status_frame = ttk.Frame(right_frame)
        video_status_frame.pack(fill=tk.X, pady=(0, 5))
        
        ttk.Label(video_status_frame, text="Status:").pack(side=tk.LEFT)
        ttk.Label(video_status_frame, textvariable=self.video_status_var).pack(side=tk.LEFT, padx=(5, 0))
        
        # Video canvas
        canvas_frame = ttk.Frame(right_frame)
        canvas_frame.pack(fill=tk.BOTH, expand=True, pady=(0, 10))
        
        if VIDEO_SUPPORT:
            self.video_canvas = tk.Canvas(canvas_frame, bg="black", width=640, height=480)
            self.video_canvas.pack(fill=tk.BOTH, expand=True)
            
            # Video player instance
            self.video_player = VideoPlayer(self.video_canvas, self.video_status_var)
        else:
            # Fallback when video support is not available
            no_video_label = ttk.Label(canvas_frame, 
                                     text="Video playback not available\n(Install opencv-python and pillow for video support)",
                                     justify=tk.CENTER)
            no_video_label.pack(expand=True)
        
        # Video controls
        self.video_control_frame = ttk.Frame(right_frame)
        self.video_control_frame.pack(fill=tk.X)
        
        if VIDEO_SUPPORT:
            self._create_video_controls()
        
        # Initialize video list
        self._refresh_video_list()
    
    def _create_video_controls(self):
        """Create video playback controls"""
        # Playback controls
        controls_frame = ttk.Frame(self.video_control_frame)
        controls_frame.pack(side=tk.LEFT, fill=tk.X, expand=True)
        
        ttk.Button(controls_frame, text="⏮", width=3, command=self._video_previous_frame).pack(side=tk.LEFT, padx=2)
        ttk.Button(controls_frame, text="▶", width=3, command=self._video_play_pause).pack(side=tk.LEFT, padx=2)
        ttk.Button(controls_frame, text="⏹", width=3, command=self._video_stop).pack(side=tk.LEFT, padx=2)
        ttk.Button(controls_frame, text="⏭", width=3, command=self._video_next_frame).pack(side=tk.LEFT, padx=2)
        
        # Progress bar (placeholder - would need more advanced implementation for seeking)
        self.video_progress = ttk.Progressbar(controls_frame, length=200, mode='determinate')
        self.video_progress.pack(side=tk.LEFT, padx=(10, 5), fill=tk.X, expand=True)
        
        # Volume/speed controls
        ttk.Label(controls_frame, text="Speed:").pack(side=tk.RIGHT, padx=(0, 2))
        speed_var = tk.StringVar(value="1.0")
        speed_combo = ttk.Combobox(controls_frame, textvariable=speed_var, values=["0.25", "0.5", "1.0", "1.5", "2.0"], 
                                   width=6, state="readonly")
        speed_combo.pack(side=tk.RIGHT, padx=(0, 10))
    
    def _refresh_video_list(self):
        """Refresh the list of available video files"""
        if not self.video_listbox:
            return
            
        # Clear current list
        self.video_listbox.delete(0, tk.END)
        
        # Find video files in sessions directory
        sessions_dir = Path("sessions")
        if not sessions_dir.exists():
            return
        
        video_extensions = {'.mp4', '.avi', '.mov', '.mkv', '.webm', '.flv', '.wmv'}
        video_files = []
        
        # Search through all session directories
        for session_dir in sessions_dir.iterdir():
            if session_dir.is_dir():
                # Check uploads folder
                uploads_dir = session_dir / "uploads"
                if uploads_dir.exists():
                    for file_path in uploads_dir.iterdir():
                        if file_path.is_file() and file_path.suffix.lower() in video_extensions:
                            relative_path = file_path.relative_to(sessions_dir)
                            video_files.append((str(relative_path), str(file_path)))
        
        # Sort and add to listbox
        video_files.sort(key=lambda x: x[0])
        for display_name, full_path in video_files:
            self.video_listbox.insert(tk.END, display_name)
        
        # Store full paths for reference
        self.video_file_paths = [full_path for _, full_path in video_files]
        
        if video_files:
            self.video_status_var.set(f"Found {len(video_files)} video files")
        else:
            self.video_status_var.set("No video files found")
    
    def _browse_video_file(self):
        """Browse for a video file to load"""
        if not VIDEO_SUPPORT:
            messagebox.showwarning("Video Support", "Video playback requires opencv-python and pillow to be installed")
            return
            
        filetypes = [
            ("Video files", "*.mp4 *.avi *.mov *.mkv *.webm *.flv *.wmv"),
            ("MP4 files", "*.mp4"),
            ("AVI files", "*.avi"),
            ("All files", "*.*")
        ]
        
        filename = filedialog.askopenfilename(title="Select Video File", filetypes=filetypes)
        if filename and self.video_player:
            self.video_player.load_video(filename)
            self.video_status_var.set(f"Loaded: {Path(filename).name}")
    
    def _on_video_select(self, event):
        """Handle video selection from list"""
        if not VIDEO_SUPPORT or not self.video_listbox or not self.video_player:
            return
            
        selection = self.video_listbox.curselection()
        if selection:
            index = selection[0]
            if index < len(self.video_file_paths):
                video_path = self.video_file_paths[index]
                self.video_player.load_video(video_path)
                self.video_status_var.set(f"Loaded: {Path(video_path).name}")
    
    def _video_play_pause(self):
        """Toggle video play/pause"""
        if self.video_player:
            self.video_player.toggle_play_pause()
    
    def _video_stop(self):
        """Stop video playback"""
        if self.video_player:
            self.video_player.stop()
    
    def _video_previous_frame(self):
        """Go to previous frame"""
        if self.video_player:
            self.video_player.previous_frame()
    
    def _video_next_frame(self):
        """Go to next frame"""
        if self.video_player:
            self.video_player.next_frame()
    
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


class VideoPlayer:
    """Simple video player for the GUI"""
    
    def __init__(self, canvas: tk.Canvas, status_var: tk.StringVar):
        self.canvas = canvas
        self.status_var = status_var
        self.video_cap: Optional[cv2.VideoCapture] = None
        self.is_playing = False
        self.current_frame = 0
        self.total_frames = 0
        self.fps = 30.0
        self.frame_delay = 33  # milliseconds
        self.play_job = None
        
    def load_video(self, video_path: str) -> bool:
        """Load a video file"""
        if not VIDEO_SUPPORT:
            return False
            
        try:
            # Release previous video
            self.stop()
            if self.video_cap:
                self.video_cap.release()
            
            # Load new video
            self.video_cap = cv2.VideoCapture(video_path)
            
            if not self.video_cap.isOpened():
                self.status_var.set(f"Error: Cannot open video {Path(video_path).name}")
                return False
            
            # Get video properties
            self.total_frames = int(self.video_cap.get(cv2.CAP_PROP_FRAME_COUNT))
            self.fps = self.video_cap.get(cv2.CAP_PROP_FPS) or 30.0
            self.frame_delay = max(1, int(1000 / self.fps))
            self.current_frame = 0
            
            # Display first frame
            self._show_frame()
            
            duration = self.total_frames / self.fps
            self.status_var.set(f"Loaded: {Path(video_path).name} ({self.total_frames} frames, {duration:.1f}s)")
            return True
            
        except Exception as e:
            self.status_var.set(f"Error loading video: {e}")
            return False
    
    def _show_frame(self) -> bool:
        """Display the current frame"""
        if not self.video_cap or not VIDEO_SUPPORT:
            return False
            
        try:
            # Set frame position
            self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, self.current_frame)
            
            # Read frame
            ret, frame = self.video_cap.read()
            if not ret:
                return False
            
            # Convert color space
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Resize frame to fit canvas
            canvas_width = self.canvas.winfo_width()
            canvas_height = self.canvas.winfo_height()
            
            if canvas_width > 1 and canvas_height > 1:  # Ensure canvas is initialized
                h, w = frame_rgb.shape[:2]
                
                # Calculate scaling to fit while maintaining aspect ratio
                scale_w = canvas_width / w
                scale_h = canvas_height / h
                scale = min(scale_w, scale_h, 1.0)  # Don't scale up
                
                new_w = int(w * scale)
                new_h = int(h * scale)
                
                frame_resized = cv2.resize(frame_rgb, (new_w, new_h))
                
                # Convert to PIL Image and then to PhotoImage
                pil_image = Image.fromarray(frame_resized)
                photo = ImageTk.PhotoImage(pil_image)
                
                # Clear canvas and display image
                self.canvas.delete("all")
                x = (canvas_width - new_w) // 2
                y = (canvas_height - new_h) // 2
                self.canvas.create_image(x, y, anchor=tk.NW, image=photo)
                
                # Keep a reference to prevent garbage collection
                self.canvas.image = photo
            
            return True
            
        except Exception as e:
            logger.error(f"Error displaying frame: {e}")
            return False
    
    def toggle_play_pause(self):
        """Toggle play/pause state"""
        if self.is_playing:
            self.pause()
        else:
            self.play()
    
    def play(self):
        """Start playing the video"""
        if not self.video_cap or self.is_playing:
            return
            
        self.is_playing = True
        self._play_next_frame()
        self.status_var.set("Playing...")
    
    def pause(self):
        """Pause the video"""
        self.is_playing = False
        if self.play_job:
            self.canvas.after_cancel(self.play_job)
            self.play_job = None
        self.status_var.set("Paused")
    
    def stop(self):
        """Stop the video and reset to beginning"""
        self.pause()
        self.current_frame = 0
        if self.video_cap:
            self._show_frame()
        self.status_var.set("Stopped")
    
    def previous_frame(self):
        """Go to previous frame"""
        if not self.video_cap:
            return
            
        self.current_frame = max(0, self.current_frame - 1)
        self._show_frame()
        self.status_var.set(f"Frame {self.current_frame + 1}/{self.total_frames}")
    
    def next_frame(self):
        """Go to next frame"""
        if not self.video_cap:
            return
            
        self.current_frame = min(self.total_frames - 1, self.current_frame + 1)
        self._show_frame()
        self.status_var.set(f"Frame {self.current_frame + 1}/{self.total_frames}")
    
    def _play_next_frame(self):
        """Play next frame (internal method for continuous playback)"""
        if not self.is_playing or not self.video_cap:
            return
        
        # Show current frame
        if self._show_frame():
            self.current_frame += 1
            
            # Check if we've reached the end
            if self.current_frame >= self.total_frames:
                self.stop()
                return
            
            # Schedule next frame
            self.play_job = self.canvas.after(self.frame_delay, self._play_next_frame)
        else:
            # Error showing frame, stop playback
            self.stop()