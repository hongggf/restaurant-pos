/**
 * The URL a customer lands on when scanning a table's printed QR code.
 * The guest app reads `qr` and exchanges it for a guest session via
 * POST /guest/sessions. The qrToken is permanent per table unless an
 * admin explicitly regenerates it.
 */
export function guestQrUrl(qrToken: string): string {
  return `${window.location.origin}/guest?qr=${encodeURIComponent(qrToken)}`
}
