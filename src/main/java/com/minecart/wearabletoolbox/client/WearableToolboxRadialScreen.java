package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.common.DepositAllPayload;
import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.OpenToolboxPayload;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<ToolboxBlockEntity, EquippedToolbox> sources;
    private final boolean openMode;
    private boolean opening;

    private WearableToolboxRadialScreen(List<ToolboxBlockEntity> toolboxes, State state,
                                        ToolboxBlockEntity selectedBox,
                                        Map<ToolboxBlockEntity, EquippedToolbox> sources,
                                        boolean openMode) {
        super(toolboxes, state, selectedBox);
        this.sources = sources;
        this.openMode = openMode;
    }

    /**
     * 只有一个穿戴版工具箱时，直接打开物品菜单。
     */
    public static void openWearable(EquippedToolbox equipped, Player player) {
        ToolboxBlockEntity toolbox = WearableToolboxMenu.createVirtualBlockEntity(equipped.stack(), player);
        boolean bound = WearableToolboxBindings.hasBinding(player, player.getInventory().selected);
        Map<ToolboxBlockEntity, EquippedToolbox> sources = new IdentityHashMap<>();
        sources.put(toolbox, equipped);
        ScreenOpener.open(new WearableToolboxRadialScreen(
                List.of(toolbox),
                bound ? State.SELECT_ITEM_UNEQUIP : State.SELECT_ITEM,
                toolbox,
                sources,
                false
        ));
    }

    /**
     * 把多个穿戴版和周围放置版一起放进 Create 的“选择工具箱”状态。
     */
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

        ScreenOpener.open(new WearableToolboxRadialScreen(boxes, state, selected, sources, false));
    }

    /**
     * V 键：在多个可选工具箱之间先选择，选中后打开对应工具箱。
     */
    public static void openMenuChoice(List<EquippedToolbox> choices, Player player) {
        List<ToolboxBlockEntity> boxes = new ArrayList<>();
        Map<ToolboxBlockEntity, EquippedToolbox> sources = new IdentityHashMap<>();
        for (EquippedToolbox choice : choices) {
            ToolboxBlockEntity virtual = WearableToolboxMenu.createVirtualBlockEntity(choice.stack(), player);
            boxes.add(virtual);
            sources.put(virtual, choice);
        }
        if (boxes.size() > 8) {
            boxes = new ArrayList<>(boxes.subList(0, 8));
        }
        ScreenOpener.open(new WearableToolboxRadialScreen(
                boxes, State.SELECT_BOX, null, sources, true
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!openMode || opening) {
            return handled;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != this) {
            // super 已经关闭界面，removed() 会负责发包。
            return handled;
        }

        RadialToolboxMenuAccess access = (RadialToolboxMenuAccess) (Object) this;
        EquippedToolbox choice = sources.get(access.wearableToolbox$selectedBox());
        if (choice != null) {
            opening = true;
            PacketDistributor.sendToServer(
                    new OpenToolboxPayload(choice.curioSlot(), choice.inventorySlot())
            );
            minecraft.setScreen(null);
        }
        return handled;
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

        if (openMode) {
            if (opening) {
                return;
            }
            EquippedToolbox choice = sources.get(selectedBox);
            if (choice != null) {
                opening = true;
                PacketDistributor.sendToServer(
                        new OpenToolboxPayload(choice.curioSlot(), choice.inventorySlot())
                );
            }
            return;
        }

        if (selected == DEPOSIT) {
            if (selectedBox == null) {
                // 选择工具箱界面：让 Create 把每个放置版工具箱都存入，再处理穿戴版。
                super.removed();
            } else if (!sources.containsKey(selectedBox)) {
                super.removed();
                return;
            }
            PacketDistributor.sendToServer(new DepositAllPayload());
            return;
        }

        // 真实工具箱仍走 Create 的 ToolboxEquipPacket。
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
        PacketDistributor.sendToServer(new WearableToolboxBindPayload(
                compartment,
                player.getInventory().selected,
                source.curioSlot(),
                source.inventorySlot()
        ));
    }
}
