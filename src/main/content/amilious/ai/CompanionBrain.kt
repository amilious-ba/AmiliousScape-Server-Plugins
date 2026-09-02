package content.amilious.ai

import content.amilious.ai.CompanionPath
import core.game.node.entity.npc.NPC

/**
 * The `CompanionBrain` class is responsible for managing and coordinating the behavior of a companion entity.
 * It provides mechanisms for registering, executing, and managing various actions that the companion can perform.
 *
 * This class operates on an actor of type `T`, dispatching `CompanionAction`s and managing their lifecycle
 * (e.g., starting, ticking, transitioning, and interrupting actions). Additionally, it ensures only one action
 * is active at a time and handles the action's readiness and cooldown mechanics.
 *
 * @param T The type of the actor that this brain interacts with.
 * @property actor The actor instance that this companion brain controls and interacts with. It is passed as a
 * parameter to all companion actions and their respective lifecycle methods.
 */
class CompanionBrain<T>(private val actor: T) {

    //#region Private Variables ########################################################################################

    val path = CompanionPath()

    /**
     * A list of actions that the companion can perform. Each action implements the [ICompanionAction]
     * interface and represents a behavior that can be executed by the [CompanionBrain].
     *
     * The actions are stored for management, allowing operations such as adding new actions,
     * removing actions, iterating through them during a tick, and checking their readiness to execute.
     *
     * This property is used in the following scenarios:
     * - To determine the total number of managed actions.
     * - To add new actions via [addAction].
     * - To remove existing actions via [removeAction].
     * - To process each action’s cooldown and determine which action can start during [tick].
     */
    private val actions = ArrayList<ICompanionAction<T>>()

    /**
     * The currently active action being executed by the [CompanionBrain].
     *
     * This property holds a reference to the [ICompanionAction] that is currently running, if any.
     * It is used to manage the lifecycle and progression of the current action, including querying
     * its properties, executing its behavior, or interrupting it when necessary.
     *
     * The property is updated during the invocation of [tick] to transition between actions based
     * on their readiness and execution state. If no action is currently running, this property will
     * be set to `null`.
     *
     * This property is accessed in the following contexts:
     * - Retrieving details about the current action, such as its name, phase, or total phases.
     * - Transitioning between actions during the [tick] lifecycle.
     * - Interrupting or removing the current action via [interrupt] or [removeAction].
     *
     * Example scenarios:
     * - If [current] is `null`, the companion is considered idle.
     * - If an action is eligible to start, it is assigned to this property.
     */
    private var current: ICompanionAction<T>? = null

    /**
     * Indicates whether the currently active action should be stopped as soon as it is safe to do so.
     *
     * When set to `true`, the active action is allowed to complete its current phase or important
     * tasks before being stopped, ensuring a smooth and graceful transition. This is typically
     * controlled by the `requestStop` method and referenced during the action's execution cycle
     * to handle stop signals appropriately.
     */
    private var stopWhenAble = false

     private var stuckTicks = 0;

    //#endregion #######################################################################################################


    //#region Getters ##################################################################################################

    /**
     * Retrieves the name of the current action being performed by the companion.
     * If no action is currently active, "idle" is returned as a default value.
     *
     * @return The name of the current action, or "idle" if no action is active.
     */
    fun getCurrentActionName(): String = current?.name() ?: "idle"

    /**
     * Retrieves the currently active action being executed by the companion.
     * If no action is currently active, this method returns null.
     *
     * @return The currently active [ICompanionAction], or null if no action is active.
     */
    fun getCurrentAction(): ICompanionAction<T>? = current

    /**
     * Retrieves the current phase of the active companion action.
     * If no action is currently active, the default phase is 0.
     *
     * @return The current phase of the active companion action, or 0 if no action is active.
     */
    fun getCurrentPhase(): Int = current?.getPhase() ?: 0

    /**
     * Retrieves the total number of phases for the currently active companion action.
     * If no action is currently active, a default value of 0 is returned.
     *
     * @return The total number of phases for the active companion action, or 0 if no action is active.
     */
    fun getCurrentActionPhases(): Int = current?.getNumberPhases() ?: 0

    /**
     * Retrieves the name of the current phase of the active companion action.
     * If no action is currently active, "no phase" is returned as a default value.
     *
     * @return The name of the current action phase, or "no phase" if no action is active.
     */
    fun getCurrentActionPhaseName(): String = current?.getPhaseName() ?: "no phase";

    /**
     * Retrieves the total number of actions available in the companion's action list.
     *
     * @return The number of actions currently available.
     */
    fun getNumberOfActions(): Int = actions.size;

    /**
     * Determines if the companion brain is currently occupied with an action.
     *
     * This method checks whether there is an active action (`current`) being executed.
     * If `current` is not null, it implies that the companion brain is busy.
     *
     * @return `true` if there is an active action, `false` otherwise.
     */
    fun busy(): Boolean = current != null

    //#endregion #######################################################################################################


    //#region Action Methods ###########################################################################################

    /**
     * Adds a new companion action to the list of actions, maintaining the list sorted by priority.
     * If the new action has a higher priority than existing actions, it is inserted at the appropriate position.
     *
     * @param action The companion action to be added. The action must implement [ICompanionAction].
     * @return The updated instance of [CompanionBrain] with the action added.
     */
    fun addAction(action: ICompanionAction<T>): CompanionBrain<T> {
        val i = actions.indexOfFirst { it.priority() < action.priority() }
        if (i < 0) actions.add(action) else actions.add(i, action)
        return this
    }

