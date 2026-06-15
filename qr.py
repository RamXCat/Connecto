import qrcode
import json
import base64
from io import BytesIO
from config import (
    DEVICE_NAME,
    HTTP_PORT,
    SECRET_TOKEN,
    get_local_ip
)

def generate_qr_payload():
    # Everything mobile app needs to connect
    payload = json.dumps({
        "service": "stitch",
        "device":  DEVICE_NAME,
        "ip":      get_local_ip(),
        "port":    HTTP_PORT,
        "token":   SECRET_TOKEN
    })
    return payload

def generate_qr_image():
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4
    )
    qr.add_data(generate_qr_payload())
    qr.make(fit=True)
    img = qr.make_image(
        fill_color="white",
        back_color="black"
    )
    return img

def print_qr_terminal():
    # Shows QR in terminal on agent start
    qr = qrcode.QRCode()
    qr.add_data(generate_qr_payload())
    qr.make(fit=True)
    print("\n  📱 Scan this QR code in Stitch app:\n")
    # invert=True is usually better for dark terminals
    qr.print_ascii(invert=True)
    print(f"\n  Or connect manually: {get_local_ip()}:{HTTP_PORT}\n")

def get_qr_base64():
    # Returns QR as base64 image string
    # For serving via HTTP endpoint
    img    = generate_qr_image()
    buffer = BytesIO()
    img.save(buffer, format="PNG")
    encoded = base64.b64encode(
        buffer.getvalue()
    ).decode('utf-8')
    return f"data:image/png;base64,{encoded}"
