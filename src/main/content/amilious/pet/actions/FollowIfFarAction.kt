package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig

class FollowIfFarAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "follow-far"
    override fun canStart(actor: AmiliousMonkey) =
        actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST
    override fun tick(actor: AmiliousMonkey): Boolean {
        actor.properties.teleportLocation = actor.owner.location
        return false
    }
}