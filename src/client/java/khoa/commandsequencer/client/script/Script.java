package khoa.commandsequencer.client.script;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single Command Sequencer script: a Run command list, a Reset command list,
 * and the Action Runner / Next Action HUD settings that apply to it.
 *
 * Commands are stored purely as text - this mod never parses or understands
 * what a command does, it only stores and dispatches it (see spec section 3).
 */
public class Script {

	private String id;
	private String name;
	private String description;

	private final List<String> runCommands = new ArrayList<>();
	private final List<String> resetCommands = new ArrayList<>();

	private boolean loop = false;
	private boolean autoReset = false;

	public Script(String name, String description) {
		this.id = UUID.randomUUID().toString();
		this.name = name;
		this.description = description;
	}

	// Used by JSON deserialization (Gson needs a no-arg constructor).
	public Script() {
		this.id = UUID.randomUUID().toString();
		this.name = "";
		this.description = "";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getRunCommands() {
		return runCommands;
	}

	public List<String> getResetCommands() {
		return resetCommands;
	}

	public boolean isLoop() {
		return loop;
	}

	public boolean isAutoReset() {
		return autoReset;
	}

	/**
	 * Enables Loop mode. Loop and Auto Reset are mutually exclusive (spec section 6):
	 * enabling one always disables the other.
	 */
	public void setLoop(boolean loop) {
		this.loop = loop;
		if (loop) {
			this.autoReset = false;
		}
	}

	/**
	 * Enables Auto Reset mode. Loop and Auto Reset are mutually exclusive (spec section 6):
	 * enabling one always disables the other.
	 */
	public void setAutoReset(boolean autoReset) {
		this.autoReset = autoReset;
		if (autoReset) {
			this.loop = false;
		}
	}
}
