package com.ultimine_rewind.client.screen;

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

import java.util.Map;

/** 与原版大箱子布局一致的恢复材料界面。 */
public class RewindScreen extends AbstractContainerScreen<RewindMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private Button confirmButton;
    private boolean showDetails;

    public RewindScreen(RewindMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = leftPos + imageWidth + 5;
        confirmButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.ultimine_rewind.button.restore"), button -> confirm())
                .bounds(buttonX, topPos + 20, 90, 30).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ultimine_rewind.button.cancel"), button -> onClose())
                .bounds(buttonX, topPos + 55, 90, 30).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ultimine_rewind.button.view_materials"), button -> showDetails = !showDetails)
                .bounds(leftPos - 155, topPos + 54, 145, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderInfoPanel(graphics);
        confirmButton.active = minecraft != null && minecraft.player != null
                && (minecraft.player.isCreative() || menu.hasData());
    }

    private void renderInfoPanel(GuiGraphicsExtractor graphics) {
        int panelX = leftPos - 160;
        graphics.fill(panelX, topPos, panelX + 150, topPos + imageHeight, 0xDD000000);
        graphics.text(font, Component.translatable("gui.ultimine_rewind.panel_title")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), panelX + 8, topPos + 8, 0xFFFFFF);
        graphics.text(font, Component.translatable(
                "gui.ultimine_rewind.material_count", menu.requiredItems().size()), panelX + 8, topPos + 30, 0xCCCCCC);
        if (showDetails) {
            int itemY = topPos + 82;
            for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : menu.requiredItems().entrySet()) {
                ItemStack stack = new ItemStack(entry.getKey());
                graphics.item(stack, panelX + 8, itemY);
                graphics.text(font, stack.getHoverName().getString() + " ×" + entry.getValue(),
                        panelX + 28, itemY + 4, 0xFFFFFF);
                itemY += 20;
                if (itemY > topPos + imageHeight - 18) {
                    break;
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0, 0, imageWidth, 125, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos + 125,
                0, 126, imageWidth, 96, 256, 256);
    }

    private void confirm() {
        Play2ServerNetworking.send(new ConfirmRewindPacket());
        onClose();
    }
}
