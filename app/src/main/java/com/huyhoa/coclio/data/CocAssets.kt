package com.huyhoa.coclio.data

import java.net.URLEncoder

object CocAssets {
    private const val ROOT =
        "https://raw.githubusercontent.com/chiefpansancolt/clash-of-clans-data/main/images/"

    private val homeBuildings = mapOf(
        1000000 to "home/army-buildings/army-camp",
        1000001 to "home/town-hall",
        1000002 to "home/resource-buildings/elixir-collector",
        1000003 to "home/resource-buildings/elixir-storage",
        1000004 to "home/resource-buildings/gold-mine",
        1000005 to "home/resource-buildings/gold-storage",
        1000006 to "home/army-buildings/barracks",
        1000007 to "home/army-buildings/laboratory",
        1000008 to "home/defenses/cannon",
        1000009 to "home/defenses/archer-tower",
        1000010 to "home/defenses/wall",
        1000011 to "home/defenses/wizard-tower",
        1000012 to "home/defenses/air-defense",
        1000013 to "home/defenses/mortar",
        1000014 to "home/resource-buildings/clan-castle",
        1000015 to "home/defenses/builders-hut",
        1000019 to "home/defenses/hidden-tesla",
        1000020 to "home/army-buildings/spell-factory",
        1000021 to "home/defenses/x-bow",
        1000023 to "home/resource-buildings/dark-elixir-drill",
        1000024 to "home/resource-buildings/dark-elixir-storage",
        1000026 to "home/army-buildings/dark-barracks",
        1000027 to "home/defenses/inferno-tower",
        1000028 to "home/defenses/air-sweeper",
        1000029 to "home/army-buildings/dark-spell-factory",
        1000031 to "home/defenses/eagle-artillery",
        1000032 to "home/defenses/bomb-tower",
        1000059 to "home/army-buildings/workshop",
        1000064 to "home/other/bobs-hut",
        1000067 to "home/defenses/scattershot",
        1000068 to "home/army-buildings/pet-house",
        1000070 to "home/army-buildings/blacksmith",
        1000071 to "home/army-buildings/hero-hall",
        1000072 to "home/defenses/spell-tower",
        1000077 to "home/defenses/monolith",
        1000079 to "home/defenses/multi-gear-tower",
        1000084 to "home/defenses/multi-archer-tower",
        1000085 to "home/defenses/ricochet-cannon",
        1000086 to "home/defenses/revenge-tower",
        1000089 to "home/defenses/firespitter",
        1000093 to "home/other/helper-hut",
        1000102 to "home/defenses/super-wizard-tower",
        1000097 to "home/defenses/crafting-station"
    )

    private val builderBuildings = mapOf(
        1000033 to "builder/walls/wall",
        1000034 to "builder/builder-hall",
        1000035 to "builder/resource-buildings/elixir-collector",
        1000036 to "builder/resource-buildings/elixir-storage",
        1000037 to "builder/resource-buildings/gold-mine",
        1000038 to "builder/resource-buildings/gold-storage",
        1000039 to "builder/other/clock-tower",
        1000040 to "builder/army-buildings/builder-barracks",
        1000041 to "builder/defenses/double-cannon",
        1000042 to "builder/army-buildings/army-camp",
        1000043 to "builder/defenses/hidden-tesla",
        1000044 to "builder/defenses/cannon",
        1000045 to "builder/defenses/multi-mortar",
        1000046 to "builder/army-buildings/star-laboratory",
        1000047 to "builder/resource-buildings/bob-control",
        1000048 to "builder/defenses/archer-tower",
        1000049 to "builder/army-buildings/reinforcement-camp",
        1000050 to "builder/defenses/firecrackers",
        1000051 to "builder/defenses/guard-post",
        1000052 to "builder/defenses/mega-tesla",
        1000053 to "builder/army-buildings/battle-machine-altar",
        1000054 to "builder/defenses/air-bombs",
        1000055 to "builder/defenses/crusher",
        1000056 to "builder/defenses/roaster",
        1000057 to "builder/defenses/giant-cannon",
        1000058 to "builder/resource-buildings/gem-mine",
        1000063 to "builder/defenses/lava-launcher",
        1000065 to "builder/resource-buildings/bob-control",
        1000078 to "builder/defenses/ottos-outpost",
        1000080 to "builder/army-buildings/battle-copter-altar",
        1000081 to "builder/defenses/x-bow",
        1000082 to "builder/army-buildings/healing-hut"
    )

    private val homeTraps = mapOf(
        12000000 to "home/traps/bomb",
        12000001 to "home/traps/spring-trap",
        12000002 to "home/traps/giant-bomb",
        12000005 to "home/traps/air-bomb",
        12000006 to "home/traps/seeking-air-mine",
        12000008 to "home/traps/skeleton-trap",
        12000016 to "home/traps/tornado-trap",
        12000020 to "home/traps/giga-bomb"
    )

    private val builderTraps = mapOf(
        12000010 to "builder/traps/spring-trap",
        12000011 to "builder/traps/push-trap",
        12000013 to "builder/traps/mine",
        12000014 to "builder/traps/mega-mine"
    )

