package khoa.commandsequencer.client.gui;

import khoa.commandsequencer.client.hud.NextActionHudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Next Action HUD settings (spec section 10): ON/OFF, Width (maximum), Height (maximum),
 * Position preset. All three of the numeric-ish settings are done with CycleButton
 * stepping through a fixed list of values rather than a free-drag slider - CycleButton's
 * API is fully verified against the shipped 26.1.2 jar (Button.builder-style pattern),
 * while AbstractSliderButton requires overriding updateMessage/applyValue whose exact
 * shape was not double-checked here. Swap in a real slider later if finer control is wanted.
 */
public class NextActionHudSettingsScreen extends Screen {

	private static final List<Integer> WIDTH_STEPS = List.of(100, 150, 200, 250, 300, 350, 400);
	private static final List<Integer> HEIGHT_STEPS = List.of(20, 30, 40, 60, 80, 100, 120);

	private final Screen parent;
	private final NextActionHudSettings settings;

	protected NextActionHudSettingsScreen(Screen parent, NextActionHudSettings settings) {
		super(Component.translatable("gui.command-sequencer.hud_settings"));
		this.parent = parent;
		this.settings = settings;
	}

	@Override
	protected void init() {
		int rowWidth = 200;
		int x = (this.width - rowWidth) / 2;
		int y = this.height / 2 - 70;
		int rowHeight = 20;
		int spacing = 6;

		this.addRenderableWidget(CycleButton.onOffBuilder(settings.enabled)
				.create(x, y, rowWidth, rowHeight,
						Component.translatable("gui.command-sequencer.hud_enabled"),
						(button, value) -> settings.enabled = value));

		y += rowHeight + spacing;

		this.addRenderableWidget(CycleButton.<Integer>builder(width -> Component.literal(width + "px"), () -> settings.maxWidth)
				.withValues(WIDTH_STEPS)
				.create(x, y, rowWidth, rowHeight,
						Component.translatable("gui.command-sequencer.hud_max_width"),
						(button, value) -> settings.maxWidth = value));

		y += rowHeight + spacing;

		this.addRenderableWidget(CycleButton.<Integer>builder(height -> Component.literal(height + "px"), () -> settings.maxHeight)
				.withValues(HEIGHT_STEPS)
				.create(x, y, rowWidth, rowHeight,
						Component.translatable("gui.command-sequencer.hud_max_height"),
						(button, value) -> settings.maxHeight = value));

		y += rowHeight + spacing;

		this.addRenderableWidget(CycleButton.<NextActionHudSettings.Position>builder(
						position -> Component.translatable(positionKey(position)), () -> settings.position)
				.withValues(List.of(NextActionHudSettings.Position.values()))
				.create(x, y, rowWidth, rowHeight,
						Component.translatable("gui.command-sequencer.hud_position"),
						(button, value) -> settings.position = value));

		y += rowHeight + spacing * 3;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(x + rowWidth / 2 - 50, y, 100, rowHeight)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.text(this.font, this.getTitle(), (this.width - this.font.width(this.getTitle())) / 2,
				this.height / 2 - 90, 0xFFFFFF, true);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	private static String positionKey(NextActionHudSettings.Position position) {
		return "gui.command-sequencer.position." + position.name().toLowerCase();
	}
}
