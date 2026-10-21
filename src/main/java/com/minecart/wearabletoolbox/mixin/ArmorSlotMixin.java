package com.minecart.wearabletoolbox.mixin;

import com.minecart.wearabletoolbox.common.ToolboxItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 允许把机械动力的工具箱放进原版胸甲栏。
 *
 * <p>工具箱是 {@code BlockItem}，没有 {@code minecraft:equippable} 组件，原版
 * {@code ArmorSlot#mayPlace(ItemStack)} 会拒绝。这里只放宽胸甲槽的判定，不改变
 * 其他物品或其他装备槽的行为。{@code ArmorSlot} 是包私有类，因此用字符串指定目标。</p>
 */
@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ArmorSlotMixin {
    @Shadow
    @Final
    private EquipmentSlot slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void wearableToolbox$allowToolboxInChestSlot(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.slot == EquipmentSlot.CHEST && ToolboxItems.isToolbox(stack)) {
            cir.setReturnValue(true);
        }
    }
}
