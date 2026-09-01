package content.amilious.ai

import core.game.global.action.DoorActionHandler
import core.game.node.entity.npc.NPC
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.map.path.Pathfinder

object GigosPath {

    private const val GATE_SCAN = 8

    fun arrived(actor: NPC, dest: Location, dist: Double = 1.5): Boolean =
        actor.location.getDistance(dest) <= dist

    fun stop(actor: NPC) {
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
    }

    fun canReach(actor: NPC, dest: Location): Boolean {
        val path = smart(actor, dest)
        if (path.isSuccessful || path.isMoveNear) return true
        return findClosedGate(actor, dest) != null
    }

    /**
     * Player-style SMART walk. If blocked by a normal gate/door, walk to it and open.
     */
    fun walk(actor: NPC, dest: Location, dist: Double = 1.5): Boolean {
        val path = smart(actor, dest)
        if (path.isSuccessful || path.isMoveNear) {
            stop(actor)
            path.walk(actor)
            return true
        }

        val gate = findClosedGate(actor, dest) ?: return false
        if (arrived(actor, gate.location, 1.5)) {
            return openGate(actor, gate)
        }
        val toGate = smart(actor, gate.location)
        if (!toGate.isSuccessful && !toGate.isMoveNear) return false
        stop(actor)
        toGate.walk(actor)
        return true
    }

    fun stuck(walkTicks: Int, limit: Int = 24): Boolean = walkTicks > limit

    private fun smart(actor: NPC, dest: Location) =
        Pathfinder.find(actor, dest, true, Pathfinder.SMART)

    private fun findClosedGate(actor: NPC, dest: Location): Scenery? {
        var best: Scenery? = null
        var bestScore = Double.MAX_VALUE
        val z = actor.location.z
        val ax = actor.location.x
        val ay = actor.location.y
        for (dx in -GATE_SCAN..GATE_SCAN) {
            for (dy in -GATE_SCAN..GATE_SCAN) {
                val loc = Location.create(ax + dx, ay + dy, z)
                val obj = RegionManager.getObject(loc) ?: continue
                if (!isOpenableGate(obj)) continue
                val score = obj.location.getDistance(actor.location) +
                        obj.location.getDistance(dest) * 0.25
                if (score < bestScore) {
                    bestScore = score
                    best = obj
                }
            }
        }
        return best
    }

    private fun isOpenableGate(obj: Scenery): Boolean {
        val name = obj.name.lowercase()
        if (!name.contains("gate") && !name.contains("door")) return false
        if (name.contains("locked")) return false
        val opts = obj.definition.options ?: return false
        return opts.any { it != null && it.equals("open", ignoreCase = true) }
    }

    private fun openGate(actor: NPC, gate: Scenery): Boolean {
        return try {
            DoorActionHandler.handleAutowalkDoor(actor, gate)
            true
        } catch (_: Exception) {
            false
        }
    }
}