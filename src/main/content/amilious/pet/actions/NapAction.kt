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
        actor.animate(Animation(MonkeyConfig.ANIM_DEATH))
        actor.graphics(Graphics(277, 20))
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2 || actor.ownerInCombat()) {
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
                if (slept % 8 == 0) {
                    actor.graphics(Graphics(277, -4))
                }
                if (slept >= 40) {
                    rest(20)
                    return false
                }
                return true
            }
        }
    }
}