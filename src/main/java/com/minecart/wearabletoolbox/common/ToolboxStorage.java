package com.minecart.wearabletoolbox.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.minecart.wearabletoolbox.mixin.ToolboxBlockEntityAccess;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 1.20.1 的工具箱库存读写。
 *
 * <p>1.21.1 里 Create 用数据组件（{@code create:toolbox_inventory} / {@code create:toolbox_uuid}）
 * 把库存挂在物品栈上；1.20.1 没有数据组件，Create 自己也是把库存写进物品 NBT 的
 * {@code Inventory} 子标签（见 {@code ToolboxBlock#getCloneItemStack} 与 {@code ToolboxBlock#setPlacedBy}）。</p>
 *
 * <p>UUID 在 1.20.1 的物品栈上没有对应物，这里用一个自定义键单独记录，用于判断
 * “身上这件工具箱是不是绑定时那一件”。</p>
 */
public final class ToolboxStorage {
    /** Create 在 1.20.1 里使用的库存子标签名。 */
    public static final String INVENTORY_KEY = "Inventory";

    /** 本模组自造的物品 UUID 键。 */
    public static final String ID_KEY = "WearableToolboxId";

    private ToolboxStorage() {
    }

    /**
     * 取出方块实体内部真正在用的库存对象（虚拟方块实体也适用）。
     */
    public static ToolboxInventory inventoryOf(ToolboxBlockEntity blockEntity) {
        return ((ToolboxBlockEntityAccess) blockEntity).wearableToolbox$inventory();
    }

    /**
     * 读取物品栈上的库存，返回一个脱离方块实体的副本。
     *
     * <p>传入 {@code null} 作为方块实体：{@code ToolboxInventory} 允许空方块实体，
     * 但不能是“有方块实体却还没 setLevel”的状态，那样 {@code onContentsChanged} 会空指针。</p>
     */
    public static ToolboxInventory readInventory(ItemStack stack) {
        ToolboxInventory inventory = new ToolboxInventory(null);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(INVENTORY_KEY, Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(tag.getCompound(INVENTORY_KEY));
        }
        return inventory;
    }

    /**
     * 把物品栈上的库存读进一个（虚拟）方块实体。
     */
    public static void readInto(ToolboxBlockEntity blockEntity, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(INVENTORY_KEY, Tag.TAG_COMPOUND)) {
            blockEntity.readInventory(tag.getCompound(INVENTORY_KEY));
        }
    }

    /**
     * 把库存写回物品栈（只动 {@code Inventory} 子标签，保留其它数据）。
     */
    public static void writeInventory(ItemStack stack, ToolboxInventory inventory) {
        stack.getOrCreateTag().put(INVENTORY_KEY, inventory.serializeNBT());
    }

    @Nullable
    public static UUID getId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(ID_KEY)) {
            return tag.getUUID(ID_KEY);
        }
        return null;
    }

    public static boolean hasId(ItemStack stack) {
        return getId(stack) != null;
    }

    /**
     * 取物品 UUID，没有就生成一个写进 NBT。
     */
    public static UUID ensureId(ItemStack stack) {
        UUID id = getId(stack);
        if (id != null) {
            return id;
        }
        UUID created = UUID.randomUUID();
        stack.getOrCreateTag().putUUID(ID_KEY, created);
        return created;
    }
}
