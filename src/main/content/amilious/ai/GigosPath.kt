package content.amilious.ai

import core.game.global.action.DoorActionHandler
import core.game.node.entity.npc.NPC
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.map.path.Path
import core.game.world.map.path.Pathfinder

object GigosPath {

    private const val GATE_SCAN = 8

    data class Route(val path: Path, val end: Location) {
        fun exact(dest: Location) = end.getDistance(dest) <= 1.0
    }

    fun arrived(actor: NPC, dest: Location, dist: Double = 1.5): Boolean =
        actor.location.getDistance(dest) <= dist

    fun stop(actor: NPC) {
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
    }

    fun route(actor: NPC, dest: Location): Route? {
        val path = smart(actor, dest)
        val last = lastPoint(path) ?: return null
        return Route(path, Location.create(last.x, last.y, actor.location.z))
    }

    /** True when SMART ends on the dest tile (open path), same as a minimap click that does not snap. */
    fun canReachExact(actor: NPC, dest: Location): Boolean {
        val r = route(actor, dest) ?: return false
        return r.exact(dest)
    }

    fun canReach(actor: NPC, dest: Location): Boolean {
        if (canReachExact(actor, dest)) return true
        return findClosedGate(actor, dest) != null
    }

    /**
     * Walk like a minimap click.
     * Exact end → walk there.
     * Snapped end → walk to / open a closed gate, then the next walk will be exact.
     */
    fun walk(actor: NPC, dest: Location, dist: Double = 1.5): Boolean {
        val r = route(actor, dest) ?: return false

        if (r.exact(dest)) {
            stop(actor)
            r.path.walk(actor)
            return true
        }

        val gate = findClosedGate(actor, dest) ?: return false
        if (arrived(actor, gate.location, 1.5)) {
            return openGate(actor, gate)
        }
        val toGate = route(actor, gate.location) ?: return false
        stop(actor)
        toGate.path.walk(actor)
        return true
    }

    fun stuck(walkTicks: Int, limit: Int = 24): Boolean = walkTicks > limit

    fun findClosedGate(actor: NPC, dest: Location): Scenery? {
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

    private fun smart(actor: NPC, dest: Location) =
        Pathfinder.find(actor, dest, true, Pathfinder.SMART)

    private fun lastPoint(path: Path): core.game.world.map.Point? {
        val pts = path.points ?: return null
        if (pts.isEmpty()) return null
        return try {
            pts.last()
        } catch (_: Exception) {
            var last: core.game.world.map.Point? = null
            for (p in pts) last = p
            last
        }
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