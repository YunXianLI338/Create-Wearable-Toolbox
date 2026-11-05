package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.common.DepositAllPayload;
import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.WearableToolboxBindPayload;
import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import com.minecart.wearabletoolbox.common.WearableToolboxNetwork;
import com.minecart.wearabletoolbox.mixin.RadialToolboxMenuAccess;
import com.minecart.wearabletoolbox.mixin.ToolboxBlockEntityAccess;
import com.minecart.wearabletoolbox.mixin.ToolboxInventoryAccess;
import com.simibubi.create.content.equipment.toolbox.RadialToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可穿戴工具箱的径向菜单。真实工具箱仍由 Create 的 {@code removed()} 发包；
 * 只有选中虚拟的可穿戴/物品栏工具箱时才发 {@link WearableToolboxBindPayload}。
 */
public final class WearableToolboxRadialScreen extends RadialToolboxMenu {
    private static final int UNEQUIP = -5;
    private static final int DEPOSIT = -7;

    private final Map<ToolboxBlockEntity, EquippedToolbox> sources;

    private WearableToolboxRadialScreen(List<ToolboxBlockEntity> toolboxes, State state,
                                        ToolboxBlockEntity selectedBox,
                                        Map<ToolboxBlockEntity, EquippedToolbox> sources) {
        super(toolboxes, state, selectedBox);
        this.sources = sources;
    }

    public static void openWearable(EquippedToolbox equipped, Player player) {
        ToolboxBlockEntity toolbox = WearableToolboxMenu.createVirtualBlockEntity(equipped.stack(), player);
        boolean bound = WearableToolboxBindings.hasBinding(player, player.getInventory().selected);
        Map<ToolboxBlockEntity, EquippedToolbox> sources = new IdentityHashMap<>();
        sources.put(toolbox, equipped);
        Minecraft.getInstance().setScreen(new WearableToolboxRadialScreen(
                List.of(toolbox),
                bound ? State.SELECT_ITEM_UNEQUIP : State.SELECT_ITEM,
                toolbox,
                sources
        ));
    }

    public static void open(ItemStack stack, Player player) {
        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped != null) {
            openWearable(equipped, player);
        }
    }

    public static void openSelection(List<EquippedToolbox> equippedBoxes,
                                     List<ToolboxBlockEntity> nearby,
                                     Player player) {
        List<ToolboxBlockEntity> boxes = new ArrayList<>();
        Map<ToolboxBlockEntity, EquippedToolbox> sources = new IdentityHashMap<>();
        for (EquippedToolbox equipped : equippedBoxes) {
            ToolboxBlockEntity virtual = WearableToolboxMenu.createVirtualBlockEntity(equipped.stack(), player);
            boxes.add(virtual);
            sources.put(virtual, equipped);
        }
        boxes.addAll(nearby);
        if (boxes.size() > 8) {
            boxes = new ArrayList<>(boxes.subList(0, 8));
        }

        boolean singleEquipped = boxes.size() == 1 && sources.containsKey(boxes.get(0));
        State state = boxes.size() > 1
                ? State.SELECT_BOX
                : (WearableToolboxBindings.hasBinding(player, player.getInventory().selected)
                ? State.SELECT_ITEM_UNEQUIP
                : State.SELECT_ITEM);
        ToolboxBlockEntity selected = singleEquipped ? boxes.get(0) : null;

        Minecraft.getInstance().setScreen(new WearableToolboxRadialScreen(boxes, state, selected, sources));
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
                super.removed();
            } else if (!sources.containsKey(selectedBox)) {
                super.removed();
                return;
            }
            WearableToolboxNetwork.sendToServer(new DepositAllPayload());
            return;
        }

        if (selectedBox == null || !sources.containsKey(selectedBox)) {
            super.removed();
            return;
        }

        EquippedToolbox source = sources.get(selectedBox);
        if (selected == UNEQUIP) {
            send(-1, source, minecraft.player);
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

        send(selected, source, minecraft.player);
    }

    private static void send(int compartment, EquippedToolbox source, Player player) {
        WearableToolboxNetwork.sendToServer(new WearableToolboxBindPayload(
                compartment, player.getInventory().selected, source.curioSlot(), source.inventorySlot()
        ));
    }
}
