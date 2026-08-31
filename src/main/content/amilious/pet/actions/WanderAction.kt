package content.amilious.pet.actions


import kotlin.random.Random
import core.game.world.map.Location
import content.amilious.pet.MonkeyConfig
import core.game.interaction.MovementPulse
import content.amilious.ai.ICompanionAction
import content.amilious.pet.AmiliousMonkey


class WanderAction(private val rank: Int = 10) : ICompanionAction<AmiliousMonkey> {

    private enum class Phase { WALK, HOLD }

    private var phase = Phase.WALK
    private var dest: Location? = null
    private var wait = 0

    override fun name() = "wander"

    override fun priority() = rank

    override fun getNumberPhases() = Phase.entries.size

    override fun getPhase() = phase.ordinal

    override fun getPhaseName() = phase.name

    override fun cooldown(actor: AmiliousMonkey) {
        if (wait > 0) wait--
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (wait > 0) return false
        if (actor.ownerIdleTicks < 25) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return true
    }

    override fun start(actor: AmiliousMonkey) {
        var dx = 0
        var dy = 0
        while (dx == 0 && dy == 0) {
            dx = Random.nextInt(-2, 3)
            dy = Random.nextInt(-2, 3)
        }
        dest = actor.owner.location.transform(dx, dy, 0)
        phase = Phase.WALK
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2) return false
        val tile = dest ?: return false
        when (phase) {
            Phase.WALK -> {
                actor.pulseManager.clear()
                actor.pulseManager.run(object : MovementPulse(actor, tile) {
                    override fun pulse(): Boolean = true
                })
                phase = Phase.HOLD
                return true
            }
            Phase.HOLD -> {
                if (actor.location.getDistance(tile) > 1.2 && actor.pulseManager.hasPulseRunning()) {
                    return true
                }
                actor.face(actor.owner)
                wait = Random.nextInt(8, 20)
                return false
            }
        }
    }
}