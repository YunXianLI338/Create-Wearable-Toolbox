package com.minecart.wearabletoolbox;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * 客户端配置：控制打开可穿戴工具箱界面时的本地玩家姿势与 Alt 菜单优先级。
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

    public static final ModConfigSpec.BooleanValue INVENTORY_TOOLBOXES = BUILDER
            .comment("允许物品栏中未穿戴的工具箱也用快捷键打开，多个工具箱时先选择。")
            .define("物品栏内工具箱快捷打开", false);

    public static final ModConfigSpec.EnumValue<Priority> TOOLBOX_PRIORITY = BUILDER
            .comment("按 Alt 打开径向菜单时，身上工具箱与周围放置工具箱的选择方式。")
            .defineEnum("工具箱优先级", Priority.EQUAL);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WearableToolboxConfig() {
    }

    public enum Priority implements TranslatableEnum {
        EQUAL("wearable_toolbox.config.toolbox_priority.equal"),
        WEARABLE("wearable_toolbox.config.toolbox_priority.wearable"),
        PLACED("wearable_toolbox.config.toolbox_priority.placed");

        private final String translationKey;

        Priority(String translationKey) {
            this.translationKey = translationKey;
        }

        @Override
        public Component getTranslatedName() {
            return Component.translatable(translationKey);
        }
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

    public static boolean inventoryToolboxes() {
        return INVENTORY_TOOLBOXES.get();
    }
}