    private val homeTroops = mapOf(
        4000000 to "home/troops/barbarian",
        4000001 to "home/troops/archer",
        4000002 to "home/troops/goblin",
        4000003 to "home/troops/giant",
        4000004 to "home/troops/wall-breaker",
        4000005 to "home/troops/balloon",
        4000006 to "home/troops/wizard",
        4000007 to "home/troops/healer",
        4000008 to "home/troops/dragon",
        4000009 to "home/troops/pekka",
        4000010 to "home/troops/minion",
        4000011 to "home/troops/hog-rider",
        4000012 to "home/troops/valkyrie",
        4000013 to "home/troops/golem",
        4000015 to "home/troops/witch",
        4000017 to "home/troops/lava-hound",
        4000022 to "home/troops/bowler",
        4000023 to "home/troops/baby-dragon",
        4000024 to "home/troops/miner",
        4000053 to "home/troops/yeti",
        4000058 to "home/troops/ice-golem",
        4000059 to "home/troops/electro-dragon",
        4000065 to "home/troops/dragon-rider",
        4000082 to "home/troops/headhunter",
        4000095 to "home/troops/electro-titan",
        4000097 to "home/troops/apprentice-warden",
        4000109 to "home/troops/ruin-witch",
        4000110 to "home/troops/root-rider",
        4000123 to "home/troops/druid",
        4000132 to "home/troops/thrower",
        4000150 to "home/troops/furnace",
        4000177 to "home/troops/meteor-golem"
    )

    private val builderTroops = mapOf(
        4000031 to "builder/troops/raged-barbarian",
        4000032 to "builder/troops/sneaky-archer",
        4000033 to "builder/troops/beta-minion",
        4000034 to "builder/troops/boxer-giant",
        4000035 to "builder/troops/bomber",
        4000036 to "builder/troops/power-pekka",
        4000037 to "builder/troops/cannon-cart",
        4000038 to "builder/troops/drop-ship",
        4000041 to "builder/troops/baby-dragon",
        4000042 to "builder/troops/night-witch",
        4000070 to "builder/troops/hog-glider",
        4000106 to "builder/troops/electrofire-wizard"
    )

    private val siegeMachines = mapOf(
        4000051 to "home/siege-machines/wall-wrecker",
        4000052 to "home/siege-machines/battle-blimp",
        4000062 to "home/siege-machines/stone-slammer",
        4000075 to "home/siege-machines/siege-barracks",
        4000087 to "home/siege-machines/log-launcher",
        4000091 to "home/siege-machines/flame-flinger",
        4000092 to "home/siege-machines/battle-drill",
        4000135 to "home/siege-machines/troop-launcher",
        4000188 to "home/siege-machines/sky-wagon"
    )

    private val heroes = mapOf(
        28000000 to "home/heroes/barbarian-king",
        28000001 to "home/heroes/archer-queen",
        28000002 to "home/heroes/grand-warden",
        28000003 to "builder/heroes/battle-machine",
        28000004 to "home/heroes/royal-champion",
        28000005 to "builder/heroes/battle-copter",
        28000006 to "home/heroes/minion-prince",
        28000007 to "home/heroes/dragon-duke"
    )

    private val spells = mapOf(
        26000000 to "home/spells/lightning-spell",
        26000001 to "home/spells/healing-spell",
        26000002 to "home/spells/rage-spell",
        26000003 to "home/spells/jump-spell",
        26000005 to "home/spells/freeze-spell",
        26000009 to "home/spells/poison-spell",
        26000010 to "home/spells/earthquake-spell",
        26000011 to "home/spells/haste-spell",
        26000016 to "home/spells/clone-spell",
        26000017 to "home/spells/skeleton-spell",
        26000028 to "home/spells/bat-spell",
        26000035 to "home/spells/invisibility-spell",
        26000053 to "home/spells/recall-spell",
        26000070 to "home/spells/overgrowth-spell",
        26000098 to "home/spells/revive-spell",
        26000109 to "home/spells/ice-block-spell",
        26000120 to "home/spells/totem-spell",
        26000123 to "home/spells/angry-spell"
    )

    private val pets = mapOf(
        73000000 to "home/pets/lassi",
        73000001 to "home/pets/mighty-yak",
        73000002 to "home/pets/electro-owl",
        73000003 to "home/pets/unicorn",
        73000004 to "home/pets/phoenix",
        73000007 to "home/pets/poison-lizard",
        73000008 to "home/pets/diggy",
        73000009 to "home/pets/frosty",
        73000010 to "home/pets/spirit-fox",
        73000011 to "home/pets/angry-jelly",
        73000016 to "home/pets/sneezy",
        73000017 to "home/pets/greedy-raven"
    )

