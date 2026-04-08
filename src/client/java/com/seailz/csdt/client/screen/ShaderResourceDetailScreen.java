package com.seailz.csdt.client.screen;

import com.seailz.csdt.client.service.ClientToastService;
import com.seailz.csdt.client.service.ForcedPostEffectService;
import com.seailz.csdt.client.service.GlslSyntaxHighlightService;
import com.seailz.csdt.client.service.PostEffectVisualizationService;
import com.seailz.csdt.client.service.ShaderInventoryService;
import com.seailz.csdt.client.service.ShaderReloadService;
import com.seailz.csdt.client.service.ShaderResourceOverrideService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ShaderResourceDetailScreen extends Screen {

    private static final int GUTTER_WIDTH = 44;

    private final Screen parent;
    private final ShaderInventoryScreen.ViewMode returnViewMode;
    private final int returnPage;
    private final String returnSearchQuery;
    private final ShaderInventoryService.ShaderResourceEntry entry;
    private final int versionIndex;
    private final String draftContent;

    private MultiLineEditBox contentBox;
    private Button previousSourceButton;
    private Button nextSourceButton;
    private Button useSourceButton;
    private Button createOverrideButton;
    private Button visualizeButton;
    private Button forcePostEffectButton;
    private Button saveButton;
    private Button deleteButton;
    private List<GlslSyntaxHighlightService.LineTokens> highlightedLines = List.of();
    private List<VisualLine> visualLines = List.of();
    private PostEffectVisualizationService.Visualization postEffectVisualization = PostEffectVisualizationService.visualize("{}");
    private List<PostSummaryRow> postSummaryRows = List.of();
    private int postSummaryScroll;

    public ShaderResourceDetailScreen(Screen parent, ShaderInventoryScreen.ViewMode returnViewMode, int returnPage, ShaderInventoryService.ShaderResourceEntry entry) {
        this(parent, returnViewMode, returnPage, entry, "", Math.max(0, entry.versions().size() - 1), null);
    }

    public ShaderResourceDetailScreen(Screen parent, ShaderInventoryScreen.ViewMode returnViewMode, int returnPage, ShaderInventoryService.ShaderResourceEntry entry, String returnSearchQuery) {
        this(parent, returnViewMode, returnPage, entry, returnSearchQuery, Math.max(0, entry.versions().size() - 1), null);
    }

    private ShaderResourceDetailScreen(Screen parent, ShaderInventoryScreen.ViewMode returnViewMode, int returnPage, ShaderInventoryService.ShaderResourceEntry entry, String returnSearchQuery, int versionIndex, String draftContent) {
        super(Component.literal("Shader File"));
        this.parent = parent;
        this.returnViewMode = returnViewMode;
        this.returnPage = returnPage;
        this.returnSearchQuery = returnSearchQuery == null ? "" : returnSearchQuery;
        this.entry = entry;
        this.versionIndex = versionIndex;
        this.draftContent = draftContent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int contentWidth = Math.min(920, this.width - 60);
        int contentHeight = Math.max(180, this.height - 170);
        int contentLeft = centerX - contentWidth / 2;
        int contentTop = 96;
        int summaryWidth = summaryWidth();
        int editorLeft = contentLeft + GUTTER_WIDTH;
        int editorWidth = contentWidth - GUTTER_WIDTH - (summaryWidth == 0 ? 0 : summaryWidth + 12);

        if (hasMultipleSources()) {
            this.previousSourceButton = this.addRenderableWidget(Button.builder(Component.literal("< Source"), button -> this.openVersion(this.versionIndex - 1))
                    .bounds(centerX - 260, 58, 100, 20)
                    .build());
            this.useSourceButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleSourceSelection())
                    .bounds(centerX - 150, 58, 130, 20)
                    .build());
            this.nextSourceButton = this.addRenderableWidget(Button.builder(Component.literal("Source >"), button -> this.openVersion(this.versionIndex + 1))
                    .bounds(centerX - 10, 58, 100, 20)
                    .build());
        } else {
            this.createOverrideButton = this.addRenderableWidget(Button.builder(Component.literal("Override"), button -> this.createOverride())
                    .bounds(centerX - 260, 58, 120, 20)
                    .build());
        }

        this.visualizeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleVisualization())
                .bounds(centerX + 110, 58, 130, 20)
                .build());
        this.forcePostEffectButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleForcedPostEffect())
                .bounds(centerX + 110, 58, 130, 20)
                .build());

        this.contentBox = this.addRenderableWidget(MultiLineEditBox.builder()
                .setX(editorLeft)
                .setY(contentTop)
                .setPlaceholder(Component.literal("Shader source"))
                .setTextColor(0x00FFFFFF)
                .setCursorColor(0xFFFFFFFF)
                .setShowBackground(false)
                .build(this.font, editorWidth, contentHeight, Component.literal("Shader source editor")));
        this.contentBox.setCharacterLimit(Integer.MAX_VALUE);
        this.contentBox.setLineLimit(Integer.MAX_VALUE);
        this.contentBox.setValue(this.draftContent == null ? loadCurrentText() : this.draftContent, true);
        this.contentBox.setValueListener(this::handleContentChanged);
        handleContentChanged(this.contentBox.getValue());
        if (canEditCurrentVersion()) {
            this.setInitialFocus(this.contentBox);
            this.contentBox.setFocused(true);
        }

        this.addRenderableWidget(Button.builder(Component.literal("Copy"), button -> {
            this.minecraft.keyboardHandler.setClipboard(currentText());
            ClientToastService.showInfo("Shader copied", currentVersion().packId());
        }).bounds(centerX - 210, this.height - 32, 80, 20).build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.saveCurrent())
                .bounds(centerX - 120, this.height - 32, 80, 20)
                .build());
        this.deleteButton = this.addRenderableWidget(Button.builder(Component.literal("Delete"), button -> this.deleteCurrent())
                .bounds(centerX - 30, this.height - 32, 80, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(centerX + 60, this.height - 32, 100, 20)
                .build());

        this.refreshButtons();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(new ShaderInventoryScreen(this.parent, this.returnViewMode, this.returnPage, this.returnSearchQuery));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overEditor(mouseX, mouseY)) {
            return this.contentBox.mouseScrolled(this.contentBox.getX() + 1.0D, mouseY, scrollX, scrollY);
        }
        if (overSummary(mouseX, mouseY)) {
            this.postSummaryScroll = Math.clamp(this.postSummaryScroll - (int) Math.signum(scrollY) * 2, 0, maxPostSummaryScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!canEditCurrentVersion() && isMutationKey(event)) {
            return false;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!canEditCurrentVersion()) {
            return false;
        }
        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB010141A);
        renderEditorChrome(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, Component.literal(ShaderInventoryService.displayPath(this.entry.path())), this.width / 2, 14, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, Component.literal(sourceSummary()), this.width / 2, 34, 0xFFA8C2D8);
        guiGraphics.centeredText(this.font, Component.literal(statusSummary()), this.width / 2, 46, 0xFFD7E3F0);

        renderHighlightedEditor(guiGraphics);
        renderPostEffectSummary(guiGraphics);
    }

    private void renderEditorChrome(GuiGraphicsExtractor guiGraphics) {
        int contentWidth = Math.min(920, this.width - 60);
        int contentHeight = Math.max(180, this.height - 170);
        int contentLeft = this.width / 2 - contentWidth / 2;
        int contentTop = 96;
        int summaryWidth = summaryWidth();
        int editorRight = contentLeft + contentWidth - (summaryWidth == 0 ? 0 : summaryWidth + 12);

        guiGraphics.fill(contentLeft, contentTop, editorRight, contentTop + contentHeight, 0x4416202A);
        guiGraphics.fill(contentLeft, contentTop, contentLeft + GUTTER_WIDTH, contentTop + contentHeight, 0x66212D38);
        guiGraphics.verticalLine(contentLeft + GUTTER_WIDTH - 1, contentTop, contentTop + contentHeight, 0xAA3A4A57);

        if (summaryWidth > 0) {
            int summaryLeft = editorRight + 12;
            guiGraphics.fill(summaryLeft, contentTop, summaryLeft + summaryWidth, contentTop + contentHeight, 0x3316202A);
            guiGraphics.outline(summaryLeft, contentTop, summaryWidth, contentHeight, 0x88445A6B);
        }
    }

    private void renderHighlightedEditor(GuiGraphicsExtractor guiGraphics) {
        int editorLeft = this.contentBox.getX();
        int editorTop = this.contentBox.getY();
        int editorRight = editorLeft + this.contentBox.getWidth();
        int editorBottom = editorTop + this.contentBox.getHeight();
        double scroll = this.contentBox.scrollAmount();
        int lineHeight = this.font.lineHeight;
        int innerLeft = innerLeft();
        int innerTop = innerTop();
        int firstLine = Math.max(0, (int) (scroll / lineHeight));
        int yOffset = (int) (scroll % lineHeight);
        int y = innerTop - yOffset;

        guiGraphics.enableScissor(editorLeft - GUTTER_WIDTH, editorTop, editorRight, editorBottom);
        for (int lineIndex = firstLine; lineIndex < this.visualLines.size() && y < editorBottom; lineIndex++) {
            if (y + lineHeight >= editorTop) {
                VisualLine visualLine = this.visualLines.get(lineIndex);
                String lineNumber = visualLine.showLineNumber() ? "%4d".formatted(visualLine.sourceLineNumber()) : "    ";
                guiGraphics.text(this.font, lineNumber, editorLeft - GUTTER_WIDTH + 6, y, 0xFF7A8795, false);
                int x = innerLeft;
                for (GlslSyntaxHighlightService.Token token : visualLine.tokens()) {
                    guiGraphics.text(this.font, token.text(), x, y, token.color(), false);
                    x += this.font.width(token.text());
                }
            }
            y += lineHeight;
        }
        guiGraphics.disableScissor();
    }

    private void renderPostEffectSummary(GuiGraphicsExtractor guiGraphics) {
        if (!isPostEffect()) {
            return;
        }

        int contentWidth = Math.min(920, this.width - 60);
        int contentHeight = Math.max(180, this.height - 170);
        int contentLeft = this.width / 2 - contentWidth / 2;
        int contentTop = 96;
        int summaryWidth = summaryWidth();
        int summaryLeft = contentLeft + contentWidth - summaryWidth;
        int y = contentTop + 8;
        int bodyTop = y + this.font.lineHeight + 6;
        int bodyBottom = contentTop + contentHeight - 8;

        guiGraphics.text(this.font, "Post Graph", summaryLeft + 8, y, 0xFFFFFFFF, false);
        y = bodyTop - this.postSummaryScroll * this.font.lineHeight;
        guiGraphics.enableScissor(summaryLeft + 6, bodyTop, summaryLeft + summaryWidth - 6, bodyBottom);

        if (!this.postEffectVisualization.parsed()) {
            int end = Math.min(this.postSummaryRows.size(), this.postSummaryScroll + visiblePostSummaryRows() + 2);
            for (int i = this.postSummaryScroll; i < end; i++) {
                PostSummaryRow row = this.postSummaryRows.get(i);
                if (y + this.font.lineHeight >= bodyTop) {
                    guiGraphics.text(this.font, row.text(), summaryLeft + 8, y, row.color(), false);
                }
                y += this.font.lineHeight;
                if (y > bodyBottom) {
                    break;
                }
            }
            guiGraphics.disableScissor();
            return;
        }

        y = renderGraphSection(guiGraphics, summaryLeft, summaryWidth, y, "Targets", 0xFF4CC9F0, buildTargetRows());
        renderGraphSection(guiGraphics, summaryLeft, summaryWidth, y + 4, "Passes", 0xFFFFD166, buildPassRows());
        guiGraphics.disableScissor();
    }

    private void toggleSourceSelection() {
        String selectedPack = ShaderResourceOverrideService.selectedPack(this.entry.path());
        if (currentVersion().packId().equals(selectedPack)) {
            ShaderResourceOverrideService.clearSourceSelection(this.entry.path());
            ClientToastService.showInfo("Shader source reset", this.entry.path());
        } else {
            ShaderResourceOverrideService.selectSource(this.entry.path(), currentVersion().packId());
            ClientToastService.showInfo("Shader source selected", currentVersion().packId());
        }
        ShaderReloadService.reloadAllShaders();
        this.refreshButtons();
    }

    private void createOverride() {
        try {
            ShaderInventoryService.OverrideCreationResult result = ShaderInventoryService.createOverrideInPreferredPack(this.entry.path(), currentText());
            ShaderResourceOverrideService.selectSource(this.entry.path(), result.packId());
            ClientToastService.showInfo("Override created", result.path().toString());
            ShaderReloadService.reloadAllShaders();
            reopenFreshEntry(result.packId());
        } catch (IOException exception) {
            ClientToastService.showInfo("Override failed", exception.getMessage());
        }
    }

    private void toggleVisualization() {
        if (!isFragmentShader()) {
            return;
        }
        boolean visualized = !ShaderResourceOverrideService.isVisualized(this.entry.path());
        ShaderResourceOverrideService.setVisualized(this.entry.path(), visualized);
        ClientToastService.showInfo(visualized ? "Visualization enabled" : "Visualization disabled", this.entry.path());
        ShaderReloadService.reloadAllShaders();
        this.refreshButtons();
    }

    private void saveCurrent() {
        if (!canEditCurrentVersion()) {
            ClientToastService.showInfo("Save blocked", "Only overridden shader sources are editable");
            return;
        }
        try {
            ShaderInventoryService.saveText(currentVersion(), currentText());
            ClientToastService.showInfo("Shader saved", currentVersion().displaySource());
            ShaderReloadService.reloadAllShaders();
        } catch (IOException exception) {
            ClientToastService.showInfo("Save failed", exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private void deleteCurrent() {
        if (!canDeleteCurrentVersion()) {
            ClientToastService.showInfo("Delete blocked", "Vanilla sources cannot be deleted");
            return;
        }
        try {
            String deletedPack = currentVersion().packId();
            ShaderInventoryService.deleteText(currentVersion());
            if (deletedPack.equals(ShaderResourceOverrideService.selectedPack(this.entry.path()))) {
                ShaderResourceOverrideService.clearSourceSelection(this.entry.path());
            }
            ClientToastService.showInfo("Source deleted", currentVersion().displaySource());
            ShaderReloadService.reloadAllShaders();
            reopenAfterDelete();
        } catch (IOException exception) {
            ClientToastService.showInfo("Delete failed", exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private void openVersion(int newVersionIndex) {
        int clamped = Math.max(0, Math.min(this.entry.versions().size() - 1, newVersionIndex));
        this.minecraft.gui.setScreen(new ShaderResourceDetailScreen(this.parent, this.returnViewMode, this.returnPage, this.entry, this.returnSearchQuery, clamped, null));
    }

    private void reopenFreshEntry(String preferredPack) {
        ShaderInventoryService.ShaderResourceEntry refreshed = ShaderInventoryService.findEntry(this.entry.path());
        if (refreshed == null) {
            return;
        }

        int refreshedIndex = Math.max(0, refreshed.versions().size() - 1);
        for (int i = 0; i < refreshed.versions().size(); i++) {
            if (refreshed.versions().get(i).packId().equals(preferredPack)) {
                refreshedIndex = i;
            }
        }
        this.minecraft.gui.setScreen(new ShaderResourceDetailScreen(this.parent, this.returnViewMode, this.returnPage, refreshed, this.returnSearchQuery, refreshedIndex, null));
    }

    private void reopenAfterDelete() {
        ShaderInventoryService.ShaderResourceEntry refreshed = ShaderInventoryService.findEntry(this.entry.path());
        if (refreshed == null) {
            this.onClose();
            return;
        }
        this.minecraft.gui.setScreen(new ShaderResourceDetailScreen(this.parent, this.returnViewMode, this.returnPage, refreshed, this.returnSearchQuery, Math.max(0, refreshed.versions().size() - 1), null));
    }

    private void handleContentChanged(String value) {
        this.highlightedLines = GlslSyntaxHighlightService.tokenize(value, highlightLanguage());
        this.visualLines = wrapVisualLines();
        if (isPostEffect()) {
            this.postEffectVisualization = PostEffectVisualizationService.visualize(value);
            this.postSummaryRows = buildPostSummaryRows();
            this.postSummaryScroll = Math.min(this.postSummaryScroll, maxPostSummaryScroll());
        }
    }

    private void refreshButtons() {
        if (this.previousSourceButton != null) {
            this.previousSourceButton.active = this.versionIndex > 0;
        }
        if (this.nextSourceButton != null) {
            this.nextSourceButton.active = this.versionIndex + 1 < this.entry.versions().size();
        }
        if (this.useSourceButton != null) {
            String selectedPack = ShaderResourceOverrideService.selectedPack(this.entry.path());
            boolean usingCurrentSource = currentVersion().packId().equals(selectedPack);
            this.useSourceButton.setMessage(Component.literal(usingCurrentSource ? "Reset Source" : "Use This Source"));
        }
        if (this.createOverrideButton != null) {
            this.createOverrideButton.visible = !hasMultipleSources();
            this.createOverrideButton.active = !hasMultipleSources();
        }

        boolean fragmentShader = isFragmentShader();
        this.visualizeButton.visible = fragmentShader;
        this.visualizeButton.active = fragmentShader;
        if (fragmentShader) {
            this.visualizeButton.setMessage(Component.literal(ShaderResourceOverrideService.isVisualized(this.entry.path()) ? "Stop Visualization" : "Visualize"));
        }

        boolean postEffect = isPostEffect();
        this.forcePostEffectButton.visible = postEffect;
        this.forcePostEffectButton.active = postEffect;
        if (postEffect) {
            this.forcePostEffectButton.setMessage(Component.literal(ForcedPostEffectService.isForced(this.entry.path()) ? "Release Effect" : "Force On"));
        }

        this.saveButton.active = canEditCurrentVersion();
        this.deleteButton.visible = canDeleteCurrentVersion();
        this.deleteButton.active = canDeleteCurrentVersion();
    }

    private ShaderInventoryService.ShaderResourceVersion currentVersion() {
        return this.entry.versions().get(this.versionIndex);
    }

    private String sourceSummary() {
        return hasMultipleSources()
                ? "Source %d/%d: %s".formatted(this.versionIndex + 1, this.entry.versions().size(), currentVersion().packId())
                : "Source: " + currentVersion().packId();
    }

    private String statusSummary() {
        String selectedPack = ShaderResourceOverrideService.selectedPack(this.entry.path());
        String active = selectedPack == null ? this.entry.activePack() : selectedPack + " (forced)";
        return canEditCurrentVersion() ? "Editable | In use: " + active : "Read only | In use: " + active;
    }

    private String loadCurrentText() {
        try {
            return ShaderInventoryService.loadText(currentVersion());
        } catch (IOException exception) {
            return "Failed to read shader content.\n" + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private String currentText() {
        return this.contentBox == null ? loadCurrentText() : this.contentBox.getValue();
    }

    private boolean hasMultipleSources() {
        return this.entry.versions().size() > 1;
    }

    private boolean canEditCurrentVersion() {
        return this.entry.overridden()
                && currentVersion().editable()
                && !"vanilla".equalsIgnoreCase(currentVersion().packId());
    }

    private boolean canDeleteCurrentVersion() {
        return currentVersion().editable() && !"vanilla".equalsIgnoreCase(currentVersion().packId());
    }

    private boolean isFragmentShader() {
        return this.entry.path().endsWith(".fsh");
    }

    private boolean isGlslFile() {
        return this.entry.path().endsWith(".fsh") || this.entry.path().endsWith(".vsh") || this.entry.path().endsWith(".glsl");
    }

    private GlslSyntaxHighlightService.Language highlightLanguage() {
        if (isPostEffect()) {
            return GlslSyntaxHighlightService.Language.JSON;
        }
        if (isGlslFile()) {
            return GlslSyntaxHighlightService.Language.GLSL;
        }
        return GlslSyntaxHighlightService.Language.PLAIN;
    }

    private boolean isPostEffect() {
        return this.entry.path().contains(":post_effect/");
    }

    private boolean overEditor(double mouseX, double mouseY) {
        int contentWidth = Math.min(920, this.width - 60);
        int contentHeight = Math.max(180, this.height - 170);
        int contentLeft = this.width / 2 - contentWidth / 2;
        int contentTop = 96;
        int editorRight = contentLeft + contentWidth - (summaryWidth() == 0 ? 0 : summaryWidth() + 12);
        return mouseX >= contentLeft && mouseX <= editorRight
                && mouseY >= contentTop && mouseY <= contentTop + contentHeight;
    }

    private void toggleForcedPostEffect() {
        if (!isPostEffect()) {
            return;
        }
        ForcedPostEffectService.toggleForcedPostEffect(this.entry.path());
        ShaderReloadService.reloadPostShadersOnly();
        this.refreshButtons();
    }

    private int innerTop() {
        return invokeIntMethod("getInnerTop", this.contentBox.getY());
    }

    private int innerLeft() {
        return invokeIntMethod("getInnerLeft", this.contentBox.getX());
    }

    private int invokeIntMethod(String name, int fallback) {
        try {
            Method method = this.contentBox.getClass().getSuperclass().getDeclaredMethod(name);
            method.setAccessible(true);
            return (int) method.invoke(this.contentBox);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean isMutationKey(KeyEvent event) {
        return switch (event.key()) {
            case 259, 261, 257 -> true;
            default -> !event.hasControlDown() && !event.hasAltDown() && event.key() >= 32;
        };
    }

    private int summaryWidth() {
        return isPostEffect() ? Math.min(280, Math.max(220, this.width / 4)) : 0;
    }

    private boolean overSummary(double mouseX, double mouseY) {
        if (!isPostEffect()) {
            return false;
        }
        int contentWidth = Math.min(920, this.width - 60);
        int contentHeight = Math.max(180, this.height - 170);
        int contentLeft = this.width / 2 - contentWidth / 2;
        int contentTop = 96;
        int summaryLeft = contentLeft + contentWidth - summaryWidth();
        return mouseX >= summaryLeft && mouseX <= summaryLeft + summaryWidth()
                && mouseY >= contentTop && mouseY <= contentTop + contentHeight;
    }

    private List<VisualLine> wrapVisualLines() {
        int maxWidth = Math.max(32, this.contentBox.getWidth() - 12);
        List<VisualLine> wrapped = new ArrayList<>();
        String[] sourceLines = currentText().split("\\R", -1);
        for (int i = 0; i < sourceLines.length; i++) {
            String remaining = sourceLines[i];
            if (remaining.isEmpty()) {
                wrapped.add(new VisualLine(i + 1, true, List.of()));
                continue;
            }

            boolean firstSegment = true;
            while (!remaining.isEmpty()) {
                String fitting = this.font.plainSubstrByWidth(remaining, maxWidth);
                if (fitting.isEmpty()) {
                    fitting = remaining.substring(0, 1);
                }
                wrapped.add(new VisualLine(
                        i + 1,
                        firstSegment,
                        GlslSyntaxHighlightService.tokenize(fitting, highlightLanguage()).getFirst().tokens()
                ));
                remaining = remaining.substring(fitting.length());
                firstSegment = false;
            }
        }
        return wrapped;
    }

    private List<PostSummaryRow> buildPostSummaryRows() {
        List<PostSummaryRow> rows = new ArrayList<>();
        int maxWidth = summaryWidth() - 16;
        for (PostEffectVisualizationService.SectionLine line : this.postEffectVisualization.lines()) {
            String remaining = line.text();
            if (remaining.isEmpty()) {
                rows.add(new PostSummaryRow("", line.color(), 0));
                continue;
            }
            while (!remaining.isEmpty()) {
                String fitting = this.font.plainSubstrByWidth(remaining, maxWidth);
                if (fitting.isEmpty()) {
                    fitting = remaining.substring(0, 1);
                }
                rows.add(new PostSummaryRow(fitting, line.color(), 0));
                remaining = remaining.substring(fitting.length());
            }
        }
        return rows;
    }

    private int visiblePostSummaryRows() {
        int contentHeight = Math.max(180, this.height - 170);
        return Math.max(1, (contentHeight - 28) / this.font.lineHeight);
    }

    private int maxPostSummaryScroll() {
        int visibleHeight = visiblePostSummaryRows() * this.font.lineHeight;
        int contentHeight = summaryContentHeight();
        return Math.max(0, (contentHeight - visibleHeight + this.font.lineHeight - 1) / this.font.lineHeight);
    }

    private int summaryContentHeight() {
        if (!this.postEffectVisualization.parsed()) {
            return this.postSummaryRows.size() * this.font.lineHeight;
        }

        int targetsHeight = buildTargetRows().size() * this.font.lineHeight + 22;
        int passesHeight = buildPassRows().size() * this.font.lineHeight + 22;
        return targetsHeight + passesHeight + 4;
    }

    private int renderGraphSection(GuiGraphicsExtractor guiGraphics, int summaryLeft, int summaryWidth, int y, String title, int accent, List<PostSummaryRow> rows) {
        int sectionLeft = summaryLeft + 8;
        int sectionWidth = summaryWidth - 16;
        int sectionHeight = rows.size() * this.font.lineHeight + 22;

        guiGraphics.fill(sectionLeft, y, sectionLeft + sectionWidth, y + sectionHeight, 0x221A2430);
        guiGraphics.outline(sectionLeft, y, sectionWidth, sectionHeight, 0x66445A6B);
        guiGraphics.text(this.font, title, sectionLeft + 6, y + 6, accent, false);

        int rowY = y + 18;
        for (PostSummaryRow row : rows) {
            if (rowY + this.font.lineHeight >= y && rowY <= y + sectionHeight) {
                guiGraphics.text(this.font, row.text(), sectionLeft + 6 + row.indent() * 10, rowY, row.color(), false);
            }
            rowY += this.font.lineHeight;
        }
        return y + sectionHeight;
    }

    private List<PostSummaryRow> buildTargetRows() {
        List<PostSummaryRow> rows = new ArrayList<>();
        if (this.postEffectVisualization.targets().isEmpty()) {
            rows.add(new PostSummaryRow("default frame targets", 0xFFB8C7D9, 0));
            return rows;
        }
        for (PostEffectVisualizationService.TargetInfo target : this.postEffectVisualization.targets()) {
            rows.add(new PostSummaryRow(target.name(), 0xFFFFFFFF, 0));
            rows.add(new PostSummaryRow(target.size(), 0xFF4CC9F0, 1));
            rows.add(new PostSummaryRow(target.flags(), 0xFFB8C7D9, 1));
        }
        return rows;
    }

    private List<PostSummaryRow> buildPassRows() {
        List<PostSummaryRow> rows = new ArrayList<>();
        if (this.postEffectVisualization.passes().isEmpty()) {
            rows.add(new PostSummaryRow("no passes", 0xFFB8C7D9, 0));
            return rows;
        }
        for (PostEffectVisualizationService.PassInfo pass : this.postEffectVisualization.passes()) {
            rows.add(new PostSummaryRow("Pass " + pass.index() + "  " + pass.fragmentShader(), 0xFFFFFFFF, 0));
            rows.add(new PostSummaryRow("vertex: " + pass.vertexShader(), 0xFF4CC9F0, 1));
            rows.add(new PostSummaryRow("output: " + pass.outputTarget(), 0xFFFFD166, 1));
            for (String input : pass.inputs()) {
                rows.add(new PostSummaryRow(input, 0xFFB8C7D9, 1));
            }
            for (String uniform : pass.uniforms()) {
                rows.add(new PostSummaryRow(uniform, 0xFF9AA8B8, 1));
            }
        }
        return rows;
    }

    private record VisualLine(int sourceLineNumber, boolean showLineNumber, List<GlslSyntaxHighlightService.Token> tokens) {
    }

    private record PostSummaryRow(String text, int color, int indent) {
    }
}
