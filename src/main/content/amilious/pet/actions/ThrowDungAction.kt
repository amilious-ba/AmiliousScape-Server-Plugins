package content.amilious.pet.actions


import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.rewardXP
import core.api.sendMessage
import core.game.node.entity.combat.ImpactHandler
import core.game.node.entity.impl.Projectile
import core.game.node.entity.npc.NPC
import core.game.node.entity.skill.Skills
import core.game.world.map.RegionManager
import core.game.world.repository.Repository
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics

class ThrowDungAction(rank: Int = 50) :
    PhasedCompanionAction<AmiliousMonkey, ThrowDungAction.Phase>(
        "dung", rank, Phase::class
    ) {

    enum class Phase { WIND, CAST, HIT }

    private var pending: NPC? = null

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.dungEnabled()) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_THROW) return false
        if (!actor.owner.properties.combatPulse.isAttacking) return false
        return target(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        pending = target(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val victim = pending
        if (victim == null || !victim.isActive) return false
        if (victim === actor || victim === actor.owner) return false
        if (actor.location.getDistance(victim.location) > 8) return false

        when (phase) {
            Phase.WIND -> {
                rest(MonkeyConfig.DUNG_COOLDOWN)
                actor.addHunger(-MonkeyConfig.HUNGER_THROW)
                actor.face(victim)
                actor.animate(Animation(MonkeyConfig.skinFor(actor.owner).attack))
                GigosHudPacket.send(actor.owner, actor)
                nextPhase()
                return true
            }
            Phase.CAST -> {
                try { //was 130
                    Projectile.create(actor, victim, 97, 12, 20, 41, 60, 5, 11).send()
                    playAudio(actor.owner, MonkeyConfig.SFX_OOK)
                } catch (_: Exception) {
                }
                nextPhase()
                return true
            }
            Phase.HIT -> {
                pending = null
                victim.graphics(Graphics(98)) //was 30
                val hit = 1 + (Math.random() * 3).toInt()
                victim.impactHandler.manualHit(actor, hit, ImpactHandler.HitsplatType.NORMAL)
                victim.properties.combatPulse.attack(actor)

                rewardXP(actor.owner, Skills.RANGE, hit * 4.0)
                rewardXP(actor.owner, Skills.HITPOINTS, hit * 1.33)

                sendMessage(actor.owner, "Gigos flings something foul. ${victim.name} looks furious.")
                return false
            }
        }
    }

    private fun target(actor: AmiliousMonkey): NPC? =
        Repository.npcs.firstOrNull {
            it !== actor &&
                    it !== actor.owner &&
                    it.isActive &&
                    !it.isInvisible &&
                    it.location.getDistance(actor.owner.location) <= 8 &&
                    it.properties.combatPulse.isAttacking
        }
}