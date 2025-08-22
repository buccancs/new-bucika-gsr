import argparse
import os
import sys
from pathlib import Path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from hand_segmentation import SessionPostProcessor, create_session_post_processor
def main():
    parser = _create_argument_parser()
    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        return 1
    processor = create_session_post_processor(args.recordings_dir)
    return _execute_command(processor, args)
def _create_argument_parser():
    parser = argparse.ArgumentParser(
        description="Hand Segmentation CLI Tool for Post-Session Processing",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=_get_help_examples(),
    )
    parser.add_argument(
        "--recordings-dir",
        default="recordings",
        help="Base directory containing session recordings (default: recordings)",
    )
    _add_subcommands(parser)
    return parser
def _get_help_examples():
    return """
Examples:
  python hand_segmentation_cli.py list-sessions
  python hand_segmentation_cli.py process-session session_20250131_143022
  python hand_segmentation_cli.py process-session session_20250131_143022 --method color_based
  python hand_segmentation_cli.py process-video /path/to/video.mp4
  python hand_segmentation_cli.py status session_20250131_143022
  python hand_segmentation_cli.py process-session SESSION_ID \\
    --method mediapipe \\
    --confidence 0.7 \\
    --max-hands 2 \\
    --output-cropped \\
    --output-masks
"""

def _add_subcommands(parser):
    """Add subcommands to the argument parser."""
    subparsers = parser.add_subparsers(dest="command", help="Available commands")

    # List sessions command
    subparsers.add_parser("list-sessions", help="List available sessions that contain videos")

    # Process session command
    session_parser = subparsers.add_parser("process-session", help="Process all videos in a session")
    session_parser.add_argument("session_id", help="Session ID to process")
    add_processing_args(session_parser)

    # Process video command
    video_parser = subparsers.add_parser("process-video", help="Process a single video file")
    video_parser.add_argument("video_path", help="Path to video file")
    video_parser.add_argument("--output-dir", help="Output directory (default: same directory as video)")
    add_processing_args(video_parser)

    # Status command
    status_parser = subparsers.add_parser("status", help="Check processing status of a session")
    status_parser.add_argument("session_id", help="Session ID to check")

    # Cleanup command
    cleanup_parser = subparsers.add_parser("cleanup", help="Clean up segmentation outputs for a session")
    cleanup_parser.add_argument("session_id", help="Session ID to clean up")

def _setup_argument_parser():
    parser = argparse.ArgumentParser(
        description="CLI for hand segmentation processing",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=_get_help_examples()
    )
    _add_subcommands(parser)
    return parser
def _execute_command(processor, args):
    try:
        command_map = {
            "list-sessions": cmd_list_sessions,
            "process-session": cmd_process_session,
            "process-video": cmd_process_video,
            "status": cmd_status,
            "cleanup": cmd_cleanup,
        }
        if args.command in command_map:
            if args.command == "list-sessions":
                return command_map[args.command](processor)
            else:
                return command_map[args.command](processor, args)
        else:
            print(f"Unknown command: {args.command}")
            return 1
    except KeyboardInterrupt:
        print("\n[INFO] Processing interrupted by user")
        return 1
    except Exception as e:
        print(f"[ERROR] {e}")
        return 1
def add_processing_args(parser):
    parser.add_argument(
        "--method",
        choices=["mediapipe", "color_based", "contour_based"],
        default="mediapipe",
        help="Segmentation method to use (default: mediapipe)",
    )
    parser.add_argument(
        "--confidence",
        type=float,
        default=0.5,
        help="Minimum detection confidence (default: 0.5)",
    )
    parser.add_argument(
        "--tracking-confidence",
        type=float,
        default=0.5,
        help="Minimum tracking confidence (default: 0.5)",
    )
    parser.add_argument(
        "--max-hands",
        type=int,
        default=2,
        help="Maximum number of hands to detect (default: 2)",
    )
    parser.add_argument(
        "--output-cropped",
        action="store_true",
        help="Generate cropped hand region videos",
    )
    parser.add_argument(
        "--output-masks", action="store_true", help="Generate hand mask videos"
    )
    parser.add_argument(
        "--crop-padding",
        type=int,
        default=20,
        help="Padding around detected hand regions (default: 20)",
    )
