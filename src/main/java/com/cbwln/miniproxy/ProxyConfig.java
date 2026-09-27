package com.cbwln.miniproxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and persists the proxy list to {@code config/miniproxy.json}.
 */
public final class ProxyConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("miniproxy.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static ProxyConfig instance;

    private final List<ProxyProfile> proxies = new CopyOnWriteArrayList<>();
    private volatile String selectedId;

    private ProxyConfig() {
    }

    public static synchronized ProxyConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public List<ProxyProfile> getProxies() {
        return Collections.unmodifiableList(proxies);
    }

    public ProxyProfile getSelected() {
        String id = selectedId;
        if (id == null) {
            return null;
        }
        for (ProxyProfile proxy : proxies) {
            if (proxy.id.equals(id)) {
                return proxy;
            }
        }
        return null;
    }

    public void select(ProxyProfile proxy) {
        selectedId = proxy == null ? null : proxy.id;
        save();
    }

    public void add(ProxyProfile proxy) {
        proxies.add(proxy);
        save();
    }

    public void replace(ProxyProfile updated) {
        for (int i = 0; i < proxies.size(); i++) {
            if (proxies.get(i).id.equals(updated.id)) {
                proxies.set(i, updated);
                break;
            }
        }
        save();
    }

    public void remove(ProxyProfile proxy) {
        proxies.removeIf(entry -> entry.id.equals(proxy.id));
        if (proxy.id.equals(selectedId)) {
            selectedId = null;
        }
        save();
    }

    private synchronized void save() {
        Data data = new Data();
        data.selected = selectedId;
        data.proxies = new ArrayList<>(proxies);

        try {
            Files.createDirectories(PATH.getParent());
            Path tmp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MiniProxy.LOGGER.warn("Could not save {}: {}", PATH.getFileName(), e.toString());
        }
    }

    private static ProxyConfig load() {
        ProxyConfig config = new ProxyConfig();

        if (!Files.exists(PATH)) {
            config.save();
            return config;
        }

        try (BufferedReader reader = Files.newBufferedReader(PATH)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                if (data.proxies != null) {
                    for (ProxyProfile proxy : data.proxies) {
                        if (proxy != null && proxy.sanitize()) {
                            config.proxies.add(proxy);
                        }
                    }
                }
                config.selectedId = data.selected;
            }
        } catch (JsonParseException | IOException e) {
            MiniProxy.LOGGER.warn("Could not read {}: {}", PATH.getFileName(), e.toString());
        }

        return config;
    }

    private static final class Data {
        String selected;
        List<ProxyProfile> proxies = new ArrayList<>();
    }
}
