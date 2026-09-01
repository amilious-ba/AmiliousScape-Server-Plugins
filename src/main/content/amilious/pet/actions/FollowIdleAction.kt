package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig

class FollowIdleAction(rank: Int = 0) :
    SimpleCompanionAction<AmiliousMonkey>("follow", rank) {

    override fun getPhaseName() = "follow"

    override fun canStart(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        if (dist <= 1.5 || dist > MonkeyConfig.FOLLOW_DIST) return false
        return actor.ownerIdleTicks < 25
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        if (dist > MonkeyConfig.FOLLOW_DIST) return false
        if (dist <= 1.5) return false
        if (actor.ownerIdleTicks >= 25) return false
        if (!actor.pulseManager.hasPulseRunning()) actor.followOwner()
        return true
    }

}