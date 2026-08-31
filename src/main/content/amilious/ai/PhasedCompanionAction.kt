package content.amilious.ai


import kotlin.reflect.KClass


/**
 * Represents an abstract companion action with multiple phases, where each phase
 * is represented by an enum type. This class can manage and transition between
 * these phases during the action's lifecycle.
 *
 * @param T The type of the actor performing this action.
 * @param P The enum type representing the phases of the action. Must extend from `Enum<P>`.
 * @param actionName The name of the action.
 * @param rank The rank or priority of the action. Defaults to `0` if not specified.
 * @param type The class of the enum type used to represent the phases.
 */
abstract class PhasedCompanionAction<T, P : Enum<P>>(actionName: String, rank: Int = 0, type: KClass<P>) :
    SimpleCompanionAction<T>(actionName, rank) {

    //#region Properties ###############################################################################################

    /**
     * Holds all the enum constants of the phase type represented by `P`.
     *
     * This array is initialized using the `enumConstants` property of the `Class` instance
     * representing the enum type `P`. It provides access to all possible phases in the order
     * they are declared within the enum definition.
     */
    private val all: Array<P> = type.java.enumConstants

    /**
     * Represents the current phase of the companion action, managed as an enum constant of type `P`.
     *
     * This variable is initialized with the first enum constant from the `P` type, as defined in the
     * associated enum class. The phase can transition between different enum constants during the
     * action's lifecycle, either explicitly or sequentially.
     *
     * Visibility is restricted to subclasses, as the variable is marked `protected`, allowing
     * controlled access and modification within the scope of inherited classes.
     */
    protected var phase: P = all.first()

    //#endregion #######################################################################################################


    //#region Override Methods #########################################################################################

    /**
     * Retrieves the number of phases available in the current context.
     *
     * @return the total number of phases.
     */
    override fun getNumberPhases(): Int = all.size

    /**
     * Retrieves the ordinal value of the current phase.
     *
     * @return the integer value representing the current phase's position in its enumeration.
     */
    override fun getPhase(): Int = phase.ordinal

    /**
     * Retrieves the name of the current phase in lowercase.
     *
     * @return the lowercase string representation of the current phase name.
     */
    override fun getPhaseName(): String = phase.name.lowercase()

    /**
     * Initializes the action by setting the current phase to the first phase in the sequence.
     *
     * @param actor the actor associated with the action being started
     */
    override fun start(actor: T) {
        phase = all.first()
    }

    //#endregion #######################################################################################################


    //#region Protected Methods ########################################################################################

    /**
     * Progresses the current phase to the next one in the sequence, if available.
     *
     * @return true if the phase was successfully advanced to the next phase, false if the current phase is the last one.
     */
    protected fun nextPhase(): Boolean {
        val i = phase.ordinal + 1
        if (i >= all.size) return false
        phase = all[i]
        return true
    }

    /**
     * Updates the current phase to the specified next phase.
     *
     * @param next the new phase to set as the current phase.
     */
    protected fun goToPhase(next: P) {
        phase = next
    }

    /**
     * Resets the current phase to the initial phase in the sequence.
     *
     * This method sets the `phase` field to the first phase in the `all` collection,
     * effectively restarting the phase progression.
     */
    protected fun resetPhases() {
        phase = all.first()
    }

    /**
     * Determines whether the current phase is the last phase in the sequence.
     *
     * This method checks if the current phase's position in the sequence
     * (determined by its ordinal value) is equal to or greater than the
     * last position in the sequence, as defined by the size of the phase collection.
     *
     * @return true if the current phase is the last in the sequence, false otherwise.
     */
    protected fun isLastPhase(): Boolean = phase.ordinal >= all.size - 1

    //#endregion #######################################################################################################

}