package khoa.commandsequencer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Add/Edit dialog for one Run or Reset command. The mod stores commands as plain text
 * and never parses them (spec section 3), so this is intentionally just a text field.
 */
public class EditCommandScreen extends Screen {

	private final Screen parent;
	private final String initialValue;
	private final Consumer<String> onConfirm;

	private EditBox commandBox;

	protected EditCommandScreen(Screen parent, String initialValue, Consumer<String> onConfirm) {
		super(Component.translatable("gui.command-sequencer.edit_command"));
		this.parent = parent;
		this.initialValue = initialValue;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init() {
		int boxWidth = Math.min(300, this.width - 40);
		int x = (this.width - boxWidth) / 2;
		int y = this.height / 2 - 10;

		commandBox = this.addRenderableWidget(new EditBox(this.font, x, y, boxWidth, 20,
				Component.translatable("gui.command-sequencer.command")));
		commandBox.setMaxLength(32500); // matches vanilla chat/command length limit
		commandBox.setValue(initialValue);
		commandBox.setFocused(true);

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
				.bounds(x, y + 28, boxWidth / 2 - 2, 20)
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(x + boxWidth / 2 + 2, y + 28, boxWidth / 2 - 2, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.text(this.font, this.getTitle(), (this.width - this.font.width(this.getTitle())) / 2,
				this.height / 2 - 30, 0xFFFFFF, true);
	}

	private void confirm() {
		String value = commandBox.getValue();
		if (!value.isBlank()) {
			onConfirm.accept(value);
		}
		onClose();
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}
}
