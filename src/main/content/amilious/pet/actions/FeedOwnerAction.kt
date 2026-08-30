package content.amilious.pet.actions

import content.amilious.food.FoodFilter
import content.amilious.food.FoodTable
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.node.item.Item
import core.game.world.update.flag.context.Animation

class FeedOwnerAction : CompanionAction<AmiliousMonkey> {

    private enum class Phase { WALK, FEED }

    private var phase = Phase.WALK
    private var cool = 0
    private var walkTicks = 0

    override fun name() = "feed"

    override fun cooldown(actor: AmiliousMonkey) {
        if (cool > 0) cool--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.feedEnabled()) return false
        if (cool > 0) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        val missing = FoodTable.missingHp(actor.owner)
        val max = actor.owner.skills.maximumLifepoints
        if (max <= 0 || actor.owner.skills.lifepoints * 2 > max) return false
        return FoodTable.bestFor(actor.bag, missing, FoodFilter.PLAIN, allowWaste = false) != null
    }

    override fun start(actor: AmiliousMonkey) {
        phase = Phase.WALK
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
                    phase = Phase.FEED
                    return true
                }
                if (walkTicks > 25) return false
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.FEED -> {
                val missing = FoodTable.missingHp(actor.owner)
                val max = actor.owner.skills.maximumLifepoints
                if (max <= 0 || actor.owner.skills.lifepoints * 2 > max) return false
                val pick = FoodTable.bestFor(actor.bag, missing, FoodFilter.PLAIN, allowWaste = false)
                if (pick == null) {
                    cool = 8
                    return false
                }
                val (item, entry) = pick
                val bite = Item(item.id, 1)
                if (!actor.bag.remove(bite)) {
                    cool = 8
                    return false
                }
                if (entry.leftover > 0) actor.bag.add(Item(entry.leftover, 1))
                actor.owner.animate(Animation(829))
                actor.owner.skills.heal(entry.healMin)
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos fed you ${a(entry.name)}.")
                cool = 5
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