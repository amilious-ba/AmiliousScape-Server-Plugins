package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage

class EatBananaAction(rank: Int = 30) :
    SimpleCompanionAction<AmiliousMonkey>("eat", rank) {

    override fun getPhaseName() = "eat"

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.eatEnabled()) return false
        if (actor.hunger() >= MonkeyConfig.HUNGER_EAT_BELOW) return false
        return actor.hasBanana()
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (!actor.takeOneBanana()) return false
        actor.addHunger(MonkeyConfig.HUNGER_BANANA)
        actor.saveBag()
        GigosHudPacket.send(actor.owner, actor)
        sendMessage(actor.owner, "Gigos eats a banana.")
        playAudio(actor.owner, MonkeyConfig.SFX_OOK)
        rest(5)
        return false
    }
}