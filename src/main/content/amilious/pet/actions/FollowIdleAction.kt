package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig

class FollowIdleAction(rank: Int = 20) :
    SimpleCompanionAction<AmiliousMonkey>("follow", rank) {

    override fun getPhaseName() = "follow"

    override fun canStart(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        return dist > 1.5 && dist <= MonkeyConfig.FOLLOW_DIST
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val path = actor.brain.path
        val dest = actor.owner.location
        val dist = actor.location.getDistance(dest)

        if (dist > MonkeyConfig.FOLLOW_DIST) {
            path.stop(actor)
            return false
        }
        if (dist <= 1.5) {
            path.stop(actor)
            return false
        }

        if (!path.walk(actor, dest)) {
            if (!actor.pulseManager.hasPulseRunning()) {
                actor.followOwner()
            }
        }
        return true
    }
}