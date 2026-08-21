package com.ultimine_rewind.client.screen;

import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.menu.RewindMenu;
import com.ultimine_rewind.network.ConfirmRewindPacket;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 与原版大箱子布局一致的恢复材料界面。 */
public class RewindScreen extends AbstractContainerScreen<RewindMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int MAX_PANEL_WIDTH = 184;
    private static final int MIN_PANEL_WIDTH = 112;
    private static final int PANEL_GAP = 8;
    private static final int BUTTON_WIDTH = 96;
    private static final int SCREEN_MARGIN = 6;
    private static final int LIST_TOP = 96;
    private static final int LIST_BOTTOM_PADDING = 12;
    private static final int MATERIAL_ROW_HEIGHT = 24;
    private Button confirmButton;
    private int panelWidth = MAX_PANEL_WIDTH;
    private boolean buttonsInPanel;
    private int materialScroll;

    public RewindScreen(RewindMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        configureResponsiveLayout();

        int buttonX;
        int buttonY;
        int buttonWidth;
        int cancelX;
        if (buttonsInPanel) {
            int availableWidth = panelWidth - 20;
            buttonWidth = (availableWidth - 4) / 2;
            buttonX = panelX() + 8;
            cancelX = buttonX + buttonWidth + 4;
            buttonY = topPos + imageHeight - 29;
        } else {
            buttonX = leftPos + imageWidth + PANEL_GAP;
            cancelX = buttonX;
            buttonY = topPos + 20;
            buttonWidth = BUTTON_WIDTH;
        }

        confirmButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.ultimine_rewind.button.restore"), button -> confirm())
                .bounds(buttonX, buttonY, buttonWidth, buttonsInPanel ? 20 : 26).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ultimine_rewind.button.cancel"), button -> onClose())
                .bounds(cancelX, buttonsInPanel ? buttonY : topPos + 52,
                        buttonWidth, buttonsInPanel ? 20 : 26).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        confirmButton.active = menu.hasData();
    }

    private void configureResponsiveLayout() {
        int fixedWideWidth = imageWidth + PANEL_GAP * 2 + BUTTON_WIDTH + SCREEN_MARGIN * 2;
        int availableWidePanelWidth = width - fixedWideWidth;
        // 横向空间不足时收起右侧按钮区，为材料面板保留可读宽度。
        buttonsInPanel = availableWidePanelWidth < MIN_PANEL_WIDTH;

        if (buttonsInPanel) {
            panelWidth = Math.min(MAX_PANEL_WIDTH,
                    Math.max(60, width - imageWidth - PANEL_GAP - SCREEN_MARGIN * 2));
            int totalWidth = panelWidth + PANEL_GAP + imageWidth;
            leftPos = (width - totalWidth) / 2 + panelWidth + PANEL_GAP;
        } else {
            panelWidth = Math.min(MAX_PANEL_WIDTH, availableWidePanelWidth);
            int totalWidth = panelWidth + PANEL_GAP * 2 + imageWidth + BUTTON_WIDTH;
            leftPos = (width - totalWidth) / 2 + panelWidth + PANEL_GAP;
        }
    }

    private void renderInfoPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int panelX = panelX();
        int panelBottom = topPos + imageHeight;

        graphics.fill(panelX - 1, topPos - 1, panelX + panelWidth + 1, panelBottom + 1, 0xFF171B22);
        graphics.fill(panelX, topPos, panelX + panelWidth, panelBottom, 0xF0222730);
        graphics.fill(panelX, topPos, panelX + 3, panelBottom, 0xFF5FC7B2);
        graphics.fill(panelX + 3, topPos, panelX + panelWidth, topPos + 42, 0xFF2C3440);

        String title = fitText(Component.translatable("gui.ultimine_rewind.panel_title").getString(), panelWidth - 24);
        graphics.text(font, Component.literal(title).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                panelX + 12, topPos + 9, 0xFFFFFFFF);
        graphics.text(font, fitText(Component.translatable("gui.ultimine_rewind.subtitle").getString(), panelWidth - 24),
                panelX + 12, topPos + 25, 0xFFAAB4C3, false);

        int statWidth = Math.max(20, (panelWidth - 26) / 2);
        renderStatCard(graphics, panelX + 10, topPos + 50, statWidth,
                Component.translatable("gui.ultimine_rewind.blocks"), menu.blockCount());
        renderStatCard(graphics, panelX + 16 + statWidth, topPos + 50, statWidth,
                Component.translatable("gui.ultimine_rewind.material_types"), menu.requiredMaterials().size());
        String materialsTitle = Component.translatable("gui.ultimine_rewind.materials").getString();
        graphics.text(font, fitText(materialsTitle, panelWidth - 22),
                panelX + 11, topPos + 82, 0xFFE7ECF3, false);

        if (minecraft != null && minecraft.player != null && minecraft.player.isCreative()) {
            String creativeText = Component.translatable("gui.ultimine_rewind.creative_free").getString();
            int creativeX = panelX + panelWidth - 10 - font.width(creativeText);
            if (creativeX > panelX + 15 + font.width(materialsTitle)) {
                graphics.text(font, creativeText, creativeX, topPos + 82, 0xFF74D9A7, false);
            }
        }

        renderMaterialList(graphics, mouseX, mouseY, panelX, panelBottom);
    }

    private void renderStatCard(GuiGraphicsExtractor graphics, int x, int y, int width,
                                Component label, int value) {
        graphics.fill(x, y, x + width, y + 25, 0xFF303946);
        graphics.fill(x, y, x + 2, y + 25, 0xFF5FC7B2);
        graphics.text(font, Integer.toString(value), x + 8, y + 4, 0xFFFFFFFF, false);
        graphics.text(font, fitText(label.getString(), width - 12), x + 8, y + 14, 0xFF9DA8B7, false);
    }

    private void renderMaterialList(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                    int panelX, int panelBottom) {
        List<MaterialRequirement> materials = menu.requiredMaterials();
        int listBottom = panelBottom - (buttonsInPanel ? 38 : LIST_BOTTOM_PADDING);
        int visibleRows = Math.max(1, (listBottom - (topPos + LIST_TOP)) / MATERIAL_ROW_HEIGHT);
        int maxScroll = Math.max(0, materials.size() - visibleRows);
        materialScroll = Math.min(materialScroll, maxScroll);

        graphics.enableScissor(panelX + 8, topPos + LIST_TOP, panelX + panelWidth - 8, listBottom);
        for (int index = materialScroll; index < Math.min(materials.size(), materialScroll + visibleRows + 1); index++) {
            int rowY = topPos + LIST_TOP + (index - materialScroll) * MATERIAL_ROW_HEIGHT;
            renderMaterialRow(graphics, materials.get(index), panelX + 9, rowY, mouseX, mouseY);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = topPos + LIST_TOP;
            int trackHeight = listBottom - trackTop;
            int thumbHeight = Math.max(18, trackHeight * visibleRows / materials.size());
            int thumbY = trackTop + (trackHeight - thumbHeight) * materialScroll / maxScroll;
            graphics.fill(panelX + panelWidth - 6, trackTop, panelX + panelWidth - 4, listBottom, 0xFF343C48);
            graphics.fill(panelX + panelWidth - 6, thumbY,
                    panelX + panelWidth - 4, thumbY + thumbHeight, 0xFF6ED7C0);
        }
    }

    private void renderMaterialRow(GuiGraphicsExtractor graphics, MaterialRequirement material,
                                   int x, int y, int mouseX, int mouseY) {
        int rowWidth = panelWidth - 19;
        boolean hovered = mouseX >= x && mouseX < x + rowWidth
                && mouseY >= y && mouseY < y + MATERIAL_ROW_HEIGHT - 2;
        graphics.fill(x, y, x + rowWidth, y + MATERIAL_ROW_HEIGHT - 2,
                hovered ? 0xFF3B4654 : 0xFF2B333E);
        graphics.item(material.stack(), x + 4, y + 3);

        String name = material.stack().getHoverName().getString();
        String count = "×" + material.count();
        int nameWidth = panelWidth - 57 - font.width(count);
        name = fitText(name, nameWidth);
        graphics.text(font, name, x + 25, y + 7, 0xFFE8EDF4, false);
        graphics.text(font, count, x + panelWidth - 23 - font.width(count), y + 7, 0xFF72DCC4, false);

        if (hovered) {
            graphics.setTooltipForNextFrame(font, material.stack(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0, 0, imageWidth, 125, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos + 125,
                0, 126, imageWidth, 96, 256, 256);
        renderInfoPanel(graphics, mouseX, mouseY);
    }

    private void confirm() {
        Play2ServerNetworking.send(new ConfirmRewindPacket());
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listTop = topPos + LIST_TOP;
        int listBottom = topPos + imageHeight - (buttonsInPanel ? 38 : LIST_BOTTOM_PADDING);
        if (mouseX >= panelX() && mouseX < panelX() + panelWidth
                && mouseY >= listTop && mouseY < listBottom && scrollY != 0.0) {
            int visibleRows = Math.max(1, (listBottom - listTop) / MATERIAL_ROW_HEIGHT);
            int maxScroll = Math.max(0, menu.requiredMaterials().size() - visibleRows);
            // 滚轮每格移动一项，保持长材料列表易于浏览。
            materialScroll = Math.max(0, Math.min(maxScroll, materialScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int panelX() {
        return leftPos - panelWidth - PANEL_GAP;
    }

    private String fitText(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = font.width("…");
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - ellipsisWidth)) + "…";
    }
}
