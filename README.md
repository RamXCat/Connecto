# 📱 Stitch Agent (Connecto Server)

![Python Version](https://img.shields.io/badge/python-3.8%2B-blue.svg)
![Flask](https://img.shields.io/badge/flask-3.0.0-green.svg)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

**Stitch Agent** is a lightweight, secure background service designed to pair your laptop/desktop with the **Connecto** mobile application. It listens for commands over your local network (LAN) to shut down, sleep, restart, or lock your machine directly from your phone. 

Featuring an innovative **UDP Auto-Broadcast system**, Stitch Agent continuously announces your machine's presence on the network, ensuring the mobile app automatically connects even if your laptop's IP address changes.

---

## ✨ Features

- **📡 UDP Auto-Discovery:** Silently broadcasts its IP address every 3 seconds so the mobile app can discover it seamlessly without manual IP entry.
- **🔒 Secure by Default:** Every endpoint is protected via an `Authorization: Bearer <token>` header configured by you.
- **🌙 Soft Sleep:** On Windows, the "sleep" command turns off the monitor display without suspending the OS, ensuring the server stays awake and reachable.
- **☀️ Programmatic Screen Wake:** Pinging the server status automatically simulates user activity to wake up your monitors.
- **💻 Cross-Platform Support:** Fully supports system control commands for Windows, macOS, and Linux.

---

## 📁 Project Structure

```text
stitch-agent/
├── main.py              # Entry point (Starts server & UDP broadcaster)
├── server.py            # HTTP Flask server (Action endpoints)
├── broadcaster.py       # UDP IP presence announcer
├── config.py            # Centralized settings & configurations
└── requirements.txt     # Python dependencies
```

---

## 🚀 Installation & Setup

1. **Clone the repository** (or download the files):
   ```bash
   git clone https://github.com/RamXCat/Connecto.git
   cd Connecto
   ```

2. **Configure your Token:**
   Open `config.py` and modify the `SECRET_TOKEN` to a secure passphrase of your choice. You will need to enter this same token in the Connecto app.
   ```python
   SECRET_TOKEN = "your-secure-passphrase-here"
   ```

3. **Install Dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

4. **Run the Agent:**
   ```bash
   python main.py
   ```
   *Note: On Windows, if prompted by Windows Defender Firewall, ensure you grant access for "Private Networks".*

---

## 🛠️ API Reference

All requests must include the following header:
`Authorization: Bearer <YOUR_SECRET_TOKEN>`

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/status` | `GET` | Verifies the server is online. Triggers monitor wake-up. |
| `/sleep` | `POST` | Turns off the system display (Windows) or triggers sleep. |
| `/shutdown`| `POST` | Initiates a system shutdown sequence. |
| `/restart` | `POST` | Initiates a system reboot. |
| `/lock` | `POST` | Locks the workstation screen. |

---

## ⚙️ Configuration (`config.py`)

You can customize the agent's behavior by editing the `config.py` file:

- `DEVICE_NAME`: The name that will appear in the Connecto app.
- `HTTP_PORT`: The port the Flask server listens on (default: `5000`).
- `BROADCAST_PORT`: The UDP port used for LAN discovery (default: `55555`).
- `BROADCAST_INTERVAL`: How often the agent broadcasts its presence (default: `3` seconds).

---

## 💻 Making it Auto-Start on Boot

### Windows
1. Create a `startup.bat` file containing:
   ```bat
   @echo off
   cd C:\Path\To\stitch-agent
   python main.py
   ```
2. Press `Win + R`, type `shell:startup`, and press Enter.
3. Place the `.bat` file (or a shortcut to it) in the startup folder.

### macOS
Create a `.plist` file in `~/Library/LaunchAgents/com.stitch.agent.plist`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>com.stitch.agent</string>
  <key>ProgramArguments</key>
  <array>
    <string>/usr/bin/python3</string>
    <string>/path/to/stitch-agent/main.py</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
</dict>
</plist>
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check out the [issues page](https://github.com/RamXCat/Connecto/issues).

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.
