#!/usr/bin/env python3
"""Generate the served food-image derivatives from the 1024px originals.

The originals in `food-images-src/` are ~800KB each and were being served
straight into 32px thumbnails. This writes three progressive JPEG variants
into `public/food-images/`, sized for how the app actually renders them:

    public/food-images/<name>.jpg        800px  dialog hero (aspect-[16/7])
    public/food-images/card/<name>.jpg   512px  menu grid cards (aspect-[4/3])
    public/food-images/thumb/<name>.jpg   96px  modifier option rows (size-8)

The bare path keeps working at hero size on purpose: menu_items.image_url and
modifier_options.image_url hold `/food-images/<name>.jpg` (see migrations
V11-V13), so anything that renders those paths without asking for a size gets
a correctly-sized image rather than a 404.

Images stay square and uncropped — every call site uses object-cover, so
cropping is the layout's job, not ours.

Run after adding or regenerating an original:

    npm run images

Output is deterministic, so re-running without source changes leaves the
working tree clean.
"""

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "food-images-src"
OUT = ROOT / "public" / "food-images"

# subdirectory (empty = served at the bare path), edge length, JPEG quality
VARIANTS = [
    ("", 800, 82),
    ("card", 512, 82),
    ("thumb", 96, 80),
]


def main() -> int:
    originals = sorted(SRC.glob("*.jpg"))
    if not originals:
        print(f"no source images found in {SRC}")
        return 1

    for subdir, _, _ in VARIANTS:
        (OUT / subdir).mkdir(parents=True, exist_ok=True)

    total_src = total_out = 0
    for path in originals:
        total_src += path.stat().st_size
        with Image.open(path) as im:
            im = im.convert("RGB")
            for subdir, edge, quality in VARIANTS:
                target = OUT / subdir / path.name
                resized = im.resize((edge, edge), Image.LANCZOS)
                resized.save(
                    target,
                    "JPEG",
                    quality=quality,
                    progressive=True,
                    optimize=True,
                    subsampling="4:2:0",
                )
                total_out += target.stat().st_size

    print(
        f"{len(originals)} originals ({total_src / 1e6:.1f} MB) "
        f"-> {len(originals) * len(VARIANTS)} variants ({total_out / 1e6:.1f} MB)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
