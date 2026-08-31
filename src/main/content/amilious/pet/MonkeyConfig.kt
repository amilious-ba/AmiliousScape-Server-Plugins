package content.amilious.pet

import core.game.node.entity.player.Player

object MonkeyConfig {
    val OWNERS = setOf("wolfy")

    //Monkey config
    const val NPC_DARK = 132
    const val NPC_LIGHT = 4344
    //const val NPC_ID = 132          //the npc that is spawned
    const val BOB_SIZE = 30         //the inventory size of pet
    const val FOLLOW_DIST = 12      //the distance the pet will follow the player
    const val LOOT_RANGE = 6        //the distance the pet will loot from the player
    const val DUNG_COOLDOWN = 8     //time between dung throws
    const val HUNGER_MAX = 100      //max hunger value
    const val HUNGER_BANANA = 25    //hunger each banana restores
    const val HUNGER_THROW = 8      //hunger cost for throwing
    const val HUNGER_LOOT = 1       //hunger cost for looting
    const val HUNGER_B2B = 5        //hunger cost for converting bones to bananas
    const val HUNGER_PICK = 2       //hunger cost for picking bananas
    const val HUNGER_FEED = 4       //hunger cost for feeding owner
    const val UNBURDEN_FREE = 4     //start taking when you have this many empty slots or fewer
    const val DRUNK_BEER = 50


    //Item Ids
    const val BANANA_ID = 1963      //the id of the banana item
    const val BANANA_NOTE_ID = 1964 //the id of noted bananas


    //Animations
    const val ANIM_ATTACK = 220
    const val ANIM_BLOCK = 221
    const val ANIM_DEATH = 223
    const val ANIM_STAND = 222


    //sounds
    const val SFX_PLAYFUL = 633   // summon / dismiss
    const val SFX_OOK = 630       // throw, pick, eat, drunk
    const val SFX_SMALL = 634     // loot, unburden, empty


    //Attributes
    const val ATTR_DRUNK = "amilious_gigos_drunk" // ticks remaining, not saved
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
    const val ATTR_DARK = "/save:amilious_gigos_dark"

    fun npcId(player: Player): Int =
        if (player.getAttribute(ATTR_DARK, true)) NPC_DARK else NPC_LIGHT

    fun isGigosId(id: Int): Boolean =
        id == NPC_DARK || id == NPC_LIGHT

}