package khoa.commandsequencer.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import khoa.commandsequencer.client.gui.ScriptManagerScreen;
import khoa.commandsequencer.client.hud.NextActionHudRegistration;
import khoa.commandsequencer.client.script.ActionRunner;
import khoa.commandsequencer.client.script.CommandCapture;
import khoa.commandsequencer.client.script.CommandDispatcher;
import khoa.commandsequencer.client.script.ScriptManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class CommandSequencerClient implements ClientModInitializer {

	private static ScriptManager scriptManager;
	private static ActionRunner actionRunner;

	@Override
	public void onInitializeClient() {
		scriptManager = new ScriptManager(FabricLoader.getInstance().getConfigDir());
		scriptManager.loadAll();
		CommandSequencerSettings.load(FabricLoader.getInstance().getConfigDir());

		actionRunner = new ActionRunner(CommandDispatcher::dispatch);

		CommandSequencerKeybinds.register();
		NextActionHudRegistration.register();

		// Client-side slash commands (/cs, /command-sequencer) as an alternative to the
		// keybind for opening the Script Manager - some launchers/keyboards make custom
		// keybinds awkward to set up. This is a client command (fabric-command-api-v2),
		// so it works in singleplayer with no server-side support needed.
		//
		// "/cs" is a full alias of "/command-sequencer", including the "setting"
		// subcommand - built twice below (see buildRootCommand) since each literal name
		// needs its own command tree. If "/command-sequencer ..." works but bare "/cs"
		// typed+entered silently does nothing, that almost always means another mod (or
		// a stale client command cache) is also claiming the literal "cs" and winning -
		// check for a second mod registering "/cs" and disable/rename one of them.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// Built twice (once per literal name) rather than registered-then-redirected:
			// a Brigadier ArgumentBuilder is mutable/single-use once passed to register(),
			// so reusing one built node for both "/command-sequencer" and "/cs" needs two
			// independent trees rather than a shared builder instance.
			dispatcher.register(buildRootCommand(literal("command-sequencer")));
			dispatcher.register(buildRootCommand(literal("cs")));
		});

		// Capture Command mode (spec: cs setting capturecommand true/false). While armed
		// (Add was pressed on the Run/Reset tab with capturecommand=true), the next chat
		// line starting with "/" is grabbed here before it is sent, appended to the
		// active script list via CommandCapture, and NOT executed. Anything not starting
		// with "/" - or typed while capture isn't armed - goes to chat as normal.
		ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
			if (!CommandCapture.isArmed()) {
				return true;
			}
			String full = "/" + command;
			if (!full.startsWith("/")) {
				return true;
			}
			return !CommandCapture.tryCapture(full);
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

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildRootCommand(
			LiteralArgumentBuilder<FabricClientCommandSource> root) {
		return root
				.executes(context -> {
					Minecraft.getInstance().setScreen(new ScriptManagerScreen());
					return 1;
				})
				.then(literal("setting")
						.then(literal("capturecommand")
								.then(argument("value", BoolArgumentType.bool())
										.executes(context -> {
											boolean value = BoolArgumentType.getBool(context, "value");
											CommandSequencerSettings.setCaptureCommand(value);
											context.getSource().sendFeedback(Component.literal(
													"Capture Command: " + (value ? "ON" : "OFF"))
													.withStyle(ChatFormatting.GREEN));
											return 1;
										}))));
	}

	public static ScriptManager getScriptManager() {
		return scriptManager;
	}

	public static ActionRunner getActionRunner() {
		return actionRunner;
	}
}
