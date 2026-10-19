#!/usr/bin/env python3
"""Export only publishable sources; never copy launcher credentials or saved games."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
destination = root / "build" / "release"
source = destination / "ca-locked"
if source.exists():
    shutil.rmtree(source)
source.mkdir(parents=True)

directories = ("src/main", "src/test", "gradle/wrapper", ".github/workflows", "docs")
files = ("README.md", "LICENSE", ".gitignore", "build.gradle", "settings.gradle",
         "gradlew", "gradlew.bat", "runelite-plugin.properties", "build.sh",
         "run-test-client.command", "tools/package-release.py")
selected = [root / name for name in files]
for name in directories:
    selected.extend(path for path in (root / name).rglob("*") if path.is_file())

manifest = []
for original in sorted(set(selected)):
    relative = original.relative_to(root)
    target = source / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(original, target)
    data = original.read_bytes()
    manifest.append({"path": relative.as_posix(), "bytes": len(data),
                     "sha256": hashlib.sha256(data).hexdigest()})

archive = destination / "ca-locked-source.zip"
with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED) as output:
    for item in manifest:
        path = source / item["path"]
        output.write(path, "ca-locked/" + item["path"])

(destination / "source-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
print(f"Packaged {len(manifest)} public source files: {archive}")
