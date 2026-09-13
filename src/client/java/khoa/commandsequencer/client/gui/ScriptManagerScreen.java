package khoa.commandsequencer.client.gui;

import khoa.commandsequencer.client.CommandSequencerClient;
import khoa.commandsequencer.client.script.Script;
import khoa.commandsequencer.client.script.ScriptManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Script Manager GUI (spec section 2): a left panel listing all scripts with
 * New / Select / Edit / Rename / Delete / Import / Export, and a right panel showing
 * the selected script's name/description/run/reset summary.
 *
 * Import/Export use fixed paths under the scripts folder rather than a native file
 * picker - Minecraft's Screen API has no built-in file-open dialog, and Fabric mods
 * conventionally use java.awt.FileDialog or a text path field for this. This skeleton
 * uses a simple text field for the file name to keep it dependency-free; swap in a
 * proper file picker later if wanted.
 */
public class ScriptManagerScreen extends Screen {

	private static final int LIST_WIDTH = 150;
	private static final int PADDING = 8;

	private final ScriptManager scriptManager;
	private ScriptListWidget scriptList;

	private Button editButton;
	private Button renameButton;
	private Button deleteButton;
	private Button exportButton;

	private EditBox importExportNameBox;

	public ScriptManagerScreen() {
		super(Component.translatable("gui.command-sequencer.script_manager"));
		this.scriptManager = CommandSequencerClient.getScriptManager();
	}

	@Override
	protected void init() {
		int listHeight = this.height - PADDING * 2 - 28;
		scriptList = new ScriptListWidget(this.minecraft, LIST_WIDTH, listHeight, PADDING);
		refreshList();
		this.addRenderableWidget(scriptList);

		int rightX = PADDING * 2 + LIST_WIDTH;
		int buttonY = PADDING;
		int buttonWidth = 90;
		int buttonHeight = 20;
		int spacing = 4;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.new_script"), b -> onNewScript())
				.bounds(rightX, buttonY, buttonWidth, buttonHeight)
				.build());

		editButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.edit"), b -> onEditScript())
				.bounds(rightX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
				.build());

		renameButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.rename"), b -> onRenameScript())
				.bounds(rightX, buttonY + buttonHeight + spacing, buttonWidth, buttonHeight)
				.build());

		deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.delete"), b -> onDeleteScript())
				.bounds(rightX + buttonWidth + spacing, buttonY + buttonHeight + spacing, buttonWidth, buttonHeight)
				.build());

		importExportNameBox = this.addRenderableWidget(new EditBox(
				this.font,
				rightX,
				buttonY + (buttonHeight + spacing) * 2,
				buttonWidth * 2 + spacing,
				buttonHeight,
				Component.translatable("gui.command-sequencer.file_name")
		));
		importExportNameBox.setValue("script");

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.import"), b -> onImport())
				.bounds(rightX, buttonY + (buttonHeight + spacing) * 3, buttonWidth, buttonHeight)
				.build());

		exportButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.export"), b -> onExport())
				.bounds(rightX + buttonWidth + spacing, buttonY + (buttonHeight + spacing) * 3, buttonWidth, buttonHeight)
				.build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.hud_settings"), b -> onHudSettings())
				.bounds(rightX, this.height - PADDING - buttonHeight * 2 - spacing, buttonWidth * 2 + spacing, buttonHeight)
				.build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(rightX, this.height - PADDING - buttonHeight, buttonWidth, buttonHeight)
				.build());

		updateButtonStates();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		Script selected = scriptManager.getSelectedScript();
		int rightX = PADDING * 2 + LIST_WIDTH;
		int textY = PADDING + 20 * 4 + 16;

		if (selected != null) {
			graphics.text(this.font, selected.getName(), rightX, textY, 0xFFFFFF, true);
			graphics.text(this.font, selected.getDescription(), rightX, textY + 12, 0xAAAAAA, true);
			graphics.text(this.font,
					"Run: " + selected.getRunCommands().size() + "   Reset: " + selected.getResetCommands().size(),
					rightX, textY + 26, 0x888888, true);
		} else {
			graphics.text(this.font, Component.translatable("gui.command-sequencer.no_script_selected"),
					rightX, textY, 0x888888, true);
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(null);
	}

	private void refreshList() {
		scriptList.refresh(scriptManager.getScripts(), scriptManager.getSelectedScript());
	}

	private void onScriptSelected(Script script) {
		scriptManager.selectScript(script == null ? null : script.getId());
		// Keep the ActionRunner (which the Run Next Action / Reset Script keybinds
		// drive) pointed at whatever script is currently selected in this GUI.
		CommandSequencerClient.getActionRunner().setScript(script);
		updateButtonStates();
	}

	private void updateButtonStates() {
		boolean hasSelection = scriptManager.getSelectedScript() != null;
		editButton.active = hasSelection;
		renameButton.active = hasSelection;
		deleteButton.active = hasSelection;
		exportButton.active = hasSelection;
	}

	private void onNewScript() {
		Script script = scriptManager.createScript("New Script", "");
		refreshList();
		onScriptSelected(script);
	}

	private void onEditScript() {
		Script selected = scriptManager.getSelectedScript();
		if (selected != null) {
			Minecraft.getInstance().setScreen(new ScriptEditorScreen(this, scriptManager, selected));
		}
	}

	private void onRenameScript() {
		Script selected = scriptManager.getSelectedScript();
		if (selected != null) {
			Minecraft.getInstance().setScreen(new RenameScriptScreen(this, scriptManager, selected));
		}
	}

	private void onDeleteScript() {
		Script selected = scriptManager.getSelectedScript();
		if (selected != null) {
			scriptManager.deleteScript(selected.getId());
			refreshList();
			updateButtonStates();
		}
	}

	private void onHudSettings() {
		Minecraft.getInstance().setScreen(new NextActionHudSettingsScreen(this,
				khoa.commandsequencer.client.hud.NextActionHudRegistration.SETTINGS));
	}

	private void onImport() {
		String fileName = importExportNameBox.getValue().trim();
		if (fileName.isEmpty()) {
			return;
		}
		try {
			Path source = scriptImportExportDir().resolve(fileName.endsWith(".json") ? fileName : fileName + ".json");
			Script imported = scriptManager.importFrom(source);
			refreshList();
			onScriptSelected(imported);
		} catch (IOException e) {
			System.err.println("[command-sequencer] Import failed: " + e);
		}
	}

	private void onExport() {
		Script selected = scriptManager.getSelectedScript();
		if (selected == null) {
			return;
		}
		String fileName = importExportNameBox.getValue().trim();
		if (fileName.isEmpty()) {
			return;
		}
		try {
			Path destination = scriptImportExportDir().resolve(fileName.endsWith(".json") ? fileName : fileName + ".json");
			scriptManager.exportTo(selected, destination);
		} catch (IOException e) {
			System.err.println("[command-sequencer] Export failed: " + e);
		}
	}

	private Path scriptImportExportDir() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("commandsequencer");
	}

	/** Left panel: selectable list of scripts. */
	private class ScriptListWidget extends ObjectSelectionList<ScriptListWidget.ScriptEntry> {

		ScriptListWidget(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, 20);
		}

		void refresh(List<Script> scripts, Script selected) {
			this.clearEntries();
			for (Script script : scripts) {
				ScriptEntry entry = new ScriptEntry(script);
				this.addEntry(entry);
				if (selected != null && selected.getId().equals(script.getId())) {
					// Restore the list's visual selection without re-triggering
					// onScriptSelected - the script was already selected before this
					// refresh, so re-running selection would needlessly reset the
					// ActionRunner's progress every time this screen is reopened.
					super.setSelected(entry);
				}
			}
		}

		@Override
		public void setSelected(ScriptEntry entry) {
			super.setSelected(entry);
			onScriptSelected(entry == null ? null : entry.script);
		}

		class ScriptEntry extends ObjectSelectionList.Entry<ScriptEntry> {
			final Script script;

			ScriptEntry(Script script) {
				this.script = script;
			}

			@Override
			public Component getNarration() {
				return Component.literal(script.getName());
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				graphics.text(font, script.getName(), getX() + 4, getY() + 6, 0xFFFFFF, true);
			}

			@Override
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
				ScriptListWidget.this.setSelected(this);
				return true;
			}
		}
	}
}
