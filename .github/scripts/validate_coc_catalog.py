import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets/coc_upgrade"
NAMES_FILE = ROOT / "app/src/main/java/com/huyhoa/coclio/data/VillageParser.kt"
IMAGES_FILE = ROOT / "app/src/main/java/com/huyhoa/coclio/data/CocAssets.kt"
SOURCE_FILE = ASSETS / "source.json"
SUPPLEMENTAL_FILE = ASSETS / "supplemental_catalog.json"

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
supercharge_item_count = 0
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
        if super_levels:
            supercharge_item_count += 1
        if len(normal_levels) != len(set(normal_levels)):
            fail(f"{label}: cấp thường bị trùng")
        if len(super_levels) != len(set(super_levels)):
            fail(f"{label}: bậc Siêu cường hóa bị trùng")
        if isinstance(max_level, int) and normal_levels and max(normal_levels) > max_level:
            fail(f"{label}: cấp thường vượt maxLevel")
        if normal_levels and sorted(normal_levels) != list(range(min(normal_levels), max(normal_levels) + 1)):
            warnings.append(f"{label}: dãy cấp thường có khoảng trống; cần đối chiếu nguồn")

        first_normal_level = min(normal_levels) if normal_levels else None
        for level in levels:
            if not isinstance(level, dict) or level.get("supercharge") is True:
                continue
            if level.get("level") == first_normal_level:
                continue
            category = item.get("category")
            upgrade_cost = level.get("upgradeCost", level.get("researchCost", level.get("buildCost", 0)))
            upgrade_cost = upgrade_cost if isinstance(upgrade_cost, (int, float)) else 0
            ore_cost = sum(
                level.get(field, 0) if isinstance(level.get(field, 0), (int, float)) else 0
                for field in ("upgradeShinyOre", "upgradeGlowingOre", "upgradeStarryOre")
            )
            resource = level.get(
                "upgradeCostResource",
                level.get("researchCostResource", level.get("buildCostResource", ""))
            )
            event_only = item.get("properties", {}).get("eventOnly") is True
            if category not in {"resource", "other"} and not event_only:
                if upgrade_cost <= 0 and ore_cost <= 0:
                    fail(f"{label}: cấp {level.get('level')} thiếu chi phí nâng cấp")
                if upgrade_cost > 0 and not str(resource).strip():
                    fail(f"{label}: cấp {level.get('level')} có chi phí nhưng thiếu loại tài nguyên")
            if category not in {"hero-equipment", "other"}:
                has_time = any(
                    isinstance(level.get(field), dict)
                    for field in ("upgradeTime", "researchTime", "buildTime")
                )
                if not has_time:
                    fail(f"{label}: cấp {level.get('level')} thiếu thời gian nâng cấp")

catalog_items = [record["item"] for record in catalog.values()]
equipment_entries = [item for item in catalog_items if item.get("category") == "hero-equipment"]
expected_equipment_heroes = {
    "barbarian-king",
    "archer-queen",
    "grand-warden",
    "royal-champion",
    "minion-prince",
    "dragon-duke",
}
if len(equipment_entries) != 43:
    fail(f"Danh mục trang bị anh hùng phải có 43 món đã phát hành; hiện có {len(equipment_entries)}")
for item in equipment_entries:
    label = f"Trang bị {item.get('dataId')}"
    if item.get("hero") not in expected_equipment_heroes:
        fail(f"{label}: thiếu hoặc sai hero sở hữu ({item.get('hero')})")
    rarity = item.get("rarity")
    expected_max = {"Common": 18, "Epic": 27}.get(rarity)
    if expected_max is None:
        fail(f"{label}: độ hiếm đã phát hành không hợp lệ ({rarity})")
    elif item.get("maxLevel") != expected_max:
        fail(f"{label}: MAX phải là {expected_max} theo độ hiếm {rarity}")
    regular_levels = {
        level.get("level")
        for level in item.get("levels", [])
        if isinstance(level, dict) and level.get("supercharge") is not True
    }
    if expected_max and regular_levels != set(range(1, expected_max + 1)):
        fail(f"{label}: thiếu cấp trang bị thường; cần đủ cấp 1–{expected_max}")

