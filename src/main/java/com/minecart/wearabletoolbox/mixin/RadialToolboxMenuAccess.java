package com.minecart.wearabletoolbox.mixin;

import com.simibubi.create.content.equipment.toolbox.RadialToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 Create 径向菜单的私有状态，供子类在关闭时决定绑定哪个隔间。
 */
@Mixin(RadialToolboxMenu.class)
public interface RadialToolboxMenuAccess {
    @Accessor("hoveredSlot")
    int wearableToolbox$hoveredSlot();

    @Accessor("scrollMode")
    boolean wearableToolbox$scrollMode();

    @Accessor("scrollSlot")
    int wearableToolbox$scrollSlot();

    @Accessor("selectedBox")
    ToolboxBlockEntity wearableToolbox$selectedBox();
}
