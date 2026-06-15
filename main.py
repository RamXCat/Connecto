import logging
import threading
from broadcaster import DeviceBroadcaster
from server import app
from config import HTTP_PORT, DEVICE_NAME, get_local_ip

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)

if __name__ == "__main__":

    # Print startup info
    print(f"""
    ╔══════════════════════════════════╗
    ║        STITCH AGENT v1.0         ║
    ╠══════════════════════════════════╣
    ║  Device : {DEVICE_NAME}
    ║  IP     : {get_local_ip()}
    ║  Port   : {HTTP_PORT}
    ║  Status : Running ✓
    ╚══════════════════════════════════╝
    """)

    # Start UDP broadcaster (IP announcer)
    broadcaster = DeviceBroadcaster()
    broadcaster.start()
    logging.info("UDP Broadcaster started")

    # Start HTTP server (command receiver)
    logging.info(f"HTTP Server started on port {HTTP_PORT}")
    app.run(host='0.0.0.0', port=HTTP_PORT)