squad_entries = [item for item in catalog_items if item.get("category") == "squad"]
expected_squad_ids = set(range(4000195, 4000207))
actual_squad_ids = {item.get("dataId") for item in squad_entries}
if actual_squad_ids != expected_squad_ids:
    fail(f"Danh mục Squad phải có đủ 12 ID 4000195–4000206; hiện có {sorted(actual_squad_ids)}")
for item in squad_entries:
    label = f"Squad {item.get('dataId')}"
    expected_town_hall = item.get("dataId", 0) - 4000187
    properties = item.get("properties", {})
    if properties.get("townHallRequired") != expected_town_hall:
        fail(f"{label}: Nhà chính mở khóa phải là TH{expected_town_hall}")
    if item.get("maxLevel") != 3:
        fail(f"{label}: cấp tối đa Squad phải là 3")
    levels = {
        level.get("level")
        for level in item.get("levels", [])
        if isinstance(level, dict)
    }
    if levels != {1, 2, 3}:
        fail(f"{label}: phải có dữ liệu cấp 1, 2 và 3")
    for required in ("composition", "abilityLevel2", "abilityLevel3"):
        if not str(properties.get(required, "")).strip():
            fail(f"{label}: thiếu {required}")

event_spells = {
    26000006: 13,
    26000073: 13,
}
for data_id, expected_max in event_spells.items():
    record = catalog.get(data_id)
    if record is None:
        fail(f"Thiếu thần chú sự kiện dataId {data_id}")
        continue
    item = record["item"]
    if item.get("category") != "spell" or item.get("maxLevel") != expected_max:
        fail(f"Thần chú sự kiện {data_id}: sai category hoặc maxLevel")
    if item.get("properties", {}).get("eventOnly") is not True:
        fail(f"Thần chú sự kiện {data_id}: phải được đánh dấu eventOnly")
    levels = {
        level.get("level")
        for level in item.get("levels", [])
        if isinstance(level, dict)
    }
    if levels != set(range(1, expected_max + 1)):
        fail(f"Thần chú sự kiện {data_id}: thiếu cấp")

required_th19_buildings = {
    1000105: ("guardian-shrine", 1),
    1000106: ("squad-barracks", 12),
    1000107: ("spiky-tower", 2),
    1000108: ("firecracker-tower", 2),
}
for data_id, (expected_id, expected_max) in required_th19_buildings.items():
    record = catalog.get(data_id)
    if record is None:
        fail(f"Thiếu công trình TH19 dataId {data_id}")
        continue
    item = record["item"]
    if item.get("id") != expected_id or item.get("maxLevel") != expected_max:
        fail(f"Công trình TH19 {data_id}: sai ID hoặc maxLevel")
    levels = {
        level.get("level")
        for level in item.get("levels", [])
        if isinstance(level, dict)
    }
    if levels != set(range(1, expected_max + 1)):
        fail(f"Công trình TH19 {data_id}: thiếu cấp nâng cấp")

boto_shack = catalog.get(1000047)
if boto_shack is None or boto_shack["item"].get("base") != "builder":
    fail("Danh mục thiếu Lều B.O.B ở Căn cứ thợ xây")

def require_item_max_levels(base, category, expected):
    for item_id, expected_max in expected.items():
        matches = [
            item for item in catalog_items
            if item.get("id") == item_id
            and item.get("base") == base
            and item.get("category") == category
        ]
        if len(matches) != 1:
            fail(f"Thiếu hoặc trùng mục {base}/{category}/{item_id}")
            continue
        item = matches[0]
        if item.get("maxLevel") != expected_max:
            fail(f"{item_id}: MAX phải là {expected_max}, hiện là {item.get('maxLevel')}")
        actual_levels = {
            level.get("level")
            for level in item.get("levels", [])
            if isinstance(level, dict) and level.get("supercharge") is not True
        }
        if actual_levels != set(range(1, expected_max + 1)):
            fail(f"{item_id}: thiếu dữ liệu cấp thường 1–{expected_max}")

