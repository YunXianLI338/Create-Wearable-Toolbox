package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.common.WearableToolboxBindPayload;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import com.minecart.wearabletoolbox.mixin.RadialToolboxMenuAccess;
import com.minecart.wearabletoolbox.mixin.ToolboxBlockEntityAccess;
import com.minecart.wearabletoolbox.mixin.ToolboxInventoryAccess;
import com.simibubi.create.content.equipment.toolbox.RadialToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 可穿戴工具箱的径向菜单。
 *
 * <p>直接复用 Create 的 {@link RadialToolboxMenu} 外观和交互：按住 Alt 时显示 8 个隔间、
 * 中间的“解除绑定”槽位，滚轮/数字键选择。区别只在于它绑定的是玩家胸甲栏里的工具箱物品，
 * 因此关闭时发送 {@link WearableToolboxBindPayload}，而不是 Create 按方块坐标的
 * {@code ToolboxEquipPacket}。</p>
 */
public final class WearableToolboxRadialScreen extends RadialToolboxMenu {
    private WearableToolboxRadialScreen(ToolboxBlockEntity toolbox) {
        super(List.of(toolbox), State.SELECT_ITEM_UNEQUIP, toolbox);
    }

    /**
     * 打开胸甲栏工具箱的径向菜单。
     */
    public static void open(ItemStack stack, Player player) {
        ToolboxBlockEntity toolbox = WearableToolboxMenu.createVirtualBlockEntity(stack, player);
        ScreenOpener.open(new WearableToolboxRadialScreen(toolbox));
    }

    @Override
    public void removed() {
        // 不调用 super：Create 的 removed() 会按虚拟方块实体的坐标发送 ToolboxEquipPacket。
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        RadialToolboxMenuAccess access = (RadialToolboxMenuAccess) (Object) this;
        int selected = access.wearableToolbox$scrollMode()
                ? access.wearableToolbox$scrollSlot()
                : access.wearableToolbox$hoveredSlot();

        if (selected == -5) {
            send(-1, minecraft.player);
            return;
        }
        if (selected < 0) {
            return;
        }

        ToolboxBlockEntity box = access.wearableToolbox$selectedBox();
        if (box == null) {
            return;
        }

        ToolboxInventory inventory = ((ToolboxBlockEntityAccess) (Object) box).wearableToolbox$inventory();
        List<ItemStack> filters = ((ToolboxInventoryAccess) (Object) inventory).wearableToolbox$filters();
        if (selected >= filters.size() || filters.get(selected).isEmpty()) {
            return;
        }
        if (inventory.getStackInSlot(selected * ToolboxInventory.STACKS_PER_COMPARTMENT).isEmpty()) {
            return;
        }

        send(selected, minecraft.player);
    }

    private static void send(int compartment, Player player) {
        PacketDistributor.sendToServer(
                new WearableToolboxBindPayload(compartment, player.getInventory().selected)
        );
    }
}
