package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.item.Item
import core.game.world.update.flag.context.Graphics

class BonesToBananasAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "b2b"
    override fun canStart(m: AmiliousMonkey): Boolean {
        if (!m.b2bEnabled()) return false
        if (m.hunger() < MonkeyConfig.HUNGER_B2B) return false
        if (m.bag.toArray().any { it != null && it.id == MonkeyConfig.BANANA_ID }) return false
        return m.bag.toArray().any { it != null && m.isBone(it) }
    }
    override fun tick(m: AmiliousMonkey): Boolean {
        val bone = m.bag.toArray().firstOrNull { it != null && m.isBone(it) } ?: return false
        if (!m.bag.remove(Item(bone.id, 1))) return false
        m.bag.add(Item(MonkeyConfig.BANANA_ID, 1))
        m.addHunger(-MonkeyConfig.HUNGER_B2B)
        m.graphics(Graphics(141))
        m.saveBag()
        GigosHudPacket.send(m.owner, m)
        sendMessage(m.owner, "Gigos turns the ${bone.name.lowercase()} into a banana.")
        return false
    }
}