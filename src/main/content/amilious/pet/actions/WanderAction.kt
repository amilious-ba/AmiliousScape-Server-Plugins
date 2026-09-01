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

    private var walkTicks = 0

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (actor.ownerIdleTicks < 25) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return true
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        walkTicks = 0
        dest = pickDest(actor)
        val tile = dest
        if (tile != null && actor.location.getDistance(tile) > 0.6) {
            actor.pulseManager.clear()
            actor.walkingQueue.reset()
            actor.pulseManager.run(object : MovementPulse(actor, tile) {
                override fun pulse(): Boolean = arrived(actor, tile)
            })
        }
    }

    private fun pickDest(actor: AmiliousMonkey): Location? {
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
            return tile
        }
        return null
    }

    private fun arrived(actor: AmiliousMonkey, tile: Location): Boolean =
        actor.location.x == tile.x && actor.location.y == tile.y && actor.location.z == tile.z

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2) return false
        val tile = dest ?: return false

        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (arrived(actor, tile)) {
                    nextPhase()
                    return true
                }
                val moving = actor.pulseManager.hasPulseRunning() || actor.walkingQueue.isMoving
                if (!moving) {
                    actor.pulseManager.clear()
                    actor.walkingQueue.reset()
                    actor.pulseManager.run(object : MovementPulse(actor, tile) {
                        override fun pulse(): Boolean = arrived(actor, tile)
                    })
                }
                if (walkTicks > 20) {
                    rest(8)
                    return false
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

}