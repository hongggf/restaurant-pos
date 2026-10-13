#!/usr/bin/env bash
# Seed the Restaurant POS API with Malatang POS test data.
# Usage: ./scripts/seed.sh  (app must be running on localhost:8080)
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@123}"

# --- helpers -----------------------------------------------------------------

post() { # post <path> <json> -> response body (fails the script on success=false)
    local res
    res=$(curl -s -X POST "$BASE$1" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$2")
    if [[ $(jq -r '.success' <<<"$res") != "true" ]]; then
        echo "FAILED: POST $1" >&2
        jq . <<<"$res" >&2
        exit 1
    fi
    echo "$res"
}

id_of() { jq -r '.data.id' <<<"$1"; }

# options <price> <name>... -> JSON array of ModifierOptionRequest (nameKm = nameEn)
options() {
    local price=$1; shift
    printf '%s\n' "$@" | jq -R . | jq -s --arg p "$price" \
        '[to_entries[] | {
            nameEn: .value,
            nameKm: .value,
            unitPrice: ($p|tonumber),
            imageUrl: (
              if (.value | test("Beef Ball"; "i")) then "/food-images/juicy-beef-balls.jpg"
              elif (.value | test("Fish Roe|Roe"; "i")) then "/food-images/fish-roe-meatballs.jpg"
              elif (.value | test("Fish Ball"; "i")) then "/food-images/mini-fish-balls.jpg"
              elif (.value | test("Tender Chicken"; "i")) then "/food-images/tender-chicken.jpg"
              elif (.value | test("USA Beef"; "i")) then "/food-images/usa-beef.jpg"
              elif (.value | test("Prawn Dumplings"; "i")) then "/food-images/prawn-dumplings.jpg"
              elif (.value | test("Mini Dumplings"; "i")) then "/food-images/mini-dumplings.jpg"
              elif (.value | test("Dumplings"; "i")) then "/food-images/sichuan-dumplings.jpg"
              elif (.value | test("Half Steamed Rice"; "i")) then "/food-images/half-steamed-rice.jpg"
              elif (.value | test("Rice Noodles"; "i")) then "/food-images/rice-noodles.jpg"
              elif (.value | test("Rice"; "i")) then "/food-images/full-steamed-rice.jpg"
              elif (.value | test("Pineapple"; "i")) then "/food-images/pineapple-tea.jpg"
              elif (.value | test("Apple"; "i")) then "/food-images/apple-tea.jpg"
              elif (.value | test("Honey Lemon"; "i")) then "/food-images/honey-lemon.jpg"
              elif (.value | test("Deep Fried Tofu|Fried Tofu"; "i")) then "/food-images/fried-tofu.jpg"
              elif (.value | test("Soft Tofu"; "i")) then "/food-images/soft-tofu.jpg"
              elif (.value | test("Lotus Roots"; "i")) then "/food-images/lotus-roots.jpg"
              elif (.value | test("Crab Mushroom"; "i")) then "/food-images/crab-mushroom.jpg"
              elif (.value | test("Needle Mushroom"; "i")) then "/food-images/needle-mushroom.jpg"
              elif (.value | test("Broccoli"; "i")) then "/food-images/broccoli.jpg"
              elif (.value | test("Crab Stick"; "i")) then "/food-images/crab-stick.jpg"
              elif (.value | test("Bacon"; "i")) then "/food-images/bacon.jpg"
              elif (.value | test("Pork Flower Sausage"; "i")) then "/food-images/flower-sausage.jpg"
              elif (.value | test("Handmade Noodles"; "i")) then "/food-images/handmade-noodles.jpg"
              elif (.value | test("White Fungus"; "i")) then "/food-images/white-fungus.jpg"
              elif (.value | test("Black Fungus"; "i")) then "/food-images/black-fungus.jpg"
              elif (.value | test("Fish Roll"; "i")) then "/food-images/fish-roll-meatball.jpg"
              elif (.value | test("Fish Cake"; "i")) then "/food-images/fish-cake.jpg"
              elif (.value | test("White Stomach"; "i")) then "/food-images/white-stomach.jpg"
              elif (.value | test("Duck Blood"; "i")) then "/food-images/duck-blood.jpg"
              elif (.value | test("Bamboo Shoot"; "i")) then "/food-images/bamboo-shoot.jpg"
              elif (.value | test("Black Chicken"; "i")) then "/food-images/black-chicken.jpg"
              elif (.value | test("Potato Noodles"; "i")) then "/food-images/potato-noodles.jpg"
              elif (.value | test("Dried Tofu Strips"; "i")) then "/food-images/dried-tofu-strips.jpg"
              elif (.value | test("Mee Chiet Noodles"; "i")) then "/food-images/mee-chiet-noodles.jpg"
              elif (.value | test("Tang-O|Chrysanthemum"; "i")) then "/food-images/chrysanthemum-greens.jpg"
              elif (.value | test("Romaine Lettuce"; "i")) then "/food-images/romaine-lettuce.jpg"
              else null end
            ),
            available: true,
            sortOrder: (.key + 1)
        }]'
}

