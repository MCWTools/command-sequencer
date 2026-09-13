package khoa.commandsequencer.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global (not per-script) mod settings. Currently just Capture Command mode:
 *
 * - captureCommand = false (default): pressing "Add" on the Run/Reset tab opens the
 *   normal text-box GUI (EditCommandScreen). Simple, but you lose Minecraft's own
 *   command suggestions/autocomplete, so it's easy to typo a command's syntax.
 * - captureCommand = true: pressing "Add" instead closes the GUI and arms capture
 *   mode. The next slash command you type in chat (must start with "/") is grabbed
 *   before it's sent to the server/client, added to the currently-open script's
 *   active command list, and NOT actually executed. Anything typed that does not
 *   start with "/" is sent to chat normally, per spec.
 */
public final class CommandSequencerSettings {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path settingsFile;
	private static boolean captureCommand = false;

	private CommandSequencerSettings() {
	}

	public static void load(Path configDir) {
		settingsFile = configDir.resolve("commandsequencer").resolve("settings.json");
		if (!Files.exists(settingsFile)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
			SettingsFile file = GSON.fromJson(reader, SettingsFile.class);
			if (file != null) {
				captureCommand = file.captureCommand;
			}
		} catch (IOException | RuntimeException e) {
			System.err.println("[command-sequencer] Failed to load settings: " + e);
		}
	}

	public static boolean isCaptureCommand() {
		return captureCommand;
	}

	public static void setCaptureCommand(boolean value) {
		captureCommand = value;
		save();
	}

	private static void save() {
		if (settingsFile == null) {
			return;
		}
		try {
			Files.createDirectories(settingsFile.getParent());
			try (Writer writer = Files.newBufferedWriter(settingsFile, StandardCharsets.UTF_8)) {
				SettingsFile file = new SettingsFile();
				file.captureCommand = captureCommand;
				GSON.toJson(file, writer);
			}
		} catch (IOException e) {
			System.err.println("[command-sequencer] Failed to save settings: " + e);
		}
	}

	private static final class SettingsFile {
		boolean captureCommand = false;
	}
}
