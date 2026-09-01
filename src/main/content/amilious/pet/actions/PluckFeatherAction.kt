package content.amilious.pet.actions

import content.amilious.ai.GigosPath
import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.node.entity.npc.NPC
import core.game.node.item.Item
import core.game.world.repository.Repository
import core.game.world.update.flag.context.Animation
import kotlin.random.Random

class PluckFeatherAction(rank: Int = 50) :
    PhasedCompanionAction<AmiliousMonkey, PluckFeatherAction.Phase>(
        "pluck", rank, Phase::class
    ) {

    enum class Phase { RUN, PLUCK, GLOAT }

    private var bird: NPC? = null
    private var runTicks = 0

    override fun priorityWeight(): Int = 6

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.lootEnabled()) return false
        if (actor.ownerInCombat()) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        if (!canHoldFeathers(actor, 1)) return false
        return findBird(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        bird = findBird(actor)
        runTicks = 0
        val t = bird ?: return
        actor.face(t)
        if (!GigosPath.walk(actor, t.location)) {
            bird = null
            rest(8)
        }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val t = bird
        if (t == null || !t.isActive) {
            return abort(actor, 10)
        }
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) {
            return abort(actor, 8)
        }
        if (!canHoldFeathers(actor, 1)) {
            return abort(actor, 12)
        }

        when (phase) {
            Phase.RUN -> {
                runTicks++
                actor.face(t)
                if (GigosPath.arrived(actor, t.location, 1.6)) {
                    GigosPath.stop(actor)
                    nextPhase()
                    return true
                }
                if (GigosPath.stuck(runTicks, 24)) {
                    return abort(actor, 12)
                }
                if (!actor.walkingQueue.isMoving) {
                    if (!GigosPath.walk(actor, t.location)) {
                        return abort(actor, 12)
                    }
                }
                return true
            }
            Phase.PLUCK -> {
                actor.face(t)
                actor.animate(Animation(MonkeyConfig.skinFor(actor.owner).attack))
                playAudio(actor.owner, sfxFor(t))
                val n = 1 + Random.nextInt(3)
                val got = takeFeathers(actor, n)
                if (got > 0) {
                    actor.addHunger(-MonkeyConfig.HUNGER_PLUCK)
                    sendMessage(
                        actor.owner,
                        "Gigos yanks $got feather${if (got == 1) "" else "s"} from the ${t.name.lowercase()}."
                    )
                }
                nextPhase()
                return true
            }
            Phase.GLOAT -> {
                actor.face(t)
                playAudio(actor.owner, MonkeyConfig.SFX_PLAYFUL)
                return abort(actor, Random.nextInt(12, 20))
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        GigosPath.stop(actor)
        bird = null
        rest(restTicks)
        return false
    }

    private fun findBird(actor: AmiliousMonkey): NPC? =
        Repository.npcs
            .filter { isBird(actor, it) }
            .filter { GigosPath.canReach(actor, it.location) }
            .minByOrNull { actor.location.getDistance(it.location) }

    private fun isBird(actor: AmiliousMonkey, npc: NPC): Boolean {
        if (npc === actor) return false
        if (npc is AmiliousMonkey) return false
        if (!npc.isActive) return false
        if (actor.location.getDistance(npc.location) > 8.0) return false
        if (npc.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        val n = npc.name.lowercase()
        return BIRDS.any { n.contains(it) }
    }

    private fun sfxFor(npc: NPC): Int {
        val n = npc.name.lowercase()
        return if (n.contains("duck") || n.contains("duckling")) {
            MonkeyConfig.SFX_DUCK
        } else {
            MonkeyConfig.SFX_CHICKEN
        }
    }

    private fun canHoldFeathers(actor: AmiliousMonkey, amount: Int): Boolean {
        val item = Item(MonkeyConfig.FEATHER_ID, amount)
        return actor.bag.hasSpaceFor(item)
    }

    private fun takeFeathers(actor: AmiliousMonkey, want: Int): Int {
        var n = want
        while (n > 0) {
            val item = Item(MonkeyConfig.FEATHER_ID, n)
            if (actor.bag.hasSpaceFor(item) && actor.bag.add(item)) {
                actor.saveBag()
                return n
            }
            n--
        }
        return 0
    }

    companion object {
        private val BIRDS = listOf("chicken", "duck", "duckling")
    }
}