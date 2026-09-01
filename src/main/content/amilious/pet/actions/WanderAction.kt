package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.game.interaction.MovementPulse
import core.game.world.map.Location
import kotlin.random.Random

class WanderAction(rank: Int = 10) :
    PhasedCompanionAction<AmiliousMonkey, WanderAction.Phase>(
        "wander", rank, Phase::class
    ) {

    enum class Phase { WALK, HOLD }

    private var dest: Location? = null

    override fun priorityWeight(): Int = 6

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (actor.ownerIdleTicks < 25) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return true
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        var dx = 0
        var dy = 0
        while (dx == 0 && dy == 0) {
            dx = Random.nextInt(-2, 3)
            dy = Random.nextInt(-2, 3)
        }
        dest = actor.owner.location.transform(dx, dy, 0)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2) return false
        val tile = dest ?: return false

        when (phase) {
            Phase.WALK -> {
                if (!actor.pulseManager.hasPulseRunning()) {
                    actor.pulseManager.clear()
                    actor.pulseManager.run(object : MovementPulse(actor, tile) {
                        override fun pulse(): Boolean {
                            return actor.location.getDistance(tile) <= 1.2
                        }
                    })
                }
                if (actor.location.getDistance(tile) <= 1.2) {
                    nextPhase()
                }
                return true
            }
            Phase.HOLD -> {
                actor.face(actor.owner)
                if (!holding() && ready()) {
                    // first time in HOLD — stand still this many ticks
                    holdFor(Random.nextInt(8, 20))
                }
                if (holding()) {
                    return true
                }
                rest(Random.nextInt(8, 20))
                return false
            }
        }
    }
}