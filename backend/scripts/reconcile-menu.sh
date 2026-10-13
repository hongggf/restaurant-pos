#!/usr/bin/env bash
# Bring an already-seeded database in line with what seed.sh now produces.
#
# seed.sh cannot do this job: it creates categories and items unconditionally,
# so re-running it against a live database duplicates the whole menu. This
# script changes only what differs, and echoes existing ids back so the server
# updates rows in place instead of replacing them.
#
# It reconciles four things:
#   1. Flavor options            -> $0.01 each
#   2. DIY Malatang base price   -> $0.00
#   3. Noodles & Rice            -> gains Sichuan Pork Dumplings (5pcs) @ $1.50
#   4. "Choice of Adds-On"       -> detached from DIY Malatang, then deleted
#                                   (it held a duplicate Full Steamed Rice)
#
# Dry run by default; re-runnable, because it only writes what is still wrong.
#
#   BASE_URL=http://localhost:8080 ADMIN_PASS=… ./scripts/reconcile-menu.sh
#   BASE_URL=http://localhost:8080 ADMIN_PASS=… ./scripts/reconcile-menu.sh --apply
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"

FLAVOR_GROUP="Flavor"
NOODLE_GROUP="Noodles & Rice"
ADDON_GROUP="Choice of Adds-On"
DIY_ITEM="DIY Malatang"
DUMPLING="Sichuan Pork Dumplings (5pcs)"

FLAVOR_PRICE="0.01"
DIY_PRICE="0.00"
DUMPLING_PRICE="1.50"

APPLY=false
[[ "${1:-}" == "--apply" ]] && APPLY=true

say() { printf '%s\n' "$*" >&2; }
changed=0

# Built as two explicit curl calls rather than one with a "${args[@]}" array:
# under set -e the array form made the whole function return non-zero, which
# killed the script at the first assignment.
raw() { # raw <method> <path> [json] -> response body, whatever it is
    if [[ -n ${3:-} ]]; then
        curl -s -X "$1" "$BASE$2" -H "Authorization: Bearer $TOKEN" \
             -H "Content-Type: application/json" -d "$3"
    else
        curl -s -X "$1" "$BASE$2" -H "Authorization: Bearer $TOKEN"
    fi
}

api() { # api <method> <path> [json] -- fails the script on success=false
    local res
    res=$(raw "$1" "$2" "${3:-}")
    if [[ $(jq -r '.success // false' <<<"$res") != "true" ]]; then
        say "FAILED: $1 $2"
        jq . <<<"$res" >&2
        exit 1
    fi
    echo "$res"
}

group_by_name() { # group_by_name <nameEn> -> full ModifierGroupResponse, or empty
    local id
    id=$(jq -r --arg n "$1" 'first(.data.content[] | select(.nameEn == $n) | .id) // empty' <<<"$ALL_GROUPS")
    [[ -z $id ]] && return 0
    api GET "/api/v1/modifier-groups/$id" | jq '.data'
}

# BigDecimal 0.00 comes back as 0 once jq normalises it, so string comparison
# would report a change on every run. Compare numerically.
num_eq() { awk -v a="$1" -v b="$2" 'BEGIN { exit !(a + 0 == b + 0) }'; }

# --- login -------------------------------------------------------------------

TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" \
    -d "$(jq -n --arg u "$ADMIN_USER" --arg p "$ADMIN_PASS" '{username: $u, password: $p}')" \
    | jq -re '.data.accessToken')
say "== $BASE — logged in as $ADMIN_USER =="
$APPLY || say "   (dry run — pass --apply to write)"
say ""

ALL_GROUPS=$(api GET "/api/v1/modifier-groups?size=200")
ITEMS=$(api GET "/api/v1/menu-items?size=200")

# --- 1. flavour options ------------------------------------------------------

say "== $FLAVOR_GROUP =="
FLAVOR=$(group_by_name "$FLAVOR_GROUP")
if [[ -z $FLAVOR ]]; then
    say "  ! group not found — skipping"
else
    FLAVOR_ID=$(jq -re '.id' <<<"$FLAVOR")
    while read -r price name; do
        if num_eq "$price" "$FLAVOR_PRICE"; then say "  = $name already \$$price"
        else say "  → $name  \$$price → \$$FLAVOR_PRICE"; changed=$((changed + 1)); fi
    done < <(jq -r '.options[] | "\(.unitPrice) \(.nameEn)"' <<<"$FLAVOR")

    if $APPLY && [[ $(jq --argjson p "$FLAVOR_PRICE" \
            '[.options[] | select(.unitPrice != $p)] | length' <<<"$FLAVOR") -gt 0 ]]; then
        api PUT "/api/v1/modifier-groups/$FLAVOR_ID" "$(jq --argjson p "$FLAVOR_PRICE" '{
            nameEn, nameKm, minChoice, maxChoice, active,
            options: [.options[] | {id, nameEn, nameKm, imageUrl, packSize, available, sortOrder,
                                    unitPrice: $p}]
        }' <<<"$FLAVOR")" >/dev/null
        say "  ✓ updated"
    fi
