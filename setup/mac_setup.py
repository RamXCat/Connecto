import os
import subprocess
import sys

def get_agent_path():
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "main.py"))

def setup_mac():
    agent_path  = get_agent_path()
    python_path = sys.executable
    plist_dir   = os.path.expanduser("~/Library/LaunchAgents")
    plist_path  = os.path.join(plist_dir, "com.stitch.agent.plist")

    # Create LaunchAgents folder if not exists
    os.makedirs(plist_dir, exist_ok=True)

    # Create plist content
    plist_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
"http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.stitch.agent</string>

    <key>ProgramArguments</key>
    <array>
        <string>{python_path}</string>
        <string>{agent_path}</string>
    </array>

    <key>RunAtLoad</key>
    <true/>

    <key>KeepAlive</key>
    <true/>

    <key>StandardOutPath</key>
    <string>/tmp/stitch-agent.log</string>

    <key>StandardErrorPath</key>
    <string>/tmp/stitch-agent-error.log</string>
</dict>
</plist>"""

    with open(plist_path, 'w') as f:
        f.write(plist_content)

    # Activate the LaunchAgent
    subprocess.run(["launchctl", "load", plist_path])

    print(f"""
    ✅ Mac auto-start configured!
    ┌─────────────────────────────────┐
    │ LaunchAgent created at:         │
    │ {plist_path}
    │                                 │
    │ Logs available at:              │
    │ /tmp/stitch-agent.log           │
    │                                 │
    │ Stitch will now start           │
    │ automatically on every boot ✓   │
    └─────────────────────────────────┘
    """)

def remove_mac():
    plist_path = os.path.expanduser("~/Library/LaunchAgents/com.stitch.agent.plist")
    if os.path.exists(plist_path):
        subprocess.run(["launchctl", "unload", plist_path])
        os.remove(plist_path)
        print("✅ Auto-start removed successfully")
    else:
        print("⚠️ No auto-start found to remove")
