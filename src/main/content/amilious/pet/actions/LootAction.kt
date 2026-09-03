package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
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
        if (!ready()) return false
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
        val dest = tile ?: return
        if (!actor.brain.path.walk(actor, dest)) {
            rest(8)
        }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val path = actor.brain.path
        val dest = tile ?: return abort(actor, 8)
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (path.arrived(actor, dest)) {
                    path.stop(actor)
                    nextPhase()
                    return true
                }
                if (path.reallyStuck(actor, dest) || path.stuck(walkTicks)) {
                    return abort(actor, 12)
                }
                if (!path.walk(actor, dest)) {
                    return abort(actor, 12)
                }
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

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        actor.brain.path.stop(actor)
        rest(restTicks)
        return false
    }

    private fun hasNearby(actor: AmiliousMonkey): Boolean =
        GroundItemManager.getItems().any {
            !it.isRemoved &&
                    inRange(actor, it.location) &&
                    actor.canTake(it)
        }

    private fun inRange(actor: AmiliousMonkey, loc: core.game.world.map.Location): Boolean {
        val range = MonkeyConfig.LOOT_RANGE
        return loc.getDistance(actor.location) <= range ||
                loc.getDistance(actor.owner.location) <= range
    }

    private fun nearest(actor: AmiliousMonkey): GroundItem? =
        GroundItemManager.getItems()
            .filter { !it.isRemoved && inRange(actor, it.location) }
            .filter { actor.canTake(it) }
            .filter {
                val itm = Item(it.id, it.amount)
                actor.isBananaItem(itm) || actor.bag.hasSpaceFor(itm)
            }
            .filter { actor.brain.path.canReach(actor, it.location) }
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