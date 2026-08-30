package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.node.item.Item

class EatBananaAction : CompanionAction<AmiliousMonkey> {

    private var eatWait = 0

    override fun name() = "eat"

    override fun cooldown(actor: AmiliousMonkey) {
        if (eatWait > 0) eatWait--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.eatEnabled()) return false
        if (eatWait > 0) return false
        if (actor.hunger() >= 30) return false
        return actor.hasBanana()
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (!actor.takeOneBanana()) return false
        actor.addHunger(MonkeyConfig.HUNGER_BANANA)
        actor.saveBag()
        GigosHudPacket.send(actor.owner, actor)
        sendMessage(actor.owner, "Gigos eats a banana.")
        playAudio(actor.owner, MonkeyConfig.SFX_OOK)
        eatWait = 5
        return false
    }

}