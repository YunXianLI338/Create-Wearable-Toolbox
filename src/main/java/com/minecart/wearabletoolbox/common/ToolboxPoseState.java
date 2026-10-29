package com.minecart.wearabletoolbox.common;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可穿戴工具箱“打开界面”姿势的同步状态。
 *
 * <p>服务端只保存当前正在摆姿势的玩家；客户端保存从服务端收到的其他玩家状态。
 * 本地玩家不使用客户端集合，而是直接读取当前是否打开了工具箱界面，避免网络延迟。</p>
 */
public final class ToolboxPoseState {
    private static final Set<UUID> SERVER_POSING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_POSING = ConcurrentHashMap.newKeySet();

    private ToolboxPoseState() {
    }

    public static void setServer(UUID playerId, boolean posing) {
        if (posing) {
            SERVER_POSING.add(playerId);
        } else {
            SERVER_POSING.remove(playerId);
        }
    }

    public static boolean isServerPosing(UUID playerId) {
        return SERVER_POSING.contains(playerId);
    }

    public static void removeServer(UUID playerId) {
        SERVER_POSING.remove(playerId);
    }

    public static void setClient(UUID playerId, boolean posing) {
        if (posing) {
            CLIENT_POSING.add(playerId);
        } else {
            CLIENT_POSING.remove(playerId);
        }
    }

    public static boolean isClientPosing(UUID playerId) {
        return CLIENT_POSING.contains(playerId);
    }

    public static void clearClient() {
        CLIENT_POSING.clear();
    }
}
