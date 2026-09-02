package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.food.FoodFilter
import content.amilious.food.FoodTable
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.item.Item
import core.game.world.update.flag.context.Animation

class FeedOwnerAction(rank: Int = 80) :
    PhasedCompanionAction<AmiliousMonkey, FeedOwnerAction.Phase>(
        "feed", rank, Phase::class
    ) {

    enum class Phase { WALK, FEED }

    private var walkTicks = 0

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.feedEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_FEED) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        if (!actor.brain.path.canReach(actor, actor.owner.location)) return false
        val missing = FoodTable.missingHp(actor.owner)
        val max = actor.owner.skills.maximumLifepoints
        if (max <= 0 || actor.owner.skills.lifepoints * 2 > max) return false
        return FoodTable.bestFor(actor.bag, missing, FoodFilter.PLAIN, allowWaste = false) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        walkTicks = 0
        if (!actor.brain.path.walk(actor, actor.owner.location)) {
            rest(8)
        }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val path = actor.brain.path
        val dest = actor.owner.location
        if (!actor.feedEnabled()) {
            return abort(actor, 8)
        }
        if (actor.location.getDistance(dest) > MonkeyConfig.FOLLOW_DIST) {
            return abort(actor, 8)
        }
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (path.arrived(actor, dest, 1.5)) {
                    path.stop(actor)
                    nextPhase()
                    return true
                }
                if (path.reallyStuck(actor, dest) || path.stuck(walkTicks, 25)) {
                    return abort(actor, 12)
                }
                if (!path.walk(actor, dest)) {
                    return abort(actor, 12)
                }
                return true
            }
            Phase.FEED -> {
                if (!path.arrived(actor, dest, 1.5)) {
                    phase = Phase.WALK
                    walkTicks = 0
                    return true
                }
                if (actor.hunger() < MonkeyConfig.HUNGER_FEED) {
                    return abort(actor, 8)
                }
                val missing = FoodTable.missingHp(actor.owner)
                val max = actor.owner.skills.maximumLifepoints
                if (max <= 0 || actor.owner.skills.lifepoints * 2 > max) {
                    return abort(actor, 8)
                }
                val pick = FoodTable.bestFor(actor.bag, missing, FoodFilter.PLAIN, allowWaste = false)
                if (pick == null) {
                    return abort(actor, 8)
                }
                val (item, entry) = pick
                val bite = Item(item.id, 1)
                if (!actor.bag.remove(bite)) {
                    return abort(actor, 8)
                }
                if (entry.leftover > 0) {
                    actor.bag.add(Item(entry.leftover, 1))
                }
                actor.owner.animate(Animation(829))
                actor.owner.skills.heal(entry.healMin)
                actor.addHunger(-MonkeyConfig.HUNGER_FEED)
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos fed you ${a(entry.name)}.")
                rest(5)
                return false
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        actor.brain.path.stop(actor)
        rest(restTicks)
        return false
    }

    private fun a(name: String): String {
        val n = name.trim().lowercase()
        val an = n.firstOrNull() in setOf('a', 'e', 'i', 'o', 'u')
        return if (an) "an $n" else "a $n"
    }
}