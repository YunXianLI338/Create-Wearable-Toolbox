package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.simibubi.create.content.equipment.toolbox.ToolboxMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.UUID;

/**
 * 可穿戴工具箱的菜单。
 *
 * <p>Create 的 {@link ToolboxMenu} 以 {@link ToolboxBlockEntity} 为内容载体。这里用一个
 * 不放进世界的虚拟方块实体来复用它的 8 隔间 / 4 槽布局、快速移动、过滤器与
 * {@code ToolboxScreen} 的全部外观；真正的物品数据保存在胸甲栏工具箱物品的
 * {@code Inventory} 子标签里（1.20.1 没有物品数据组件），菜单关闭时写回胸甲栏。</p>
 */
public class WearableToolboxMenu extends ToolboxMenu {
    private final int curioSlot;
    private final UUID toolboxId;

    /**
     * 客户端构造器：从网络缓冲区读取被打开的工具箱物品栈。
     */
    public WearableToolboxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
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
        super(WearableToolbox.TOOLBOX_MENU.get(), id, inventory,
                createVirtualBlockEntity(stack, inventory.player));
        this.curioSlot = curioSlot;
        this.toolboxId = ToolboxStorage.getId(stack);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ToolboxBlockEntity createOnClient(FriendlyByteBuf extraData) {
        ItemStack stack = extraData.readItem();
        return createVirtualBlockEntity(stack, Minecraft.getInstance().player);
    }

    public static ToolboxBlockEntity createVirtualBlockEntity(ItemStack stack, Player player) {
        // 方块直接取自物品本身：这样不必触碰 Registrate 的 BlockEntry（AllBlocks.TOOLBOXES）。
        Block block = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : Blocks.AIR;
        BlockState state = block.defaultBlockState();

        ToolboxBlockEntity blockEntity = new VirtualToolboxBlockEntity(state);
        if (player != null) {
            blockEntity.setLevel(player.level());
        }
        ToolboxStorage.readInto(blockEntity, stack);

        UUID uniqueId = ToolboxStorage.getId(stack);
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
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }

        ItemStack stack = equipped.stack();
        ToolboxStorage.ensureId(stack);
        EquippedToolbox.set(player, equipped.curioSlot(), stack);

        int curioSlot = equipped.curioSlot();
        // 1.20.1 的 Player#openMenu 不能附带额外数据，Forge 提供 NetworkHooks.openScreen。
        NetworkHooks.openScreen(
                serverPlayer,
                new SimpleMenuProvider(
                        (id, inventory, p) -> new WearableToolboxMenu(id, inventory, stack, curioSlot),
                        stack.getHoverName()),
                buffer -> buffer.writeItem(stack)
        );
    }

    /**
     * 用服务端发来的最新工具箱物品重建当前菜单的虚拟库存。
     * 过滤器不是真实槽位，只改服务端不会让已经打开的界面刷新。
     */
    public void refreshFromStack(ItemStack stack) {
        ToolboxStorage.readInto(contentHolder, stack);
    }

    /**
     * 点击空的过滤器槽位时，Create 会清空该隔间的过滤器；但过滤器不是普通槽位，
     * 不会触发原版的槽位变更同步，因此这里主动把物品写回装备槽并发给客户端。
     */
    @Override
    public void clicked(int index, int flags, ClickType type, Player player) {
        ItemStack before = index >= 0 && index < 32 ? getFilter(index / 4).copy() : ItemStack.EMPTY;
        super.clicked(index, flags, type, player);

        if (player.level().isClientSide || index < 0 || index >= 32) {
            return;
        }

        if (ItemStack.matches(before, getFilter(index / 4))) {
            return;
        }

        ItemStack stack = curioSlot >= 0
                ? CuriosCompat.getBody(player, curioSlot)
                : player.getItemBySlot(EquipmentSlot.CHEST);
        if (ToolboxItems.isToolbox(stack)) {
            writeToEquipped(contentHolder, stack);
        }
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
        UUID currentId = ToolboxStorage.getId(stack);
        if (toolboxId != null && !toolboxId.equals(currentId)) {
            return;
        }

        writeToEquipped(contentHolder, stack);
    }

    /**
     * 把菜单里的库存写回工具箱物品 NBT，再放回原装备槽。
     */
    private void writeToEquipped(ToolboxBlockEntity contentHolder, ItemStack stack) {
        ToolboxStorage.writeInventory(stack, ToolboxStorage.inventoryOf(contentHolder));
        ToolboxStorage.ensureId(stack);
        EquippedToolbox.set(player, curioSlot, stack);
        if (player instanceof ServerPlayer serverPlayer) {
            WearableToolboxBindings.syncToolbox(serverPlayer, curioSlot, stack);
        }
    }

    /**
     * 把玩家背包里能放进工具箱的物品全部存入当前打开的工具箱。
     */
    public static void depositAll(ServerPlayer player) {
        if (player.containerMenu instanceof WearableToolboxMenu menu) {
            ToolboxInventory inventory = ToolboxStorage.inventoryOf(menu.contentHolder);
            if (!depositInto(player, inventory)) {
                return;
            }
            // 与 Create 的 ToolboxDisposeAllPacket 一致：先解除绑定，防止 tick 又把物品补回快捷栏。
            WearableToolboxBindings.clearForDeposit(player);

            ItemStack stack = menu.curioSlot >= 0
                    ? CuriosCompat.getBody(player, menu.curioSlot)
                    : player.getItemBySlot(EquipmentSlot.CHEST);
            if (ToolboxItems.isToolbox(stack)) {
                menu.writeToEquipped(menu.contentHolder, stack);
            }
            player.getInventory().setChanged();
            menu.broadcastChanges();
            return;
        }

        // 径向菜单没有 containerMenu，直接操作当前穿戴的工具箱物品。
        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }

        ItemStack stack = equipped.stack();
        ToolboxInventory inventory = ToolboxStorage.readInventory(stack);
        if (!depositInto(player, inventory)) {
            return;
        }
        WearableToolboxBindings.clearForDeposit(player);

        ToolboxStorage.writeInventory(stack, inventory);
        ToolboxStorage.ensureId(stack);
        EquippedToolbox.set(player, equipped.curioSlot(), stack);
        WearableToolboxBindings.syncToolbox(player, equipped.curioSlot(), stack);
        player.getInventory().setChanged();
    }

    private static boolean depositInto(ServerPlayer player, ToolboxInventory inventory) {
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
        return changed.isTrue();
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
