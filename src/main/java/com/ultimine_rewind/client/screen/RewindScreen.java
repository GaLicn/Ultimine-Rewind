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
    private Button detailsButton;
    private List<Component> requiredItemsText;
    
    private final int containerRows = 6;
    private boolean showDetails = false;
    
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
    
    /**
     * 切换材料详情显示
     */
    private void toggleDetails() {
        showDetails = !showDetails;
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
        } else if (menu.hasData()) {
            Map<Item, Integer> required = menu.getRequiredItems();
            
            requiredItemsText.add(Component.literal("需要的物品:").withStyle(ChatFormatting.BOLD));
            requiredItemsText.add(Component.literal("共 " + required.size() + " 种材料").withStyle(ChatFormatting.GRAY));
            requiredItemsText.add(Component.literal("点击下方按钮查看详情").withStyle(ChatFormatting.GRAY));
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
            
            // 绘制材料详情按钮（在文本下方）
            int buttonY = textY + 10;
            int buttonHeight = 20;
            
            // 移除旧按钮
            if (detailsButton != null) {
                this.removeWidget(detailsButton);
            }
            
            // 始终显示按钮
            detailsButton = Button.builder(
                showDetails ? Component.literal("✖ 关闭详情") : Component.literal("📋 查看材料"),
                btn -> toggleDetails()
            )
            .bounds(panelX + 5, buttonY, panelWidth - 10, buttonHeight)
            .build();
            this.addRenderableWidget(detailsButton);
            
            // 如果显示详情，绘制物品列表
            if (showDetails) {
                int detailY = buttonY + buttonHeight + 10;
                
                // 绘制分隔线
                guiGraphics.fill(panelX + 5, detailY - 5, panelX + panelWidth - 5, detailY - 4, 0xFF888888);
                
                // 检查是否有数据
                if (menu.hasData()) {
                    Map<Item, Integer> required = menu.getRequiredItems();
                    for (Map.Entry<Item, Integer> entry : required.entrySet()) {
                        // 绘制物品图标
                        guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(entry.getKey()), panelX + 10, detailY);
                        
                        // 绘制物品名称和数量
                        Component itemName = entry.getKey().getDescription();
                        String text = itemName.getString() + " x" + entry.getValue();
                        guiGraphics.drawString(this.font, text, panelX + 30, detailY + 4, 0xFFFFFF);
                        
                        detailY += 20;
                        
                        // 防止超出面板范围
                        if (detailY > panelY + panelHeight - 10) {
                            guiGraphics.drawString(this.font, "...", panelX + 10, detailY, 0xFF888888);
                            break;
                        }
                    }
                } else {
                    // 没有数据时显示提示
                    guiGraphics.drawString(this.font, "材料信息", panelX + 10, detailY, 0xFFFFFF);
                    detailY += 15;
                    guiGraphics.drawString(this.font, "将在放入材料后", panelX + 10, detailY, 0xFF888888);
                    detailY += 12;
                    guiGraphics.drawString(this.font, "自动显示", panelX + 10, detailY, 0xFF888888);
                }
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

