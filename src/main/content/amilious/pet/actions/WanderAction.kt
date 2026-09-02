package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.game.world.map.Location
import kotlin.random.Random

class WanderAction(rank: Int = 10) :
    PhasedCompanionAction<AmiliousMonkey, WanderAction.Phase>(
        "wander", rank, Phase::class
    ) {

    enum class Phase { WALK, HOLD }

    private var dest: Location? = null
    private var walkTicks = 0

    override fun priorityWeight(): Int = 6

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (actor.ownerIdleTicks < 25) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return pickDest(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        walkTicks = 0
        dest = pickDest(actor)
        val tile = dest ?: return
        if (actor.location.getDistance(tile) > 0.6) {
            if (!actor.brain.path.walk(actor, tile)) {
                rest(8)
            }
        }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val path = actor.brain.path
        if (actor.ownerIdleTicks < 2) {
            return abort(actor, 4)
        }
        val tile = dest ?: return abort(actor, 8)

        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (path.arrived(actor, tile, 0.6)) {
                    path.stop(actor)
                    nextPhase()
                    return true
                }
                if (path.reallyStuck(actor, tile) || path.stuck(walkTicks, 20)) {
                    return abort(actor, 8)
                }
                if (!path.walk(actor, tile)) {
                    return abort(actor, 8)
                }
                return true
            }
            Phase.HOLD -> {
                actor.face(actor.owner)
                if (hold <= 0) {
                    holdFor(Random.nextInt(8, 20))
                }
                if (holding()) return true
                rest(Random.nextInt(8, 20))
                return false
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        actor.brain.path.stop(actor)
        rest(restTicks)
        return false
    }

    private fun pickDest(actor: AmiliousMonkey): Location? {
        val path = actor.brain.path
        val origin = actor.owner.location
        for (i in 0 until 16) {
            var dx = 0
            var dy = 0
            while (dx == 0 && dy == 0) {
                dx = Random.nextInt(-5, 6)
                dy = Random.nextInt(-5, 6)
            }
            val tile = origin.transform(dx, dy, 0)
            if (blocked(tile)) continue
            if (origin.getDistance(tile) > MonkeyConfig.FOLLOW_DIST) continue
            if (actor.location.getDistance(tile) < 1.5) continue
            if (!path.canReachExact(actor, tile)) continue
            return tile
        }
        return null
    }
}