package content.amilious.pet.actions

import content.amilious.ai.SimpleCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.rewardXP
import core.api.sendMessage
import core.game.node.item.Item
import core.game.node.entity.skill.Skills
import core.game.world.update.flag.context.Graphics

class BonesToBananasAction(rank: Int = 20) :
    SimpleCompanionAction<AmiliousMonkey>("b2b", rank) {

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.b2bEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_B2B) return false
        if (actor.hasBanana()) return false
        return actor.bag.toArray().any { it != null && actor.isBone(it) }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val bone = actor.bag.toArray().firstOrNull { it != null && actor.isBone(it) } ?: return false
        val one = Item(bone.id, 1)
        if (!actor.bag.remove(one)) return false
        if (!actor.addBananasNoted(1)) {
            actor.bag.add(one)
            rest(8)
            return false
        }
        actor.addHunger(-MonkeyConfig.HUNGER_B2B)
        actor.graphics(Graphics(141))
        rewardXP(actor.owner, Skills.MAGIC, 25.0)
        actor.saveBag()
        GigosHudPacket.send(actor.owner, actor)
        sendMessage(actor.owner, "Gigos turns the ${bone.name.lowercase()} into a banana.")
        rest(5)
        return false
    }
}