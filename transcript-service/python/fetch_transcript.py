#!/usr/bin/env python3
"""
fetch_transcript.py

Given a YouTube video ID, downloads audio and transcribes it with Whisper,
returning the transcript as JSON on stdout.

NOTE: an earlier version of this script tried YouTube captions first via
youtube-transcript-api. That path was dropped - YouTube actively blocks
caption-scraping requests from datacenter/cloud IPs (confirmed during
development), which would make this unreliable on any cloud deployment,
not just unreliable in testing. Whisper-only is slower per video but
doesn't depend on YouTube not blocking us, and Whisper's Telugu/Hindi
accuracy is solid - a reasonable fit since several of Click's trusted
channels post in Telugu.

Usage:
  python3 fetch_transcript.py <video_id>

Output (stdout, JSON only - all logging goes to stderr so it doesn't
pollute the JSON the Java side parses):
  {"source": "whisper", "transcript": "...", "videoId": "..."}
  {"error": "whisper_failed", "videoId": "...", "details": "..."}
"""
import sys
import os
import json
import tempfile
import contextlib


def log(message: str) -> None:
    """All diagnostic output goes to stderr - stdout is reserved for the final JSON result."""
    print(message, file=sys.stderr)


@contextlib.contextmanager
def suppress_stdout():
    """
    yt-dlp's 'quiet' option and Whisper's verbose setting don't reliably
    suppress 100% of their own output in every version/platform combo (this
    was confirmed during development - some yt-dlp postprocessor messages
    and Whisper's internal progress output leaked onto stdout even with
    quiet=True, breaking the "stdout is JSON-only" contract this script
    promises). Rather than chase every library's quietness flag, this
    redirects the actual stdout file descriptor to null for the duration
    of the risky calls, then restores it before we print our real JSON.
    """
    original_stdout_fd = os.dup(1)
    devnull_fd = os.open(os.devnull, os.O_WRONLY)
    try:
        os.dup2(devnull_fd, 1)
        yield
    finally:
        os.dup2(original_stdout_fd, 1)
        os.close(devnull_fd)
        os.close(original_stdout_fd)


def transcribe_with_whisper(video_id: str) -> str | None:
    """
    Downloads audio via yt-dlp, transcribes with Whisper.
    Whisper auto-detects language, so this works for English, Telugu,
    Hindi, or mixed-language videos without extra configuration.
    """
    try:
        import yt_dlp
        import whisper
    except ImportError as e:
        log(f"Required dependency not installed: {e}. Run: pip install -r requirements.txt --break-system-packages")
        return None

    with tempfile.TemporaryDirectory() as tmpdir:
        audio_path = os.path.join(tmpdir, f"{video_id}.mp3")

        ydl_opts = {
            "format": "bestaudio/best",
            "outtmpl": os.path.join(tmpdir, f"{video_id}.%(ext)s"),
            "postprocessors": [{
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "128",
            }],
            "quiet": True,
            "no_warnings": True,
            "noprogress": True,
        }

        try:
            with suppress_stdout():
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    ydl.download([f"https://www.youtube.com/watch?v={video_id}"])
        except Exception as e:
            log(f"yt-dlp audio download failed for {video_id}: {e}")
            return None

        if not os.path.exists(audio_path):
            log(f"Expected audio file not found after download: {audio_path}")
            return None

        # Trim intro/outro before sending to Whisper - Telugu tech reviews
        # typically have 60-90s intros and outros that are pure noise for
        # product extraction. Skipping them saves real transcription time.
        trimmed_path = os.path.join(tmpdir, f"{video_id}_trimmed.mp3")
        try:
            import subprocess
            duration_result = subprocess.run(
                ["ffprobe", "-v", "error", "-show_entries", "format=duration",
                 "-of", "default=noprint_wrappers=1:nokey=1", audio_path],
                capture_output=True, text=True
            )
            duration = float(duration_result.stdout.strip())
            start = int(duration * 0.10)    # skip first 10% (intro)
            length = int(duration * 0.80)   # take middle 80% (actual review)
            subprocess.run(
                ["ffmpeg", "-ss", str(start), "-t", str(length),
                 "-i", audio_path, "-y", trimmed_path],
                capture_output=True
            )
            if os.path.exists(trimmed_path):
                audio_to_transcribe = trimmed_path
                log(f"Trimmed audio to middle 80% ({length}s of {int(duration)}s total)")
            else:
                audio_to_transcribe = audio_path
                log("Trimming failed, using full audio")
        except Exception as e:
            log(f"Audio trimming failed ({e}), using full audio")
            audio_to_transcribe = audio_path

        try:
            # "small" model on CPU - better Telugu accuracy than "tiny" at the
            # cost of speed (~3-5 mins per video on laptop CPU). Acceptable for
            # a portfolio demo where you explain the architecture while it runs.
            log(f"Loading Whisper 'small' model on CPU and transcribing {video_id}...")
            with suppress_stdout():
                model = whisper.load_model("small")
                result = model.transcribe(audio_to_transcribe, verbose=None, fp16=False)
            return result["text"]
        except Exception as e:
            log(f"Whisper transcription failed for {video_id}: {e}")
            return None


def main():
    if len(sys.argv) != 2:
        print(json.dumps({"error": "missing_video_id", "details": "Usage: fetch_transcript.py <video_id>"}))
        sys.exit(1)

    video_id = sys.argv[1]

    transcript = transcribe_with_whisper(video_id)
    if transcript:
        print(json.dumps({"source": "whisper", "transcript": transcript, "videoId": video_id}))
        return

    print(json.dumps({
        "error": "whisper_failed",
        "videoId": video_id,
        "details": "Whisper transcription did not produce a result - check stderr for details"
    }))
    sys.exit(1)


if __name__ == "__main__":
    main()
