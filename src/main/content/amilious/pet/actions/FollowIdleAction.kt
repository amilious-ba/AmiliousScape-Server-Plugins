package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.game.world.map.Location

class FollowIdleAction(rank: Int = 10) :
    SimpleCompanionAction<AmiliousMonkey>("follow", rank) {

    private var lastX = Int.MIN_VALUE
    private var lastY = Int.MIN_VALUE
    private var lastZ = Int.MIN_VALUE
    private var still = 0

    override fun getPhaseName() = "follow"

    override fun canStart(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        return dist > 1.5 && dist <= MonkeyConfig.FOLLOW_DIST
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        mark(actor.location)
        still = 0
        actor.followOwner()
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        if (dist > MonkeyConfig.FOLLOW_DIST) {
            stopFollow(actor)
            return false
        }

        if (dist <= 1.5 && actor.ownerIdleTicks >= STOP_IDLE) {
            stopFollow(actor)
            return false
        }

        val here = actor.location
        if (here.x == lastX && here.y == lastY && here.z == lastZ) {
            still++
        } else {
            still = 0
            mark(here)
        }

        val blocked = still >= 5 ||
                (!actor.walkingQueue.isMoving && !actor.pulseManager.hasPulseRunning())
        if (dist > 3.0 && blocked) {
            actor.snapToOwner()
            return false
        }

        if (dist > 1.5) {
            if (!actor.pulseManager.hasPulseRunning() && !actor.walkingQueue.isMoving) {
                actor.followOwner()
            }
        } else {
            actor.brain.path.stop(actor)
            actor.pulseManager.clear()
        }
        return true
    }

    private fun mark(loc: Location) {
        lastX = loc.x
        lastY = loc.y
        lastZ = loc.z
    }

    private fun stopFollow(actor: AmiliousMonkey) {
        actor.brain.path.stop(actor)
        actor.pulseManager.clear()
        still = 0
    }

    companion object {
        private const val STOP_IDLE = 16
    }
}