package com.cbwln.miniproxy;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

class ProxyEditScreen extends Screen {
    private static final int LABEL_COLOR = 0xFFA0A0A0;
    private static final int ERROR_COLOR = 0xFFFF5555;
    private static final int TITLE_COLOR = 0xFFFFFFFF;

    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTONS_Y = 176;

    private final Screen parent;
    private final ProxyProfile original;
    private final Consumer<ProxyProfile> onSave;

    private String name;
    private String host;
    private String port;
    private String username;
    private String password;

    private EditBox nameBox;
    private EditBox hostBox;
    private EditBox portBox;
    private EditBox usernameBox;
    private EditBox passwordBox;
    private Button saveButton;

    public ProxyEditScreen(Screen parent, ProxyProfile original, Consumer<ProxyProfile> onSave) {
        super(Component.literal(original == null ? "Add Proxy" : "Edit Proxy"));
        this.parent = parent;
        this.original = original;
        this.onSave = onSave;
        this.name = original == null ? "" : original.name;
        this.host = original == null ? "" : original.host;
        this.port = original == null ? Integer.toString(ProxyProfile.DEFAULT_PORT) : Integer.toString(original.port);
        this.username = original == null ? "" : original.username;
        this.password = original == null ? "" : original.password;
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;

        nameBox = addBox(left, 45, 200, 32, name, "Optional", value -> name = value);
        hostBox = addBox(left, 79, 136, 255, host, "127.0.0.1", value -> host = value);
        portBox = addBox(left + 142, 79, 58, 5, port, Integer.toString(ProxyProfile.DEFAULT_PORT),
                value -> port = value);
        usernameBox = addBox(left, 113, 200, 255, username, "Leave empty for none", value -> username = value);
        passwordBox = addBox(left, 147, 200, 255, password, "Leave empty for none", value -> password = value);

        passwordBox
                .addFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));

        saveButton = addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(left, BUTTONS_Y, 98, FIELD_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(left + 102, BUTTONS_Y, 98, FIELD_HEIGHT).build());

        validate();
    }

    private EditBox addBox(int x, int y, int width, int maxLength, String value, String hint,
            Consumer<String> onChange) {
        EditBox box = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.literal(hint));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setHint(Component.literal(hint));
        box.setResponder(text -> {
            onChange.accept(text);
            validate();
        });
        return addRenderableWidget(box);
    }

    @Override
    protected void setInitialFocus() {
        super.setInitialFocus(hostBox);
    }

    private OptionalInt parsedPort() {
        String trimmed = port.strip();
        if (trimmed.isEmpty() || trimmed.chars().anyMatch(c -> !Character.isDigit(c))) {
            return OptionalInt.empty();
        }
        int parsed = Integer.parseInt(trimmed);
        return parsed >= ProxyProfile.MIN_PORT && parsed <= ProxyProfile.MAX_PORT
                ? OptionalInt.of(parsed)
                : OptionalInt.empty();
    }

    private boolean hostValid() {
        String trimmed = host.strip();
        return !trimmed.isEmpty() && trimmed.chars().noneMatch(Character::isWhitespace);
    }

    private void validate() {
        if (saveButton == null) {
            return;
        }
        OptionalInt parsed = parsedPort();
        saveButton.active = hostValid() && parsed.isPresent();
        portBox.setTextColor(parsed.isPresent() ? EditBox.DEFAULT_TEXT_COLOR : ERROR_COLOR);
    }

    private void save() {
        OptionalInt parsed = parsedPort();
        if (!hostValid() || parsed.isEmpty()) {
            return;
        }
        String id = original == null ? UUID.randomUUID().toString() : original.id;
        onSave.accept(new ProxyProfile(id, name.strip(), host.strip(), parsed.getAsInt(), username.strip(),
                password.strip()));
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && saveButton.active) {
            save();
            return true;
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 100;
        graphics.centeredText(font, title, width / 2, 17, TITLE_COLOR);
        graphics.text(font, "Name", left, 34, LABEL_COLOR);
        graphics.text(font, "Host", left, 68, LABEL_COLOR);
        graphics.text(font, "Port", left + 142, 68, parsedPort().isPresent() ? LABEL_COLOR : ERROR_COLOR);
        graphics.text(font, "Username", left, 102, LABEL_COLOR);
        graphics.text(font, "Password", left, 136, LABEL_COLOR);
    }
}
