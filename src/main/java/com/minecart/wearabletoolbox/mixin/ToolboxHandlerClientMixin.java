package com.minecart.wearabletoolbox.mixin;

import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家身上有可穿戴工具箱时，跳过 Create 的“打开附近工具箱”快捷键逻辑。
 *
 * <p>Create 的 {@code InputEvents} 只在事件入口检查一次 {@code Minecraft.screen}，之后才调用
 * {@link ToolboxHandlerClient#onKeyInput(int, boolean)}。我们的 HIGHEST 优先级监听器先打开
 * 可穿戴版径向菜单时，Create 仍会继续执行并再打开放置版菜单，把前者顶掉。这里直接取消
 * Create 的这次按键处理，让“穿戴版优先”真正生效。</p>
 */
@Mixin(ToolboxHandlerClient.class)
public final class ToolboxHandlerClientMixin {
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
    private static void wearableToolbox$skipCreateMenu(int key, boolean pressed, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && EquippedToolbox.find(minecraft.player) != null) {
            callback.cancel();
        }
    }
}
