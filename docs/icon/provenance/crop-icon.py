#!/usr/bin/env python3
"""Cut the 1024x1024 icon out of the 1600x1600 native F2 frame.

The delivered icon is the stern-on cinematic frame with 288 px removed from every
edge and no resampling: crop((288, 288, 1312, 1312)) of
provenance/source-frame/cine-03-stern-on.png.

Run with the same Pillow build the capture workspace used:

    PYTHONPATH=/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages \
        python3 crop-icon.py provenance/source-frame/cine-03-stern-on.png icon.png

With that Pillow build the output is byte-identical to the delivered icon.png
(sha256 0f797da0515c205c5ddf8394dc60ffa2904dc6eb0c4e5afdf956f3d8229e8396); the
script asserts this so a future re-run cannot silently drift.
"""

import hashlib
import sys

from PIL import Image

EDGE = 288
EXPECTED = "0f797da0515c205c5ddf8394dc60ffa2904dc6eb0c4e5afdf956f3d8229e8396"


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main(source, target):
    with Image.open(source) as frame:
        frame.load()
        width, height = frame.size
        assert width == height, (width, height)
        icon = frame.crop((EDGE, EDGE, width - EDGE, height - EDGE))
        assert icon.size == (width - 2 * EDGE, height - 2 * EDGE), icon.size
        icon.save(target, format="PNG")
    written = sha256(target)
    print(f"{target}: {written}")
    assert written == EXPECTED, f"expected {EXPECTED}, wrote {written}"
    print("matches the delivered icon byte for byte")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    main(sys.argv[1], sys.argv[2])
