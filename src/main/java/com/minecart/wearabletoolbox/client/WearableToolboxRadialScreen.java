package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.common.DepositAllPayload;
import com.minecart.wearabletoolbox.common.WearableToolboxBindPayload;
import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 可穿戴工具箱的径向菜单。
 *
 * <p>直接复用 Create 的 {@link RadialToolboxMenu} 外观和交互：按住 Alt 时显示 8 个隔间/
 * 工具箱、中间的“解除绑定”槽位，滚轮/数字键选择。真实工具箱仍由 Create 的
 * {@code removed()} 发包；只有选中的是虚拟的可穿戴工具箱时，才发送
 * {@link WearableToolboxBindPayload}。</p>
 */
public final class WearableToolboxRadialScreen extends RadialToolboxMenu {
    private static final int UNEQUIP = -5;
    private static final int DEPOSIT = -7;

    private final ToolboxBlockEntity wearable;

    private WearableToolboxRadialScreen(List<ToolboxBlockEntity> toolboxes, State state,
                                        ToolboxBlockEntity selectedBox, ToolboxBlockEntity wearable) {
        super(toolboxes, state, selectedBox);
        this.wearable = wearable;
    }

    /**
     * 只打开穿戴版工具箱的物品菜单（穿戴优先，或周围没有放置版工具箱）。
     */
    public static void openWearable(ItemStack stack, Player player) {
        ToolboxBlockEntity toolbox = WearableToolboxMenu.createVirtualBlockEntity(stack, player);
        boolean bound = WearableToolboxBindings.hasBinding(player, player.getInventory().selected);
        ScreenOpener.open(new WearableToolboxRadialScreen(
                List.of(toolbox),
                bound ? State.SELECT_ITEM_UNEQUIP : State.SELECT_ITEM,
                toolbox,
                toolbox
        ));
    }

    /**
     * 兼容旧调用点。
     */
    public static void open(ItemStack stack, Player player) {
        openWearable(stack, player);
    }

    /**
     * 把穿戴版和周围放置版一起放进 Create 的“选择工具箱”状态。
     * 第一个位置放穿戴版，后面是 Create 已按距离排好序的放置版工具箱。
     */
    public static void openSelection(List<ToolboxBlockEntity> nearby, ItemStack stack, Player player) {
        ToolboxBlockEntity wearable = WearableToolboxMenu.createVirtualBlockEntity(stack, player);
        List<ToolboxBlockEntity> boxes = new ArrayList<>();
        boxes.add(wearable);
        boxes.addAll(nearby);
        if (boxes.size() > 8) {
            boxes = new ArrayList<>(boxes.subList(0, 8));
        }
        ScreenOpener.open(new WearableToolboxRadialScreen(
                boxes,
                State.SELECT_BOX,
                null,
                wearable
        ));
    }

    @Override
    public void removed() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        RadialToolboxMenuAccess access = (RadialToolboxMenuAccess) (Object) this;
        int selected = access.wearableToolbox$scrollMode()
                ? access.wearableToolbox$scrollSlot()
                : access.wearableToolbox$hoveredSlot();
        ToolboxBlockEntity selectedBox = access.wearableToolbox$selectedBox();

        if (selected == DEPOSIT) {
            if (selectedBox == null) {
                // 选择工具箱界面：让 Create 把每个放置版工具箱都存入，再处理穿戴版。
                super.removed();
            } else if (selectedBox != wearable) {
                super.removed();
                return;
            }
            PacketDistributor.sendToServer(new DepositAllPayload());
            return;
        }

        // 真实工具箱仍走 Create 的 ToolboxEquipPacket。
        if (selectedBox == null || selectedBox != wearable) {
            super.removed();
            return;
        }

        if (selected == UNEQUIP) {
            send(-1, minecraft.player);
            return;
        }
        if (selected < 0) {
            return;
        }

        ToolboxInventory inventory = ((ToolboxBlockEntityAccess) (Object) selectedBox).wearableToolbox$inventory();
        List<ItemStack> filters = ((ToolboxInventoryAccess) (Object) inventory).wearableToolbox$filters();
        if (selected >= filters.size() || filters.get(selected).isEmpty()) {
            return;
        }
        if (!WearableToolboxBindings.hasCompartmentItems(inventory, selected)) {
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
