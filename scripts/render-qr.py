#!/usr/bin/env python3
"""
Terminal QR code generator helper.
Supports plain URLs, text, or Wi-Fi configuration strings.
"""
import sys

def main():
    if len(sys.argv) < 2:
        print("Usage: render-qr.py <text_or_url>")
        sys.exit(1)

    data = sys.argv[1]

    try:
        import qrcode
        qr = qrcode.QRCode(border=1)
        qr.add_data(data)
        qr.print_ascii(invert=True)
    except ImportError:
        # Fallback to online curl if python qrcode is unavailable
        import urllib.parse
        import subprocess
        encoded = urllib.parse.quote(data, safe="")
        try:
            subprocess.run(["curl", "-s", "--connect-timeout", "3", f"https://qrenco.de/{encoded}"], check=True)
        except Exception:
            print(f"[!] Could not render QR code visually. Please open manually:\n{data}")

if __name__ == "__main__":
    main()
