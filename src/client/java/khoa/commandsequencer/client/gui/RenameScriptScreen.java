package khoa.commandsequencer.client.gui;

import khoa.commandsequencer.client.script.Script;
import khoa.commandsequencer.client.script.ScriptManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minimal rename dialog for a script (spec section 2, "Rename"). */
public class RenameScriptScreen extends Screen {

	private final Screen parent;
	private final ScriptManager scriptManager;
	private final Script script;

	private EditBox nameBox;

	protected RenameScriptScreen(Screen parent, ScriptManager scriptManager, Script script) {
		super(Component.translatable("gui.command-sequencer.rename"));
		this.parent = parent;
		this.scriptManager = scriptManager;
		this.script = script;
	}

	@Override
	protected void init() {
		int boxWidth = 200;
		int x = (this.width - boxWidth) / 2;
		int y = this.height / 2 - 10;

		nameBox = this.addRenderableWidget(new EditBox(this.font, x, y, boxWidth, 20,
				Component.translatable("gui.command-sequencer.script_name")));
		nameBox.setValue(script.getName());
		nameBox.setFocused(true);

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
				.bounds(x, y + 28, 96, 20)
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(x + 104, y + 28, 96, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.text(this.font, this.getTitle(), (this.width - this.font.width(this.getTitle())) / 2,
				this.height / 2 - 30, 0xFFFFFFFF, true);
	}

	private void confirm() {
		String newName = nameBox.getValue().trim();
		if (!newName.isEmpty()) {
			script.setName(newName);
			scriptManager.save(script);
		}
		onClose();
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}
}
