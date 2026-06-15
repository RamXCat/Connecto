import socket
import json
import time
import threading
import logging
import platform
from config import (
    DEVICE_NAME,
    HTTP_PORT,
    BROADCAST_PORT,
    BROADCAST_INTERVAL,
    get_local_ip
)

class DeviceBroadcaster:
    def __init__(self):
        self.running = False
        self.thread  = None

    def _broadcast_loop(self):
        sock = socket.socket(
            socket.AF_INET,
            socket.SOCK_DGRAM
        )
        sock.setsockopt(
            socket.SOL_SOCKET,
            socket.SO_BROADCAST, 1
        )

        logging.info("Broadcaster started")

        while self.running:
            try:
                current_ip = get_local_ip()

                # Message sent to mobile app
                message = json.dumps({
                    "service":  "stitch",
                    "device":   DEVICE_NAME,
                    "ip":       current_ip,
                    "port":     HTTP_PORT,
                    "platform": platform.system(),  # Windows/Mac/Linux
                    "timestamp": time.time()
                })

                sock.sendto(
                    message.encode(),
                    ('<broadcast>', BROADCAST_PORT)
                )

                logging.debug(
                    f"Broadcast sent: {DEVICE_NAME} at {current_ip}:{HTTP_PORT}"
                )

            except Exception as e:
                logging.error(f"Broadcast error: {e}")

            time.sleep(BROADCAST_INTERVAL)

        sock.close()
        logging.info("Broadcaster stopped")

    def start(self):
        self.running = True
        self.thread  = threading.Thread(
            target=self._broadcast_loop,
            daemon=True
        )
        self.thread.start()

    def stop(self):
        self.running = False
