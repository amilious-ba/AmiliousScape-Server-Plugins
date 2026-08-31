package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.world.map.Location
import core.game.world.map.RegionManager

class PickBananaTreeAction(rank: Int = 40) :
    PhasedCompanionAction<AmiliousMonkey, PickBananaTreeAction.Phase>(
        "pick-banana", rank, Phase::class
    ) {

    enum class Phase { WALK, PICK, HOLD }

    private var dest: Location? = null
    private var wait = 0
    private var walkTicks = 0
    private var picksOnThis = 0
    private val treeCd = HashMap<String, Int>()

    override fun cooldown(actor: AmiliousMonkey) {
        super.cooldown(actor)
        val it = treeCd.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.setValue(e.value - 1)
            if (e.value <= 0) it.remove()
        }
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.pickEnabled()) return false
        if (actor.ownerIdleTicks < IDLE_NEED) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_PICK) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return nearestTile(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        dest = nearestTile(actor)
        wait = 0
        walkTicks = 0
        picksOnThis = 0
        walkTo(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2) {
            rest(4)
            return false
        }
        val tile = dest ?: return false
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (actor.location.getDistance(tile) <= 2.0) {
                    nextPhase()
                    return true
                }
                if (walkTicks > 25) return false
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.PICK -> {
                if (actor.location.getDistance(tile) > 2.0) {
                    goToPhase(Phase.WALK)
                    walkTicks = 0
                    walkTo(actor)
                    return true
                }
                if (actor.hunger() < MonkeyConfig.HUNGER_PICK) {
                    rest(8)
                    return false
                }
                if (!actor.addBananasNoted(1)) {
                    sendMessage(actor.owner, "Gigos wants a banana but his pack is full.")
                    rest(25)
                    return false
                }
                actor.addHunger(-MonkeyConfig.HUNGER_PICK)
                playAudio(actor.owner, MonkeyConfig.SFX_OOK)
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos picks a banana.")
                picksOnThis++
                if (picksOnThis >= PICKS_PER_TREE) {
                    treeCd[key(tile)] = TREE_REST
                    rest(6)
                    return false
                }
                goToPhase(Phase.HOLD)
                wait = 3
                return true
            }
            Phase.HOLD -> {
                wait--
                if (wait > 0) return true
                goToPhase(Phase.PICK)
                return true
            }
        }
    }

    private fun walkTo(actor: AmiliousMonkey) {
        val tile = dest ?: return
        actor.pulseManager.clear()
        actor.pulseManager.run(object : MovementPulse(actor, tile) {
            override fun pulse(): Boolean = true
        })
    }

    private fun nearestTile(actor: AmiliousMonkey): Location? {
        val origin = actor.location
        val z = origin.z
        var best: Location? = null
        var bestDist = RANGE
        for (dx in -RANGE.toInt()..RANGE.toInt()) {
            for (dy in -RANGE.toInt()..RANGE.toInt()) {
                val loc = Location.create(origin.x + dx, origin.y + dy, z)
                if (treeCd.containsKey(key(loc))) continue
                val obj = RegionManager.getObject(z, loc.x, loc.y) ?: continue
                if (!isTree(obj.id, obj.name)) continue
                val d = origin.getDistance(obj.location)
                if (d < bestDist) {
                    bestDist = d
                    best = obj.location
                }
            }
        }
        return best
    }

    private fun isTree(id: Int, name: String): Boolean {
        if (id == 2078) return false
        if (id in PICKABLE) return true
        val n = name.lowercase()
        return n.contains("banana") && n.contains("tree")
    }

    private fun key(loc: Location) = "${loc.x},${loc.y},${loc.z}"

    companion object {
        private const val RANGE = 10.0
        private const val PICKS_PER_TREE = 3
        private const val TREE_REST = 40
        private const val IDLE_NEED = 8
        private val PICKABLE = intArrayOf(2073, 2074, 2075, 2076, 2077)
    }
}