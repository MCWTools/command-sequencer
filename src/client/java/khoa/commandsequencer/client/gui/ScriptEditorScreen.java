package khoa.commandsequencer.client.gui;

import khoa.commandsequencer.client.CommandSequencerSettings;
import khoa.commandsequencer.client.script.CommandCapture;
import khoa.commandsequencer.client.script.Script;
import khoa.commandsequencer.client.script.ScriptManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Script Editor (spec sections 3-6): exactly two tabs, Run and Reset, each holding an
 * ordered command list with Add / Edit / Delete / Move Up / Move Down. Also exposes
 * the Action Runner Loop / Auto Reset toggle, which enforces mutual exclusion
 * (Script.setLoop/setAutoReset already does this - the buttons just reflect state).
 */
public class ScriptEditorScreen extends Screen {

	private enum Tab { RUN, RESET }

	private final Screen parent;
	private final ScriptManager scriptManager;
	private final Script script;

	private Tab activeTab = Tab.RUN;

	private CommandListWidget commandList;
	private Button runTabButton;
	private Button resetTabButton;
	private Button loopButton;
	private Button autoResetButton;
	private Button editButton;
	private Button deleteButton;
	private Button moveUpButton;
	private Button moveDownButton;

	protected ScriptEditorScreen(Screen parent, ScriptManager scriptManager, Script script) {
		super(Component.literal(script.getName()));
		this.parent = parent;
		this.scriptManager = scriptManager;
		this.script = script;
	}

