package com.cbwln.miniproxy;

import java.util.UUID;
import net.minecraft.util.StringUtil;

/**
 * A single SOCKS5 proxy entry. Fields are public so Gson can serialize them
 * directly.
 */
public final class ProxyProfile {
    public static final int DEFAULT_PORT = 1080;
    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65535;

    public String id;
    public String name;
    public String host;
    public int port;
    public String username;
    public String password;

    public ProxyProfile(String id, String name, String host, int port, String username, String password) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String label() {
        return StringUtil.isBlank(name) ? address() : name;
    }

    public String address() {
        return host + ":" + port;
    }

    public boolean hasAuth() {
        return !StringUtil.isBlank(username);
    }

    /**
     * Normalizes fields loaded from disk.
     *
     * @return false if the entry is unusable and should be skipped.
     */
    boolean sanitize() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        name = name == null ? "" : name;
        host = host == null ? "" : host.strip();
        username = username == null ? "" : username.strip();
        password = password == null ? "" : password.strip();

        if (port < MIN_PORT || port > MAX_PORT) {
            port = DEFAULT_PORT;
        }
        return !host.isEmpty();
    }
}
