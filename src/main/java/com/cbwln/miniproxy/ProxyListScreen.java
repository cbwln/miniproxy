package com.cbwln.miniproxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ProxyListScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFA0A0A0;
    private static final int GREEN = 0xFF55FF55;

    private static final int HEADER_HEIGHT = 33;
    private static final int FOOTER_HEIGHT = 60;
    private static final int ENTRY_HEIGHT = 30;
    private static final int LIST_ROW_WIDTH = 280;
    private static final int ACTION_BUTTON_WIDTH = 74;
    private static final int DONE_BUTTON_WIDTH = 200;
    private static final int BUTTON_SPACING = 4;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);
    private final Screen parent;
    private final Runnable onDone;

    private ProxyList list;
    private Button useButton;
    private Button editButton;
    private Button deleteButton;

    public ProxyListScreen(Screen parent, Runnable onDone) {
        super(Component.literal("Proxy Settings"));
        this.parent = parent;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(
                new ProxyList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), ENTRY_HEIGHT));
        list.reload(currentSelectedId());

        LinearLayout footer = layout.addToFooter(LinearLayout.vertical().spacing(BUTTON_SPACING));
        footer.defaultCellSetting().alignHorizontallyCenter();

        LinearLayout topRow = footer.addChild(LinearLayout.horizontal().spacing(BUTTON_SPACING));
        useButton = topRow.addChild(Button.builder(Component.literal("Use Proxy"), button -> useSelected())
                .width(ACTION_BUTTON_WIDTH).build());
        topRow.addChild(
                Button.builder(Component.literal("Add"), button -> addProxy()).width(ACTION_BUTTON_WIDTH).build());
        editButton = topRow.addChild(
                Button.builder(Component.literal("Edit"), button -> editSelected()).width(ACTION_BUTTON_WIDTH).build());
        deleteButton = topRow.addChild(Button.builder(Component.literal("Delete"), button -> deleteSelected())
                .width(ACTION_BUTTON_WIDTH).build());
        footer.addChild(
                Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(DONE_BUTTON_WIDTH).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        updateButtons();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
        if (onDone != null) {
            onDone.run();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            useSelected();
            return true;
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (ProxyConfig.get().getProxies().isEmpty()) {
            graphics.centeredText(font, "No proxies yet. Click Add to create one.", width / 2,
                    layout.getHeaderHeight() + 48, GRAY);
        }
    }

    private String currentSelectedId() {
        ProxyProfile selected = ProxyConfig.get().getSelected();
        return selected == null ? null : selected.id;
    }

    private void updateButtons() {
        if (useButton == null) {
            return;
        }
        ProxyEntry entry = list.getSelected();
        boolean hasEntry = entry != null && entry.profile != null;
        useButton.active = entry != null;
        editButton.active = hasEntry;
        deleteButton.active = hasEntry;
    }

    private void useSelected() {
        ProxyEntry entry = list.getSelected();
        if (entry == null) {
            return;
        }
        // A null profile means the "No proxy" (direct connection) row.
        ProxyConfig.get().select(entry.profile);
        onClose();
    }

    private void addProxy() {
        minecraft.gui.setScreen(new ProxyEditScreen(this, null, created -> {
            ProxyConfig.get().add(created);
            list.reload(created.id);
        }));
    }

    private void editSelected() {
        ProxyEntry entry = list.getSelected();
        if (entry == null || entry.profile == null) {
            return;
        }
        minecraft.gui.setScreen(new ProxyEditScreen(this, entry.profile, edited -> {
            ProxyConfig.get().replace(edited);
            list.reload(edited.id);
        }));
    }

    private void deleteSelected() {
        ProxyEntry entry = list.getSelected();
        if (entry == null || entry.profile == null) {
            return;
        }
        ProxyProfile target = entry.profile;
        boolean inUse = target.id.equals(currentSelectedId());
        String message = "\"" + target.label() + "\" will be removed."
                + (inUse ? " It is the proxy in use, so connections will go direct afterwards." : "");

        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ProxyConfig.get().remove(target);
                list.reload(currentSelectedId());
            }
            minecraft.gui.setScreen(this);
        }, Component.literal("Delete this proxy?"), Component.literal(message)));
    }

    private final class ProxyList extends ObjectSelectionList<ProxyEntry> {
        ProxyList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        void reload(String selectId) {
            clearEntries();

            ProxyEntry direct = new ProxyEntry(null);
            addEntry(direct);

            ProxyEntry toSelect = selectId == null ? direct : null;
            for (ProxyProfile proxy : ProxyConfig.get().getProxies()) {
                ProxyEntry entry = new ProxyEntry(proxy);
                addEntry(entry);
                if (proxy.id.equals(selectId)) {
                    toSelect = entry;
                }
            }
            setSelected(toSelect != null ? toSelect : direct);
        }

        @Override
        public void setSelected(ProxyEntry entry) {
            super.setSelected(entry);
            updateButtons();
        }

        @Override
        public int getRowWidth() {
            return LIST_ROW_WIDTH;
        }
    }

    private final class ProxyEntry extends ObjectSelectionList.Entry<ProxyEntry> {
        final ProxyProfile profile;

        ProxyEntry(ProxyProfile profile) {
            this.profile = profile;
        }

        private boolean isInUse() {
            if (profile == null) {
                return ProxyConfig.get().getSelected() == null;
            }
            return profile.id.equals(currentSelectedId());
        }

        @Override
        public Component getNarration() {
            return Component.literal(profile == null ? "No proxy" : profile.label());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
                float partialTick) {
            int x = getContentX();
            int y = getContentY();

            String heading = profile == null ? "No proxy" : profile.label();
            String detail = profile == null
                    ? "Direct connection"
                    : "SOCKS5 " + profile.address() + (profile.hasAuth() ? " (login)" : "");

            int tagWidth = 0;
            if (isInUse()) {
                String tag = "In use";
                tagWidth = font.width(tag) + 8;
                graphics.text(font, tag, getContentRight() - tagWidth + 4,
                        y + (getContentHeight() - font.lineHeight) / 2, GREEN);
            }

            int textWidth = getContentWidth() - tagWidth;
            graphics.text(font, font.plainSubstrByWidth(heading, textWidth), x, y + 2, WHITE);
            graphics.text(font, font.plainSubstrByWidth(detail, textWidth), x, y + 13, GRAY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            list.setSelected(this);
            if (doubleClick && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                useSelected();
            }
            return true;
        }
    }
}
