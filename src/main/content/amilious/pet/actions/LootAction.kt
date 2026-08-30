package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.node.item.GroundItem
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.world.map.Location

class LootAction : CompanionAction<AmiliousMonkey> {

    private enum class Phase { WALK, WAIT, SCOOP }

    private var phase = Phase.WALK
    private var tile: Location? = null
    private var wait = 0
    private var lootBusy = false

    override fun name() = "loot"

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.lootEnabled() || lootBusy) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_LOOT) return false
        if (actor.bag.freeSlots() <= 0) return false
        return nearest(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        tile = nearest(actor)?.location
        phase = Phase.WALK
        wait = 0
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val dest = tile ?: return false
        when (phase) {
            Phase.WALK -> {
                if (actor.location.getDistance(dest) <= 1.0) {
                    phase = Phase.WAIT
                    wait = 2
                    lootBusy = false
                    return true
                }
                if (!lootBusy) {
                    lootBusy = true
                    actor.pulseManager.run(object : MovementPulse(actor, dest) {
                        override fun pulse(): Boolean {
                            lootBusy = false
                            return true
                        }
                    })
                }
                return true
            }
            Phase.WAIT -> {
                wait--
                if (wait > 0) return true
                phase = Phase.SCOOP
                return true
            }
            Phase.SCOOP -> {
                scoop(actor, dest)
                return false
            }
        }
    }

    private fun nearest(actor: AmiliousMonkey): GroundItem? =
        GroundItemManager.getItems()
            .filter { !it.isRemoved && it.location.getDistance(actor.location) <= MonkeyConfig.LOOT_RANGE }
            .filter { actor.canTake(it) }
            .filter {
                val itm = Item(it.id, it.amount)
                actor.isBananaItem(itm) || actor.bag.hasSpaceFor(itm)
            }
            .minByOrNull { it.location.getDistance(actor.location) }

    private fun scoop(m: AmiliousMonkey, tile: Location) {
        val here = GroundItemManager.getItems()
            .filter { !it.isRemoved && it.location == tile }
            .filter { m.canTake(it) }
        var any = false
        for (gi in here) {
            if (m.hunger() < MonkeyConfig.HUNGER_LOOT) break
            val copy = Item(gi.id, gi.amount)
            if (m.isBananaItem(copy)) {
                if (m.addBananasNoted(copy.amount)) {
                    GroundItemManager.destroy(gi)
                    m.addHunger(-MonkeyConfig.HUNGER_LOOT)
                    any = true
                    sendMessage(m.owner, "Gigos notes the bananas.")
                }
                continue
            }
            if (m.bag.freeSlots() <= 0) break
            if (!m.bag.hasSpaceFor(copy)) continue
            if (m.bag.add(copy)) {
                GroundItemManager.destroy(gi)
                m.addHunger(-MonkeyConfig.HUNGER_LOOT)
                any = true
                sendMessage(m.owner, "Gigos scoops up the ${copy.name.lowercase()}.")
            }
        }
        if (any) {
            m.saveBag()
            GigosHudPacket.send(m.owner, m)
        }
    }
}