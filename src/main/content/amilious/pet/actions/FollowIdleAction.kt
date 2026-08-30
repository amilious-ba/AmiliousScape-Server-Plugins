package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig

class FollowIdleAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "follow"

    override fun canStart(actor: AmiliousMonkey) =
        actor.location.getDistance(actor.owner.location) > 1.5 &&
                actor.location.getDistance(actor.owner.location) <= MonkeyConfig.FOLLOW_DIST

    override fun tick(actor: AmiliousMonkey): Boolean {
        val dist = actor.location.getDistance(actor.owner.location)
        if (dist > MonkeyConfig.FOLLOW_DIST) return false
        if (dist <= 1.5) return false
        if (!actor.pulseManager.hasPulseRunning()) actor.followOwner()
        return true
    }
}