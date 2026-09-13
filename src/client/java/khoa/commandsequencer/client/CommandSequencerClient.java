package khoa.commandsequencer.client;

import khoa.commandsequencer.client.gui.ScriptManagerScreen;
import khoa.commandsequencer.client.hud.NextActionHudRegistration;
import khoa.commandsequencer.client.script.ActionRunner;
import khoa.commandsequencer.client.script.CommandDispatcher;
import khoa.commandsequencer.client.script.ScriptManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class CommandSequencerClient implements ClientModInitializer {

	private static ScriptManager scriptManager;
	private static ActionRunner actionRunner;

	@Override
	public void onInitializeClient() {
		scriptManager = new ScriptManager(FabricLoader.getInstance().getConfigDir());
		scriptManager.loadAll();

		actionRunner = new ActionRunner(CommandDispatcher::dispatch);

		CommandSequencerKeybinds.register();
		NextActionHudRegistration.register();

		// Client-side slash commands (/cs, /command-sequencer) as an alternative to the
		// keybind for opening the Script Manager - some launchers/keyboards make custom
		// keybinds awkward to set up. This is a client command (fabric-command-api-v2),
		// so it works in singleplayer with no server-side support needed.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("command-sequencer")
					.executes(context -> {
						Minecraft.getInstance().setScreen(new ScriptManagerScreen());
						return 1;
					}));
			dispatcher.register(literal("cs")
					.executes(context -> {
						Minecraft.getInstance().setScreen(new ScriptManagerScreen());
						return 1;
					}));
		});

		// while(...consumeClick()) drains queued presses from one tick - each call still
		// triggers exactly one command, per spec section 7 (one Run command per key press).
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (CommandSequencerKeybinds.runNextAction.consumeClick()) {
				actionRunner.runNextAction();
			}
			while (CommandSequencerKeybinds.resetScript.consumeClick()) {
				actionRunner.resetScript();
			}
			while (CommandSequencerKeybinds.openScriptManager.consumeClick()) {
				client.setScreen(new ScriptManagerScreen());
			}
		});
	}

	public static ScriptManager getScriptManager() {
		return scriptManager;
	}

	public static ActionRunner getActionRunner() {
		return actionRunner;
	}
}
