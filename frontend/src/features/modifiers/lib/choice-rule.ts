/** Human-readable summary of a group's min/max choice rule. */
export function choiceRule(minChoice?: number, maxChoice?: number) {
  const min = minChoice ?? 0
  if (maxChoice == null) {
    return min > 0 ? `Pick at least ${min}` : "Optional"
  }
  if (min === maxChoice) return `Pick ${min}`
  if (min === 0) return `Up to ${maxChoice}`
  return `Pick ${min}–${maxChoice}`
}