	@Override
	protected void init() {
		int padding = 8;
		int tabY = padding;
		int tabWidth = 80;
		int tabHeight = 20;

		runTabButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.tab_run"),
						b -> switchTab(Tab.RUN))
				.bounds(padding, tabY, tabWidth, tabHeight)
				.build());
		resetTabButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.tab_reset"),
						b -> switchTab(Tab.RESET))
				.bounds(padding + tabWidth, tabY, tabWidth, tabHeight)
				.build());

		int listY = tabY + tabHeight + padding;
		int sideWidth = 110;
		int listWidth = Math.max(80, this.width - padding * 2 - sideWidth);

		int sideX = padding + listWidth + padding;
		int sideButtonWidth = Math.min(100, sideWidth - padding);

		// 7 side-panel rows total (Add/Edit/Delete/MoveUp/MoveDown/Loop/AutoReset), plus the
		// bottom Done row. On small/scaled-up screens (e.g. GUI Scale on a phone) this.height
		// can be too short to fit fixed 20px rows with 4px spacing, which used to push rows
		// below the visible/clickable area. Shrink row height/spacing to whatever fits instead.
		int rows = 7;
		int doneRowHeight = 20;
		int availableForRows = this.height - listY - padding - doneRowHeight - padding;
		int buttonHeight = Math.max(12, Math.min(20, availableForRows / rows - 2));
		int spacing = Math.max(1, Math.min(4, (availableForRows - buttonHeight * rows) / Math.max(1, rows - 1)));
		int rowStep = buttonHeight + spacing;

		int listHeight = Math.max(30, this.height - listY - padding - doneRowHeight - padding);
		commandList = new CommandListWidget(this.minecraft, listWidth, listHeight, listY);
		this.addRenderableWidget(commandList);
		// NOTE: refreshCommandList() is deferred to the end of init() - it calls
		// updateButtonStates(), which touches editButton/deleteButton/moveUpButton/
		// moveDownButton. Those fields aren't assigned until the button-creation lines
		// below run, so calling refresh here (before they exist) threw an NPE on
		// "Cannot assign field active because this.editButton is null".

		this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.add"), b -> onAdd())
				.bounds(sideX, listY, sideButtonWidth, buttonHeight)
				.build());
		editButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.edit"), b -> onEdit())
				.bounds(sideX, listY + rowStep, sideButtonWidth, buttonHeight)
				.build());
		deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.delete"), b -> onDelete())
				.bounds(sideX, listY + rowStep * 2, sideButtonWidth, buttonHeight)
				.build());
		moveUpButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.move_up"), b -> onMoveUp())
				.bounds(sideX, listY + rowStep * 3, sideButtonWidth, buttonHeight)
				.build());
		moveDownButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.command-sequencer.move_down"), b -> onMoveDown())
				.bounds(sideX, listY + rowStep * 4, sideButtonWidth, buttonHeight)
				.build());

		// Action Runner mutual-exclusion toggles (spec section 5-6). Only relevant for the
		// Run tab conceptually, but they are script-level settings, so shown regardless of tab.
		loopButton = this.addRenderableWidget(Button.builder(loopLabel(), b -> onToggleLoop())
				.bounds(sideX, listY + rowStep * 5, sideButtonWidth, buttonHeight)
				.build());
		autoResetButton = this.addRenderableWidget(Button.builder(autoResetLabel(), b -> onToggleAutoReset())
				.bounds(sideX, listY + rowStep * 6, sideButtonWidth, buttonHeight)
				.build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(padding, this.height - padding - doneRowHeight, 100, doneRowHeight)
				.build());

		refreshCommandList();
		updateButtonStates();
		updateTabButtons();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.text(this.font, script.getName(), 8, this.height - 8 - this.font.lineHeight, 0xFF888888, true);
	}

	@Override
	public void onClose() {
		scriptManager.save(script);
		Minecraft.getInstance().setScreen(parent);
	}

	private List<String> activeList() {
		return activeTab == Tab.RUN ? script.getRunCommands() : script.getResetCommands();
	}

	private void switchTab(Tab tab) {
		this.activeTab = tab;
		refreshCommandList();
		updateTabButtons();
		updateButtonStates();
	}

	private void updateTabButtons() {
		runTabButton.active = activeTab != Tab.RUN;
		resetTabButton.active = activeTab != Tab.RESET;
	}

	private void refreshCommandList() {
		commandList.refresh(activeList());
	}

	private void updateButtonStates() {
		boolean hasSelection = commandList.getSelected() != null;
		editButton.active = hasSelection;
		deleteButton.active = hasSelection;

		int selectedIndex = commandList.getSelectedIndex();
		moveUpButton.active = hasSelection && selectedIndex > 0;
		moveDownButton.active = hasSelection && selectedIndex >= 0 && selectedIndex < activeList().size() - 1;
	}

	private Component loopLabel() {
		return Component.translatable(script.isLoop()
				? "gui.command-sequencer.loop_on"
				: "gui.command-sequencer.loop_off");
	}

	private Component autoResetLabel() {
		return Component.translatable(script.isAutoReset()
				? "gui.command-sequencer.auto_reset_on"
				: "gui.command-sequencer.auto_reset_off");
	}

	private void onToggleLoop() {
		script.setLoop(!script.isLoop());
		loopButton.setMessage(loopLabel());
		autoResetButton.setMessage(autoResetLabel());
	}

	private void onToggleAutoReset() {
		script.setAutoReset(!script.isAutoReset());
		loopButton.setMessage(loopLabel());
		autoResetButton.setMessage(autoResetLabel());
	}

	private void onAdd() {
		if (CommandSequencerSettings.isCaptureCommand()) {
			// Capture Command mode: close the GUI so the next slash command typed in
			// chat (with Minecraft's own suggestions/autocomplete) is grabbed instead
			// of sent, and appended to this tab's list. The list only exists in memory
			// on this screen instance, so we save-as-we-go via scriptManager.save().
			ScriptEditorScreen self = this;
			CommandCapture.arm(command -> {
				activeList().add(command);
				scriptManager.save(script);
				// Re-open this same editor screen (with the list refreshed) once the
				// player's next chat line has been captured, instead of leaving them
				// stuck in the game/chat screen after capture completes.
				Minecraft.getInstance().setScreen(self);
				self.refreshCommandList();
			});
			Minecraft.getInstance().setScreen(null);
			return;
		}
		Minecraft.getInstance().setScreen(new EditCommandScreen(this, "", command -> {
			activeList().add(command);
			refreshCommandList();
		}));
	}

	private void onEdit() {
		int index = commandList.getSelectedIndex();
		if (index < 0) {
			return;
		}
		String current = activeList().get(index);
		Minecraft.getInstance().setScreen(new EditCommandScreen(this, current, command -> {
			activeList().set(index, command);
			refreshCommandList();
		}));
	}

	private void onDelete() {
		int index = commandList.getSelectedIndex();
		if (index < 0) {
			return;
		}
		activeList().remove(index);
		refreshCommandList();
		updateButtonStates();
	}

	private void onMoveUp() {
		int index = commandList.getSelectedIndex();
		if (index <= 0) {
			return;
		}
		List<String> list = activeList();
		String moved = list.remove(index);
		list.add(index - 1, moved);
		refreshCommandList();
		commandList.selectIndex(index - 1);
		updateButtonStates();
	}

	private void onMoveDown() {
		int index = commandList.getSelectedIndex();
		List<String> list = activeList();
		if (index < 0 || index >= list.size() - 1) {
			return;
		}
		String moved = list.remove(index);
		list.add(index + 1, moved);
		refreshCommandList();
		commandList.selectIndex(index + 1);
		updateButtonStates();
	}

	/** Ordered command list for whichever tab (Run or Reset) is active. */
	private class CommandListWidget extends ObjectSelectionList<CommandListWidget.CommandEntry> {

		CommandListWidget(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, 16);
		}

		void refresh(List<String> commands) {
			this.clearEntries();
			for (int i = 0; i < commands.size(); i++) {
				this.addEntry(new CommandEntry(commands.get(i), i));
			}
			ScriptEditorScreen.this.updateButtonStates();
		}

		int getSelectedIndex() {
			CommandEntry selected = this.getSelected();
			return selected == null ? -1 : selected.index;
		}

		void selectIndex(int index) {
			// Entries are rebuilt (not mutated) on every refresh(), so selecting by index
			// after a refresh means walking the freshly built entries and matching on the
			// index each CommandEntry was constructed with.
			for (CommandEntry entry : entriesView()) {
				if (entry.index == index) {
					this.setSelected(entry);
					return;
				}
			}
		}

		private Iterable<CommandEntry> entriesView() {
			// AbstractSelectionList exposes children() as the backing entry list in this
			// version; if fabric-loom flags this as missing, use whatever the equivalent
			// entry-iteration accessor is called (children()/getEntries()) in your mappings.
			return this.children();
		}

		class CommandEntry extends ObjectSelectionList.Entry<CommandEntry> {
			final String command;
			final int index;

			CommandEntry(String command, int index) {
				this.command = command;
				this.index = index;
			}

			@Override
			public Component getNarration() {
				return Component.literal(command);
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				if (hovered) {
					graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80FFFFFF);
				}
				graphics.text(ScriptEditorScreen.this.font, command, getX() + 4, getY() + 4, 0xFFFFFFFF, true);
			}

			@Override
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
				CommandListWidget.this.setSelected(this);
				ScriptEditorScreen.this.updateButtonStates();
				return true;
			}
		}
	}
}
