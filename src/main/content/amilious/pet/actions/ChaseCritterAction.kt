package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.game.interaction.MovementPulse
import core.game.node.entity.npc.NPC
import core.game.world.repository.Repository
import core.game.world.update.flag.context.Animation
import kotlin.random.Random

class ChaseCritterAction(rank: Int = 10) :
    PhasedCompanionAction<AmiliousMonkey, ChaseCritterAction.Phase>(
        "chase", rank, Phase::class
    ) {

    enum class Phase { RUN, POUNCE, GLOAT }

    private var prey: NPC? = null
    private var runTicks = 0

    override fun priorityWeight(): Int = 5

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (actor.ownerInCombat()) return false
        if (actor.ownerIdleTicks < 15) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return findPrey(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        prey = findPrey(actor)
        runTicks = 0
        val t = prey ?: return
        actor.face(t)
        walkTo(actor, t)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2 || actor.ownerInCombat()) {
            return abort(actor, 8)
        }
        val t = prey
        if (t == null || !t.isActive) {
            return abort(actor, 10)
        }
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) {
            return abort(actor, 8)
        }

        when (phase) {
            Phase.RUN -> {
                runTicks++
                val dist = actor.location.getDistance(t.location)
                if (dist <= 1.6) {
                    stopMove(actor)
                    nextPhase()
                    return true
                }
                val moving = actor.pulseManager.hasPulseRunning() || actor.walkingQueue.isMoving
                if (!moving) walkTo(actor, t)
                if (runTicks > 24) {
                    return abort(actor, 12)
                }
                return true
            }
            Phase.POUNCE -> {
                actor.face(t)
                actor.animate(Animation(MonkeyConfig.skinFor(actor.owner).attack))
                playAudio(actor.owner, MonkeyConfig.SFX_PLAYFUL)
                nextPhase()
                return true
            }
            Phase.GLOAT -> {
                actor.face(t)
                return abort(actor, Random.nextInt(10, 18))
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        stopMove(actor)
        prey = null
        rest(restTicks)
        return false
    }

    private fun stopMove(actor: AmiliousMonkey) {
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
    }

    private fun walkTo(actor: AmiliousMonkey, t: NPC) {
        stopMove(actor)
        actor.pulseManager.run(object : MovementPulse(actor, t) {
            override fun pulse(): Boolean =
                actor.location.getDistance(t.location) <= 1.6
        })
    }

    private fun findPrey(actor: AmiliousMonkey): NPC? =
        Repository.npcs
            .filter { isPrey(actor, it) }
            .minByOrNull { actor.location.getDistance(it.location) }

    private fun isPrey(actor: AmiliousMonkey, npc: NPC): Boolean {
        if (npc === actor) return false
        if (npc is AmiliousMonkey) return false
        if (!npc.isActive) return false
        if (actor.location.getDistance(npc.location) > 8.0) return false
        if (npc.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        val n = npc.name.lowercase()
        if (n.contains("familiar") || n.contains("pet")) return false
        return PREY.any { token ->
            n == token || n.endsWith(" $token") || n.startsWith("$token ")
        }
    }

    companion object {
        private val PREY = listOf(
            "rat", "crow", "seagull", "chicken", "duck", "duckling",
            "spider", "cockroach", "bat", "lizard", "gecko", "imp",
            "penguin", "rabbit", "frog", "toad", "snail", "crab"
        )
    }
}