#!/usr/bin/env bash
set -e

CONTAINER_NAME="restaurant-pos-tunnel"
DOCKER_NETWORK="assignment_default"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
QR_SCRIPT="$SCRIPT_DIR/scripts/render-qr.py"

stop_tunnel() {
    echo "Stopping tunnel container '$CONTAINER_NAME'..."
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
    echo "Tunnel stopped."
}

status_tunnel() {
    if docker ps --format '{{.Names}}' | grep -Eq "^${CONTAINER_NAME}\$"; then
        echo "Tunnel is currently running."
        URL=$(docker logs "$CONTAINER_NAME" 2>&1 | grep -o "https://[a-zA-Z0-9.-]*\.trycloudflare\.com" | tail -n 1 || true)
        if [ -n "$URL" ]; then
            echo "URL: $URL"
            if [ -f "$QR_SCRIPT" ]; then
                python3 "$QR_SCRIPT" "$URL"
            fi
        fi
    else
        echo "Tunnel is not running."
    fi
}

case "$1" in
    stop)
        stop_tunnel
        exit 0
        ;;
    status)
        status_tunnel
        exit 0
        ;;
esac

echo "=========================================================="
echo " Starting Restaurant POS Tunnel"
echo "=========================================================="

# 1. Ensure core containers are running
if ! docker ps --format '{{.Names}}' | grep -Eq "^restaurant-pos-frontend\$"; then
    echo "[+] Starting Docker Compose application containers..."
    docker compose up -d
fi

# 2. Stop any existing tunnel container
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

# 3. Clean exit handler
cleanup() {
    echo ""
    echo "[!] Caught signal, shutting down tunnel..."
    docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
    exit 0
}
trap cleanup SIGINT SIGTERM

echo "[+] Connecting Quick Tunnel to frontend container..."
docker run -d --rm \
    --name "$CONTAINER_NAME" \
    --network "$DOCKER_NETWORK" \
    cloudflare/cloudflared:latest \
    tunnel --url http://restaurant-pos-frontend:80 --no-autoupdate >/dev/null

echo "[+] Waiting for Cloudflare to assign a public HTTPS URL..."
FOUND_URL=""
for i in {1..25}; do
    FOUND_URL=$(docker logs "$CONTAINER_NAME" 2>&1 | grep -o "https://[a-zA-Z0-9.-]*\.trycloudflare\.com" | tail -n 1 || true)
    if [ -n "$FOUND_URL" ]; then
        break
    fi
    sleep 1
done

if [ -z "$FOUND_URL" ]; then
    echo "[X] Error: Could not retrieve tunnel URL from Cloudflare within 25 seconds."
    echo "Check container logs with: docker logs $CONTAINER_NAME"
    exit 1
fi

echo ""
echo "=========================================================================="
echo " 🌐 CLOUDFLARE QUICK TUNNEL ACTIVE!"
echo "=========================================================================="
echo " Public URL:"
echo "   $FOUND_URL"
echo ""
echo " Scan this QR code with your phone camera (Works on ANY Wi-Fi or 4G/5G data):"
echo ""
if [ -f "$QR_SCRIPT" ]; then
    python3 "$QR_SCRIPT" "$FOUND_URL"
fi
echo ""
echo "=========================================================================="
echo " Keep this terminal open to keep the tunnel alive."
echo " Press [Ctrl+C] to stop, or run: ./run-tunnel.sh stop"
echo "=========================================================================="
echo ""

# Follow logs in foreground until user presses Ctrl+C
docker logs -f "$CONTAINER_NAME"
