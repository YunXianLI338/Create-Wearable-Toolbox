package com.minecart.wearabletoolbox;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置：控制打开可穿戴工具箱界面时的本地玩家姿势。
 *
 * <p>首次启动后生成 {@code config/wearable_toolbox-client.toml}。</p>
 */
public final class WearableToolboxConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue POSE_ARMS = BUILDER
            .comment("打开可穿戴工具箱界面时，是否让本地玩家双臂向前伸出。")
            .define("手臂前伸", true);

    public static final ModConfigSpec.BooleanValue LOWER_HEAD = BUILDER
            .comment("打开可穿戴工具箱界面时，是否让本地玩家朝身体正前方低头。")
            .define("低头", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WearableToolboxConfig() {
    }

    public static boolean poseArms() {
        return POSE_ARMS.get();
    }

    public static boolean lowerHead() {
        return LOWER_HEAD.get();
    }
}
