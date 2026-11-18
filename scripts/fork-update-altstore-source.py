#!/usr/bin/env python3
"""Fork-only: maintain altstore/source.json for personal SideStore/AltStore test builds.

Unlike upstream's update-store-source.py (one entry per release in store.json), this keeps
several test builds of the same marketing version, newest first. All app metadata (version,
build, minimum iOS, privacy usage strings) is read from the IPA itself so SideStore's
install-time checks match what it downloads. IPA parsing is reused from upstream's script.
"""

import argparse
import importlib.util
import json
from datetime import datetime, timezone
from pathlib import Path

MAX_VERSIONS = 10


def load_upstream_helpers():
    path = Path(__file__).with_name("update-store-source.py")
    spec = importlib.util.spec_from_file_location("upstream_store_source", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--ipa", required=True, type=Path)
    parser.add_argument("--download-url", required=True)
    parser.add_argument("--repository", required=True, help="owner/repo hosting the source")
    parser.add_argument("--branch", required=True, help="branch the source is committed to")
    parser.add_argument("--notes", required=True)
    parser.add_argument("--date", help="ISO-8601 date; defaults to now (UTC)")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    upstream = load_upstream_helpers()
    upstream.validate_download_url(args.download_url)

    app_info, privacy = upstream.read_ipa_metadata(args.ipa)
    bundle_id = upstream.require_string(app_info, "CFBundleIdentifier", "Info.plist")
    version = upstream.require_string(app_info, "CFBundleShortVersionString", "Info.plist")
    build = upstream.require_string(app_info, "CFBundleVersion", "Info.plist")
    minimum_os = upstream.require_string(app_info, "MinimumOSVersion", "Info.plist")

    raw_base = f"https://raw.githubusercontent.com/{args.repository}/{args.branch}"
    icon_url = f"{raw_base}/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"
    owner = args.repository.split("/")[0].lower()

    previous = []
    if args.source.is_file():
        with args.source.open(encoding="utf-8") as handle:
            existing = json.load(handle)
        for app in existing.get("apps", []):
            if app.get("bundleIdentifier") == bundle_id:
                previous = app.get("versions", [])

    entry = {
        "version": version,
        "buildVersion": build,
        "date": args.date or datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "localizedDescription": args.notes,
        "downloadURL": args.download_url,
        "size": args.ipa.stat().st_size,
        "sha256": upstream.sha256(args.ipa),
        "minOSVersion": minimum_os,
    }
    # Keep only entries written by this script (they carry sha256 and IPA-accurate build
    # numbers); older hand-built entries had build numbers that didn't match their IPAs.
    kept = [
        v for v in previous
        if isinstance(v, dict) and v.get("sha256")
        and (v.get("version"), v.get("buildVersion")) != (version, build)
    ]
    versions = [entry, *kept][:MAX_VERSIONS]

    source = {
        "name": "Nuvio (Personal Test Builds)",
        "identifier": f"io.github.{owner}.nuvio",
        "sourceURL": f"{raw_base}/{args.source.as_posix()}",
        "subtitle": "Unofficial personal test builds of Nuvio",
        "description": "Personal SideStore/AltStore source for sideloading test builds of Nuvio.",
        "iconURL": icon_url,
        "tintColor": "#F54F32",
        "apps": [
            {
                "name": "Nuvio",
                "bundleIdentifier": bundle_id,
                "developerName": "Nuvio Media",
                "subtitle": "Unofficial personal test build",
                "localizedDescription": "Unofficial personal test build of Nuvio for iOS.",
                "iconURL": icon_url,
                "tintColor": "#F54F32",
                "category": "entertainment",
                "versions": versions,
                "appPermissions": {"entitlements": [], "privacy": privacy},
            }
        ],
        "news": [],
    }
    upstream.write_source(args.source, source)
    print(f"Updated {args.source} with {version} ({build}); {len(versions)} version(s) listed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
