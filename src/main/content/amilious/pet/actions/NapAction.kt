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

    enum class Phase { LIE, SLEEP, WAKE }

    private var slept = 0
    private var wakeTicks = 0

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
        if ((actor.ownerIdleTicks < 2 || actor.ownerInCombat()) && phase != Phase.WAKE) {
            return beginWake(actor)
        }
        when (phase) {
            Phase.LIE -> {
                nextPhase()
                return true
            }
            Phase.SLEEP -> {
                slept++
                if (slept % 8 == 0) {
                    actor.graphics(Graphics(349, 0))
                }
                if (slept >= 40) {
                    return beginWake(actor)
                }
                return true
            }
            Phase.WAKE -> {
                wakeTicks++
                if (wakeTicks < 4) return true
                finishStand(actor)
                rest(20)
                return false
            }
        }
    }

    private fun lieAnim(actor: AmiliousMonkey): Int {
        val skin = MonkeyConfig.skinFor(actor.owner)
        if (skin.sleep > 0) return skin.sleep
        return skin.death // 223 on Gigos — same as old ANIM_DEATH
    }

    private fun beginWake(actor: AmiliousMonkey): Boolean {
        val skin = MonkeyConfig.skinFor(actor.owner)
        actor.graphics(Graphics(-1))
        if (skin.sleep > 0 && skin.wake > 0) {
            actor.animator.forceAnimation(Animation(skin.wake))
            wakeTicks = 0
            phase = Phase.WAKE
            return true
        }
        finishStand(actor)
        rest(20)
        return false
    }

    private fun finishStand(actor: AmiliousMonkey) {
        actor.refreshPose()
        val standId = actor.definition?.standAnimation
            ?: MonkeyConfig.skinFor(actor.owner).stand
        if (standId > 0) {
            actor.animator.forceAnimation(Animation(standId))
        }
    }

}