fi
say ""

# --- 2. DIY base price -------------------------------------------------------

say "== $DIY_ITEM =="
ITEM=$(jq -r --arg n "$DIY_ITEM" 'first(.data.content[] | select(.nameEn == $n)) // empty' <<<"$ITEMS")
if [[ -z $ITEM ]]; then say "  ! \"$DIY_ITEM\" not found — aborting"; exit 1; fi
ITEM_ID=$(jq -re '.id' <<<"$ITEM")
CURRENT=$(jq -r '.price' <<<"$ITEM")
if num_eq "$CURRENT" "$DIY_PRICE"; then
    say "  = base already \$$CURRENT"
else
    say "  → base  \$$CURRENT → \$$DIY_PRICE"
    changed=$((changed + 1))
    if $APPLY; then
        api PUT "/api/v1/menu-items/$ITEM_ID" "$(jq --argjson p "$DIY_PRICE" '{
            nameEn, nameKm, descriptionEn, descriptionKm, currencyCode,
            imageUrl, available, categoryId, price: $p
        }' <<<"$ITEM")" >/dev/null
        say "  ✓ updated"
    fi
fi
say ""

# --- 3. dumplings into Noodles & Rice ----------------------------------------

say "== $NOODLE_GROUP =="
NOODLE=$(group_by_name "$NOODLE_GROUP")
if [[ -z $NOODLE ]]; then
    say "  ! group not found — skipping"
else
    NOODLE_ID=$(jq -re '.id' <<<"$NOODLE")
    if [[ $(jq --arg n "$DUMPLING" '[.options[] | select(.nameEn == $n)] | length' <<<"$NOODLE") -gt 0 ]]; then
        say "  = $DUMPLING already present"
    else
        say "  → add $DUMPLING @ \$$DUMPLING_PRICE"
        changed=$((changed + 1))
        if $APPLY; then
            api PUT "/api/v1/modifier-groups/$NOODLE_ID" "$(jq \
                --arg n "$DUMPLING" --argjson p "$DUMPLING_PRICE" '{
                nameEn, nameKm, minChoice, maxChoice, active,
                options: ([.options[] | {id, nameEn, nameKm, imageUrl, packSize, available,
                                         sortOrder, unitPrice}]
                          + [{id: null, nameEn: $n, nameKm: $n,
                              imageUrl: "/food-images/sichuan-dumplings.jpg",
                              packSize: null, available: true,
                              sortOrder: (.options | length), unitPrice: $p}])
            }' <<<"$NOODLE")" >/dev/null
            say "  ✓ added"
        fi
    fi
fi
say ""

# --- 4. retire Choice of Adds-On ---------------------------------------------

say "== $ADDON_GROUP =="
ADDON=$(group_by_name "$ADDON_GROUP")
if [[ -z $ADDON ]]; then
    say "  = already gone"
else
    ADDON_ID=$(jq -re '.id' <<<"$ADDON")
    ATTACHED=$(api GET "/api/v1/menu-items/$ITEM_ID/modifier-groups" \
        | jq --arg id "$ADDON_ID" '[.data[] | select(.group.id == $id)] | length')
    if [[ $ATTACHED -gt 0 ]]; then
        say "  → detach from $DIY_ITEM (holds a duplicate Full Steamed Rice)"
        changed=$((changed + 1))
        if $APPLY; then
            api DELETE "/api/v1/menu-items/$ITEM_ID/modifier-groups/$ADDON_ID" >/dev/null
            say "  ✓ detached"
        fi
    else
        say "  = not attached to $DIY_ITEM"
    fi
    if $APPLY; then
        res=$(raw DELETE "/api/v1/modifier-groups/$ADDON_ID")
        if [[ $(jq -r '.success // false' <<<"$res") == "true" ]]; then
            say "  ✓ group deleted"
        else
            say "  ! group kept: $(jq -r '.message // "unknown error"' <<<"$res")"
        fi
    else
        say "  → then delete the group itself"
    fi
fi

say ""
if [[ $changed -eq 0 ]]; then
    say "Nothing to do — the live menu already matches seed.sh."
elif $APPLY; then
    say "Done: $changed change(s) applied."
else
    say "$changed change(s) pending. Re-run with --apply to write."
fi
