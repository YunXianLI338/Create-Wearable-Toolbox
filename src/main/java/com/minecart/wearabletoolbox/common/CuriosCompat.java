package com.minecart.wearabletoolbox.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Curios 的可选兼容层。
 *
 * <p>只有确认 Curios 已加载后才会调用本类；未安装 Curios 时，引用 Curios 类型的
 * 方法不会被执行，类也不会被提前加载。</p>
 */
final class CuriosCompat {
    private CuriosCompat() {
    }

    @Nullable
    static EquippedToolbox findBody(Player player) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) {
            return null;
        }
        Optional<ICurioStacksHandler> handler = curios.get().getStacksHandler(EquippedToolbox.CURIO_SLOT);
        if (handler.isEmpty()) {
            return null;
        }

        IDynamicStackHandler stacks = handler.get().getStacks();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack stack = stacks.getStackInSlot(slot);
            if (ToolboxItems.isToolbox(stack)) {
                return new EquippedToolbox(stack, slot);
            }
        }
        return null;
    }

    static void setBody(Player player, int slot, ItemStack stack) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) {
            return;
        }
        Optional<ICurioStacksHandler> handler = curios.get().getStacksHandler(EquippedToolbox.CURIO_SLOT);
        if (handler.isEmpty()) {
            return;
        }
        IDynamicStackHandler stacks = handler.get().getStacks();
        if (slot >= 0 && slot < stacks.getSlots()) {
            stacks.setStackInSlot(slot, stack);
        }
    }

    static ItemStack getBody(Player player, int slot) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Optional<ICurioStacksHandler> handler = curios.get().getStacksHandler(EquippedToolbox.CURIO_SLOT);
        if (handler.isEmpty()) {
            return ItemStack.EMPTY;
        }
        IDynamicStackHandler stacks = handler.get().getStacks();
        return slot >= 0 && slot < stacks.getSlots() ? stacks.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    /**
     * Curios 的“隐藏饰品”开关在当前槽位上是否允许渲染。
     *
     * <p>没有对应槽位或数据时返回 {@code true}，避免因为数据缺失误把模型藏掉。</p>
     */
    static boolean isBodySlotVisible(Player player, int slot) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) {
            return true;
        }
        Optional<ICurioStacksHandler> handler = curios.get().getStacksHandler(EquippedToolbox.CURIO_SLOT);
        if (handler.isEmpty()) {
            return true;
        }
        var renders = handler.get().getRenders();
        return renders.size() <= slot || renders.get(slot);
    }
}
