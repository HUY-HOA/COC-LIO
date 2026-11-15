import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets/coc_upgrade"
REMOTE = ROOT / "remote-content/catalog"
NAMES_FILE = ROOT / "app/src/main/java/com/huyhoa/coclio/data/VillageParser.kt"
IMAGES_FILE = ROOT / "app/src/main/java/com/huyhoa/coclio/data/CocAssets.kt"
SOURCE_FILE = ASSETS / "source.json"

errors = []
warnings = []


def fail(message):
    errors.append(message)


def read_json(path):
    try:
        with path.open("r", encoding="utf-8") as stream:
            return json.load(stream)
    except (OSError, json.JSONDecodeError) as error:
        fail(f"{path.relative_to(ROOT)}: {error}")
        return {}


def kotlin_ids(path):
    try:
        source = path.read_text(encoding="utf-8")
    except OSError as error:
        fail(f"Không đọc được {path.relative_to(ROOT)}: {error}")
        return set()
    return {int(value) for value in re.findall(r"(\d+)\s+to\s+\"", source)}


local_files = sorted(ASSETS.glob("part-*.json"))
expected_names = [f"part-{index:02d}.json" for index in range(1, len(local_files) + 1)]
actual_names = [path.name for path in local_files]
if not local_files:
    fail("Không có tệp part-XX.json trong app/src/main/assets/coc_upgrade")
if actual_names != expected_names:
    fail(f"Danh sách part JSON không liên tục: {actual_names}")

catalog = {}
supercharge_count = 0
normal_level_count = 0
for path in local_files:
    document = read_json(path)
    items = document.get("items")
    if not isinstance(items, list):
        fail(f"{path.relative_to(ROOT)}: thiếu mảng items")
        continue
    for index, item in enumerate(items):
        label = f"{path.name}[{index}]"
        if not isinstance(item, dict):
            fail(f"{label}: bản ghi không phải object")
            continue
        data_id = item.get("dataId")
        if not isinstance(data_id, int) or data_id <= 0:
            fail(f"{label}: dataId không hợp lệ")
            continue
        if data_id in catalog:
            fail(f"dataId {data_id} bị trùng ở {catalog[data_id]['file']} và {label}")
            continue
        catalog[data_id] = {"item": item, "file": label}
        for field in ("id", "name", "base", "category"):
            if not isinstance(item.get(field), str) or not item[field].strip():
                fail(f"{label}: thiếu {field}")
        if item.get("base") not in {"home", "builder"}:
            fail(f"{label}: base phải là home hoặc builder")
        max_level = item.get("maxLevel")
        if not isinstance(max_level, int) or max_level < 1:
            fail(f"{label}: maxLevel không hợp lệ")
        levels = item.get("levels")
        if not isinstance(levels, list) or not levels:
            fail(f"{label}: thiếu levels")
            continue
        normal_levels = []
        super_levels = []
        for level_index, level in enumerate(levels):
            if not isinstance(level, dict):
                fail(f"{label}: levels[{level_index}] không phải object")
                continue
            level_number = level.get("level")
            if not isinstance(level_number, int) or level_number < 1:
                fail(f"{label}: levels[{level_index}] có level không hợp lệ")
                continue
            if level.get("supercharge") is True:
                super_levels.append(level_number)
                supercharge_count += 1
            else:
                normal_levels.append(level_number)
                normal_level_count += 1
        if len(normal_levels) != len(set(normal_levels)):
            fail(f"{label}: cấp thường bị trùng")
        if len(super_levels) != len(set(super_levels)):
            fail(f"{label}: bậc Siêu cường hóa bị trùng")
        if isinstance(max_level, int) and normal_levels and max(normal_levels) > max_level:
            fail(f"{label}: cấp thường vượt maxLevel")
        if normal_levels and sorted(normal_levels) != list(range(min(normal_levels), max(normal_levels) + 1)):
            warnings.append(f"{label}: dãy cấp thường có khoảng trống; cần đối chiếu nguồn")

name_ids = kotlin_ids(NAMES_FILE)
image_ids = kotlin_ids(IMAGES_FILE)
missing_names = sorted(set(catalog) - name_ids)
missing_images = sorted(set(catalog) - image_ids)
for data_id in missing_names:
    fail(f"ID {data_id} chưa có tên trong CocNames")
for data_id in missing_images:
    fail(f"ID {data_id} chưa có ánh xạ ảnh trong CocAssets")

remote_files = sorted(REMOTE.glob("part-*.json"))
remote_ids = set()
for path in remote_files:
    document = read_json(path)
    entries = document.get("catalog")
    if not isinstance(entries, list):
        fail(f"{path.relative_to(ROOT)}: thiếu mảng catalog")
        continue
    for index, entry in enumerate(entries):
        label = f"{path.name}[{index}]"
        if not isinstance(entry, dict):
            fail(f"{label}: bản ghi không phải object")
            continue
        data_id = entry.get("dataId")
        if not isinstance(data_id, int) or data_id <= 0:
            fail(f"{label}: dataId không hợp lệ")
            continue
        if data_id in remote_ids:
            fail(f"remote dataId {data_id} bị trùng")
        remote_ids.add(data_id)
        if data_id not in catalog:
            fail(f"remote dataId {data_id} không có trong danh mục JSON nội bộ")
        if not isinstance(entry.get("name"), str) or not entry["name"].strip():
            fail(f"{label}: thiếu name")
        if not isinstance(entry.get("maxLevel"), int) or entry["maxLevel"] < 1:
            fail(f"{label}: maxLevel không hợp lệ")
        level_details = entry.get("levelDetails", {})
        if not isinstance(level_details, dict):
            fail(f"{label}: levelDetails phải là object")
            continue
        for raw_level, stats in level_details.items():
            level_number = int(raw_level) if str(raw_level).isdigit() else 0
            if not 1 <= level_number <= 200:
                fail(f"{label}: levelDetails có cấp không hợp lệ {raw_level}")
            if not isinstance(stats, list) or any(not isinstance(value, str) for value in stats):
                fail(f"{label}: levelDetails[{raw_level}] phải là danh sách chuỗi")
        if len(level_details) < entry["maxLevel"]:
            warnings.append(f"{label}: chi tiết cấp mới có {len(level_details)}/{entry['maxLevel']} cấp; giữ rõ tình trạng dữ liệu chưa đầy đủ")

source = read_json(SOURCE_FILE)
if source.get("sourceLicense") != "MIT":
    fail("source.json không khai báo giấy phép MIT")
if not re.fullmatch(r"[0-9a-f]{40}", str(source.get("sourceCommit", ""))):
    fail("source.json thiếu sourceCommit SHA hợp lệ")
if not source.get("snapshotDate"):
    fail("source.json thiếu snapshotDate")

print(f"Danh mục nội bộ: {len(catalog)} ID; {normal_level_count} cấp thường; {supercharge_count} bậc Siêu cường hóa.")
print(f"Ánh xạ tên: {len(set(catalog) & name_ids)}/{len(catalog)}; ánh xạ ảnh: {len(set(catalog) & image_ids)}/{len(catalog)}.")
print(f"Danh mục tra cứu từ xa: {len(remote_ids)} ID.")
for warning in warnings:
    print(f"WARNING: {warning}")
for error in errors:
    print(f"ERROR: {error}")
if errors:
    sys.exit(1)
print("Kiểm định danh mục JSON và ánh xạ ID hoàn tất.")
