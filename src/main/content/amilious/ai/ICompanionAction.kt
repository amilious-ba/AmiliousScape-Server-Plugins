package content.amilious.ai

/**
 * Interface defining a generic companion action with multiple phases and priority,
 * allowing for execution and control of an actor's behavior through start, tick, and cooldown steps.
 *
 * @param T The type of the actor performing the action.
 */
interface ICompanionAction<T> {

    /**
     * Retrieves the name associated with the companion action.
     *
     * @return The name of the companion action as a string.
     */
    fun name(): String

    /**
     * Retrieves the priority level of the companion action.
     *
     * @return The priority level as an integer.
     */
    fun priority(): Int

    fun priorityWeight(): Int

    /**
     * Retrieves the total number of phases involved in the companion action.
     *
     * @return The total number of phases as an integer.
     */
    fun getNumberPhases(): Int

    /**
     * Retrieves the current phase of the companion action.
     *
     * @return The current phase as an integer.
     */
    fun getPhase(): Int

    /**
     * Retrieves the name of the current phase of the action.
     *
     * @return The name of the current phase as a string.
     */
    fun getPhaseName(): String

    /**
     * Determines whether the action can start for the given actor.
     *
     * @param actor The actor performing the action.
     * @return True if the action can start, otherwise false.
     */
    fun canStart(actor: T): Boolean

    /**
     * Initiates the action for the given actor.
     *
     * @param actor The actor performing the action.
     */
    fun start(actor: T) {}

    /**
     * Called during each tick to execute a phase of the companion action.
     *
     * @param actor The actor performing the action.
     * @return True if the action should continue, false if it is completed.
     */
    fun tick(actor: T): Boolean

    /**
     * Triggers the cooldown phase for the given actor.
     *
     * @param actor The actor for which the cooldown action will be executed.
     */
    fun cooldown(actor: T) {}

}