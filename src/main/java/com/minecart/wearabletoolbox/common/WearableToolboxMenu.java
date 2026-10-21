package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.simibubi.create.content.equipment.toolbox.ToolboxMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;
import java.util.UUID;

/**
 * 可穿戴工具箱的菜单。
 *
 * <p>Create 的 {@link ToolboxMenu} 以 {@link ToolboxBlockEntity} 为内容载体。这里用一个
 * 不放进世界的虚拟方块实体来复用它的 8 隔间 / 4 槽布局、快速移动、过滤器与
 * {@code ToolboxScreen} 的全部外观；真正的物品数据仍保存在工具箱物品的
 * {@code create:toolbox_inventory} 组件里，菜单关闭时写回 Curios 槽位。</p>
 */
public class WearableToolboxMenu extends ToolboxMenu {
    public static final String SLOT_ID = "toolbox";

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
     * 服务端构造器：curioSlot 是工具箱在 Curios 槽位中的下标。
     */
    public WearableToolboxMenu(int id, Inventory inventory, int curioSlot, ItemStack stack) {
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

    private static ToolboxBlockEntity createVirtualBlockEntity(ItemStack stack, Player player) {
        Block block = isToolbox(stack)
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

    public static boolean isToolbox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ToolboxBlock;
    }

    /**
     * 打开玩家 Curios 的“工具箱”槽位中的第一个工具箱。
     */
    public static void openFor(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) {
            return;
        }
        Optional<ICurioStacksHandler> handler = curios.get().getStacksHandler(SLOT_ID);
        if (handler.isEmpty()) {
            return;
        }

        IDynamicStackHandler stacks = handler.get().getStacks();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (!isToolbox(stack)) {
                continue;
            }

            if (!stack.has(AllDataComponents.TOOLBOX_UUID)) {
                stack.set(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID());
            }

            int openedSlot = slot;
            player.openMenu(
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, p) -> new WearableToolboxMenu(id, inventory, openedSlot, stack),
                            stack.getHoverName()),
                    buffer -> ItemStack.STREAM_CODEC.encode(buffer, stack)
            );
            return;
        }
    }

    @Override
    protected void saveData(ToolboxBlockEntity contentHolder) {
        if (player == null || player.level().isClientSide || curioSlot < 0) {
            return;
        }

        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) {
            return;
        }
        Optional<ICurioStacksHandler> handler = curios.get().getStacksHandler(SLOT_ID);
        if (handler.isEmpty()) {
            return;
        }

        IDynamicStackHandler stacks = handler.get().getStacks();
        if (curioSlot >= stacks.getSlots()) {
            return;
        }

        ItemStack stack = stacks.getStackInSlot(curioSlot);
        if (!isToolbox(stack)) {
            return;
        }

        // 玩家可能在界面打开期间把工具箱换到别的槽位，或换成了另一个工具箱。
        UUID currentId = stack.get(AllDataComponents.TOOLBOX_UUID);
        if (toolboxId != null && !toolboxId.equals(currentId)) {
            return;
        }

        CompoundTag saved = contentHolder.saveWithoutMetadata(player.level().registryAccess());
        ToolboxInventory inventory = new ToolboxInventory(null);
        inventory.deserializeNBT(player.level().registryAccess(), saved.getCompound("Inventory"));

        stack.set(AllDataComponents.TOOLBOX_INVENTORY, inventory);
        if (!stack.has(AllDataComponents.TOOLBOX_UUID)) {
            stack.set(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID());
        }
        stacks.setStackInSlot(curioSlot, stack);
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