    /**
     * Removes the specified companion action from the list of actions.
     * If the action is currently active, it interrupts the action before removal.
     *
     * @param action The companion action to be removed. Must implement [ICompanionAction].
     * @return `true` if the action was successfully removed, `false` if the action was not present in the list.
     */
    fun removeAction(action: ICompanionAction<T>): Boolean {
        if (!actions.contains(action)) return false
        if (current == action) interrupt()
        actions.remove(action)
        return true
    }

    //#endregion #######################################################################################################


    //#region Brain Actions ############################################################################################

    /**
     * Advances the state of the companion brain by performing a single tick cycle.
     *
     * This method coordinates the execution of companion actions, managing their transitions
     * and behavior based on their state and conditions. It follows several steps:
     *
     * 1. Invokes the `cooldown` method on all available actions to reduce their cooldown if applicable.
     * 2. Checks if there's a currently active action (`current`) and processes its `tick` method:
     *    - If the action completes or `stopWhenAble` is set, the current action is stopped.
     *    - If the action remains ongoing, the method exits early to prevent starting a new action.
     * 3. If no active action is ongoing, finds the next action that can start (`canStart` condition).
     * 4. Starts the next action by invoking its `start` method.
     * 5. Processes the `tick` method of the newly started action; if it cannot continue, resets the current action.
     *
     * This ensures that actions are executed sequentially and transition smoothly, based on defined conditions.
     */
    fun tick() {
        actions.forEach { it.cooldown(actor) }

        val npc = actor as? NPC
        if (npc != null) {
            path.noteMove(npc)
            val moving = npc.walkingQueue.isMoving || npc.pulseManager.hasPulseRunning()
            val walking = path.hasTarget()
            if (!moving && walking && current != null) {
                stuckTicks++
            } else {
                stuckTicks = 0
            }
            if (stuckTicks > 10) {
                release(npc)
                current = null
                stopWhenAble = false
            }
        }

        val running = current
        if (running != null) {
            val keep = running.tick(actor)
            if (!keep || stopWhenAble) {
                if (npc != null) release(npc)
                current = null
                stopWhenAble = false
            }
            if (keep && current != null) return
        }

        current = pickNext()
        if (current == null) {return}
        if (npc != null) release(npc)
        current?.start(actor)
        val next = current ?: return
        if (!next.tick(actor)) {
            if (npc != null) release(npc)
            current = null
        }
    }

    private fun release(npc: NPC) {
        path.stop(npc)
        npc.pulseManager.clear()
        npc.walkingQueue.reset()
        path.resetStuck()
        stuckTicks = 0
    }

    /**
     * Signals the companion brain to stop the currently active action when it is safe to do so.
     *
     * This method sets the `stopWhenAble` flag to `true`, which is checked during the
     * execution of the active action. The action will complete its current work if possible
     * before being stopped. This is useful for gracefully halting actions without abrupt interruptions.
     */
    fun requestStop() {
        stopWhenAble = true
    }

    /**
     * Generates a list of debug information strings representing the current state of the companion brain.
     *
     * The first entry in the list reflects the main status of the companion brain, including:
     * - The name of the currently active action, or "idle" if no action is active.
     * - The current phase of the active action, or a default value if no action is active.
     * - Whether the companion brain is "busy" based on its internal state.
     *
     * Subsequent entries contain information about each action in the action list, including:
     * - The priority of the action.
     * - The name of the action.
     * - The current phase of the action.
     *
     * @return A list of strings summarizing the current state of the companion brain and its actions.
     */
    fun debugLines(): List<String> {
        val lines = ArrayList<String>()
        lines.add(
            "current=${current?.name() ?: "idle"} phase=${current?.getPhaseName() ?: "-"} busy=${busy()}"
        )
        for (a in actions) {
            lines.add("  p=${a.priority()} ${a.name()} phase=${a.getPhaseName()}")
        }
        return lines
    }

    /**
     * Immediately interrupts the currently active companion action, if any.
     *
     * This method resets the state of the companion brain by performing the following:
     * - Clears the `current` active action, setting it to `null`.
     * - Sets the `stopWhenAble` flag to `false`, ensuring that any pending stop requests are disregarded.
     *
     * Use this method to abruptly halt the current action, even if it is in progress,
     * without waiting for it to complete or transition cleanly.
     */
    fun interrupt() {
        stopWhenAble = false
        current = null
    }

    //#endregion #######################################################################################################

    private fun pickNext(): ICompanionAction<T>? {
        val ready = actions.filter { it.canStart(actor) }
        if (ready.isEmpty()) return null

        val bands = ready.groupBy { it.priority() }
        val bestPri = bands.keys.maxOrNull() ?: return null
        val pool = bands[bestPri] ?: return null

        val total = pool.sumOf { it.priorityWeight().coerceAtLeast(1) }
        var roll = kotlin.random.Random.nextInt(total)
        val startRoll = roll
        for (a in pool) {
            roll -= a.priorityWeight().coerceAtLeast(1)
            if (roll < 0) {
                println("[brain] pri=$bestPri bag=${pool.joinToString(",") { "${it.name()}x${it.priorityWeight()}" }} roll=$startRoll/${total} -> ${a.name()}")
                return a
            }
        }
        return pool.last()
    }

}