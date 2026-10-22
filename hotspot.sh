#!/usr/bin/env bash
set -e

HOTSPOT_CON_NAME="Restaurant-POS-Hotspot"
HOTSPOT_SSID="${HOTSPOT_SSID:-Restaurant-POS}"
HOTSPOT_PASSWORD="${HOTSPOT_PASSWORD:-restaurant123}"
STATIC_IP="10.42.0.1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
QR_SCRIPT="$SCRIPT_DIR/scripts/render-qr.py"

# Detect Wi-Fi interface (wlp2s0 or first wifi device)
WIFI_IFACE=$(nmcli -t -f DEVICE,TYPE device status | grep ':wifi$' | cut -d: -f1 | head -n 1)

if [ -z "$WIFI_IFACE" ]; then
    echo "[X] Error: No Wi-Fi device found on this laptop."
    exit 1
fi

status_hotspot() {
    if nmcli -t -f NAME,DEVICE connection show --active | grep -Eq "^${HOTSPOT_CON_NAME}:"; then
        echo "=========================================================="
        echo "  STATUS: Offline Wi-Fi Hotspot is ACTIVE"
        echo "=========================================================="
        echo " Interface: $WIFI_IFACE"
        echo " SSID:      $HOTSPOT_SSID"
        echo " Password:  $HOTSPOT_PASSWORD"
        echo " URL:       http://${STATIC_IP}"
        echo "=========================================================="
    else
        echo "Offline Wi-Fi Hotspot is currently INACTIVE (normal Wi-Fi mode)."
    fi
}

stop_hotspot() {
    echo "[+] Turning off Wi-Fi Hotspot '$HOTSPOT_CON_NAME'..."
    nmcli connection down "$HOTSPOT_CON_NAME" >/dev/null 2>&1 || true
    echo "[✓] Hotspot turned off. Wi-Fi adapter restored to normal client mode."
}

start_hotspot() {
    echo "=========================================================="
    echo " Starting Offline Restaurant Wi-Fi Hotspot"
    echo "=========================================================="
    echo "[+] Wi-Fi Device: $WIFI_IFACE"
    echo "[+] SSID:         $HOTSPOT_SSID"
    echo "[+] Password:     $HOTSPOT_PASSWORD"

    # Ensure application containers are up
    if ! docker ps --format '{{.Names}}' | grep -Eq "^restaurant-pos-frontend\$"; then
        echo "[+] Starting Docker containers..."
        docker compose up -d
    fi

    # Check if connection profile already exists
    if nmcli -t -f NAME connection show | grep -Eq "^${HOTSPOT_CON_NAME}\$"; then
        echo "[+] Activating existing Hotspot profile..."
        nmcli connection up "$HOTSPOT_CON_NAME"
    else
        echo "[+] Creating new Hotspot connection profile..."
        nmcli device wifi hotspot \
            ifname "$WIFI_IFACE" \
            con-name "$HOTSPOT_CON_NAME" \
            ssid "$HOTSPOT_SSID" \
            password "$HOTSPOT_PASSWORD"
    fi

    sleep 1

    echo ""
    echo "=========================================================================="
    echo " 📶 OFFLINE WI-FI HOTSPOT IS RUNNING!"
    echo "=========================================================================="
    echo " 1. CONNECT PHONE TO THIS WI-FI:"
    echo "    SSID:     $HOTSPOT_SSID"
    echo "    Password: $HOTSPOT_PASSWORD"
    echo ""
    echo "    (Scan with phone camera to auto-join Wi-Fi):"
    if [ -f "$QR_SCRIPT" ]; then
        python3 "$QR_SCRIPT" "WIFI:T:WPA;S:${HOTSPOT_SSID};P:${HOTSPOT_PASSWORD};;"
    fi
    echo ""
    echo "--------------------------------------------------------------------------"
    echo " 2. OPEN THE RESTAURANT POS ON YOUR PHONE:"
    echo "    URL: http://${STATIC_IP}"
    echo ""
    echo "    (Scan to open website):"
    if [ -f "$QR_SCRIPT" ]; then
        python3 "$QR_SCRIPT" "http://${STATIC_IP}"
    fi
    echo "=========================================================================="
    echo " To turn off the hotspot and restore normal Wi-Fi, run:"
    echo "   ./hotspot.sh stop"
    echo "=========================================================================="
}

case "$1" in
    stop)
        stop_hotspot
        ;;
    status)
        status_hotspot
        ;;
    start|"")
        start_hotspot
        ;;
    *)
        echo "Usage: $0 [start | stop | status]"
        exit 1
        ;;
esac
