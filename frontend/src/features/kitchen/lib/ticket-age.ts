/** Minutes a ticket has been waiting, and the thresholds it ages through. */
export const WARN_AFTER_MIN = 10
export const URGENT_AFTER_MIN = 20

export function minutesWaiting(sentAt: string | undefined, now: number) {
  if (!sentAt) return 0
  return Math.max(0, Math.floor((now - new Date(sentAt).getTime()) / 60000))
}
