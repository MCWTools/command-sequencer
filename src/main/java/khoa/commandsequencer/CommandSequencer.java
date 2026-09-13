package khoa.commandsequencer;

import khoa.commandsequencer.vector.VectorCommands;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandSequencer implements ModInitializer {
	public static final String MOD_ID = "command-sequencer";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Command Sequencer initialized");

		// /vectortp và /vectorsummon — đăng ký server-side, hoạt động cả
		// trong command block lẫn multiplayer chat (xem VectorCommands).
		VectorCommands.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
