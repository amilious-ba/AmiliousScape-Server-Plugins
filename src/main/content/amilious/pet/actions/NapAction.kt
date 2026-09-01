package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics

class NapAction(rank: Int = 10) :
    PhasedCompanionAction<AmiliousMonkey, NapAction.Phase>(
        "nap", rank, Phase::class
    ) {

    enum class Phase { LIE, SLEEP }

    private var slept = 0

    override fun priorityWeight(): Int = 1

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (actor.ownerInCombat()) return false
        if (actor.ownerIdleTicks < 25) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return true
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        slept = 0
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
        val lie = lieAnim(actor)
        if (lie > 0) actor.animate(Animation(lie))
        actor.graphics(Graphics(277, 20))
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2 || actor.ownerInCombat()) {
            stand(actor)
            rest(8)
            return false
        }
        when (phase) {
            Phase.LIE -> {
                nextPhase()
                return true
            }
            Phase.SLEEP -> {
                slept++
                val skin = MonkeyConfig.skinFor(actor.owner)
                if (skin.sleep > 0 && slept % 8 == 0) {
                    actor.animate(Animation(skin.sleep))
                }
                if (slept % 8 == 0) actor.graphics(Graphics(349, 0))
                if (slept >= 40) {
                    stand(actor)
                    rest(20)
                    return false
                }
                return true
            }
        }
    }

    private fun lieAnim(actor: AmiliousMonkey): Int {
        val skin = MonkeyConfig.skinFor(actor.owner)
        if (skin.sleep > 0) return skin.sleep
        if (skin.deathOnSleep) return skin.death
        return 0
    }

    private fun stand(actor: AmiliousMonkey) {
        val skin = MonkeyConfig.skinFor(actor.owner)
        actor.graphics(Graphics(-1))
        actor.walkingQueue.reset()
        actor.pulseManager.clear()

        if (skin.deathOnSleep) {
            actor.reviveFromSleep()
            return
        }

        when {
            skin.wake > 0 -> actor.animate(Animation(skin.wake))
            skin.stand > 0 -> actor.animate(Animation(skin.stand))
            else -> actor.animate(Animation(-1))
        }
    }

}