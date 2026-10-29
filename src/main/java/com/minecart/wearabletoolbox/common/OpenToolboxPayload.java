package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 客户端请求：打开身上工具箱的界面。
 */
public record OpenToolboxPayload() {

    public OpenToolboxPayload(FriendlyByteBuf buffer) {
        this();
    }

    public static void write(OpenToolboxPayload message, FriendlyByteBuf buffer) {
        // 无数据
    }
}
