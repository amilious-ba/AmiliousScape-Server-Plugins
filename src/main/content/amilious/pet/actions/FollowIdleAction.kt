package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.actions.CompanionAction

class FollowIdleAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "follow"
    override fun canStart(m: AmiliousMonkey) = !m.pulseManager.hasPulseRunning()
    override fun tick(m: AmiliousMonkey): Boolean {
        m.followOwner()
        return false
    }
}