package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Animation

class PickBananaTreeAction : CompanionAction<AmiliousMonkey> {

    private enum class Phase { WALK, PICK, HOLD }

    private var phase = Phase.WALK
    private var target: Scenery? = null
    private var wait = 0
    private var cool = 0
    private var lastOwner = Location.create(0, 0, 0)
    private var idleTicks = 0
    private var picksOnThis = 0
    private val rest = HashMap<Location, Int>()

    override fun name() = "pick-banana"

    override fun cooldown(actor: AmiliousMonkey) {
        if (cool > 0) cool--
        val here = actor.owner.location
        if (here == lastOwner) idleTicks++ else idleTicks = 0
        lastOwner = here
        val it = rest.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.setValue(e.value - 1)
            if (e.value <= 0) it.remove()
        }
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (cool > 0) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return nearest(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        target = nearest(actor)
        phase = Phase.WALK
        wait = 0
        picksOnThis = 0
        sendMessage(actor.owner, "Gigos heads for a banana tree.")
        walkTo(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val tree = target ?: return false
        when (phase) {
            Phase.WALK -> {
                if (actor.location.getDistance(tree.location) <= 2.0) {
                    phase = Phase.PICK
                    return true
                }
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.PICK -> {
                if (actor.location.getDistance(tree.location) > 2.0) {
                    phase = Phase.WALK
                    walkTo(actor)
                    return true
                }
                if (!actor.addBananasNoted(1)) {
                    sendMessage(actor.owner, "Gigos wants a banana but his pack is full.")
                    cool = 25
                    return false
                }
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos picks a banana.")
                actor.animate(Animation(827))
                picksOnThis++
                if (picksOnThis >= PICKS_PER_TREE) {
                    rest[tree.location] = TREE_REST
                    target = nearest(actor)
                    picksOnThis = 0
                    if (target == null) {
                        cool = 8
                        return false
                    }
                    phase = Phase.WALK
                    walkTo(actor)
                    return true
                }
                phase = Phase.HOLD
                wait = if (idleTicks >= 8) 2 else 5
                return true
            }
            Phase.HOLD -> {
                wait--
                if (wait > 0) return true
                phase = Phase.PICK
                return true
            }
        }
    }

    private fun walkTo(actor: AmiliousMonkey) {
        val tree = target ?: return
        actor.pulseManager.clear()
        actor.pulseManager.run(object : MovementPulse(actor, tree) {
            override fun pulse(): Boolean = true
        })
    }

    private fun nearest(actor: AmiliousMonkey): Scenery? {
        val origin = actor.location
        val z = origin.z
        var best: Scenery? = null
        var bestDist = RANGE
        for (dx in -RANGE.toInt()..RANGE.toInt()) {
            for (dy in -RANGE.toInt()..RANGE.toInt()) {
                val loc = Location.create(origin.x + dx, origin.y + dy, z)
                if (rest.containsKey(loc)) continue
                val obj = RegionManager.getObject(z, loc.x, loc.y) ?: continue
                if (!isTree(obj)) continue
                val d = origin.getDistance(obj.location)
                if (d < bestDist) {
                    bestDist = d
                    best = obj
                }
            }
        }
        return best
    }

    private fun isTree(obj: Scenery): Boolean {
        if (obj.id == 2078) return false
        if (obj.id in PICKABLE) return true
        val n = obj.name.lowercase()
        return n.contains("banana") && n.contains("tree")
    }

    companion object {
        private const val RANGE = 8.0
        private const val PICKS_PER_TREE = 3
        private const val TREE_REST = 40
        private val PICKABLE = intArrayOf(2073, 2074, 2075, 2076, 2077)
    }
}