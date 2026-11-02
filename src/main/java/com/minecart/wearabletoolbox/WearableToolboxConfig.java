package com.minecart.wearabletoolbox;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置：控制打开可穿戴工具箱界面时的本地玩家姿势与 Alt 菜单优先级。
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

    public static final ForgeConfigSpec.EnumValue<Priority> TOOLBOX_PRIORITY = BUILDER
            .comment("按 Alt 打开径向菜单时，身上工具箱与周围放置工具箱的选择方式。")
            .defineEnum("toolbox_priority", Priority.EQUAL);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WearableToolboxConfig() {
    }

    public enum Priority {
        EQUAL,
        WEARABLE,
        PLACED
    }

    public static Priority priority() {
        return TOOLBOX_PRIORITY.get();
    }

    public static boolean poseArms() {
        return POSE_ARMS.get();
    }

    public static boolean lowerHead() {
        return LOWER_HEAD.get();
    }
}
