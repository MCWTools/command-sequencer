package khoa.commandsequencer.client.script;

import java.util.List;
import java.util.function.Consumer;

/**
 * Drives Run Next Action / Reset Script execution for a single selected Script,
 * per spec section 13 (Execution Architecture).
 *
 * This class does not know how to actually run a Minecraft command - it is handed
 * a {@code commandExecutor} callback that does the real dispatch (see CommandDispatcher
 * usage in ScriptExecutorClient). That keeps this class trivially testable and keeps
 * Minecraft/Fabric API usage in one place.
 */
public class ActionRunner {

	public enum State {
		READY,      // at command 0, nothing run yet (or just reset)
		RUNNING,    // 0 < currentRunIndex < run.size(), mid-sequence
		FINISHED,   // last run command executed, Loop/Auto Reset both off
		RESETTING   // Auto Reset is currently replaying the reset[] list
	}

	private Script script;
	private int currentRunIndex = 0;
	private State state = State.READY;

	private final Consumer<String> commandExecutor;

	public ActionRunner(Consumer<String> commandExecutor) {
		this.commandExecutor = commandExecutor;
	}

	public void setScript(Script script) {
		this.script = script;
		resetIndexOnly();
	}

	public Script getScript() {
		return script;
	}

	public State getState() {
		return state;
	}

	public int getCurrentRunIndex() {
		return currentRunIndex;
	}

	/**
	 * The command that will be executed on the *next* Run Next Action press,
	 * or null if Finished / no script selected. Used by the Next Action HUD (spec section 9).
	 */
	public String peekNextCommand() {
		if (script == null) {
			return null;
		}
		List<String> run = script.getRunCommands();
		if (state == State.FINISHED || run.isEmpty()) {
			return null;
		}
		if (currentRunIndex >= run.size()) {
			return null;
		}
		return run.get(currentRunIndex);
	}

	/**
	 * Executes exactly one Run command, per spec section 7: this method runs at most
	 * one command per call, regardless of Loop / Auto Reset state.
	 */
	public void runNextAction() {
		if (script == null) {
			return;
		}
		List<String> run = script.getRunCommands();
		if (run.isEmpty()) {
			return;
		}

		if (state == State.FINISHED) {
			// Spec section 5 (default mode): further presses do nothing until Reset Script.
			return;
		}
		if (state == State.RESETTING) {
			// Reset is auto-playing; Run Next Action has no effect mid-reset.
			return;
		}

		commandExecutor.accept(run.get(currentRunIndex));
		currentRunIndex++;

		if (currentRunIndex >= run.size()) {
			// Last Run command was just executed - behavior depends on Loop / Auto Reset.
			if (script.isLoop()) {
				currentRunIndex = 0;
				state = State.READY;
			} else if (script.isAutoReset()) {
				runResetSequence();
				currentRunIndex = 0;
				state = State.READY;
			} else {
				state = State.FINISHED;
			}
		} else {
			state = State.RUNNING;
		}
	}

	/**
	 * Manual Reset Script keybind (spec section 8): runs the full reset[] list and
	 * returns Run to command 0, regardless of current state (works mid-Loop, mid-Auto Reset, etc).
	 */
	public void resetScript() {
		if (script == null) {
			return;
		}
		runResetSequence();
		currentRunIndex = 0;
		state = State.READY;
	}

	private void runResetSequence() {
		if (script == null) {
			return;
		}
		state = State.RESETTING;
		for (String command : script.getResetCommands()) {
			commandExecutor.accept(command);
		}
	}

	/** Called on script selection: index/state reset WITHOUT running reset commands. */
	private void resetIndexOnly() {
		currentRunIndex = 0;
		state = State.READY;
	}
}
