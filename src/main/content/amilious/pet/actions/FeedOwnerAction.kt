package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.food.FoodFilter
import content.amilious.food.FoodTable
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.interaction.MovementPulse
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
        val missing = FoodTable.missingHp(actor.owner)
        val max = actor.owner.skills.maximumLifepoints
        if (max <= 0 || actor.owner.skills.lifepoints * 2 > max) return false
        return FoodTable.bestFor(actor.bag, missing, FoodFilter.PLAIN, allowWaste = false) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        walkTicks = 0
        walkTo(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (!actor.feedEnabled()) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (actor.location.getDistance(actor.owner.location) <= 1.5) {
                    nextPhase()
                    return true
                }
                if (walkTicks > 25) return false
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.FEED -> {
                if (actor.hunger() < MonkeyConfig.HUNGER_FEED) {
                    rest(8)
                    return false
                }
                val missing = FoodTable.missingHp(actor.owner)
                val max = actor.owner.skills.maximumLifepoints
                if (max <= 0 || actor.owner.skills.lifepoints * 2 > max) return false
                val pick = FoodTable.bestFor(actor.bag, missing, FoodFilter.PLAIN, allowWaste = false)
                if (pick == null) {
                    rest(8)
                    return false
                }
                val (item, entry) = pick
                val bite = Item(item.id, 1)
                if (!actor.bag.remove(bite)) {
                    rest(8)
                    return false
                }
                if (entry.leftover > 0) actor.bag.add(Item(entry.leftover, 1))
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

    private fun a(name: String): String {
        val n = name.trim().lowercase()
        val an = n.firstOrNull() in setOf('a', 'e', 'i', 'o', 'u')
        return if (an) "an $n" else "a $n"
    }

    private fun walkTo(actor: AmiliousMonkey) {
        actor.pulseManager.clear()
        actor.pulseManager.run(object : MovementPulse(actor, actor.owner) {
            override fun pulse(): Boolean = true
        })
    }
}