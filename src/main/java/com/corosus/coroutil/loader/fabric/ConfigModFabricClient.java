package com.corosus.coroutil.loader.fabric;

import com.corosus.coroutil.common.core.command.CommandCoroConfigClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ConfigModFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.


		CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
			CommandCoroConfigClient.register(dispatcher);
		}));
	}


}