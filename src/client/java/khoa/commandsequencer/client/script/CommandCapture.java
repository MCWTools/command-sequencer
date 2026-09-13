package khoa.commandsequencer.client.script;

import java.util.function.Consumer;

/**
 * One-shot "arm" for Capture Command mode (spec: capture-command setting).
 *
 * When the Run/Reset editor's Add button is pressed while
 * {@link khoa.commandsequencer.client.CommandSequencerSettings#isCaptureCommand()}
 * is true, the editor screen is closed and {@link #arm(Consumer)} is called instead
 * of opening EditCommandScreen. The next chat line the player types is intercepted:
 *
 * - If it starts with "/", it is NOT sent (so it never actually runs), it is instead
 *   handed to the armed consumer (which appends it to the active command list) and
 *   capture is disarmed.
 * - If it does NOT start with "/", capture stays armed and the message is sent to
 *   chat normally - only slash commands are ever captured.
 */
public final class CommandCapture {

	private static Consumer<String> pending;

	private CommandCapture() {
	}

	public static void arm(Consumer<String> onCaptured) {
		pending = onCaptured;
	}

	public static void disarm() {
		pending = null;
	}

	public static boolean isArmed() {
		return pending != null;
	}

	/**
	 * Called from the chat-send hook for every outgoing command. Returns true if the
	 * command was captured (and must NOT be sent), false if it should be sent as normal.
	 */
	public static boolean tryCapture(String fullCommandWithSlash) {
		if (pending == null) {
			return false;
		}
		Consumer<String> consumer = pending;
		pending = null;
		consumer.accept(fullCommandWithSlash);
		return true;
	}
}
