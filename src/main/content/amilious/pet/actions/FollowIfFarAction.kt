package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import content.amilious.pet.actions.CompanionAction

class FollowIfFarAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "follow-far"
    override fun canStart(m: AmiliousMonkey) =
        m.location.getDistance(m.owner.location) > MonkeyConfig.FOLLOW_DIST
    override fun tick(m: AmiliousMonkey): Boolean {
        m.properties.teleportLocation = m.owner.location
        return false
    }
}