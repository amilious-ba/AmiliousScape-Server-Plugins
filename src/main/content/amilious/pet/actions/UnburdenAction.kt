package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.node.item.Item

class UnburdenAction(rank: Int = 70) :
    SimpleCompanionAction<AmiliousMonkey>("unburden", rank) {

    private var warned = 0

    override fun getPhaseName() = "Unburden"

    override fun cooldown(actor: AmiliousMonkey) {
        super.cooldown(actor)
        if (warned > 0) warned--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.unburdenEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_LOOT) return false
        if (!actor.ownerGathering()) return false
        if (actor.owner.inventory.freeSlots() > MonkeyConfig.UNBURDEN_FREE) return false
        return nextItem(actor) != null
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.hunger() < MonkeyConfig.HUNGER_LOOT) {
            rest(8)
            return false
        }
        val item = nextItem(actor)
        if (item == null) {
            rest(4)
            return false
        }
        val room = actor.bag.getMaximumAdd(item)
        if (room <= 0) {
            if (warned == 0) {
                sendMessage(actor.owner, "Gigos cannot carry any more.")
                warned = 25
            }
            rest(8)
            return false
        }
        val move = Item(item.id, minOf(item.amount, room))
        if (actor.owner.inventory.remove(move) && actor.bag.add(move)) {
            actor.addHunger(-MonkeyConfig.HUNGER_LOOT)
            actor.saveBag()
            GigosHudPacket.send(actor.owner, actor)
            playAudio(actor.owner, MonkeyConfig.SFX_SMALL)
            sendMessage(actor.owner, "Gigos takes the ${move.name.lowercase()} from you.")
        }
        rest(2)
        return false
    }

    private fun nextItem(actor: AmiliousMonkey): Item? {
        for (it in actor.owner.inventory.toArray()) {
            if (it == null) continue
            if (actor.isBananaItem(it)) continue
            if (it.id !in GATHER_IDS) continue
            if (actor.bag.getMaximumAdd(it) <= 0) continue
            return it
        }
        return null
    }

    companion object {
        private val GATHER_IDS = setOf(
            // ores / rock
            434, 436, 438, 440, 442, 444, 447, 449, 451, 453,
            697, 698, 699, 700,          // sandstone
            6979, 6981, 6983,            // granite
            // logs
            1511, 1521, 1519, 1517, 1515, 1513,
            6332, 6333, 2862, 3239,
            // raw fish
            317, 321, 327, 345, 353, 335, 341, 349, 331,
            359, 377, 363, 371, 383, 7944, 3142, 3379, 5001,
            // wool / flax / essence
            1737, 1779, 1436, 7936,
            // uncut gems
            1617, 1619, 1621, 1623, 1625, 1627, 1629, 1631,
            // grimy herbs
            199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219,
            2485, 3049, 3051,
            // farm
            1947, 1942, 1987
        )
    }

}