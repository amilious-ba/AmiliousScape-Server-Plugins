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

    override fun cooldown(m: AmiliousMonkey) {
        val loc = m.owner.location
        if (loc.x == lastX && loc.y == lastY) {
            idleTicks++
        } else {
            idleTicks = 0
            lastX = loc.x
            lastY = loc.y
        }
        if (wait > 0) wait--
    }

    override fun canStart(m: AmiliousMonkey): Boolean {
        if (wait > 0) return false
        if (idleTicks < 15) return false
        if (m.owner.properties.combatPulse.isAttacking) return false
        if (m.location.getDistance(m.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return true
    }

    override fun start(m: AmiliousMonkey) {
        var dx = 0
        var dy = 0
        while (dx == 0 && dy == 0) {
            dx = Random.nextInt(-2, 3)
            dy = Random.nextInt(-2, 3)
        }
        dest = m.owner.location.transform(dx, dy,0)
        phase = 0
    }

    override fun tick(m: AmiliousMonkey): Boolean {
        val tile = dest ?: return false
        return when (phase) {
            0 -> {
                m.pulseManager.run(object : MovementPulse(m, tile) {
                    override fun pulse(): Boolean = true
                })
                phase = 1
                true
            }
            1 -> {
                if (m.location.getDistance(tile) > 1.2 && m.pulseManager.hasPulseRunning()) {
                    return true
                }
                m.face(m.owner)
                wait = Random.nextInt(8, 20)
                phase = 2
                true
            }
            else -> false
        }
    }
}