    private val equipment = mapOf(
        90000000 to "barbarian-puppet",
        90000001 to "rage-vial",
        90000002 to "archer-puppet",
        90000003 to "invisibility-vial",
        90000004 to "eternal-tome",
        90000005 to "life-gem",
        90000006 to "seeking-shield",
        90000007 to "royal-gem",
        90000008 to "earthquake-boots",
        90000009 to "hog-rider-puppet",
        90000010 to "giant-gauntlet",
        90000011 to "vampstache",
        90000012 to "haste-vial",
        90000013 to "rocket-spear",
        90000014 to "spiky-ball",
        90000015 to "frozen-arrow",
        90000016 to "electro-fangs",
        90000017 to "giant-arrow",
        90000019 to "heroic-torch",
        90000020 to "healer-puppet",
        90000022 to "fireball",
        90000024 to "rage-gem",
        90000032 to "snake-bracelet",
        90000034 to "healing-tome",
        90000035 to "dark-crown",
        90000039 to "magic-mirror",
        90000040 to "electro-boots",
        90000041 to "lavaloon-puppet",
        90000042 to "henchmen-puppet",
        90000043 to "dark-orb",
        90000044 to "metal-pants",
        90000047 to "noble-iron",
        90000048 to "action-figure",
        90000049 to "meteor-staff",
        90000050 to "frost-flake",
        90000051 to "stick-horse",
        90000052 to "fire-heart",
        90000053 to "rocket-backpack",
        90000056 to "stun-blaster",
        90000057 to "flame-blower",
        90000059 to "monolith-arrow",
        90000060 to "revenge-deck",
        90000061 to "portal-pendant"
    )

    private val squads = mapOf(
        95000000 to "home/troops/barbarian",
        95000001 to "home/troops/giant",
        95000002 to "home/troops/wall-breaker",
        95000003 to "home/troops/minion",
        95000004 to "home/troops/balloon",
        95000005 to "home/troops/golem",
        95000006 to "home/troops/hog-rider",
        95000007 to "home/troops/miner",
        95000008 to "home/troops/balloon",
        95000009 to "home/troops/pekka",
        95000010 to "home/troops/dragon",
        95000011 to "home/troops/baby-dragon"
    )

    private val helpers = mapOf(
        93000000 to "builders-apprentice",
        93000001 to "lab-assistant",
        93000002 to "prospector"
    )

    private val guardians = mapOf(
        107000008 to "logger",
        107000000 to "longshot",
        107000001 to "smasher"
    )

    fun heroImageUrl(heroId: String): String? {
        val dataId = when (heroId.trim().lowercase()) {
            "barbarian-king" -> 28000000
            "archer-queen" -> 28000001
            "grand-warden" -> 28000002
            "royal-champion" -> 28000004
            "minion-prince" -> 28000006
            "dragon-duke" -> 28000007
            else -> return null
        }
        val folder = heroes[dataId] ?: return null
        return ROOT + "$folder/icon.png".split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
    }

    fun imageUrl(entry: CocUpgradeEntry): String? {
        val item = VillageItem(
            sectionKey = entry.sectionKey,
            sectionLabel = entry.category,
            tab = if (entry.sectionKey in setOf("buildings", "buildings2", "traps", "traps2")) "buildings" else "army",
            base = entry.base,
            dataId = entry.dataId,
            level = 1,
            count = 1,
            timerSeconds = null,
            helperCooldownSeconds = null,
            gearUp = false,
            weaponLevel = null,
            details = emptyList()
        )
        return imageUrl(item)
    }

    fun imageUrl(item: VillageItem): String? {
        val level = item.level?.takeIf { it > 0 } ?: 1
        val folder = when (item.sectionKey) {
            "buildings" -> homeBuildings[item.dataId]
            "buildings2" -> builderBuildings[item.dataId]
            "traps" -> homeTraps[item.dataId]
            "traps2" -> builderTraps[item.dataId]
            "units" -> homeTroops[item.dataId]
            "units2" -> builderTroops[item.dataId]
            "siege_machines" -> siegeMachines[item.dataId]
            "heroes", "heroes2" -> heroes[item.dataId]
            "spells" -> spells[item.dataId]
            "pets" -> pets[item.dataId]
            "guardians" -> guardians[item.dataId]?.let { "home/guardians/$it" }
            "equipment" -> equipment[item.dataId]?.let { "home/hero-equipment/$it" }
            "squads" -> squads[item.dataId]
            "helpers" -> helpers[item.dataId]?.let { "home/other/helpers/$it" }
            else -> null
        } ?: return null

        val useLevelImage = item.sectionKey in setOf("buildings", "buildings2", "traps", "traps2")
        val imageLevel = if (item.sectionKey == "buildings" && item.dataId == 1000070) {
            ((level + 1) / 2) * 2 - 1
        } else {
            level
        }
        val gearUpVariant = item.gearUp &&
            item.sectionKey == "buildings" &&
            item.dataId in setOf(1000008, 1000009, 1000013)
        val relativePath = when {
            useLevelImage && gearUpVariant -> "$folder/geared-up-normal/level-$imageLevel.png"
            useLevelImage -> "$folder/normal/level-$imageLevel.png"
            item.sectionKey == "helpers" -> "$folder/normal.png"
            else -> "$folder/icon.png"
        }
        return ROOT + relativePath.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
        }
    }
}
