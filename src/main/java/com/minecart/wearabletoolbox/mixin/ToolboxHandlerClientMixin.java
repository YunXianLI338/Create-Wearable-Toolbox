package com.minecart.wearabletoolbox.mixin;

import com.minecart.wearabletoolbox.client.WearableToolboxClientEvents;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 我们的 HIGHEST 监听器决定打开自己的界面时，取消 Create 同一次按键的处理；
 * 交还给 Create 的模式（放置优先、或平级下没有穿戴版）则放行。
 */
@Mixin(ToolboxHandlerClient.class)
public final class ToolboxHandlerClientMixin {
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
    private static void wearableToolbox$skipCreateMenu(int key, boolean pressed, CallbackInfo callback) {
        if (pressed && WearableToolboxClientEvents.shouldSuppressCreateMenu()) {
            callback.cancel();
        }
    }
}
