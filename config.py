import socket

# All settings in one place
DEVICE_NAME   = "My Laptop"      # Name shown in mobile app
HTTP_PORT     = 5000             # Port for shutdown commands
BROADCAST_PORT = 55555           # Port for UDP announcements
BROADCAST_INTERVAL = 3           # Seconds between broadcasts

# I have carried over your custom token from the previous file!
SECRET_TOKEN  = "cxtzilla@123"

# Auto-detect local IP
def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip
