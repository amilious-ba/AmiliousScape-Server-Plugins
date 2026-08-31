package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage

class SnackUntilFullAction(rank: Int = 10) :
    SimpleCompanionAction<AmiliousMonkey>("snack", rank) {

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.eatEnabled()) return false
        if (actor.ownerIdleTicks < 25) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        if (!actor.hasBanana()) return false
        return actor.hunger() + MonkeyConfig.HUNGER_BANANA <= MonkeyConfig.HUNGER_MAX
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (!actor.hasBanana()||!actor.eatEnabled()) {
            rest(8)
            return false
        }
        if (actor.hunger() + MonkeyConfig.HUNGER_BANANA > MonkeyConfig.HUNGER_MAX) {
            rest(12)
            return false
        }
        if (!actor.takeOneBanana()) {
            rest(8)
            return false
        }
        actor.addHunger(MonkeyConfig.HUNGER_BANANA)
        actor.saveBag()
        GigosHudPacket.send(actor.owner, actor)
        playAudio(actor.owner, MonkeyConfig.SFX_OOK)
        sendMessage(actor.owner, "Gigos snacks on a banana.")
        rest(5)
        return false
    }
}