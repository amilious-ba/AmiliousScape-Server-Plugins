package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.node.entity.combat.graves.GraveController
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.world.map.Location

class GraveLootAction(rank: Int = 110) :
    PhasedCompanionAction<AmiliousMonkey, GraveLootAction.Phase>(
        "grave", rank, Phase::class
    ) {

    enum class Phase { WALK, SCOOP }

    private var dest: Location? = null
    private var walkTicks = 0

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        val grave = GraveController.activeGraves[actor.owner.details.uid] ?: return false
        return grave.getItems().any { !it.isRemoved }
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        dest = GraveController.activeGraves[actor.owner.details.uid]?.location
        walkTicks = 0
        walkTo(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val tile = dest ?: return false
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (actor.location.getDistance(tile) <= 1.5) {
                    nextPhase()
                    return true
                }
                if (walkTicks > 40) {
                    actor.properties.teleportLocation = tile
                    nextPhase()
                    return true
                }
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.SCOOP -> {
                val grave = GraveController.activeGraves[actor.owner.details.uid]
                var any = false
                if (grave != null) {
                    for (gi in grave.getItems()) {
                        if (gi.isRemoved) continue
                        val copy = Item(gi.id, gi.amount)
                        if (!actor.bag.hasSpaceFor(copy)) continue
                        if (actor.bag.add(copy)) {
                            GroundItemManager.destroy(gi)
                            any = true
                        }
                    }
                }
                if (any) {
                    actor.saveBag()
                    GigosHudPacket.send(actor.owner, actor)
                    sendMessage(actor.owner, "Gigos looted your grave.")
                }
                rest(5)
                return false
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
}