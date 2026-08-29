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

    override fun canStart(m: AmiliousMonkey): Boolean {
        if (!m.lootEnabled() || lootBusy) return false
        if (m.hunger() < MonkeyConfig.HUNGER_LOOT) return false
        if (m.bag.freeSlots() <= 0) return false
        return nearest(m) != null
    }

    override fun start(m: AmiliousMonkey) {
        tile = nearest(m)?.location
        phase = Phase.WALK
        wait = 0
    }

    override fun tick(m: AmiliousMonkey): Boolean {
        val dest = tile ?: return false
        when (phase) {
            Phase.WALK -> {
                if (m.location.getDistance(dest) <= 1.0) {
                    phase = Phase.WAIT
                    wait = 2
                    lootBusy = false
                    return true
                }
                if (!lootBusy) {
                    lootBusy = true
                    m.pulseManager.run(object : MovementPulse(m, dest) {
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
                scoop(m, dest)
                return false
            }
        }
    }

    private fun nearest(m: AmiliousMonkey): GroundItem? =
        GroundItemManager.getItems()
            .filter { !it.isRemoved && it.location.getDistance(m.location) <= MonkeyConfig.LOOT_RANGE }
            .filter { m.canTake(it) }
            .filter { m.bag.hasSpaceFor(Item(it.id, it.amount)) }
            .minByOrNull { it.location.getDistance(m.location) }

    private fun scoop(m: AmiliousMonkey, tile: Location) {
        val here = GroundItemManager.getItems()
            .filter { !it.isRemoved && it.location == tile }
            .filter { m.canTake(it) }
        var any = false
        for (gi in here) {
            if (m.hunger() < MonkeyConfig.HUNGER_LOOT) break
            if (m.bag.freeSlots() <= 0) break
            val copy = Item(gi.id, gi.amount)
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