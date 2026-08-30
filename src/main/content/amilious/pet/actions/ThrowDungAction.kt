package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.entity.combat.ImpactHandler
import core.game.node.entity.impl.Projectile
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics

class ThrowDungAction : CompanionAction<AmiliousMonkey> {
    private var dungWait = 0
    private var phase = 0
    private var pending: core.game.node.entity.npc.NPC? = null

    override fun name() = "dung"

    override fun cooldown(actor: AmiliousMonkey) {
        if (dungWait > 0) dungWait--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (dungWait > 0 || !actor.dungEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_THROW) return false
        return actor.owner.properties.combatPulse.isAttacking
    }

    override fun start(actor: AmiliousMonkey) {
        phase = 0
        pending = null
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        when (phase) {
            0 -> {
                val victim = RegionManager.getLocalNpcs(actor.owner, 8)
                    .firstOrNull {
                        it !== actor && it.isActive && !it.isInvisible &&
                                it.properties.combatPulse.isAttacking
                    } ?: return false
                if (victim === actor.owner || victim === actor) return false
                if (actor.location.getDistance(victim.location) > 8) return false
                pending = victim
                dungWait = MonkeyConfig.DUNG_COOLDOWN
                actor.addHunger(-MonkeyConfig.HUNGER_THROW)
                actor.face(victim)
                try {
                    Projectile.create(actor, victim, 130).send()
                } catch (_: Exception) {
                }
                GigosHudPacket.send(actor.owner, actor)
                phase = 1
                return true
            }
            1 -> {
                actor.animate(Animation(MonkeyConfig.ANIM_ATTACK))
                phase = 2
                return true
            }
            else -> {
                val victim = pending ?: return false
                pending = null
                if (!victim.isActive) return false
                victim.graphics(Graphics(30))
                val hit = 1 + (Math.random() * 3).toInt()
                victim.impactHandler.manualHit(actor, hit, ImpactHandler.HitsplatType.NORMAL)
                victim.properties.combatPulse.attack(actor)
                sendMessage(actor.owner, "Gigos flings something foul. ${victim.name} looks furious.")
                return false
            }
        }
    }
}