def cmd_list_sessions(processor: SessionPostProcessor) -> int:
    sessions = processor.discover_sessions()
    if not sessions:
        print("No sessions with videos found.")
        return 0
    print(f"Available sessions ({len(sessions)}):")
    for session in sessions:
        videos = processor.get_session_videos(session)
        print(f"  {session} ({len(videos)} videos)")
        status = processor.get_processing_status(session)
        processed_count = sum(1 for is_processed in status.values() if is_processed)
        if processed_count > 0:
            print(f"    - {processed_count}/{len(videos)} videos already processed")
    return 0
def cmd_process_session(processor: SessionPostProcessor, args) -> int:
    print(f"Processing session: {args.session_id}")
    print(f"Method: {args.method}")
    print(f"Recordings directory: {processor.base_recordings_dir}")
    sessions = processor.discover_sessions()
    if args.session_id not in sessions:
        print(f"[ERROR] Session not found: {args.session_id}")
        print(f"Available sessions: {', '.join(sessions) if sessions else 'None'}")
        return 1
    config_kwargs = {
        "min_detection_confidence": args.confidence,
        "min_tracking_confidence": args.tracking_confidence,
        "max_num_hands": args.max_hands,
        "output_cropped": args.output_cropped,
        "output_masks": args.output_masks,
        "crop_padding": args.crop_padding,
    }
    results = processor.process_session(args.session_id, args.method, **config_kwargs)
    if results:
        successful = sum(1 for r in results.values() if r.success)
        total = len(results)
        total_detections = sum(r.detected_hands_count for r in results.values())
        total_time = sum(r.processing_time for r in results.values())
        print(f"\nProcessing Summary:")
        print(f"  Videos processed: {successful}/{total}")
        print(f"  Total hand detections: {total_detections}")
        print(f"  Total processing time: {total_time:.2f}s")
        if successful < total:
            print(f"  Failed videos:")
            for video_path, result in results.items():
                if not result.success:
                    print(f"    - {video_path}: {result.error_message}")
        return 0 if successful > 0 else 1
    else:
        print("No videos processed.")
        return 1
def cmd_process_video(processor: SessionPostProcessor, args) -> int:
    video_path = Path(args.video_path)
    if not video_path.exists():
        print(f"[ERROR] Video file not found: {video_path}")
        return 1
    print(f"Processing video: {video_path}")
    print(f"Method: {args.method}")
    config_kwargs = {
        "min_detection_confidence": args.confidence,
        "min_tracking_confidence": args.tracking_confidence,
        "max_num_hands": args.max_hands,
        "output_cropped": args.output_cropped,
        "output_masks": args.output_masks,
        "crop_padding": args.crop_padding,
    }
    result = processor.process_video_file(
        str(video_path), args.output_dir, args.method, **config_kwargs
    )
    if result.success:
        print(f"\nProcessing completed successfully:")
        print(f"  Frames processed: {result.processed_frames}")
        print(f"  Hand detections: {result.detected_hands_count}")
        print(f"  Processing time: {result.processing_time:.2f}s")
        print(f"  Output directory: {result.output_directory}")
        if result.output_files:
            print(f"  Output files:")
            for file_type, file_path in result.output_files.items():
                print(f"    - {file_type}: {file_path}")
        return 0
    else:
        print(f"[ERROR] Processing failed: {result.error_message}")
        return 1
def cmd_status(processor: SessionPostProcessor, args) -> int:
    sessions = processor.discover_sessions()
    if args.session_id not in sessions:
        print(f"[ERROR] Session not found: {args.session_id}")
        return 1
    status = processor.get_processing_status(args.session_id)
    if not status:
        print(f"No videos found in session: {args.session_id}")
        return 0
    print(f"Processing status for session: {args.session_id}")
    processed_count = 0
    for video_path, is_processed in status.items():
        status_str = "[PASS] Processed" if is_processed else "[FAIL] Not processed"
        print(f"  {Path(video_path).name}: {status_str}")
        if is_processed:
            processed_count += 1
    print(f"\nSummary: {processed_count}/{len(status)} videos processed")
    return 0
def cmd_cleanup(processor: SessionPostProcessor, args) -> int:
    sessions = processor.discover_sessions()
    if args.session_id not in sessions:
        print(f"[ERROR] Session not found: {args.session_id}")
        return 1
    print(f"Cleaning up segmentation outputs for session: {args.session_id}")
    processor.cleanup_session_outputs(args.session_id)
    print("Cleanup completed.")
    return 0
if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)