require_item_max_levels("home", "hero", {
    "barbarian-king": 115,
    "archer-queen": 115,
    "minion-prince": 100,
    "grand-warden": 90,
    "royal-champion": 60,
    "dragon-duke": 30,
})

require_item_max_levels("home", "troop", {
    "barbarian": 14,
    "archer": 15,
    "giant": 15,
    "goblin": 11,
    "wall-breaker": 15,
    "balloon": 14,
    "wizard": 15,
    "healer": 12,
    "dragon": 14,
    "pekka": 14,
    "baby-dragon": 13,
    "miner": 13,
    "electro-dragon": 10,
    "yeti": 9,
    "dragon-rider": 7,
    "electro-titan": 6,
    "root-rider": 5,
    "thrower": 5,
    "meteor-golem": 4,
    "minion": 15,
    "hog-rider": 16,
    "valkyrie": 13,
    "golem": 16,
    "witch": 9,
    "lava-hound": 9,
    "bowler": 11,
    "headhunter": 5,
    "druid": 7,
    "ruin-witch": 5,
})

require_item_max_levels("home", "spell", {
    "healing-spell": 13,
    "lightning-spell": 14,
    "totem-spell": 5,
    "poison-spell": 13,
    "haste-spell": 8,
    "bat-spell": 9,
})

require_item_max_levels("home", "pet", {"angry-jelly": 15})
require_item_max_levels("home", "guardian", {"smasher": 10})

power_pekka = next(
    (item for item in catalog_items if item.get("id") == "power-pekka" and item.get("base") == "builder"),
    None,
)
power_pekka_expected = {
    13: (0, 0),
    14: (3600000, 388800),
    15: (3800000, 475200),
    16: (4000000, 475200),
    17: (4600000, 518400),
    18: (4800000, 518400),
    19: (5600000, 561600),
    20: (5800000, 561600),
}
if power_pekka is None or power_pekka.get("maxLevel") != 20:
    fail("Power P.E.K.K.A phải có cấp trong game 13–20 và MAX 20")
else:
    power_levels = {
        level.get("level"): level
        for level in power_pekka.get("levels", [])
        if isinstance(level, dict) and level.get("supercharge") is not True
    }
    if set(power_levels) != set(power_pekka_expected):
        fail("Power P.E.K.K.A phải có đủ cấp trong game 13–20")
    for number, (cost, duration) in power_pekka_expected.items():
        level = power_levels.get(number)
        if level is None:
            continue
        time = level.get("researchTime", {})
        seconds = (
            time.get("days", 0) * 86400
            + time.get("hours", 0) * 3600
            + time.get("minutes", 0) * 60
            + time.get("seconds", 0)
        ) if isinstance(time, dict) else -1
        if level.get("researchCost") != cost or seconds != duration:
            fail(f"Power P.E.K.K.A cấp {number}: sai chi phí/thời gian sau khi ánh xạ cấp nguồn 1–8")

town_hall = catalog.get(1000001)
if town_hall is None or town_hall["item"].get("maxLevel") != 19 or not any(
    isinstance(level, dict) and level.get("level") == 19
    for level in town_hall["item"].get("levels", [])
):
    fail("Nhà chính phải có đầy đủ cấp 19")

heroic_spells = [
    item for item in catalog_items
    if item.get("dataId") == 26000153 and item.get("category") == "spell"
]
if len(heroic_spells) != 1:
    fail("Heroic Spell phải có đúng một bản ghi dataId 26000153 trong danh mục Thần chú")
