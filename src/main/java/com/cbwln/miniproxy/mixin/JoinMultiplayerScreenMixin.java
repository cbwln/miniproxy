package com.cbwln.miniproxy.mixin;

import com.cbwln.miniproxy.ProxyConfig;
import com.cbwln.miniproxy.ProxyListScreen;
import com.cbwln.miniproxy.ProxyProfile;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Unique
    private static final int MINIPROXY_MAX_WIDTH = 130;
    @Unique
    private static final int MINIPROXY_MARGIN = 6;
    @Unique
    private static final int MINIPROXY_PADDING = 12;

    @Unique
    private Button miniproxy$button;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract void refreshServerList();

    @Inject(method = "init()V", at = @At("TAIL"))
    private void miniproxy$addButton(CallbackInfo info) {
        miniproxy$button = addRenderableWidget(Button.builder(Component.empty(),
                button -> minecraft.gui.setScreen(new ProxyListScreen(this, this::refreshServerList)))
                .bounds(0, MINIPROXY_MARGIN, MINIPROXY_MAX_WIDTH, 20)
                .build());
        miniproxy$updateButton();
    }

    @Inject(method = "repositionElements()V", at = @At("TAIL"))
    private void miniproxy$reposition(CallbackInfo info) {
        miniproxy$updateButton();
    }

    @Unique
    private void miniproxy$updateButton() {
        if (miniproxy$button == null) {
            return;
        }

        ProxyProfile selected = ProxyConfig.get().getSelected();
        String prefix = "Proxy: ";
        String name = selected == null ? "None" : selected.label();

        int available = MINIPROXY_MAX_WIDTH - MINIPROXY_PADDING - font.width(prefix);
        if (font.width(name) > available) {
            String ellipsis = CommonComponents.ELLIPSIS.getString();
            name = font.plainSubstrByWidth(name, available - font.width(ellipsis)) + ellipsis;
        }

        String text = prefix + name;
        int buttonWidth = Math.min(MINIPROXY_MAX_WIDTH, font.width(text) + MINIPROXY_PADDING);

        miniproxy$button.setMessage(Component.literal(text));
        miniproxy$button.setWidth(buttonWidth);
        miniproxy$button.setX(width - buttonWidth - MINIPROXY_MARGIN);
        miniproxy$button.setY(MINIPROXY_MARGIN);
        miniproxy$button.setTooltip(Tooltip.create(Component.literal(selected == null
                ? "Direct connection (no proxy). Click to manage proxies."
                : "Using " + selected.label() + " (" + selected.address() + "). Click to manage proxies.")));
    }
}
