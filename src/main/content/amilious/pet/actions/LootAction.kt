package content.amilious.pet.actions


import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.node.item.GroundItem
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.world.map.Location

class LootAction(rank: Int = 60) :
    PhasedCompanionAction<AmiliousMonkey, LootAction.Phase>(
        "loot", rank, Phase::class
    ) {

    enum class Phase { WALK, SCOOP, HOLD }

    private var tile: Location? = null
    private var wait = 0
    private var walkTicks = 0
    private var fullWait = 0

    override fun cooldown(actor: AmiliousMonkey) {
        super.cooldown(actor)
        if (fullWait > 0) fullWait--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.lootEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_LOOT) return false
        val item = nearest(actor)
        if (item != null) return true
        if (hasNearby(actor) && fullWait == 0) {
            sendMessage(actor.owner, "Gigos wants to loot but his pack is full.")
            fullWait = 25
        }
        return false
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        tile = nearest(actor)?.location
        wait = 0
        walkTicks = 0
        walkTo(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val dest = tile ?: return false
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (actor.location.getDistance(dest) <= 1.5) {
                    nextPhase()
                    return true
                }
                if (walkTicks > 25) return false
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.SCOOP -> {
                scoop(actor, dest)
                wait = 3
                nextPhase()
                return true
            }
            Phase.HOLD -> {
                wait--
                return wait > 0
            }
        }
    }

    private fun walkTo(actor: AmiliousMonkey) {
        val dest = tile ?: return
        actor.pulseManager.clear()
        actor.pulseManager.run(object : MovementPulse(actor, dest) {
            override fun pulse(): Boolean = true
        })
    }

    private fun hasNearby(actor: AmiliousMonkey): Boolean =
        GroundItemManager.getItems().any {
            !it.isRemoved &&
                    it.location.getDistance(actor.location) <= MonkeyConfig.LOOT_RANGE &&
                    actor.canTake(it)
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

    private fun scoop(m: AmiliousMonkey, dest: Location) {
        val here = GroundItemManager.getItems()
            .filter { !it.isRemoved && it.location.getDistance(dest) <= 1.0 }
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
            if (!m.bag.hasSpaceFor(copy)) continue
            if (m.bag.add(copy)) {
                GroundItemManager.destroy(gi)
                m.addHunger(-MonkeyConfig.HUNGER_LOOT)
                any = true
                sendMessage(m.owner, "Gigos scoops up the ${copy.name.lowercase()}.")
            }
        }
        if (any) {
            playAudio(m.owner, MonkeyConfig.SFX_SMALL)
            m.saveBag()
            GigosHudPacket.send(m.owner, m)
        }
    }
}