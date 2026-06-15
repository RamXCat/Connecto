from flask import Flask, request, jsonify
from config import SECRET_TOKEN, DEVICE_NAME
import subprocess
import platform
import logging
import ctypes
import os

app = Flask(__name__)

# ── Auth check ──────────────────────────────
def is_authorized(req):
    token = req.headers.get('Authorization', '')
    return token == f"Bearer {SECRET_TOKEN}"

# ── Custom wake up logic carried over ────────
def wake_up_screen():
    """Simulates an invisible system movement event to wake up the screen display."""
    os_name = platform.system()
    try:
        if os_name == "Windows":
            # Simulate a 1-pixel horizontal mouse displacement and back
            # MOUSEEVENTF_MOVE = 0x0001
            ctypes.windll.user32.mouse_event(1, 1, 0, 0, 0)
            ctypes.windll.user32.mouse_event(1, -1, 0, 0, 0)
        elif os_name == "Darwin": # macOS
            # Tap a harmless key code (e.g. left arrow key 123) to wake the display
            subprocess.run(["osascript", "-e", 'tell application "System Events" to key code 123'], check=True)
    except Exception as e:
        print(f"Failed to trigger programmatic screen wake: {e}")

# ── Status endpoint ──────────────────────────
@app.route('/status', methods=['GET'])
def status():
    """
    Endpoint for the app to verify the server is running.
    Pinging this from Connecto automatically wakes up the screen!
    """
    wake_up_screen()
    return jsonify({
        "status": "online",
        "device": DEVICE_NAME,
        "platform": platform.system()
    })

# ── Shutdown endpoint ────────────────────────
@app.route('/shutdown', methods=['POST'])
def shutdown():
    if not is_authorized(request):
        return jsonify({"error": "Unauthorized"}), 401

    logging.warning("SHUTDOWN command received!")

    os_name = platform.system()
    if os_name == "Windows":
        subprocess.run(["shutdown", "/s", "/t", "5"])
    elif os_name in ["Linux", "Darwin"]:
        subprocess.run(["sudo", "shutdown", "-h", "now"])

    return jsonify({"success": True, "action": "shutdown"})

# ── Sleep endpoint ───────────────────────────
@app.route('/sleep', methods=['POST'])
def sleep():
    if not is_authorized(request):
        return jsonify({"error": "Unauthorized"}), 401

    os_name = platform.system()
    if os_name == "Windows":
        # SOFT SLEEP: Instantly turn off the screen monitor instead of suspending the full OS.
        # This keeps your Flask server online so you can wake it up later!
        # WM_SYSCOMMAND = 0x0112, SC_MONITORPOWER = 0xF170, Power Off = 2
        ctypes.windll.user32.SendMessageW(65535, 274, 61808, 2)
        
        # NOTE: If you ever want full system S3 standby instead (where the server goes down),
        # uncomment the line below:
        # subprocess.run(["rundll32.exe", "powrprof.dll,SetSuspendState", "0,1,0"], check=True)
    elif os_name == "Darwin":
        subprocess.run(["pmset", "sleepnow"])
    elif os_name == "Linux":
        subprocess.run(["systemctl", "suspend"])

    return jsonify({"success": True, "action": "sleep"})

# ── Restart endpoint ─────────────────────────
@app.route('/restart', methods=['POST'])
def restart():
    if not is_authorized(request):
        return jsonify({"error": "Unauthorized"}), 401

    os_name = platform.system()
    if os_name == "Windows":
        subprocess.run(["shutdown", "/r", "/t", "5"])
    elif os_name in ["Linux", "Darwin"]:
        subprocess.run(["sudo", "reboot"])

    return jsonify({"success": True, "action": "restart"})

# ── Lock endpoint ────────────────────────────
@app.route('/lock', methods=['POST'])
def lock():
    if not is_authorized(request):
        return jsonify({"error": "Unauthorized"}), 401

    os_name = platform.system()
    if os_name == "Windows":
        subprocess.run(["rundll32.exe", "user32.dll,LockWorkStation"])
    elif os_name == "Darwin":
        subprocess.run([
            "osascript", "-e",
            'tell application "System Events" to keystroke "q" '
            'using {command down, control down}'
        ])
    elif os_name == "Linux":
        subprocess.run(["loginctl", "lock-session"])

    return jsonify({"success": True, "action": "lock"})