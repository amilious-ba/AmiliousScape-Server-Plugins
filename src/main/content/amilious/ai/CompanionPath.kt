package content.amilious.ai

import core.game.global.action.DoorActionHandler
import core.game.node.entity.npc.NPC
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.Point
import core.game.world.map.RegionManager
import core.game.world.map.path.Path
import core.game.world.map.path.Pathfinder

class CompanionPath {

    private val GATE_SCAN = 8
    private var lastX = Int.MIN_VALUE
    private var lastY = Int.MIN_VALUE
    private var stillTicks = 0
    private var openWait = 0
    private var lastDoor: Location? = null

    data class Route(val path: Path, val end: Location) {
        fun exact(dest: Location) = end.getDistance(dest) <= 1.0
    }

    fun arrived(actor: NPC, dest: Location, dist: Double = 1.5): Boolean =
        actor.location.getDistance(dest) <= dist

    fun stop(actor: NPC) {
        openWait = 0
        lastDoor = null
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
    }

    fun takeOver(actor: NPC) = stop(actor)

    fun stopPath(actor: NPC) {
        actor.walkingQueue.reset()
    }

    fun route(actor: NPC, dest: Location): Route? {
        val path = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        val last = lastPoint(path) ?: return null
        return Route(path, Location.create(last.x, last.y, actor.location.z))
    }

    fun canReachExact(actor: NPC, dest: Location): Boolean {
        val r = route(actor, dest) ?: return false
        return r.path.isSuccessful && r.exact(dest)
    }

    fun canReach(actor: NPC, dest: Location): Boolean {
        if (canReachExact(actor, dest)) return true
        val path = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        if (path.isSuccessful || path.isMoveNear) return true
        return findGate(actor, dest) != null
    }

    fun walk(actor: NPC, dest: Location, dist: Double = 1.5): Boolean {
        if (arrived(actor, dest, dist)) {
            stopPath(actor)
            return true
        }

        if (openWait > 0) {
            openWait--
            val door = lastDoor
            if (openWait == 0 && door != null && actor.location.getDistance(door) <= 1.0) {
                val through = throughFrom(actor.location, door, dest)
                actor.properties.teleportLocation = through
                lastDoor = null
                resetStuck()
            }
            return true
        }

        val gate = findGate(actor, dest)
        if (gate != null && isBetween(actor.location, gate.location, dest)) {
            val through = throughFrom(actor.location, gate.location, dest)
            if (isClosed(gate)) {
                if (actor.location.getDistance(gate.location) <= 1.5) {
                    if (!openGate(actor, gate)) return false
                    lastDoor = gate.location
                    openWait = 2
                    return true
                }
                return walkNear(actor, standTile(actor, gate))
            }
            return walkNear(actor, through)
        }

        val path = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        if (path.isSuccessful || path.isMoveNear) {
            actor.walkingQueue.reset()
            path.walk(actor)
            return true
        }
        return false
    }

    fun stuck(walkTicks: Int, limit: Int = 24): Boolean = walkTicks > limit

    fun noteMove(actor: NPC) {
        val x = actor.location.x
        val y = actor.location.y
        if (x == lastX && y == lastY) stillTicks++ else stillTicks = 0
        lastX = x
        lastY = y
    }

    fun reallyStuck(actor: NPC, dest: Location, dist: Double = 1.5): Boolean {
        noteMove(actor)
        if (actor.walkingQueue.isMoving) return false
        if (actor.pulseManager.hasPulseRunning()) return false
        if (stillTicks < 6) return false
        return !arrived(actor, dest, dist)
    }

    fun findGate(actor: NPC, dest: Location): Scenery? {
        var best: Scenery? = null
        var bestScore = Double.MAX_VALUE
        val z = actor.location.z
        val ax = actor.location.x
        val ay = actor.location.y
        for (dx in -GATE_SCAN..GATE_SCAN) {
            for (dy in -GATE_SCAN..GATE_SCAN) {
                val loc = Location.create(ax + dx, ay + dy, z)
                val obj = RegionManager.getObject(loc) ?: continue
                if (!isGate(obj)) continue
                if (!isBetween(actor.location, loc, dest)) continue
                val score = loc.getDistance(actor.location) + loc.getDistance(dest) * 0.25
                if (score < bestScore) {
                    bestScore = score
                    best = obj
                }
            }
        }
        return best
    }

    fun resetStuck() {
        stillTicks = 0
        lastX = Int.MIN_VALUE
        lastY = Int.MIN_VALUE
    }

    private fun walkNear(actor: NPC, dest: Location): Boolean {
        val p = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        if (!p.isSuccessful && !p.isMoveNear) return false
        actor.walkingQueue.reset()
        p.walk(actor)
        return true
    }

    /** Tile next to the gate on our side — never the hinge tile. */
    private fun standTile(actor: NPC, gate: Scenery): Location {
        val g = gate.location
        val a = actor.location
        val sx = when {
            a.x < g.x -> -1
            a.x > g.x -> 1
            else -> 0
        }
        val sy = when {
            a.y < g.y -> -1
            a.y > g.y -> 1
            else -> 0
        }
        return Location.create(g.x + sx, g.y + sy, g.z)
    }

    /** One tile on the dest side of the gate. */
    private fun throughFrom(from: Location, gate: Location, dest: Location): Location {
        val dx = when {
            dest.x > gate.x -> 1
            dest.x < gate.x -> -1
            from.x <= gate.x -> 1
            else -> -1
        }
        val dy = when {
            dest.y > gate.y -> 1
            dest.y < gate.y -> -1
            from.y <= gate.y -> 1
            else -> -1
        }
        val stepX = kotlin.math.abs(dest.x - from.x) >= kotlin.math.abs(dest.y - from.y)
        return if (stepX) {
            Location.create(gate.x + dx, gate.y, gate.z)
        } else {
            Location.create(gate.x, gate.y + dy, gate.z)
        }
    }

    private fun isBetween(from: Location, mid: Location, dest: Location): Boolean {
        val direct = from.getDistance(dest)
        val via = from.getDistance(mid) + mid.getDistance(dest)
        return via <= direct + 3.0
    }

    private fun lastPoint(path: Path): Point? {
        val pts = path.points ?: return null
        if (pts.isEmpty()) return null
        return try {
            pts.last()
        } catch (_: Exception) {
            var last: Point? = null
            for (p in pts) last = p
            last
        }
    }

    private fun isGate(obj: Scenery): Boolean {
        val name = obj.name.lowercase()
        if (name.contains("locked")) return false
        if (!(name.contains("gate") || name.contains("door"))) return false
        val opts = obj.definition.options ?: return false
        return opts.any { it != null && (it.equals("open", true) || it.equals("close", true)) }
    }

    private fun isClosed(obj: Scenery): Boolean {
        val opts = obj.definition.options ?: return false
        return opts.any { it != null && it.equals("open", true) }
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