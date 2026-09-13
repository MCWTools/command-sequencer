package khoa.commandsequencer.client.script;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Sends a stored command string to the server, the same way as if the player had
 * typed it in chat.
 *
 * Verified against the 26.1.2 client jar: {@link ClientPacketListener#sendCommand(String)}
 * exists and takes the command WITHOUT a leading slash. Scripts in this mod are stored
 * with a leading "/" (per the spec examples), so it is stripped here before sending.
 */
public final class CommandDispatcher {

	private CommandDispatcher() {
	}

	public static void dispatch(String command) {
		if (command == null || command.isBlank()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		ClientPacketListener connection = client.getConnection();
		if (connection == null) {
			// Not connected to a world/server - nothing to send to.
			return;
		}
		String text = command.startsWith("/") ? command.substring(1) : command;
		connection.sendCommand(text);
	}
}
