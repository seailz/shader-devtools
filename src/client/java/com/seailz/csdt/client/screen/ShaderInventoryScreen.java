package com.seailz.csdt.client.screen;

import com.seailz.csdt.client.service.PipelineInventoryService;
import com.seailz.csdt.client.service.ShaderInventoryService;
import com.seailz.csdt.client.service.ShaderResourceOverrideService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShaderInventoryScreen extends Screen {

    private static final int ENTRY_HEIGHT = 34;

    private final Screen parent;
    private ViewMode viewMode = ViewMode.CORE;
    private int page;
    private Button previousPageButton;
    private Button nextPageButton;
    private Button coreTabButton;
    private Button postTabButton;
    private Button pipelinesTabButton;
    private EditBox searchBox;
    private final List<Button> entryButtons = new ArrayList<>();
    private final List<BrowserEntry> visibleEntries = new ArrayList<>();
    private String searchQuery = "";

    public ShaderInventoryScreen(Screen parent) {
        super(Component.translatable("screen.coreshader-devtools.shader_inventory"));
        this.parent = parent;
    }

    public ShaderInventoryScreen(Screen parent, ViewMode viewMode, int page) {
        this(parent);
        this.viewMode = viewMode;
        this.page = page;
    }

    public ShaderInventoryScreen(Screen parent, ViewMode viewMode, int page, String searchQuery) {
        this(parent, viewMode, page);
        this.searchQuery = searchQuery == null ? "" : searchQuery;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int tabY = 24;
        int tabWidth = 100;
        int gap = 8;

        this.coreTabButton = this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.core_shaders"), button -> {
            this.viewMode = ViewMode.CORE;
            this.page = 0;
            this.refreshView();
        }).bounds(centerX - (tabWidth * 3 + gap * 2) / 2, tabY, tabWidth, 20).build());
        this.postTabButton = this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.post_shaders"), button -> {
            this.viewMode = ViewMode.POST;
            this.page = 0;
            this.refreshView();
        }).bounds(centerX - tabWidth / 2, tabY, tabWidth, 20).build());
        this.pipelinesTabButton = this.addRenderableWidget(Button.builder(Component.literal("Pipelines"), button -> {
            this.viewMode = ViewMode.PIPELINES;
            this.page = 0;
            this.refreshView();
        }).bounds(centerX + tabWidth + gap, tabY, tabWidth, 20).build());

        this.searchBox = this.addRenderableWidget(new EditBox(this.font, centerX - 160, 54, 320, 20, Component.literal("Search")));
        this.searchBox.setHint(Component.literal("Search shaders or pipelines"));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(text -> {
            this.searchQuery = text == null ? "" : text;
            this.page = 0;
            this.refreshView();
        });

        int entryWidth = Math.min(440, this.width - 80);
        int listLeft = centerX - entryWidth / 2;
        int startY = 108;
        for (int i = 0; i < pageSize(); i++) {
            final int index = i;
            Button entryButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
                if (index < this.visibleEntries.size()) {
                    this.visibleEntries.get(index).open(this);
                }
            }).bounds(listLeft, startY + i * ENTRY_HEIGHT, entryWidth, 20).build());
            this.entryButtons.add(entryButton);
        }

        this.previousPageButton = this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.previous_page"), button -> {
            if (this.page > 0) {
                this.page--;
                this.refreshView();
            }
        }).bounds(centerX - 154, this.height - 30, 100, 20).build());
        this.nextPageButton = this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.next_page"), button -> {
            this.page++;
            this.refreshView();
        }).bounds(centerX - 44, this.height - 30, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(centerX + 66, this.height - 30, 100, 20)
                .build());

        this.refreshView();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        List<BrowserEntry> entries = allEntries();
        int totalPages = totalPages(entries.size());
        int safePage = Math.min(this.page, totalPages - 1);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, Component.literal(this.viewMode.description()), this.width / 2, 78, 0xFFA8C2D8);
        guiGraphics.centeredText(this.font, Component.literal("Page %d / %d".formatted(safePage + 1, totalPages)), this.width / 2, this.height - 46, 0xFFD7E3F0);

        int listCenterX = this.width / 2;
        for (int i = 0; i < this.visibleEntries.size(); i++) {
            BrowserEntry entry = this.visibleEntries.get(i);
            Button button = this.entryButtons.get(i);
            guiGraphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + button.getHeight(), entry.buttonTint());
            guiGraphics.centeredText(this.font, Component.literal(entry.title()), listCenterX, button.getY() + 6, entry.titleColor());
            guiGraphics.centeredText(this.font, Component.literal(entry.subtitle()), listCenterX, button.getY() + 22, 0xFFB8C7D9);
        }

        if (this.visibleEntries.isEmpty()) {
            guiGraphics.centeredText(this.font, Component.literal("No entries found"), this.width / 2, this.height / 2, 0xFFD7E3F0);
        }
    }

    private void refreshView() {
        List<BrowserEntry> entries = allEntries();
        int totalPages = totalPages(entries.size());
        this.page = Math.min(this.page, totalPages - 1);
        int start = this.page * pageSize();
        int end = Math.min(start + pageSize(), entries.size());

        this.visibleEntries.clear();
        if (start < end) {
            this.visibleEntries.addAll(entries.subList(start, end));
        }

        for (int i = 0; i < this.entryButtons.size(); i++) {
            Button button = this.entryButtons.get(i);
            boolean visible = i < this.visibleEntries.size();
            button.visible = visible;
            button.active = visible;
            if (visible) {
                button.setMessage(Component.empty());
            }
        }

        this.previousPageButton.active = this.page > 0;
        this.nextPageButton.active = this.page + 1 < totalPages;
        this.coreTabButton.active = this.viewMode != ViewMode.CORE;
        this.postTabButton.active = this.viewMode != ViewMode.POST;
        this.pipelinesTabButton.active = this.viewMode != ViewMode.PIPELINES;
    }

    private List<BrowserEntry> allEntries() {
        return switch (this.viewMode) {
            case CORE -> ShaderInventoryService.listCoreShaders().stream().map(ShaderEntry::new).map(entry -> (BrowserEntry) entry).filter(this::matchesSearch).toList();
            case POST -> ShaderInventoryService.listPostShaders().stream().map(ShaderEntry::new).map(entry -> (BrowserEntry) entry).filter(this::matchesSearch).toList();
            case PIPELINES -> PipelineInventoryService.listPipelines().stream().map(PipelineEntry::new).map(entry -> (BrowserEntry) entry).filter(this::matchesSearch).toList();
        };
    }

    private int pageSize() {
        return Math.max(1, (this.height - 198) / ENTRY_HEIGHT);
    }

    private int totalPages(int entryCount) {
        return Math.max(1, (entryCount + pageSize() - 1) / pageSize());
    }

    private boolean matchesSearch(BrowserEntry entry) {
        if (this.searchQuery.isBlank()) {
            return true;
        }
        String needle = this.searchQuery.toLowerCase(Locale.ROOT);
        return entry.title().toLowerCase(Locale.ROOT).contains(needle)
                || entry.subtitle().toLowerCase(Locale.ROOT).contains(needle);
    }

    public enum ViewMode {
        CORE("Core shaders, overridden first"),
        POST("Post shaders, overridden first"),
        PIPELINES("All registered RenderPipelines");

        private final String description;

        ViewMode(String description) {
            this.description = description;
        }

        public String description() {
            return this.description;
        }
    }

    private interface BrowserEntry {
        String title();

        String subtitle();

        int titleColor();

        int buttonTint();

        void open(Screen parent);
    }

    private record ShaderEntry(ShaderInventoryService.ShaderResourceEntry entry) implements BrowserEntry {
        @Override
        public String title() {
            String displayPath = ShaderInventoryService.displayPath(this.entry.path());
            return this.entry.overridden() ? "[Override] " + displayPath : displayPath;
        }

        @Override
        public String subtitle() {
            String forcedPack = ShaderResourceOverrideService.selectedPack(this.entry.path());
            String active = forcedPack == null ? this.entry.activePack() : forcedPack + " (forced)";
            String base = "Active: " + active;
            if (ShaderResourceOverrideService.isVisualized(this.entry.path())) {
                base += " | Visualized";
            }
            return this.entry.overridden()
                    ? base + " | Stack: " + String.join(" -> ", this.entry.packStack())
                    : base;
        }

        @Override
        public int titleColor() {
            return this.entry.overridden() ? 0xFFFFD166 : 0xFFE6EEF7;
        }

        @Override
        public int buttonTint() {
            return this.entry.overridden() ? 0x44FFD166 : 0x2216202A;
        }

        @Override
        public void open(Screen parent) {
            ShaderInventoryScreen inventory = (ShaderInventoryScreen) parent;
            Minecraft.getInstance().gui.setScreen(new ShaderResourceDetailScreen(inventory.parent, inventory.viewMode, inventory.page, this.entry, inventory.searchQuery));
        }
    }

    private record PipelineEntry(PipelineInventoryService.PipelineEntry entry) implements BrowserEntry {
        @Override
        public String title() {
            String location = this.entry.location();
            return location.startsWith("minecraft:") ? location.substring("minecraft:".length()) : location;
        }

        @Override
        public String subtitle() {
            return this.entry.pipeline().getVertexShader() + " -> " + this.entry.pipeline().getFragmentShader();
        }

        @Override
        public int titleColor() {
            return 0xFFE6EEF7;
        }

        @Override
        public int buttonTint() {
            return 0x2216202A;
        }

        @Override
        public void open(Screen parent) {
            ShaderInventoryScreen inventory = (ShaderInventoryScreen) parent;
            Minecraft.getInstance().gui.setScreen(new PipelineDetailScreen(inventory.parent, inventory.viewMode, inventory.page, this.entry, inventory.searchQuery));
        }
    }
}
