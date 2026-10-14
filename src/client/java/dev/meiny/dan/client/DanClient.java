package dev.meiny.dan.client;

import com.geckolib.renderer.GeoEntityRenderer;
import dev.meiny.dan.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class DanClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntityTypes.DAN,
				context -> new GeoEntityRenderer<>(context, ModEntityTypes.DAN).withScale(0.9375f));
	}
}