package dev.meiny.dan;

import dev.meiny.dan.block.ModBlocks;
import dev.meiny.dan.entity.ModEntityTypes;
import dev.meiny.dan.item.ModCreativeTabs;
import dev.meiny.dan.item.ModItems;
import dev.meiny.dan.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dan implements ModInitializer {
	public static final String MOD_ID = "dan";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.init();
		ModEntityTypes.init();
		ModItems.init();
		ModCreativeTabs.init();
		ModSounds.init();
		LOGGER.info("Dan Mod init!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
