package content.amilious.pet

object MonkeyConfig {
    val OWNERS = setOf("wolfy")

    const val NPC_ID = 132
    const val BOB_SIZE = 30
    const val FOLLOW_DIST = 12
    const val LOOT_RANGE = 6
    const val DUNG_COOLDOWN = 8
    const val BANANA_ID = 1963
    const val BANANA_NOTE_ID = 1964
    const val ANIM_ATTACK = 220
    const val ANIM_BLOCK = 221
    const val ANIM_DEATH = 223
    const val RUN_GAP = 2.5

    const val HUNGER_MAX = 100
    const val HUNGER_BANANA = 25
    const val HUNGER_THROW = 8
    const val HUNGER_LOOT = 1
    const val HUNGER_B2B = 5
    const val HUNGER_PICK = 2
    const val HUNGER_FEED = 4

    const val UNBURDEN_FREE = 4   // start taking when you have this many empty slots or fewer

    const val ATTR_DRUNK = "amilious_gigos_drunk" // ticks remaining, not saved
    const val DRUNK_BEER = 50
    const val ATTR_OWNED = "/save:amilious_monkey_owned"
    const val ATTR_ACTIVE = "amilious_monkey"
    const val ATTR_BAG = "/save:amilious_monkey_bag"
    const val ATTR_LOOT = "/save:amilious_monkey_loot"
    const val ATTR_HUNGER = "/save:amilious_gigos_hunger"
    const val ATTR_DUNG = "/save:amilious_monkey_dung"
    const val ATTR_EAT = "/save:amilious_monkey_eat"
    const val ATTR_B2B = "/save:amilious_monkey_b2b"
    const val ATTR_FEED = "/save:amilious_monkey_feed"
    const val ATTR_PICK = "/save:amilious_monkey_pick"
    const val ATTR_UNBURDEN = "/save:amilious_monkey_unburden"

    const val SFX_PLAYFUL = 633   // summon / dismiss
    const val SFX_OOK = 630       // throw, pick, eat, drunk
    const val SFX_SMALL = 634     // loot, unburden, empty

}