package khoa.commandsequencer.client;

import khoa.commandsequencer.client.gui.ScriptManagerScreen;
import khoa.commandsequencer.client.hud.NextActionHudRegistration;
import khoa.commandsequencer.client.script.ActionRunner;
import khoa.commandsequencer.client.script.CommandDispatcher;
import khoa.commandsequencer.client.script.ScriptManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

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
