package khoa.commandsequencer.client;

import com.mojang.blaze3d.platform.InputConstants;

import khoa.commandsequencer.CommandSequencer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

/**
 * Run Next Action / Reset Script keybindings (spec sections 7 and 8).
 *
 * NOTE ON KeyMapping.Category: 26.1.2 changed KeyMapping's constructor to take a
 * KeyMapping.Category record instead of a raw translation-key String (verified
 * against the shipped client jar). A mod's own category must be registered once
 * via KeyMapping.Category.register(Identifier) before use.
 */
public final class CommandSequencerKeybinds {

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CommandSequencer.MOD_ID, "main"));

	public static KeyMapping runNextAction;
	public static KeyMapping resetScript;
	public static KeyMapping openScriptManager;

	private CommandSequencerKeybinds() {
	}

	public static void register() {
		runNextAction = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.command-sequencer.run_next_action",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				CATEGORY
		));

		resetScript = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.command-sequencer.reset_script",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				CATEGORY
		));

		openScriptManager = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.command-sequencer.open_script_manager",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY
		));
	}
}
