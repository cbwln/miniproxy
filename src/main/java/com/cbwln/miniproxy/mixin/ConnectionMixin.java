package com.cbwln.miniproxy.mixin;

import com.cbwln.miniproxy.MiniProxy;
import com.cbwln.miniproxy.ProxyConfig;
import com.cbwln.miniproxy.ProxyProfile;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.InetSocketAddress;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "configurePacketHandler", at = @At("HEAD"))
    private void miniproxy$install(ChannelPipeline pipeline, CallbackInfo info) {
        ProxyProfile proxy = ProxyConfig.get().getSelected();
        if (proxy == null || proxy.host.isBlank()) {
            return;
        }

        // Must be resolved: ProxyHandler.connect() forwards the proxy address to
        // ctx.connect(), which ends in SocketChannel.connect() and throws
        // UnresolvedAddressException on an unresolved address. The blocking lookup
        // here is one hostname resolution per server connect (IP literals such as
        // 127.0.0.1 do no lookup at all).
        InetSocketAddress proxyAddress = new InetSocketAddress(proxy.host, proxy.port);
        if (proxyAddress.isUnresolved()) {
            MiniProxy.LOGGER.warn("MiniProxy: could not resolve proxy host '{}', failing closed", proxy.host);
        }
        Socks5ProxyHandler handler = proxy.hasAuth()
                ? new Socks5ProxyHandler(proxyAddress, proxy.username, proxy.password)
                : new Socks5ProxyHandler(proxyAddress);
        pipeline.addFirst("miniproxy", handler);
    }
}
