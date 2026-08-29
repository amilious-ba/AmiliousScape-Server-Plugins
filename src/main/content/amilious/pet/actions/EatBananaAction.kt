package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
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
        return actor.bag.toArray().any { it != null && it.id == MonkeyConfig.BANANA_ID }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (!actor.bag.remove(Item(MonkeyConfig.BANANA_ID, 1))) return false
        actor.addHunger(MonkeyConfig.HUNGER_BANANA)
        actor.saveBag()
        GigosHudPacket.send(actor.owner, actor)
        sendMessage(actor.owner, "Gigos eats a banana.")
        eatWait = 5
        return false
    }
}