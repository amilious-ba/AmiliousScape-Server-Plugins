package content.amilious.pet

import core.api.TickListener
import core.game.world.repository.Repository
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyTicker : TickListener {
    override fun tick() {
        for (npc in Repository.npcs) {
            if (npc !is AmiliousMonkey) continue
            val owner = npc.owner
            val live = owner.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            val ownerGone = !owner.isActive || owner.session == null
            if (ownerGone || live !== npc) {
                npc.clear()
            }
        }
        for (player in Repository.players) {
            val monkey = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (monkey == null) {
                GigosHudPacket.hide(player)
                continue
            }
            monkey.tickCompanion()
        }
    }
}