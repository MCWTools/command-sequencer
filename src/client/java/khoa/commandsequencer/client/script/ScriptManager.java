package khoa.commandsequencer.client.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the script list, and load/save of each script to its own JSON file under
 * config/commandsequencer/scripts/ (spec section 12). Filenames are derived from
 * the script id, so renaming a script never breaks the on-disk link (section 12:
 * "Không được dựa hoàn toàn vào filename để xác định script").
 */
public class ScriptManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path scriptsDir;

	// id -> Script, insertion-ordered so the Script Manager GUI list is stable.
	private final Map<String, Script> scripts = new LinkedHashMap<>();

	private String selectedScriptId = null;

	public ScriptManager(Path configDir) {
		this.scriptsDir = configDir.resolve("commandsequencer").resolve("scripts");
	}

	public void loadAll() {
		scripts.clear();
		try {
			Files.createDirectories(scriptsDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create scripts directory: " + scriptsDir, e);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDir, "*.json")) {
			for (Path path : stream) {
				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
					ScriptFile file = GSON.fromJson(reader, ScriptFile.class);
					if (file == null) {
						continue;
					}
					Script script = file.toScript();
					scripts.put(script.getId(), script);
				} catch (IOException | RuntimeException e) {
					// A single malformed script file should not take down the whole mod.
					System.err.println("[command-sequencer] Failed to load " + path + ": " + e);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to list scripts directory: " + scriptsDir, e);
		}
	}

	public List<Script> getScripts() {
		return new ArrayList<>(scripts.values());
	}

	public Script getScript(String id) {
		return scripts.get(id);
	}

	public Script getSelectedScript() {
		return selectedScriptId == null ? null : scripts.get(selectedScriptId);
	}

	public void selectScript(String id) {
		this.selectedScriptId = id;
	}

	public Script createScript(String name, String description) {
		Script script = new Script(name, description);
		scripts.put(script.getId(), script);
		save(script);
		return script;
	}

	public void deleteScript(String id) {
		scripts.remove(id);
		if (id.equals(selectedScriptId)) {
			selectedScriptId = null;
		}
		Path path = scriptsDir.resolve(fileNameFor(id));
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			System.err.println("[command-sequencer] Failed to delete " + path + ": " + e);
		}
	}

	public void save(Script script) {
		Path path = scriptsDir.resolve(fileNameFor(script.getId()));
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(ScriptFile.fromScript(script), writer);
		} catch (IOException e) {
			System.err.println("[command-sequencer] Failed to save " + path + ": " + e);
		}
	}

	/** Export a script to an arbitrary destination path (spec section 11). */
	public void exportTo(Script script, Path destination) throws IOException {
		try (Writer writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
			GSON.toJson(ScriptFile.fromScript(script), writer);
		}
	}

	/** Import a script from an arbitrary source path (spec section 11); returns the new script. */
	public Script importFrom(Path source) throws IOException {
		try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
			ScriptFile file = GSON.fromJson(reader, ScriptFile.class);
			if (file == null) {
				throw new IOException("Empty or invalid script JSON: " + source);
			}
			Script script = file.toScript();
			// Imported scripts get a fresh id so they never collide with an existing one.
			script.setId(java.util.UUID.randomUUID().toString());
			scripts.put(script.getId(), script);
			save(script);
			return script;
		}
	}

	private static String fileNameFor(String id) {
		return id + ".json";
	}

	/**
	 * On-disk JSON shape (spec section 11). Kept separate from {@link Script} so the
	 * schema version and file layout can evolve independently of the in-memory model.
	 */
	private static final class ScriptFile {
		int version = 1;
		String id;
		String name;
		String description;
		List<String> run = new ArrayList<>();
		List<String> reset = new ArrayList<>();
		ActionRunnerSettings actionRunner = new ActionRunnerSettings();

		static ScriptFile fromScript(Script script) {
			ScriptFile file = new ScriptFile();
			file.id = script.getId();
			file.name = script.getName();
			file.description = script.getDescription();
			file.run = new ArrayList<>(script.getRunCommands());
			file.reset = new ArrayList<>(script.getResetCommands());
			file.actionRunner.loop = script.isLoop();
			file.actionRunner.autoReset = script.isAutoReset();
			return file;
		}

		Script toScript() {
			Script script = new Script();
			if (id != null) {
				script.setId(id);
			}
			script.setName(name == null ? "" : name);
			script.setDescription(description == null ? "" : description);
			if (run != null) {
				script.getRunCommands().addAll(run);
			}
			if (reset != null) {
				script.getResetCommands().addAll(reset);
			}
			if (actionRunner != null) {
				// setLoop/setAutoReset enforce mutual exclusion even if a hand-edited
				// JSON file has both true.
				if (actionRunner.loop) {
					script.setLoop(true);
				}
				if (actionRunner.autoReset) {
					script.setAutoReset(true);
				}
			}
			return script;
		}
	}

	private static final class ActionRunnerSettings {
		boolean loop = false;
		boolean autoReset = false;
	}
}