elif heroic_spells[0].get("maxLevel") != 4 or {
    level.get("level")
    for level in heroic_spells[0].get("levels", [])
    if isinstance(level, dict)
} != {1, 2, 3, 4}:
    fail("Heroic Spell phải có đủ dữ liệu cấp 1–4")

name_ids = kotlin_ids(NAMES_FILE)
image_ids = kotlin_ids(IMAGES_FILE)
missing_names = sorted(set(catalog) - name_ids)
missing_images = sorted(set(catalog) - image_ids)
for data_id in missing_names:
    fail(f"ID {data_id} chưa có tên trong CocNames")
for data_id in missing_images:
    fail(f"ID {data_id} chưa có ánh xạ ảnh trong CocAssets")

supplemental_document = read_json(SUPPLEMENTAL_FILE)
supplemental_items = supplemental_document.get("catalog")
if not isinstance(supplemental_items, list):
    fail("supplemental_catalog.json thiếu mảng catalog")
    supplemental_items = []
supplemental_ids = set()
for index, item in enumerate(supplemental_items):
    label = f"supplemental_catalog.json[{index}]"
    if not isinstance(item, dict):
        fail(f"{label}: bản ghi không phải object")
        continue
    data_id = item.get("dataId")
    if not isinstance(data_id, int) or data_id <= 0:
        fail(f"{label}: dataId không hợp lệ")
        continue
    if data_id in supplemental_ids:
        fail(f"{label}: dataId {data_id} bị trùng")
        continue
    supplemental_ids.add(data_id)
    if not isinstance(item.get("name"), str) or not item["name"].strip():
        fail(f"{label}: thiếu name")
    if not isinstance(item.get("category"), str):
        fail(f"{label}: thiếu category")
    details = item.get("levelDetails", {})
    if not isinstance(details, dict):
        fail(f"{label}: levelDetails không phải object")
    elif any(not isinstance(values, list) or not all(isinstance(value, str) for value in values)
             for values in details.values()):
        fail(f"{label}: levelDetails chứa dữ liệu không hợp lệ")

source = read_json(SOURCE_FILE)
if source.get("sourceLicense") != "MIT":
    fail("source.json không khai báo giấy phép MIT")
if not re.fullmatch(r"[0-9a-f]{40}", str(source.get("sourceCommit", ""))):
    fail("source.json thiếu sourceCommit SHA hợp lệ")
if not source.get("snapshotDate"):
    fail("source.json thiếu snapshotDate")
integrity = source.get("catalogIntegrity", {})
if integrity.get("localIdCount") != len(catalog):
    fail(f"source.json localIdCount không khớp: {integrity.get('localIdCount')} != {len(catalog)}")
if integrity.get("localSupplementalIdCount") != len(supplemental_ids):
    fail(f"source.json localSupplementalIdCount không khớp: {integrity.get('localSupplementalIdCount')} != {len(supplemental_ids)}")
if integrity.get("superchargeItemCount") != supercharge_item_count:
    fail(f"source.json superchargeItemCount không khớp: {integrity.get('superchargeItemCount')} != {supercharge_item_count}")

print(f"Danh mục nội bộ: {len(catalog)} ID; {normal_level_count} cấp thường; {supercharge_count} bậc Siêu cường hóa ở {supercharge_item_count} công trình.")
print(f"Ánh xạ tên: {len(set(catalog) & name_ids)}/{len(catalog)}; ánh xạ ảnh: {len(set(catalog) & image_ids)}/{len(catalog)}.")
print(f"Dữ liệu bổ sung đóng gói trong APK: {len(supplemental_ids)} ID.")
for warning in warnings:
    print(f"WARNING: {warning}")
for error in errors:
    print(f"ERROR: {error}")
if errors:
    sys.exit(1)
print("Kiểm định danh mục JSON và ánh xạ ID hoàn tất.")
