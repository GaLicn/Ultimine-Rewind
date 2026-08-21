package com.ultimine_rewind.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.menu.RewindMenu;
import com.ultimine_rewind.network.ConfirmRewindPacket;
import com.ultimine_rewind.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** 使用材料清单面板展示撤回所需材料。 */
public class RewindScreen extends AbstractContainerScreen<RewindMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "textures/gui/container/generic_54.png");
    private static final int MAX_PANEL_WIDTH = 164;
    private static final int PANEL_GAP = 8;
    private static final int MATERIAL_ROW_HEIGHT = 22;
    private Button confirmButton;
    private int materialScroll;
    private int panelWidth = MAX_PANEL_WIDTH;
    private boolean buttonsInPanel;

    public RewindScreen(RewindMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        configureResponsiveLayout();
        int buttonWidth = buttonsInPanel ? (panelWidth - 24) / 2 : 96;
        int buttonX = buttonsInPanel ? panelX() + 8 : leftPos + imageWidth + PANEL_GAP;
        int buttonY = buttonsInPanel ? topPos + imageHeight - 29 : topPos + 22;
        confirmButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimine_rewind.button.restore"), button -> confirm())
                .bounds(buttonX, buttonY, buttonWidth, buttonsInPanel ? 20 : 24)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimine_rewind.button.cancel"), button -> onClose())
                .bounds(buttonsInPanel ? buttonX + buttonWidth + 4 : buttonX,
                        buttonsInPanel ? buttonY : topPos + 52, buttonWidth, buttonsInPanel ? 20 : 24)
                .build());
    }

    private void configureResponsiveLayout() {
        int availablePanelWidth = width - imageWidth - PANEL_GAP * 2 - 96 - 6;
        buttonsInPanel = availablePanelWidth < 112;
        if (buttonsInPanel) {
            panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(110, width - imageWidth - PANEL_GAP - 8));
            int totalWidth = panelWidth + PANEL_GAP + imageWidth;
            leftPos = (width - totalWidth) / 2 + panelWidth + PANEL_GAP;
        } else {
            panelWidth = Math.min(MAX_PANEL_WIDTH, availablePanelWidth);
            int totalWidth = panelWidth + PANEL_GAP * 2 + imageWidth + 96;
            leftPos = (width - totalWidth) / 2 + panelWidth + PANEL_GAP;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderInfoPanel(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        confirmButton.active = menu.hasData();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 125, 256, 256);
        graphics.blit(TEXTURE, leftPos, topPos + 125, 0, 126, imageWidth, 96, 256, 256);
    }

    private void renderInfoPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelX = panelX();
        int panelBottom = topPos + imageHeight;
        graphics.fill(panelX - 1, topPos - 1, panelX + panelWidth + 1, panelBottom + 1, 0xFF171B22);
        graphics.fill(panelX, topPos, panelX + panelWidth, panelBottom, 0xF0222730);
        graphics.fill(panelX, topPos, panelX + 3, panelBottom, 0xFF5FC7B2);
        graphics.fill(panelX + 3, topPos, panelX + panelWidth, topPos + 42, 0xFF2C3440);

        graphics.drawString(font, Component.translatable("gui.ultimine_rewind.panel_title")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), panelX + 12, topPos + 9, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("gui.ultimine_rewind.subtitle"),
                panelX + 12, topPos + 25, 0xFFAAB4C3);

        int statWidth = Math.max(40, (panelWidth - 26) / 2);
        renderStatCard(graphics, panelX + 10, topPos + 50, statWidth,
                Component.translatable("gui.ultimine_rewind.blocks"), menu.getBlockCount());
        renderStatCard(graphics, panelX + 16 + statWidth, topPos + 50, statWidth,
                Component.translatable("gui.ultimine_rewind.material_types"), menu.requiredMaterials().size());
        graphics.drawString(font, Component.translatable("gui.ultimine_rewind.materials"),
                panelX + 11, topPos + 82, 0xFFE7ECF3);

        if (minecraft != null && minecraft.player != null && minecraft.player.isCreative()) {
            graphics.drawString(font, Component.translatable("gui.ultimine_rewind.creative_free"),
                    panelX + 11, topPos + 94, 0xFF74D9A7);
        }
        renderMaterialList(graphics, mouseX, mouseY, panelX, panelBottom);
    }

    private void renderStatCard(GuiGraphics graphics, int x, int y, int cardWidth,
                                Component label, int value) {
        graphics.fill(x, y, x + cardWidth, y + 25, 0xFF303946);
        graphics.fill(x, y, x + 2, y + 25, 0xFF5FC7B2);
        graphics.drawString(font, Integer.toString(value), x + 8, y + 4, 0xFFFFFFFF);
        graphics.drawString(font, label, x + 8, y + 14, 0xFF9DA8B7);
    }

    private void renderMaterialList(GuiGraphics graphics, int mouseX, int mouseY, int panelX, int panelBottom) {
        List<MaterialRequirement> materials = menu.requiredMaterials();
        int listTop = topPos + 108;
        int listBottom = panelBottom - 10;
        int visibleRows = Math.max(1, (listBottom - listTop) / MATERIAL_ROW_HEIGHT);
        int maxScroll = Math.max(0, materials.size() - visibleRows);
        materialScroll = Math.min(materialScroll, maxScroll);

        for (int index = materialScroll; index < Math.min(materials.size(), materialScroll + visibleRows); index++) {
            int rowY = listTop + (index - materialScroll) * MATERIAL_ROW_HEIGHT;
            renderMaterialRow(graphics, materials.get(index), panelX + 9, rowY, mouseX, mouseY);
        }
        if (maxScroll > 0) {
            int trackHeight = listBottom - listTop;
            int thumbHeight = Math.max(18, trackHeight * visibleRows / materials.size());
            int thumbY = listTop + (trackHeight - thumbHeight) * materialScroll / maxScroll;
            graphics.fill(panelX + panelWidth - 6, listTop, panelX + panelWidth - 4, listBottom, 0xFF343C48);
            graphics.fill(panelX + panelWidth - 6, thumbY, panelX + panelWidth - 4,
                    thumbY + thumbHeight, 0xFF6ED7C0);
        }
    }

    private void renderMaterialRow(GuiGraphics graphics, MaterialRequirement material, int x, int y,
                                   int mouseX, int mouseY) {
        int rowWidth = panelWidth - 19;
        boolean hovered = mouseX >= x && mouseX < x + rowWidth
                && mouseY >= y && mouseY < y + MATERIAL_ROW_HEIGHT - 2;
        graphics.fill(x, y, x + rowWidth, y + MATERIAL_ROW_HEIGHT - 2,
                hovered ? 0xFF3B4654 : 0xFF2B333E);
        graphics.renderItem(material.stack(), x + 4, y + 3);

        String name = font.plainSubstrByWidth(material.stack().getHoverName().getString(), 78);
        String count = "×" + material.count();
        graphics.drawString(font, name, x + 25, y + 7, 0xFFE8EDF4);
        graphics.drawString(font, count, x + rowWidth - 5 - font.width(count), y + 7, 0xFF72DCC4);
        if (hovered) {
            graphics.renderTooltip(font, material.stack(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        int panelX = panelX();
        int listTop = topPos + 108;
        int listBottom = topPos + imageHeight - 10;
        if (mouseX >= panelX && mouseX < panelX + panelWidth
                && mouseY >= listTop && mouseY < listBottom && scrollY != 0.0D) {
            int visibleRows = Math.max(1, (listBottom - listTop) / MATERIAL_ROW_HEIGHT);
            int maxScroll = Math.max(0, menu.requiredMaterials().size() - visibleRows);
            materialScroll = Math.max(0, Math.min(maxScroll, materialScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    private void confirm() {
        NetworkHandler.INSTANCE.sendToServer(new ConfirmRewindPacket());
        onClose();
    }

    private int panelX() {
        return leftPos - panelWidth - PANEL_GAP;
    }
}
