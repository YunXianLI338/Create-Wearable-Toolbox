package com.minecart.wearabletoolbox;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置：控制打开可穿戴工具箱界面时的本地玩家姿势。
 *
 * <p>首次启动后生成 {@code config/wearable_toolbox-client.toml}。
 * 1.20.1 的 Forge 没有内置的配置界面，只能手动编辑该文件。</p>
 */
public final class WearableToolboxConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue POSE_ARMS = BUILDER
            .comment("打开可穿戴工具箱界面时，是否让本地玩家双臂向前伸出。")
            .define("pose_arms", true);

    public static final ForgeConfigSpec.BooleanValue LOWER_HEAD = BUILDER
            .comment("打开可穿戴工具箱界面时，是否让本地玩家朝身体正前方低头。")
            .define("lower_head", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WearableToolboxConfig() {
    }

    public static boolean poseArms() {
        return POSE_ARMS.get();
    }

    public static boolean lowerHead() {
        return LOWER_HEAD.get();
    }
}
