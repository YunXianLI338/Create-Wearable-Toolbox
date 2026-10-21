package com.minecart.wearabletoolbox.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 径向菜单需要读取虚拟方块实体的库存（包私有字段），这里只暴露访问器。
 */
@Mixin(ToolboxBlockEntity.class)
public interface ToolboxBlockEntityAccess {
    @Accessor("inventory")
    ToolboxInventory wearableToolbox$inventory();
}
