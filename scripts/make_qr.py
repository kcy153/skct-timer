#!/usr/bin/env python3
"""§9-4: APK 다운로드 URL을 QR PNG로 만든다.

사용법(버전을 올려서 새로 릴리스할 때마다 이 한 줄만 다시 실행):
    python scripts/make_qr.py <APK 다운로드 URL>

예:
    python scripts/make_qr.py https://github.com/kcy153/skct-timer/releases/download/v1.0.0/skct-timer-v1.0.0.apk
"""
import sys
from pathlib import Path

import qrcode


def main() -> None:
    if len(sys.argv) != 2:
        print("사용법: python scripts/make_qr.py <APK 다운로드 URL>", file=sys.stderr)
        raise SystemExit(1)

    url = sys.argv[1]
    out_path = Path(__file__).resolve().parent.parent / "dist" / "install-qr.png"
    out_path.parent.mkdir(parents=True, exist_ok=True)

    img = qrcode.make(url)
    img.save(out_path)
    print(f"저장됨: {out_path}")
    print(f"인코딩된 URL: {url}")


if __name__ == "__main__":
    main()