category() { # category <nameEn> <sortOrder> [description] [allowNotes]
    local allow_notes="${4:-true}"
    local res
    res=$(post /api/v1/categories "$(jq -n --arg n "$1" --argjson s "$2" --arg d "${3:-}" --argjson an "$allow_notes" \
        '{nameEn: $n, nameKm: $n, description: $d, sortOrder: $s, active: true, allowNotes: $an}')")
    echo "  ✓ category  $1" >&2
    id_of "$res"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

menu_item() { # menu_item <nameEn> <price> <categoryId> [descriptionEn] [imageFile] [station]
    local img_url=""
    if [[ $# -ge 5 && -n "$5" ]]; then
        img_url="/food-images/$5"
    fi
    local station="${6:-KITCHEN}"
    local res
    res=$(post /api/v1/menu-items "$(jq -n --arg n "$1" --arg p "$2" --arg c "$3" --arg d "${4:-}" --arg img "$img_url" --arg st "$station" \
        '{nameEn: $n, nameKm: $n, descriptionEn: $d, descriptionKm: $d, price: ($p|tonumber),
          currencyCode: "USD", imageUrl: (if $img == "" then null else $img end), available: true, categoryId: $c, station: $st}')")
    echo "  ✓ item      $1 (\$$2) [$station]" >&2
    local item_id
    item_id=$(id_of "$res")
    if [[ $# -ge 5 && -n "$5" ]]; then
        local img_path="$SCRIPT_DIR/../uploads/seed-images/$5"
        if [[ -f "$img_path" ]]; then
            curl -s -X POST "$BASE/api/v1/menu-items/$item_id/image" \
                -H "Authorization: Bearer $TOKEN" \
                -F "file=@$img_path" >/dev/null
            echo "    ↳ image   uploaded ($5)" >&2
        fi
    fi
    echo "$item_id"
}

modifier_group() { # modifier_group <nameEn> <minChoice> <maxChoice> <optionsJson>
    local res
    res=$(post /api/v1/modifier-groups "$(jq -n --arg n "$1" --argjson min "$2" --argjson max "$3" --argjson o "$4" \
        '{nameEn: $n, nameKm: $n, minChoice: $min, maxChoice: $max, active: true, options: $o}')")
    echo "  ✓ group     $1 ($(jq 'length' <<<"$4") options)" >&2
    id_of "$res"
}

# --- 0. login ----------------------------------------------------------------

echo "== Login =="
TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" \
    -d "$(jq -n --arg u "$ADMIN_USER" --arg p "$ADMIN_PASS" '{username: $u, password: $p}')" \
    | jq -re '.data.accessToken')
echo "  ✓ logged in as $ADMIN_USER"

# --- 1. categories -----------------------------------------------------------

echo "== Categories =="
CAT_DIY=$(category "DIY Malatang" 1 "Build your own Malatang")
CAT_SIDE=$(category "Side Dishes" 2)
CAT_DRINK=$(category "Special Love Drink" 3)
CAT_SOFT=$(category "I Love Soft Drinks" 4 "" false)
CAT_CANDY=$(category "I Love Candy" 5 "" false)
CAT_COMBO=$(category "Combo Set" 6)

# --- 2. modifier groups ------------------------------------------------------

echo "== Modifier groups =="
# The bowl itself is $0.00 and the flavour carries the nominal cent, so a cart
# line adds up from its options alone with no base-price row to explain.
FLAVOR_OPTS='[
  {"nameEn":"Dry Malatang","nameKm":"Dry","unitPrice":0.01,"imageUrl":"/food-images/dry-malatang.jpg","available":true,"sortOrder":1},
  {"nameEn":"Sichuan Spicy Soup","nameKm":"Soup","unitPrice":0.01,"imageUrl":"/food-images/sichuan-spicy-soup.jpg","available":true,"sortOrder":2},
  {"nameEn":"Milky Spicy Soup","nameKm":"Milk Soup","unitPrice":0.01,"imageUrl":"/food-images/milky-spicy-soup.jpg","available":true,"sortOrder":3},
  {"nameEn":"Chicken Broth Soup","nameKm":"Chicken","unitPrice":0.01,"imageUrl":"/food-images/chicken-broth-soup.jpg","available":true,"sortOrder":4},
  {"nameEn":"Mushroom Soup","nameKm":"Mushroom","unitPrice":0.01,"imageUrl":"/food-images/mushroom-soup.jpg","available":true,"sortOrder":5}
]'
GRP_FLAVOR=$(modifier_group "Flavor" 1 1 "$FLAVOR_OPTS")

GRP_MEAT=$(modifier_group "Meat" 0 10 "$(options 0.90 \
    "Tender Chicken" "USA Beef" "Black Chicken" "White Stomach")")

GRP_MEATBALL=$(modifier_group "Meat Ball" 0 15 "$(options 0.30 \
    "Juicy Beef Ball (2pcs)" "Mini Juicy Fish Ball" "Fish Roe Meatball" \
    "Crab Stick" "Duck Blood" "Potato Noodles" \
    "Bacon (4pcs)" "Signature Dumplings" \
    "Pork Flower Sausage" "Fish Cake" "Fish Roll Meatball")")

GRP_VEGGIE=$(modifier_group "Veggie" 0 10 "$(options 0.30 \
    "Broccoli" "White Fungus" "Black Fungus" "Soft Tofu" "Bamboo Shoot" \
    "Deep Fried Tofu" "Dried Tofu Strips" "Needle Mushroom" "Crab Mushroom" \
    "Lotus Roots" "Tang-O" "Romaine Lettuce")")

# Dumplings ride along in this group but are priced per portion, not per noodle.
GRP_NOODLE=$(modifier_group "Noodles & Rice" 0 5 "$(options 0.70 \
    "Mee Chiet Noodles" "Handmade Noodles" "Full Steamed Rice" "Rice Noodles" \
    "Sichuan Pork Dumplings (5pcs)" \
    | jq 'map(if .nameEn | test("Sichuan Pork Dumplings") then .unitPrice = 1.50 else . end)')")

GRP_EXTRA=$(modifier_group "Extra Love Add-Ons" 0 3 "$(options 1.58 \
    "Red Apple Jasmine Tea" "Pineapple Lemon Jasmine Tea" "Honey Lemon Kiss")")

# --- 3. menu items -----------------------------------------------------------

echo "== Menu items =="
# Free base: a build is priced entirely by the options stacked on it.
ITEM_DIY=$(menu_item "DIY Malatang" 0.00 "$CAT_DIY" "Build your own Malatang" "diy-malatang.jpg" "KITCHEN")
menu_item "Malatang World Football Set" 8.99 "$CAT_COMBO" "2 Signature Malatang + Dumplings + Drink" "combo-set.jpg" "KITCHEN" >/dev/null
menu_item "Half Steamed Rice" 0.35 "$CAT_SIDE" "" "steamed-rice.jpg" "KITCHEN" >/dev/null
menu_item "Full Steamed Rice" 0.70 "$CAT_SIDE" "" "steamed-rice.jpg" "KITCHEN" >/dev/null
menu_item "Sichuan Pork Dumplings" 1.50 "$CAT_SIDE" "" "sichuan-dumplings.jpg" "KITCHEN" >/dev/null
menu_item "Prawn Dumplings" 1.50 "$CAT_SIDE" "" "prawn-dumplings.jpg" "KITCHEN" >/dev/null
menu_item "Mini Dumplings" 1.50 "$CAT_SIDE" "" "mini-dumplings.jpg" "KITCHEN" >/dev/null
menu_item "Pineapple Lemon Jasmine Tea" 1.98 "$CAT_DRINK" "" "pineapple-tea.jpg" "COUNTER" >/dev/null
menu_item "Red Apple Jasmine Tea" 1.98 "$CAT_DRINK" "" "apple-tea.jpg" "COUNTER" >/dev/null
menu_item "Honey Lemon Kiss" 1.98 "$CAT_DRINK" "" "honey-lemon.jpg" "COUNTER" >/dev/null
menu_item "Cambodia Water" 1.00 "$CAT_SOFT" "" "cambodia-water.jpg" "COUNTER" >/dev/null
menu_item "Cambodia Cola" 1.00 "$CAT_SOFT" "" "cambodia-cola.jpg" "COUNTER" >/dev/null
menu_item "Coca-Cola Classic" 1.00 "$CAT_SOFT" "" "coca-cola.jpg" "COUNTER" >/dev/null
menu_item "Jia Duo Bao" 1.20 "$CAT_SOFT" "" "herbal-tea.jpg" "COUNTER" >/dev/null
menu_item "Big Heart Lollipop" 1.00 "$CAT_CANDY" "" "lollipop.jpg" "COUNTER" >/dev/null

# --- 4. attach modifier groups to DIY Malatang -------------------------------

echo "== Attach modifier groups to DIY Malatang =="
i=0
for grp in "$GRP_FLAVOR" "$GRP_MEAT" "$GRP_MEATBALL" "$GRP_VEGGIE" "$GRP_NOODLE" "$GRP_EXTRA"; do
    post "/api/v1/menu-items/$ITEM_DIY/modifier-groups" \
        "$(jq -n --arg g "$grp" --argjson s "$i" '{modifierGroupId: $g, sortOrder: $s}')" >/dev/null
    echo "  ✓ attached  sortOrder=$i groupId=$grp"
    i=$((i + 1))
done

# --- 5. tables ----------------------------------------------------------------

echo "== Tables =="
for t in T-01 T-02 T-03; do
    post /api/v1/tables "$(jq -n --arg n "$t" '{tableNumber: $n}')" >/dev/null
    echo "  ✓ table     $t"
done

echo
echo "Seed complete."
