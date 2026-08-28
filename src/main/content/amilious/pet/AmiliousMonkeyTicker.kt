package content.amilious.pet

import core.api.TickListener
import core.game.world.repository.Repository
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyTicker : TickListener {
    override fun tick() {
        for (player in Repository.players) {
            val monkey = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE) ?: continue
            monkey.tickCompanion()
        }
    }
}