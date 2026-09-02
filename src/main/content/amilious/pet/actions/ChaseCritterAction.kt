package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
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
        if (!actor.brain.path.walk(actor, t.location)) {
            prey = null
            rest(8)
        }
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
                actor.face(t)
                val dest = t.location
                if (actor.brain.path.arrived(actor, dest, 1.6)) {
                    actor.brain.path.stop(actor)
                    nextPhase()
                    return true
                }
                if (actor.brain.path.reallyStuck(actor, dest) || actor.brain.path.stuck(runTicks, 24)) {
                    return abort(actor, 12)
                }
                // prey moves — always retarget, not only when the queue is empty
                if (!actor.brain.path.walk(actor, dest)) {
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
                playAudio(actor.owner, MonkeyConfig.SFX_PLAYFUL)
                return abort(actor, Random.nextInt(10, 18))
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        actor.brain.path.stop(actor)
        prey = null
        rest(restTicks)
        return false
    }

    private fun findPrey(actor: AmiliousMonkey): NPC? =
        Repository.npcs
            .filter { isPrey(actor, it) }
            .filter { actor.brain.path.canReach(actor, it.location) }
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