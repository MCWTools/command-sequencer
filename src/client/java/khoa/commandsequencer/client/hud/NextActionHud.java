package khoa.commandsequencer.client.hud;

import khoa.commandsequencer.client.CommandSequencerClient;
import khoa.commandsequencer.client.script.ActionRunner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Renders the "Next Action" box (spec section 9). Shows the command that WILL run on
 * the next Run Next Action press, not the one that just ran.
 *
 * Width/Height/Position settings (spec section 10) are read from {@link NextActionHudSettings}.
 * Truncation with maximum width, and wrapping/clipping with maximum height, are both
 * handled here rather than delegated to Minecraft's Font class, since Font has no
 * built-in "truncate with ellipsis to a pixel width" helper.
 */
public final class NextActionHud {

	private NextActionHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, NextActionHudSettings settings) {
		if (!settings.enabled) {
			return;
		}

		ActionRunner runner = CommandSequencerClient.getActionRunner();
		if (runner == null || runner.getScript() == null) {
			return;
		}

		String label = "Next Action:";
		String command = describeState(runner);

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;

		int padding = 4;
		int lineHeight = font.lineHeight;

		String truncatedCommand = truncateToWidth(font, command, settings.maxWidth - padding * 2);
		int contentWidth = Math.max(font.width(label), font.width(truncatedCommand)) + padding * 2;
		int boxWidth = Math.min(contentWidth, settings.maxWidth);

		int linesNeeded = 2; // label + command, no wrapping in this minimal skeleton
		int boxHeight = Math.min(lineHeight * linesNeeded + padding * 2, settings.maxHeight);

		int[] origin = settings.position.resolveOrigin(
				client.getWindow().getGuiScaledWidth(),
				client.getWindow().getGuiScaledHeight(),
				boxWidth,
				boxHeight
		);
		int x = origin[0];
		int y = origin[1];

		graphics.fill(x, y, x + boxWidth, y + boxHeight, 0x90000000);
		graphics.text(font, label, x + padding, y + padding, 0xFFFFFFFF, true);
		graphics.text(font, truncatedCommand, x + padding, y + padding + lineHeight, 0xFFAAAAAA, true);
	}

	private static String describeState(ActionRunner runner) {
		switch (runner.getState()) {
			case FINISHED:
				return "Finished";
			case RESETTING:
				return "Resetting...";
			default:
				String next = runner.peekNextCommand();
				return next == null ? "Finished" : next;
		}
	}

	private static String truncateToWidth(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		int ellipsisWidth = font.width(ellipsis);
		StringBuilder builder = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (font.width(builder.toString() + c) + ellipsisWidth > maxWidth) {
				break;
			}
			builder.append(c);
		}
		return builder + ellipsis;
	}
}
