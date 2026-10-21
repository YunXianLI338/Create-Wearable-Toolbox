package com.minecart.wearabletoolbox.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Create 的 {@link ToolboxInventory#filters} 是包私有字段，径向菜单与绑定逻辑需要读取
 * 每个隔间的代表物。这里只暴露一个只读访问器，不改动 Create 的库存行为。
 */
@Mixin(ToolboxInventory.class)
public interface ToolboxInventoryAccess {
    @Accessor("filters")
    List<ItemStack> wearableToolbox$filters();
}
