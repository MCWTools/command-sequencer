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
	private int listWidth = LIST_WIDTH;

	private Button editButton;
	private Button renameButton;
	private Button deleteButton;
	private Button exportButton;
	private Button runNextActionButton;
	private Button resetScriptButton;

	private EditBox importExportNameBox;

	private int cachedRowStep = 24;
	private int cachedButtonHeight = 20;

	public ScriptManagerScreen() {
		super(Component.translatable("gui.command-sequencer.script_manager"));
		this.scriptManager = CommandSequencerClient.getScriptManager();
	}

	@Override
	protected void init() {
		int listWidth = Math.min(LIST_WIDTH, Math.max(80, (this.width - PADDING * 3) / 2));
		this.listWidth = listWidth;

		int rightX = PADDING * 2 + listWidth;
		int buttonWidth = Math.max(60, Math.min(90, (this.width - rightX - PADDING - 4) / 2));
		int spacing = 4;

		// 5 top rows (New/Edit, Rename/Delete, file-name box, Import/Export,
		// Run Next Action/Reset) + 2 bottom rows (HUD Settings, Done). Same reasoning as
		// ScriptEditorScreen: don't cement 20px rows when this.height might be too short
		// for all of them (phone GUI Scale) - shrink instead of letting rows collide/hide.
		int topRows = 5;
		int bottomRows = 2;
		int totalRows = topRows + bottomRows;
		int availableForRows = this.height - PADDING * 2;
		int buttonHeight = Math.max(12, Math.min(20, availableForRows / totalRows - 2));
		int rowSpacing = Math.max(1, Math.min(spacing, (availableForRows - buttonHeight * totalRows) / Math.max(1, totalRows - 1)));
		int rowStep = buttonHeight + rowSpacing;
		this.cachedRowStep = rowStep;
		this.cachedButtonHeight = buttonHeight;

		int listHeight = Math.max(40, this.height - PADDING * 2);
		scriptList = new ScriptListWidget(this.minecraft, listWidth, listHeight, PADDING);
		refreshList();
		this.addRenderableWidget(scriptList);

		int buttonY = PADDING;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.new_script"), b -> onNewScript())
				.bounds(rightX, buttonY, buttonWidth, buttonHeight)
				.build());

		editButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.edit"), b -> onEditScript())
				.bounds(rightX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
				.build());

		renameButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.rename"), b -> onRenameScript())
				.bounds(rightX, buttonY + rowStep, buttonWidth, buttonHeight)
				.build());

		deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.delete"), b -> onDeleteScript())
				.bounds(rightX + buttonWidth + spacing, buttonY + rowStep, buttonWidth, buttonHeight)
				.build());

		importExportNameBox = this.addRenderableWidget(new EditBox(
				this.font,
				rightX,
				buttonY + rowStep * 2,
				buttonWidth * 2 + spacing,
				buttonHeight,
				Component.translatable("gui.command-sequencer.file_name")
		));
		importExportNameBox.setValue("script");

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.import"), b -> onImport())
				.bounds(rightX, buttonY + rowStep * 3, buttonWidth, buttonHeight)
				.build());

		exportButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.export"), b -> onExport())
				.bounds(rightX + buttonWidth + spacing, buttonY + rowStep * 3, buttonWidth, buttonHeight)
				.build());

		// Run Next Action / Reset Script as clickable buttons, mirroring the keybinds
		// (spec sections 7-8) - some players don't want to bind extra keys just to test
		// a script from the manager screen.
		runNextActionButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.run_next_action"), b -> onRunNextAction())
				.bounds(rightX, buttonY + rowStep * 4, buttonWidth, buttonHeight)
				.build());
		resetScriptButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.reset_script"), b -> onResetScript())
				.bounds(rightX + buttonWidth + spacing, buttonY + rowStep * 4, buttonWidth, buttonHeight)
				.build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.hud_settings"), b -> onHudSettings())
				.bounds(rightX, this.height - PADDING - rowStep - buttonHeight, buttonWidth * 2 + spacing, buttonHeight)
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
		int rightX = PADDING * 2 + listWidth;
		// Sits just below the Run Next Action / Reset row (5 top rows, 0-indexed 0-4),
		// clamped so it can't overlap the HUD Settings / Done buttons anchored to the
		// bottom of the screen on short/scaled-up GUIs.
		int textY = Math.min(PADDING + cachedRowStep * 5 + 4, this.height - PADDING - cachedRowStep - cachedButtonHeight - 40);

		if (selected != null) {
			graphics.text(this.font, selected.getName(), rightX, textY, 0xFFFFFFFF, true);
			graphics.text(this.font, selected.getDescription(), rightX, textY + 12, 0xFFAAAAAA, true);
			graphics.text(this.font,
					"Run: " + selected.getRunCommands().size() + "   Reset: " + selected.getResetCommands().size(),
					rightX, textY + 26, 0xFF888888, true);
		} else {
			graphics.text(this.font, Component.translatable("gui.command-sequencer.no_script_selected"),
					rightX, textY, 0xFF888888, true);
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
		runNextActionButton.active = hasSelection;
		resetScriptButton.active = hasSelection;
	}

	private void onRunNextAction() {
		CommandSequencerClient.getActionRunner().runNextAction();
	}

	private void onResetScript() {
		CommandSequencerClient.getActionRunner().resetScript();
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
				if (hovered) {
					graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80FFFFFF);
				}
				graphics.text(ScriptManagerScreen.this.font, script.getName(), getX() + 4, getY() + 6, 0xFFFFFFFF, true);
			}

			@Override
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
				ScriptListWidget.this.setSelected(this);
				return true;
			}
		}
	}
}
