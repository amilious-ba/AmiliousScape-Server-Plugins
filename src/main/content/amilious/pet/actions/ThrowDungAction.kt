package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.entity.combat.ImpactHandler
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Graphics

class ThrowDungAction : CompanionAction<AmiliousMonkey> {
    private var dungWait = 0

    override fun name() = "dung"

    override fun cooldown(m: AmiliousMonkey) {
        if (dungWait > 0) dungWait--
    }

    override fun canStart(m: AmiliousMonkey): Boolean {
        if (dungWait > 0 || !m.dungEnabled()) return false
        if (m.hunger() < MonkeyConfig.HUNGER_THROW) return false
        return m.owner.properties.combatPulse.isAttacking
    }

    override fun tick(m: AmiliousMonkey): Boolean {
        val victim = RegionManager.getLocalNpcs(m.owner, 8)
            .firstOrNull {
                it !== m && it.isActive && !it.isInvisible &&
                        it.properties.combatPulse.isAttacking
            } ?: return false
        if (victim === m.owner || victim === m) return false
        if (m.location.getDistance(victim.location) > 8) return false
        dungWait = MonkeyConfig.DUNG_COOLDOWN
        m.addHunger(-MonkeyConfig.HUNGER_THROW)
        m.face(victim)
        victim.graphics(Graphics(30))
        val hit = 1 + (Math.random() * 3).toInt()
        victim.impactHandler.manualHit(m, hit, ImpactHandler.HitsplatType.NORMAL)
        victim.properties.combatPulse.attack(m)
        GigosHudPacket.send(m.owner, m)
        sendMessage(m.owner, "Gigos flings something foul. ${victim.name} looks furious.")
        return false
    }
}