package com.ultimine_rewind.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 撤销界面 - 客户端GUI（使用原版箱子材质）
 */
public class RewindScreen extends AbstractContainerScreen<RewindMenu> {
    
    // 使用原版大箱子材质
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    
    private Button confirmButton;
    private Button cancelButton;
    private List<Component> requiredItemsText;
    
    private final int containerRows = 6;
    
    public RewindScreen(RewindMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + containerRows * 18; // 原版箱子高度计算
        this.inventoryLabelY = this.imageHeight - 94;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 按钮位置在界面右侧
        int buttonX = this.leftPos + this.imageWidth + 5;
        int buttonWidth = 80;
        
        // 确认按钮 - 靠右上
        this.confirmButton = Button.builder(
            Component.literal("✓ 恢复方块"),
            btn -> onConfirmClicked()
        )
        .bounds(buttonX, this.topPos + 20, buttonWidth, 30)
        .build();
        this.addRenderableWidget(confirmButton);
        
        // 取消按钮 - 确认按钮下方
        this.cancelButton = Button.builder(
            Component.literal("✗ 取消"),
            btn -> this.onClose()
        )
        .bounds(buttonX, this.topPos + 55, buttonWidth, 30)
        .build();
        this.addRenderableWidget(cancelButton);
        
        // 计算需要的物品文本
        updateRequiredItemsText();
    }
    
    private void updateRequiredItemsText() {
        requiredItemsText = new ArrayList<>();
        
        // 检查是否是创造模式
        boolean isCreative = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative();
        
        if (isCreative) {
            requiredItemsText.add(Component.literal("创造模式").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            requiredItemsText.add(Component.literal("无需材料即可恢复").withStyle(ChatFormatting.GREEN));
            requiredItemsText.add(Component.literal("").withStyle(ChatFormatting.GRAY));
            requiredItemsText.add(Component.literal("点击 '恢复方块' 即可").withStyle(ChatFormatting.GRAY));
        } else if (menu.record != null) {
            Map<Item, Integer> required = menu.record.getRequiredItems();
            
            requiredItemsText.add(Component.literal("需要的物品:").withStyle(ChatFormatting.BOLD));
            for (Map.Entry<Item, Integer> entry : required.entrySet()) {
                Component itemName = entry.getKey().getDescription();
                requiredItemsText.add(
                    Component.literal("  • ")
                        .append(itemName)
                        .append(" x" + entry.getValue())
                );
            }
        } else {
            // 客户端没有record数据时的提示
            requiredItemsText.add(Component.literal("撤销提示").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            requiredItemsText.add(Component.literal("").withStyle(ChatFormatting.GRAY));
            requiredItemsText.add(Component.literal("将需要的方块").withStyle(ChatFormatting.GRAY));
            requiredItemsText.add(Component.literal("放入容器中").withStyle(ChatFormatting.GRAY));
            requiredItemsText.add(Component.literal("").withStyle(ChatFormatting.GRAY));
            requiredItemsText.add(Component.literal("创造模式无需材料").withStyle(ChatFormatting.GREEN));
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 绘制需要的物品列表（左侧信息面板 - 更长）
        if (requiredItemsText != null && !requiredItemsText.isEmpty()) {
            int panelX = this.leftPos - 160;
            int panelY = this.topPos;
            int panelWidth = 150;
            int panelHeight = this.imageHeight; // 与主界面同高
            
            // 绘制信息面板背景（半透明）
            guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD000000);
            // 面板边框（立体效果）
            guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFFAAAAAA);
            guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFFAAAAAA);
            guiGraphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
            guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF555555);
            
            // 绘制标题
            Component panelTitle = Component.literal("撤销信息").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            int titleWidth = this.font.width(panelTitle);
            guiGraphics.drawString(this.font, panelTitle, panelX + (panelWidth - titleWidth) / 2, panelY + 8, 0xFFFFFF);
            
            // 绘制分隔线
            guiGraphics.fill(panelX + 5, panelY + 20, panelX + panelWidth - 5, panelY + 21, 0xFF888888);
            
            // 绘制文本
            int textY = panelY + 28;
            for (Component text : requiredItemsText) {
                guiGraphics.drawString(this.font, text, panelX + 8, textY, 0xFFFFFF);
                textY += 12;
            }
        }
        
        // 更新确认按钮状态
        // 在客户端，如果record为null，默认启用按钮（由服务端验证）
        if (menu.record == null) {
            confirmButton.active = true;
        } else {
            confirmButton.active = menu.validateItems();
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;
        
        // 绘制容器顶部
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, containerRows * 18 + 17);
        
        // 绘制玩家背包部分
        guiGraphics.blit(TEXTURE, x, y + containerRows * 18 + 17, 0, 126, this.imageWidth, 96);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 绘制标题
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        
        // 绘制背包标签
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
    
    private void onConfirmClicked() {
        // 发送确认恢复数据包到服务端
        NetworkHandler.INSTANCE.sendToServer(new ConfirmRewindPacket());
        this.onClose();
    }
}

