package com.minecart.wearabletoolbox.mixin;

import com.minecart.wearabletoolbox.common.ToolboxItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让工具箱走原版的“装备槽判定”入口。
 *
 * <p>原版 {@code InventoryMenu#quickMoveStack}、{@code ArmorSlot#mayPlace} 以及
 * {@code ItemStack#canEquip} 都会调用这个方法。把它重定向到胸甲槽后，生存模式下的
 * Shift 快捷穿戴、手动拖入胸甲槽等行为都由原版逻辑完成，不需要自定义点击包。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getEquipmentSlotForItem", at = @At("HEAD"), cancellable = true)
    private void wearableToolbox$useChestSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (ToolboxItems.isToolbox(stack)) {
            cir.setReturnValue(EquipmentSlot.CHEST);
        }
    }
}
