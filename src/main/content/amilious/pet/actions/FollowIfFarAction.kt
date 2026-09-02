package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig

class FollowIfFarAction(rank: Int = 100) :
    SimpleCompanionAction<AmiliousMonkey>("follow-far", rank) {

    override fun getPhaseName() = "teleport"

    override fun canStart(actor: AmiliousMonkey): Boolean =
        actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST

    override fun tick(actor: AmiliousMonkey): Boolean {
        actor.brain.path.stop(actor)
        actor.properties.teleportLocation = actor.owner.location
        return false
    }
}