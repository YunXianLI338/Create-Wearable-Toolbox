package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.SyncBindingsPayload;
import com.minecart.wearabletoolbox.common.SyncToolboxPayload;
import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;

/**
 * 客户端接收服务端数据的落地处理。
 *
 * <p>单独放在 client 包里，避免公共代码在服务端加载到 {@link Minecraft}。</p>
 */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void syncToolbox(SyncToolboxPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            EquippedToolbox.set(player, payload.curioSlot(), payload.stack());
            if (player.containerMenu instanceof WearableToolboxMenu menu) {
                menu.refreshFromStack(payload.stack());
            }
        }
    }

    public static void syncBindings(SyncBindingsPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        CompoundTag root = player.getPersistentData();
        if (payload.data().isEmpty()) {
            root.remove(WearableToolboxBindings.DATA_KEY);
        } else {
            root.put(WearableToolboxBindings.DATA_KEY, payload.data().copy());
        }
    }
}
