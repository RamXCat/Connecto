# Connecto Laptop Agent Guide 🚀

If you ran a basic script to sleep your laptop, you likely noticed that the laptop screen turns off but **fails to wake up** physically (or programmatically) when desired. 

This guide explains **why this happens**, how to fix it, provides complete working scripts for **Node.js** and **Python**, and includes the exact **Antigravity Prompt** you can use to have your laptop agent configured and run automatically!

---

## 🔍 The Root Cause: Why Sleep stays stuck on a Black Screen

When a sleep command is triggered remotely, one of two things happens depending on how the agent script put the laptop to sleep:

### 1. True System Standby (S3/Modern Standby)
If the laptop went into a deep low-power sleep state (Power State S3 or Standby):
- **Network Disconnection:** The laptop's CPU and Wi-Fi/Ethernet card went into a sleep state.
- **Connection Lost:** The agent server software running on your laptop was suspended, so it **could no longer hear** incoming HTTP status requests from your phone.
- **Waking Up:** Waking up from true sleep programmatically requires **Wake-on-LAN (WoL)** magic packets sent directly to your network interface's hardware MAC address.

### 2. Standard Monitor Power-Off (SC_MONITORPOWER SendMessage)
If the script ran a command like Windows `SendMessage` with parameter `2` (Off) to turn off the screen, but left the PC running:
- **Locked Power State:** Windows locks the monitor in a power-saving mode. On some hardware drivers, standard keyboard or mouse input is discarded in this state, meaning physically shifting the mouse or tapping keys won't light up the screen.
- **Programmatic Wake Needed:** To revive the display, you must run a script command that releases the block or simulates a high-level system wake event (like pressing a harmless system key like **Shift** or slightly moving the mouse position).

---

## 🛠️ The Perfect laptop Agent Configuration

To solve this, the agent should:
1. **Turn off the screen cleanly** (for a "soft sleep" where the server stays online and you can check status).
2. **Handle physical wake-up interaction** by ensuring standard input wakes it up, or offering a programmatical wake-up (e.g. if you open the app or ping the status, it wakes the display up!).

Here are the complete, robust scripts for Windows. Note that these scripts use safe authorization token checks to secure your laptop.

### Choice A: Python Agent (Recommended)
This uses **FastAPI** (an ultra-fast Python web server) and **pyautogui** / **ctypes** to safely toggle screen power and handle simulated key presses to guarantee instantaneous wake.

```python
# Save this file as agent.py
# Install requirements: pip install fastapi uvicorn pyautogui screen-brightness-control
import os
import ctypes
import time
import pyautogui
import uvicorn
from fastapi import FastAPI, Header, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Connecto Laptop Agent")

# Enable CORS so your local network devices can connect
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 🔐 SECURITY TOKEN (Change this to match your Connecto App Configuration Token!)
AUTH_TOKEN = "your_secure_token_here"

def verify_token(authorization: str):
    if not authorization:
        raise HTTPException(status_code=401, detail="Unauthorized: No Token")
    
    # Handle both "Bearer <token>" and raw token styles
    token = authorization.split(" ")[1] if " " in authorization else authorization
    if token != AUTH_TOKEN:
        raise HTTPException(status_code=401, detail="Unauthorized: Invalid Token")

@app.get("/status")
def get_status(authorization: str = Header(None)):
    verify_token(authorization)
    # WAKE UP DISPLAY: If we ping status from the phone app, let's proactively wake the monitor up!
    try:
        # Press SHIFT twice to wake up monitor state safely
        pyautogui.press('shift')
        # Simulate a 1-pixel mouse move and back to trigger display power on
        pyautogui.moveRel(1, 0, duration=0.1)
        pyautogui.moveRel(-1, 0, duration=0.1)
    except Exception:
        pass
    return {"status": "online", "message": "Connecto Laptop Agent is listening."}

@app.post("/lock")
def lock_device(authorization: str = Header(None)):
    verify_token(authorization)
    # Windows Lock: Rundll32 user32.dll,LockWorkStation
    os.system("rundll32.exe user32.dll,LockWorkStation")
    return {"success": True, "message": "Device session locked"}

@app.post("/sleep")
def sleep_device(authorization: str = Header(None)):
    verify_token(authorization)
    # Safely Turn Off DISPLAY (Instead of Standby, so server stays online to receive wake commands!)
    # WM_SYSCOMMAND = 0x0112, SC_MONITORPOWER = 0xF170, Parameter = 2 (Power Off)
    ctypes.windll.user32.SendMessageW(65535, 274, 61808, 2)
    return {"success": True, "message": "Display went to sleep. Move mouse or tap Shift physically to wake it!"}

@app.post("/restart")
def restart_device(authorization: str = Header(None)):
    verify_token(authorization)
    os.system("shutdown /r /t 5")
    return {"success": True, "message": "Reboot initiated in 5 seconds"}

@app.post("/shutdown")
def shutdown_device(authorization: str = Header(None)):
    verify_token(authorization)
    os.system("shutdown /s /t 5")
    return {"success": True, "message": "Shutdown initiated in 5 seconds"}

if __name__ == "__main__":
    print(f"Starting Connecto Agent... Your token is: {AUTH_TOKEN}")
    uvicorn.run(app, host="0.0.0.0", port=5000)
```

