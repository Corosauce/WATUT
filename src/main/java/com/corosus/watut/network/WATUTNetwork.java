package com.corosus.watut.network;

import com.corosus.watut.WATUT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class WATUTNetwork {
	private static final String PROTOCOL_VERSION = "1";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(WATUT.MODID, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	public static void register() {

		int id = 0;

		CHANNEL.messageBuilder(ToClientPlayerStatusMessage.class, id++)
				.encoder(ToClientPlayerStatusMessage::encode)
				.decoder(ToClientPlayerStatusMessage::decode)
				.consumer(ToClientPlayerStatusMessage::handle)
				.add();

		CHANNEL.messageBuilder(ToServerPlayerStatusMessage.class, id++)
				.encoder(ToServerPlayerStatusMessage::encode)
				.decoder(ToServerPlayerStatusMessage::decode)
				.consumer(ToServerPlayerStatusMessage::handle)
				.add();
	}
}
