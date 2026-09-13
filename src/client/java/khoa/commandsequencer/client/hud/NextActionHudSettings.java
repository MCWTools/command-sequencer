package khoa.commandsequencer.client.hud;

/** Next Action HUD settings (spec section 10): ON/OFF, maximum width, maximum height, position. */
public class NextActionHudSettings {

	public boolean enabled = true;
	public int maxWidth = 200;
	public int maxHeight = 40;
	public Position position = Position.TOP_LEFT;

	public enum Position {
		TOP_LEFT, TOP_CENTER, TOP_RIGHT,
		MIDDLE_LEFT, CENTER, MIDDLE_RIGHT,
		BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

		/** Returns {x, y} for the top-left corner of a box of the given size on this preset. */
		public int[] resolveOrigin(int screenWidth, int screenHeight, int boxWidth, int boxHeight) {
			int margin = 8;
			int x;
			int y;

			switch (this) {
				case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> x = margin;
				case TOP_CENTER, CENTER, BOTTOM_CENTER -> x = (screenWidth - boxWidth) / 2;
				default -> x = screenWidth - boxWidth - margin; // *_RIGHT
			}

			switch (this) {
				case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> y = margin;
				case MIDDLE_LEFT, CENTER, MIDDLE_RIGHT -> y = (screenHeight - boxHeight) / 2;
				default -> y = screenHeight - boxHeight - margin; // BOTTOM_*
			}

			return new int[] { x, y };
		}
	}
}
