package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.entity.combat.ImpactHandler
import core.game.node.entity.impl.Projectile
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Graphics

class ThrowDungAction : CompanionAction<AmiliousMonkey> {
    private var dungWait = 0

    private var phase = 0
    private var pending: core.game.node.entity.npc.NPC? = null

    override fun name() = "dung"

    override fun cooldown(m: AmiliousMonkey) {
        if (dungWait > 0) dungWait--
    }

    override fun canStart(m: AmiliousMonkey): Boolean {
        if (dungWait > 0 || !m.dungEnabled()) return false
        if (m.hunger() < MonkeyConfig.HUNGER_THROW) return false
        return m.owner.properties.combatPulse.isAttacking
    }

    override fun start(m: AmiliousMonkey) {
        phase = 0
        pending = null
    }

    override fun tick(m: AmiliousMonkey): Boolean {
        when (phase) {
            0 -> {
                val victim = RegionManager.getLocalNpcs(m.owner, 8)
                    .firstOrNull {
                        it !== m && it.isActive && !it.isInvisible &&
                                it.properties.combatPulse.isAttacking
                    } ?: return false
                if (victim === m.owner || victim === m) return false
                if (m.location.getDistance(victim.location) > 8) return false
                pending = victim
                dungWait = MonkeyConfig.DUNG_COOLDOWN
                m.addHunger(-MonkeyConfig.HUNGER_THROW)
                m.face(victim)
                m.animate(m.properties.attackAnimation)
                GigosHudPacket.send(m.owner, m)
                phase = 1
                return true
            }
            1, 2 -> {
                phase++
                return true
            }
            3 -> {
                val victim = pending ?: return false
                try {
                    Projectile.create(m, victim, 130).send()
                } catch (_: Exception) {
                }
                phase = 4
                return true
            }
            4, 5 -> {
                phase++
                return true
            }
            else -> {
                val victim = pending ?: return false
                pending = null
                if (!victim.isActive) return false
                victim.graphics(Graphics(30))
                val hit = 1 + (Math.random() * 3).toInt()
                victim.impactHandler.manualHit(m, hit, ImpactHandler.HitsplatType.NORMAL)
                victim.properties.combatPulse.attack(m)
                sendMessage(m.owner, "Gigos flings something foul. ${victim.name} looks furious.")
                return false
            }
        }
    }

}