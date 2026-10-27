#!/usr/bin/env python3
"""Generates the full Android icon set for TizNet Manager from the Hiddify logo.

Outputs:
  app/src/main/res/mipmap-*/ic_launcher.webp          legacy rounded icons
  app/src/main/res/mipmap-*/ic_launcher_round.webp    legacy round icons
  app/src/main/res/mipmap-*/ic_launcher_foreground.png adaptive foreground
  app/src/main/res/drawable-*/ic_launcher_monochrome.png themed (API 33)
  app/src/main/res/drawable/hiddify_logo*.png         in-app logos
  assets/store/icon-512.png                           store listing
"""
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "assets", "logo", "no_back_hiddify.png")
RES = os.path.join(ROOT, "app", "src", "main", "res")

BRAND = (69, 95, 233)          # #455FE9 - Hiddify blue
GRAD_TOP = (58, 82, 226)       # deep indigo
GRAD_BOTTOM = (96, 118, 245)   # bright periwinkle


def load_mark() -> Image.Image:
    """The Hi bar-chart mark, without the 'Hiddify' wordmark underneath."""
    im = Image.open(SRC).convert("RGBA")
    w, h = im.size
    alpha = im.split()[3]
    # Rows that carry a visible pixel; the wide empty band between the symbol
    # and the wordmark tells us exactly where to cut.
    def row_has(y: int) -> bool:
        return any(alpha.getpixel((x, y)) > 40 for x in range(0, w, 3))
    cut = h
    gap_start = None
    for y in range(h):
        if not row_has(y):
            if gap_start is None:
                gap_start = y
        elif gap_start is not None:
            if y - gap_start >= 20 and gap_start > h // 8:
                cut = gap_start
                break
            gap_start = None
    top = im.crop((0, 0, w, cut))
    bbox = top.getbbox()
    mark = top.crop(bbox)
    # trim to a square canvas keeping the mark centered
    side = max(mark.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(mark, ((side - mark.width) // 2, (side - mark.height) // 2), mark)
    return square


def recolor(im: Image.Image, rgb) -> Image.Image:
    out = Image.new("RGBA", im.size, rgb + (255,))
    out.putalpha(im.split()[3])
    return out


def gradient_bg(size: int, rounded: float = 0.0) -> Image.Image:
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    grad = Image.new("RGBA", (size, size))
    gd = grad.load()
    for y in range(size):
        t = y / max(1, size - 1)
        r = int(GRAD_TOP[0] + (GRAD_BOTTOM[0] - GRAD_TOP[0]) * t)
        g = int(GRAD_TOP[1] + (GRAD_BOTTOM[1] - GRAD_TOP[1]) * t)
        b = int(GRAD_TOP[2] + (GRAD_BOTTOM[2] - GRAD_TOP[2]) * t)
        for x in range(size):
            gd[x, y] = (r, g, b, 255)
    # soft decorative glows
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    d.ellipse([int(-size * .35), int(-size * .30), int(size * .55), int(size * .45)],
              fill=(255, 255, 255, 26))
    d.ellipse([int(size * .55), int(size * .62), int(size * 1.25), int(size * 1.28)],
              fill=(20, 32, 130, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(size // 12))
    grad.alpha_composite(glow)
    if rounded:
        mask = Image.new("L", (size, size), 0)
        md = ImageDraw.Draw(mask)
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * rounded), fill=255)
        bg.paste(grad, (0, 0), mask)
    else:
        bg = grad
    return bg


def legacy_icon(size: int, mark: Image.Image, circle: bool) -> Image.Image:
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if circle:
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
        bg.paste(gradient_bg(size), (0, 0), mask)
        clip = mask
    else:
        radius = int(size * 0.18)
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
        bg.paste(gradient_bg(size), (0, 0), mask)
        clip = mask
    logo = mark.resize((int(size * 0.60),) * 2, Image.LANCZOS)
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    layer.paste(logo, ((size - logo.width) // 2, (size - logo.height) // 2), logo)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(bg, (0, 0))
    out.alpha_composite(layer)
    out.putalpha(clip)
    return out


def adaptive_foreground(canvas: int, mark: Image.Image) -> Image.Image:
    """108dp adaptive canvas; the mark stays inside the 66dp safe zone."""
    fg = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    logo = mark.resize((int(canvas * 0.46),) * 2, Image.LANCZOS)
    fg.paste(logo, ((canvas - logo.width) // 2, (canvas - logo.height) // 2), logo)
    return fg


def main():
    mark = load_mark()
    white = recolor(mark, (255, 255, 255))

    densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

    for name, factor in densities.items():
        out_dir = os.path.join(RES, f"mipmap-{name}")
        os.makedirs(out_dir, exist_ok=True)
        legacy_size = int(48 * factor)
        legacy_icon(legacy_size, white, False).save(
            os.path.join(out_dir, "ic_launcher.webp"), "WEBP", quality=92)
        legacy_icon(legacy_size, white, True).save(
            os.path.join(out_dir, "ic_launcher_round.webp"), "WEBP", quality=92)
        fg_size = int(108 * factor)
        adaptive_foreground(fg_size, white).save(
            os.path.join(out_dir, "ic_launcher_foreground.png"))

    # monochrome themed icon (API 33): white silhouette, one density in drawable-nodpi
    mono_dir = os.path.join(RES, "drawable-nodpi")
    os.makedirs(mono_dir, exist_ok=True)
    mono = recolor(mark.resize((432, 432), Image.LANCZOS), (255, 255, 255))
    canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    canvas.paste(mono, (0, 0), mono)
    canvas.save(os.path.join(mono_dir, "ic_launcher_monochrome.png"))

    # in-app logos
    drawable = os.path.join(RES, "drawable-nodpi")
    mark.resize((320, 320), Image.LANCZOS).save(os.path.join(drawable, "hiddify_logo.png"))
    recolor(mark, (255, 255, 255)).resize((320, 320), Image.LANCZOS).save(
        os.path.join(drawable, "hiddify_logo_white.png"))

    # store icon
    store = os.path.join(ROOT, "assets", "store")
    os.makedirs(store, exist_ok=True)
    legacy_icon(512, white, False).convert("RGB").save(
        os.path.join(store, "icon-512.png"))

    # adaptive foreground also in drawable for fallback
    print("Icons generated:")
    for root, _, files in os.walk(RES):
        for f in sorted(files):
            if "ic_launcher" in f or "hiddify_logo" in f:
                print(" ", os.path.relpath(os.path.join(root, f), ROOT))
    print(" ", os.path.relpath(os.path.join(store, "icon-512.png"), ROOT))


if __name__ == "__main__":
    main()
