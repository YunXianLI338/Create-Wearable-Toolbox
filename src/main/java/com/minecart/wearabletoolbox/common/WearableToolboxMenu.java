package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.mixin.ToolboxBlockEntityAccess;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.simibubi.create.content.equipment.toolbox.ToolboxMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.UUID;

/**
 * 可穿戴工具箱的菜单。
 *
 * <p>Create 的 {@link ToolboxMenu} 以 {@link ToolboxBlockEntity} 为内容载体。这里用一个
 * 不放进世界的虚拟方块实体来复用它的 8 隔间 / 4 槽布局、快速移动、过滤器与
 * {@code ToolboxScreen} 的全部外观；真正的物品数据保存在胸甲栏工具箱物品的
 * {@code create:toolbox_inventory} 组件里，菜单关闭时写回胸甲栏。</p>
 */
public class WearableToolboxMenu extends ToolboxMenu {
    private final int curioSlot;
    private final UUID toolboxId;

    /**
     * 客户端构造器：从网络缓冲区读取被打开的工具箱物品栈。
     */
    public WearableToolboxMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        super(WearableToolbox.TOOLBOX_MENU.get(), id, inventory, extraData);
        this.curioSlot = -1;
        this.toolboxId = null;
    }

    /**
     * 服务端构造器：工具箱来自原版胸甲栏。
     */
    public WearableToolboxMenu(int id, Inventory inventory, ItemStack stack) {
        this(id, inventory, stack, -1);
    }

    /**
     * 服务端构造器：curioSlot 为 -1 表示原版胸甲栏，否则是 Curios body 槽位下标。
     */
    public WearableToolboxMenu(int id, Inventory inventory, ItemStack stack, int curioSlot) {
        super(WearableToolbox.TOOLBOX_MENU.get(), id, inventory, createVirtualBlockEntity(stack, inventory.player));
        this.curioSlot = curioSlot;
        this.toolboxId = stack.get(AllDataComponents.TOOLBOX_UUID);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ToolboxBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
        ItemStack stack = ItemStack.STREAM_CODEC.decode(extraData);
        return createVirtualBlockEntity(stack, Minecraft.getInstance().player);
    }

    public static ToolboxBlockEntity createVirtualBlockEntity(ItemStack stack, Player player) {
        Block block = ToolboxItems.isToolbox(stack)
                ? ((BlockItem) stack.getItem()).getBlock()
                : AllBlocks.TOOLBOXES.get(DyeColor.BROWN).get();
        BlockState state = block.defaultBlockState();

        ToolboxBlockEntity blockEntity = new VirtualToolboxBlockEntity(state);
        if (player != null) {
            blockEntity.setLevel(player.level());
        }
        blockEntity.readInventory(stack.get(AllDataComponents.TOOLBOX_INVENTORY));

        UUID uniqueId = stack.get(AllDataComponents.TOOLBOX_UUID);
        if (uniqueId != null) {
            blockEntity.setUniqueId(uniqueId);
        }

        // 虚拟方块实体不会 tick，直接把盖子/抽屉置于打开状态，避免界面里显示成关着。
        blockEntity.lid.startWithValue(1);
        blockEntity.drawers.startWithValue(1);
        return blockEntity;
    }

    /**
     * 打开玩家身上（胸甲栏或 Curios 胸饰栏）的工具箱。
     */
    public static void openFor(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }

        ItemStack stack = equipped.stack();
        if (!stack.has(AllDataComponents.TOOLBOX_UUID)) {
            stack.set(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID());
            EquippedToolbox.set(player, equipped.curioSlot(), stack);
        }

        int curioSlot = equipped.curioSlot();
        player.openMenu(
                new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, p) -> new WearableToolboxMenu(id, inventory, stack, curioSlot),
                        stack.getHoverName()),
                buffer -> ItemStack.STREAM_CODEC.encode(buffer, stack)
        );
    }

    @Override
    protected void saveData(ToolboxBlockEntity contentHolder) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        ItemStack stack = curioSlot >= 0
                ? CuriosCompat.getBody(player, curioSlot)
                : player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(stack)) {
            return;
        }

        // 玩家可能在界面打开期间换掉了这个工具箱。
        UUID currentId = stack.get(AllDataComponents.TOOLBOX_UUID);
        if (toolboxId != null && !toolboxId.equals(currentId)) {
            return;
        }

        writeToEquipped(contentHolder, stack);
    }

    /**
     * 把菜单里的库存写回工具箱物品组件，再放回原装备槽。
     */
    private void writeToEquipped(ToolboxBlockEntity contentHolder, ItemStack stack) {
        CompoundTag saved = contentHolder.saveWithoutMetadata(player.level().registryAccess());
        ToolboxInventory inventory = new ToolboxInventory(null);
        inventory.deserializeNBT(player.level().registryAccess(), saved.getCompound("Inventory"));

        stack.set(AllDataComponents.TOOLBOX_INVENTORY, inventory);
        if (!stack.has(AllDataComponents.TOOLBOX_UUID)) {
            stack.set(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID());
        }
        if (curioSlot >= 0) {
            CuriosCompat.setBody(player, curioSlot, stack);
        } else {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
        }
    }

    /**
     * 把玩家背包里能放进工具箱的物品全部存入当前打开的工具箱。
     */
    public static void depositAll(ServerPlayer player) {
        if (!(player.containerMenu instanceof WearableToolboxMenu menu)) {
            return;
        }

        ToolboxInventory inventory = ((ToolboxBlockEntityAccess) (Object) menu.contentHolder)
                .wearableToolbox$inventory();
        MutableBoolean changed = new MutableBoolean(false);
        inventory.inLimitedMode(handler -> {
            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (remainder.getCount() != stack.getCount()) {
                    player.getInventory().setItem(slot, remainder);
                    changed.setTrue();
                }
            }
        });

        if (changed.isTrue()) {
            ItemStack stack = menu.curioSlot >= 0
                    ? CuriosCompat.getBody(player, menu.curioSlot)
                    : player.getItemBySlot(EquipmentSlot.CHEST);
            if (ToolboxItems.isToolbox(stack)) {
                menu.writeToEquipped(menu.contentHolder, stack);
            }
            player.getInventory().setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive();
    }

    /**
     * 不进入世界的工具箱方块实体。只用于承载菜单/界面的库存与颜色。
     */
    private static class VirtualToolboxBlockEntity extends ToolboxBlockEntity {
        private VirtualToolboxBlockEntity(BlockState state) {
            super(AllBlockEntityTypes.TOOLBOX.get(), new BlockPos(0, -1000, 0), state);
        }

        @Override
        public void sendData() {
            // 没有真实方块，不需要向客户端同步方块实体。
        }

        @Override
        public void notifyUpdate() {
            // 没有真实方块，不需要标记区块/方块更新。
        }

        @Override
        public boolean canPlayerUse(Player player) {
            return true;
        }
    }
}
