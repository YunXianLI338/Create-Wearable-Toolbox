package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 客户端请求：把玩家背包里能放进当前工具箱的物品全部存入。
 */
public record DepositAllPayload() {

    public DepositAllPayload(FriendlyByteBuf buffer) {
        this();
    }

    public static void write(DepositAllPayload message, FriendlyByteBuf buffer) {
        // 无数据
    }
}
