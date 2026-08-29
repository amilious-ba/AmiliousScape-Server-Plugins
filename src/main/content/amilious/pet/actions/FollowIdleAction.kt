package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey

class FollowIdleAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "follow"
    override fun canStart(actor: AmiliousMonkey) = !actor.pulseManager.hasPulseRunning()
    override fun tick(actor: AmiliousMonkey): Boolean {
        actor.followOwner()
        return false
    }
}