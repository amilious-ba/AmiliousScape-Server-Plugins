package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.game.interaction.MovementPulse
import core.game.world.map.Location
import kotlin.random.Random

class WanderAction : CompanionAction<AmiliousMonkey> {

    private var lastX = Int.MIN_VALUE
    private var lastY = Int.MIN_VALUE
    private var idleTicks = 0
    private var wait = 0
    private var phase = 0
    private var dest: Location? = null

    override fun name() = "wander"

    override fun cooldown(actor: AmiliousMonkey) {
        val loc = actor.owner.location
        if (loc.x == lastX && loc.y == lastY) {
            idleTicks++
        } else {
            idleTicks = 0
            lastX = loc.x
            lastY = loc.y
        }
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
        dest = actor.owner.location.transform(dx, dy,0)
        phase = 0
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2) return false
        val tile = dest ?: return false
        return when (phase) {
            0 -> {
                actor.pulseManager.run(object : MovementPulse(actor, tile) {
                    override fun pulse(): Boolean = true
                })
                phase = 1
                true
            }
            1 -> {
                if (actor.location.getDistance(tile) > 1.2 && actor.pulseManager.hasPulseRunning()) {
                    return true
                }
                actor.face(actor.owner)
                wait = Random.nextInt(8, 20)
                phase = 2
                true
            }
            else -> false
        }
    }
}