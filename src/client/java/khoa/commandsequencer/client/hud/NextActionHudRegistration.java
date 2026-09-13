package khoa.commandsequencer.client.hud;

import khoa.commandsequencer.CommandSequencer;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.minecraft.resources.Identifier;

/**
 * NOTE: HudElementRegistry is a Fabric API class, not bundled in the vanilla client jar,
 * so its exact signature could not be verified against the uploaded 26_1_2_Fabric.jar
 * (that jar only contains Mojang's client, not Fabric API). This uses the registration
 * shape Fabric API has kept stable across recent versions
 * (register(Identifier, HudElement) with a functional render(GuiGraphicsExtractor, DeltaTracker)).
 * If fabric-loom reports a signature mismatch here, check the fabric-api version's
 * HudElementRegistry javadoc/source directly - this is the one call in the mod most
 * likely to need a small adjustment.
 */
public final class NextActionHudRegistration {

	public static final NextActionHudSettings SETTINGS = new NextActionHudSettings();

	private NextActionHudRegistration() {
	}

	public static void register() {
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(CommandSequencer.MOD_ID, "next_action"),
				(graphics, tickCounter) -> NextActionHud.render(graphics, SETTINGS)
		);
	}
}
