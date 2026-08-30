package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.item.Item

class UnburdenAction : CompanionAction<AmiliousMonkey> {

    private var cool = 0
    private var warned = 0

    override fun name() = "unburden"

    override fun cooldown(actor: AmiliousMonkey) {
        if (cool > 0) cool--
        if (warned > 0) warned--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.unburdenEnabled()) return false
        if (cool > 0) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_LOOT) return false
        if (!actor.ownerGathering()) return false
        if (actor.owner.inventory.freeSlots() > MonkeyConfig.UNBURDEN_FREE) return false
        return nextItem(actor) != null
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.hunger() < MonkeyConfig.HUNGER_LOOT) {
            cool = 8
            return false
        }
        val item = nextItem(actor)
        if (item == null) {
            cool = 4
            return false
        }
        val room = actor.bag.getMaximumAdd(item)
        if (room <= 0) {
            if (warned == 0) {
                sendMessage(actor.owner, "Gigos cannot carry any more.")
                warned = 25
            }
            cool = 8
            return false
        }
        val move = Item(item.id, minOf(item.amount, room))
        if (actor.owner.inventory.remove(move) && actor.bag.add(move)) {
            actor.addHunger(-MonkeyConfig.HUNGER_LOOT)
            actor.saveBag()
            GigosHudPacket.send(actor.owner, actor)
            sendMessage(actor.owner, "Gigos takes the ${move.name.lowercase()} from you.")
        }
        cool = 2
        return false
    }

    private fun nextItem(actor: AmiliousMonkey): Item? {
        for (it in actor.owner.inventory.toArray()) {
            if (it == null) continue
            if (actor.isBananaItem(it)) continue
            if (!looksGathered(it.name)) continue
            if (actor.bag.getMaximumAdd(it) <= 0) continue
            return it
        }
        return null
    }

    private fun looksGathered(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("ore") ||
                n.contains("log") ||
                n.contains("wool") ||
                n.contains("fish") ||
                n.endsWith(" shrimp") ||
                n.contains("anchov") ||
                n.contains("gem") ||
                n.contains("bar") ||
                n.contains("essence") ||
                n.contains("flax") ||
                n.contains("herb") ||
                n.contains("grapes") ||
                n.contains("potato") ||
                n.contains("wheat")
    }
}