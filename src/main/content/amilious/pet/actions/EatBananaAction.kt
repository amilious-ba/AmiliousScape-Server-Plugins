package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import content.amilious.pet.actions.CompanionAction
import core.api.sendMessage
import core.game.node.item.Item

class EatBananaAction : CompanionAction<AmiliousMonkey> {

    private var eatWait = 0

    override fun name() = "eat"

    override fun cooldown(m: AmiliousMonkey) {
        if (eatWait > 0) eatWait--
    }

    override fun canStart(m: AmiliousMonkey): Boolean {
        if (!m.eatEnabled()) return false
        if (eatWait > 0) return false
        if (m.hunger() >= 30) return false
        return m.bag.toArray().any { it != null && it.id == MonkeyConfig.BANANA_ID }
    }

    override fun tick(m: AmiliousMonkey): Boolean {
        if (!m.bag.remove(Item(MonkeyConfig.BANANA_ID, 1))) return false
        m.addHunger(MonkeyConfig.HUNGER_BANANA)
        m.saveBag()
        GigosHudPacket.send(m.owner, m)
        sendMessage(m.owner, "Gigos eats a banana.")
        eatWait = 5
        return false
    }
}