package content.amilious.pet

import core.api.sendMessage
import core.game.component.CloseEvent
import core.game.component.Component
import core.game.container.Container
import core.game.container.access.InterfaceContainer
import core.game.interaction.MovementPulse
import core.game.interaction.Option
import core.game.node.entity.combat.ImpactHandler
import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.game.node.item.GroundItem
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Graphics
import core.game.world.repository.Repository

class AmiliousMonkey(val owner: Player) : NPC(MonkeyConfig.NPC_ID) {

    val bag = Container(MonkeyConfig.BOB_SIZE)
    private var dungWait = 0
    private var lootBusy = false
    private var eatWait = 0

    private fun dungEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_DUNG, true)

    fun hunger(): Int = owner.getAttribute(MonkeyConfig.ATTR_HUNGER, MonkeyConfig.HUNGER_MAX)

    fun setHunger(v: Int) {
        owner.setAttribute(MonkeyConfig.ATTR_HUNGER, v.coerceIn(0, MonkeyConfig.HUNGER_MAX))
    }

    fun addHunger(delta: Int): Int {
        setHunger(hunger() + delta)
        return hunger()
    }

    fun spawnAtOwner() {
        owner.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)?.let { old ->
            if (old !== this) old.dismiss()
        }

        try {
            val locals = core.game.world.map.RegionManager.getLocalNpcs(owner, 64)
            for (npc in locals) {
                if (npc !== this && npc is AmiliousMonkey && npc.owner == owner) {
                    npc.dismiss()
                }
            }
        } catch (_: Exception) {
        }

        location = owner.location.transform(1, 0, 0)
        init()
        name = "Gigos"
        isWalks = true
        interaction.set(Option("Pack", 0))
        interaction.set(Option("Talk-to", 1))
        interaction.set(Option("Loot", 2))
        interaction.set(Option("Dismiss", 3))
        loadBag()
        owner.setAttribute(MonkeyConfig.ATTR_ACTIVE, this)
        refreshMenu()
        sendMessage(owner, "Gigos hops down beside you.")
        followOwner()
    }

    fun refreshMenu() {
        val lootOn = owner.getAttribute(MonkeyConfig.ATTR_LOOT, true)
        NpcMenuPacket.send(
            owner, this, "Gigos",
            0 to "Pack",
            1 to "Talk-to",
            2 to if (lootOn) "Autoloot-on" else "Autoloot-off",
            3 to "Dismiss"
        )
        GigosHudPacket.send(owner, this)
    }

    fun dismiss() {
        saveBag()
        NpcMenuPacket.clear(owner, this)
        owner.removeAttribute(MonkeyConfig.ATTR_ACTIVE)
        GigosHudPacket.hide(owner)
        clear()
        sendMessage(owner, "Gigos scurries off. His pack is safe. ::monkey to call him back.")
    }

    fun tickCompanion() {
        val gone = !owner.isActive
                || owner.session == null
                || Repository.getPlayerByName(owner.name) == null
        if (gone) {
            dismiss()
            return
        }
        if (dungWait > 0) dungWait--
        if (location.getDistance(owner.location) > MonkeyConfig.FOLLOW_DIST) {
            properties.teleportLocation = owner.location
        }
        tryLoot()
        tryDung()
        tryFeed()
        if (!pulseManager.hasPulseRunning()) {
            followOwner()
        }
    }

    fun saveBag() {
        val encoded = bag.toArray()
            .filterNotNull()
            .joinToString(";") { "${it.id}:${it.amount}" }
        owner.setAttribute(MonkeyConfig.ATTR_BAG, encoded)
    }

    fun loadBag() {
        bag.clear()
        val raw = owner.getAttribute(MonkeyConfig.ATTR_BAG, "") ?: return
        if (raw.isBlank()) return
        for (part in raw.split(";")) {
            val bits = part.split(":")
            if (bits.size != 2) continue
            val id = bits[0].toIntOrNull() ?: continue
            val amt = bits[1].toIntOrNull() ?: continue
            if (id > 0 && amt > 0) bag.add(Item(id, amt))
        }
    }

    fun openBagUi() {
        owner.interfaceManager.open(Component(671)).setCloseEvent(CloseEvent { player, _ ->
            saveBag()
            player.interfaceManager.closeSingleTab()
            true
        })
        bag.shift()
        owner.interfaceManager.openSingleTab(Component(665))
        InterfaceContainer.generateItems(
            owner, owner.inventory.toArray(),
            arrayOf("Examine", "Store-X", "Store-All", "Store-10", "Store-5", "Store-1"),
            665, 0, 7, 4, 93
        )
        InterfaceContainer.generateItems(
            owner, bag.toArray(),
            arrayOf("Examine", "Withdraw-X", "Withdraw-All", "Withdraw-10", "Withdraw-5", "Withdraw-1"),
            671, 27, 5, 6, 30
        )
    }

    private fun followOwner() {
        pulseManager.run(object : MovementPulse(this, owner) {
            override fun pulse(): Boolean = false
        })
    }

    private fun lootEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_LOOT, true)

    private fun isOwnerDrop(gi: GroundItem): Boolean {
        if (gi.isRemoved) return false
        if (gi.droppedBy(owner)) return true
        if (gi.dropper == owner) return true
        return false
    }

    private fun tryLoot() {
        if (lootBusy) return
        if (!lootEnabled()) return
        if (hunger() < MonkeyConfig.HUNGER_LOOT) return
        if (bag.freeSlots() <= 0) return

        val piles = GroundItemManager.getItems()
            .filter { !it.isRemoved && it.location.getDistance(location) <= MonkeyConfig.LOOT_RANGE }
            .filter { isOwnerDrop(it) }
            .filter { bag.hasSpaceFor(Item(it.id, it.amount)) }
        val first = piles.minByOrNull { it.location.getDistance(location) } ?: return
        val tile = first.location

        fun scoopTile(): Boolean {
            val here = GroundItemManager.getItems()
                .filter { !it.isRemoved && it.location == tile }
                .filter { isOwnerDrop(it) }
            var any = false
            for (gi in here) {
                if (hunger() < MonkeyConfig.HUNGER_LOOT) break
                if (bag.freeSlots() <= 0) break
                val copy = Item(gi.id, gi.amount)
                if (!bag.hasSpaceFor(copy)) continue
                if (bag.add(copy)) {
                    GroundItemManager.destroy(gi)
                    addHunger(-MonkeyConfig.HUNGER_LOOT)
                    any = true
                    sendMessage(owner, "Gigos scoops up the ${copy.name.lowercase()}.")
                }
            }
            if (any) {
                saveBag()
                GigosHudPacket.send(owner, this@AmiliousMonkey)
            }
            return true
        }

        if (location.getDistance(tile) <= 1.5) {
            scoopTile()
            return
        }

        lootBusy = true
        pulseManager.run(object : MovementPulse(this, tile) {
            override fun pulse(): Boolean {
                lootBusy = false
                return scoopTile()
            }
        })
    }

    private fun tryDung() {
        if (dungWait > 0) return
        if (!dungEnabled()) return
        if (hunger() < MonkeyConfig.HUNGER_THROW) return
        if (!owner.properties.combatPulse.isAttacking) return
        val victim = RegionManager.getLocalNpcs(owner, 8)
            .firstOrNull {
                it !== this && it.isActive && !it.isInvisible &&
                        it.properties.combatPulse.isAttacking
            } ?: return
        if (victim === owner || victim === this) return
        if (location.getDistance(victim.location) > 8) return
        dungWait = MonkeyConfig.DUNG_COOLDOWN
        addHunger(-MonkeyConfig.HUNGER_THROW)
        face(victim)
        victim.graphics(Graphics(30))
        val hit = 1 + (Math.random() * 3).toInt()
        victim.impactHandler.manualHit(this, hit, core.game.node.entity.combat.ImpactHandler.HitsplatType.NORMAL)
        victim.properties.combatPulse.attack(this)
        GigosHudPacket.send(owner, this)
        sendMessage(owner, "Gigos flings something foul. ${victim.name} looks furious.")
    }

    private fun tryFeed() {
        if (eatWait > 0) {
            eatWait--
            return
        }
        if (hunger() >= 30) return

        fun eatBanana(): Boolean {
            val slot = bag.toArray().indexOfFirst { it != null && it.id == MonkeyConfig.BANANA_ID }
            if (slot < 0) return false
            val it = bag.get(slot) ?: return false
            if (!bag.remove(Item(MonkeyConfig.BANANA_ID, 1))) return false
            addHunger(MonkeyConfig.HUNGER_BANANA)
            saveBag()
            GigosHudPacket.send(owner, this)
            sendMessage(owner, "Gigos eats a banana.")
            eatWait = 5
            return true
        }

        if (eatBanana()) return

        val bone = bag.toArray().firstOrNull { it != null && isBone(it) } ?: return
        if (!bag.remove(Item(bone.id, 1))) return
        bag.add(Item(MonkeyConfig.BANANA_ID, 1))
        graphics(Graphics(141))
        sendMessage(owner, "Gigos turns the ${bone.name.lowercase()} into a banana.")
        eatBanana()
    }

    private fun isBone(item: Item): Boolean {
        val n = item.name.lowercase()
        return n == "bones" || n.endsWith(" bones") || n == "bone"
    }

}