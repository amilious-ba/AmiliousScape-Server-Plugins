package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.game.world.map.Location

class FollowIdleAction(rank: Int = 10) :
    SimpleCompanionAction<AmiliousMonkey>("follow", rank) {

    private var last: Location? = null
    private var still = 0

    override fun getPhaseName() = "follow"

    override fun canStart(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        return dist > 1.5 && dist <= MonkeyConfig.FOLLOW_DIST
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        last = actor.location
        still = 0
        actor.followOwner()
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        if (dist > MonkeyConfig.FOLLOW_DIST) {
            stopFollow(actor)
            return false
        }
        if (dist <= 1.5) {
            stopFollow(actor)
            return false
        }

        val here = actor.location
        if (last != null && here.x == last!!.x && here.y == last!!.y && here.z == last!!.z) {
            still++
        } else {
            still = 0
            last = here
        }

        if (still >= 8) {
            actor.snapToOwner()
            return false
        }

        if (!actor.pulseManager.hasPulseRunning() && !actor.walkingQueue.isMoving) {
            actor.followOwner()
        }
        return true
    }

    private fun stopFollow(actor: AmiliousMonkey) {
        actor.brain.path.stop(actor)
        actor.pulseManager.clear()
        still = 0
    }

}