---

### Choice B: Node.js Agent (Express)
This uses **Express** and `exec` commands to cleanly perform the same behavior using Windows API or PowerShell.

```javascript
// Save this file as agent.js
// Run: npm init -y && npm install express
const express = require('express');
const { exec } = require('child_process');
const app = express();

const PORT = 5000;
const AUTH_TOKEN = "your_secure_token_here"; // Match with your Connecto App!

// Token verification middleware
const checkAuth = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    return res.status(401).json({ error: 'Unauthorized: No token provided' });
  }
  const token = authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : authHeader;
  if (token !== AUTH_TOKEN) {
    return res.status(401).json({ error: 'Unauthorized: Invalid token' });
  }
  next();
};

app.use(checkAuth);

// Status / Wake Screen on Ping
app.get('/status', (req, res) => {
  // Simulate active keyboard input to lift screen-saver/black lock screen immediately on request
  const wakePowerShell = `powershell -Command "$wsh = New-Object -ComObject Wscript.Shell; $wsh.SendKeys('{SHIFT}')"`;
  exec(wakePowerShell);
  res.json({ status: "online", message: "Connecto Agent is running and awake!" });
});

// Sleep screen
app.post('/sleep', (req, res) => {
  // Turn off monitor using system message
  const sleepPowerShell = `powershell (Add-Type '[DllImport(\\"user32.dll\\")]public class W{[DllImport(\\"user32.dll\\")]public static extern int SendMessage(int h,int m,int w,int l);}' -Name W -Passthru)::SendMessage(-1,0x0112,0xF170,2)`;
  exec(sleepPowerShell, (err) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json({ success: true, message: "Sleep command executed. Tap physical keyboard Shift key to wake up." });
  });
});

// Screenlock
app.post('/lock', (req, res) => {
  exec('rundll32.exe user32.dll,LockWorkStation', (err) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json({ success: true });
  });
});

// Reboot
app.post('/restart', (req, res) => {
  exec('shutdown /r /t 5');
  res.json({ success: true });
});

// Shutdown
app.post('/shutdown', (req, res) => {
  exec('shutdown /s /t 5');
  res.json({ success: true });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Connecto Agent app listening at http://0.0.0.0:${PORT}`);
});
```

---

## 🤖 Antigravity Prompt (Copy-Paste to generate custom script!)

Copy and paste the exact prompt below into your **Antigravity** developer chat to generate the agent on your laptop.

```text
Help me write a lightweight local web server agent for Windows that works perfectly with my remote app "Connecto". 
Connecto controls laptop actions over a local network.

The agent must:
1. Listen on port 5000 and bind to 0.0.0.0 (supporting local network connections).
2. Authenticate requests using a Bearer token. Validate headers against a secret token (default: "your_secure_token_here"). Return 401 on discrepancy.
3. Support the following endpoints:
   - GET `/status` : Returns {"status": "online"}. Crucially, whenever /status is called, it should simulate a safe keypress (like 'Shift' key) or tiny mouse movement so that if the laptop monitor was sleep/blacked out, it instantly wakes back up.
   - POST `/lock` : Lock device session instantly.
   - POST `/sleep` : Safely sleeps the screen monitor (not full system S3 hibernation to prevent server cutoff). Run: powershell (Add-Type '[DllImport(\"user32.dll\")]public class W{[DllImport(\"user32.dll\")]public static extern int SendMessage(int h,int m,int w,int l);}' -Name W -Passthru)::SendMessage(-1,0x0112,0xF170,2). This keeps the HTTP server alive while keeping the display off!
   - POST `/restart` : Initiate system reboot in 5 seconds (`shutdown /r /t 5`).
   - POST `/shutdown` : Power off the device in 5 seconds (`shutdown /s /t 5`).

Please generate the agent in Python using FastAPI/Uvicorn, and include instructions on how to install requirements and run it. Provide clear notes on how I can wake the screen back up physically (using mouse/Shift key) or programmatically via the phone app connecting.
```

---
*Created automatically for Connecto v1.0.0.*
