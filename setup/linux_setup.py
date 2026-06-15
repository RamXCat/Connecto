import os
import sys
import subprocess
import getpass

def get_agent_path():
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "main.py"))

def setup_linux():
    agent_path   = get_agent_path()
    python_path  = sys.executable
    username     = getpass.getuser()
    service_path = "/etc/systemd/system/stitch.service"

    # Create systemd service content
    service_content = f"""[Unit]
Description=Stitch Laptop Agent
After=network.target

[Service]
ExecStart={python_path} {agent_path}
WorkingDirectory={os.path.dirname(agent_path)}
Restart=always
RestartSec=5
User={username}
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
"""

    # Write service file (needs sudo)
    with open("/tmp/stitch.service", 'w') as f:
        f.write(service_content)

    subprocess.run(["sudo", "mv", "/tmp/stitch.service", service_path])

    # Enable and start service
    subprocess.run(["sudo", "systemctl", "daemon-reload"])
    subprocess.run(["sudo", "systemctl", "enable", "stitch"])
    subprocess.run(["sudo", "systemctl", "start",  "stitch"])

    print(f"""
    ✅ Linux auto-start configured!
    ┌─────────────────────────────────┐
    │ Systemd service created ✓       │
    │                                 │
    │ Useful commands:                │
    │ > sudo systemctl status stitch  │
    │ > sudo systemctl stop stitch    │
    │ > sudo systemctl restart stitch │
    │ > journalctl -u stitch -f       │
    │                                 │
    │ Stitch will now start           │
    │ automatically on every boot ✓   │
    └─────────────────────────────────┘
    """)

def remove_linux():
    subprocess.run(["sudo", "systemctl", "stop",    "stitch"])
    subprocess.run(["sudo", "systemctl", "disable", "stitch"])
    subprocess.run(["sudo", "rm", "/etc/systemd/system/stitch.service"])
    subprocess.run(["sudo", "systemctl", "daemon-reload"])
    print("✅ Auto-start removed successfully")
