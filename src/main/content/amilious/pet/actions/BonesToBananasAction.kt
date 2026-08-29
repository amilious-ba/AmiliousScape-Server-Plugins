package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.item.Item
import core.game.world.update.flag.context.Graphics

class BonesToBananasAction : CompanionAction<AmiliousMonkey> {
    override fun name() = "b2b"
    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.b2bEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_B2B) return false
        if (actor.bag.toArray().any { it != null && it.id == MonkeyConfig.BANANA_ID }) return false
        return actor.bag.toArray().any { it != null && actor.isBone(it) }
    }
    override fun tick(actor: AmiliousMonkey): Boolean {
        val bone = actor.bag.toArray().firstOrNull { it != null && actor.isBone(it) } ?: return false
        if (!actor.bag.remove(Item(bone.id, 1))) return false
        actor.bag.add(Item(MonkeyConfig.BANANA_ID, 1))
        actor.addHunger(-MonkeyConfig.HUNGER_B2B)
        actor.graphics(Graphics(141))
        actor.saveBag()
        GigosHudPacket.send(actor.owner, actor)
        sendMessage(actor.owner, "Gigos turns the ${bone.name.lowercase()} into a banana.")
        return false
    }
}