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
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
        val land = actor.owner.location.transform(1, 0, 0)
        actor.location = land
        actor.properties.teleportLocation = land
        actor.refreshPose()
        return false
    }
}