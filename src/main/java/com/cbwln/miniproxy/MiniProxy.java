package com.cbwln.miniproxy;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniProxy implements ClientModInitializer {
    public static final String MOD_ID = "miniproxy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ProxyConfig config = ProxyConfig.get();
        ProxyProfile selected = config.getSelected();

        String status = selected == null ? "none in use" : "using " + selected.address();
        LOGGER.info("MiniProxy initialized with {} proxies ({})", config.getProxies().size(), status);
    }
}
