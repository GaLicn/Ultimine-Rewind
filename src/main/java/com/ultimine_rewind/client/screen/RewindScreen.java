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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 撤销界面 - 客户端GUI
 */
public class RewindScreen extends AbstractContainerScreen<RewindMenu> {
    
    private Button confirmButton;
    private Button cancelButton;
    private List<Component> requiredItemsText;
    
    public RewindScreen(RewindMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 确认按钮
        this.confirmButton = Button.builder(
            Component.literal("恢复方块"),
            btn -> onConfirmClicked()
        )
        .bounds(this.leftPos + 10, this.topPos + 120, 70, 20)
        .build();
        this.addRenderableWidget(confirmButton);
        
        // 取消按钮
        this.cancelButton = Button.builder(
            Component.literal("取消"),
            btn -> this.onClose()
        )
        .bounds(this.leftPos + 96, this.topPos + 120, 70, 20)
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
            // 客户端没有record数据，显示提示
            requiredItemsText.add(Component.literal("等待服务器数据...").withStyle(ChatFormatting.GRAY));
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // 绘制需要的物品列表
        if (requiredItemsText != null && !requiredItemsText.isEmpty()) {
            int textY = this.topPos + 10;
            for (Component text : requiredItemsText) {
                guiGraphics.drawString(this.font, text, 
                    this.leftPos - 150, textY, 0xFFFFFF);
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
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // 绘制简单的灰色背景
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        // 绘制边框
        guiGraphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF000000);
        guiGraphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF000000);
        guiGraphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF000000);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
    
    private void onConfirmClicked() {
        // 发送确认恢复数据包到服务端
        NetworkHandler.INSTANCE.sendToServer(new ConfirmRewindPacket());
        this.onClose();
    }
}

