# 可穿戴的工具箱（Wearable Toolbox）

**Minecraft 1.21.1 · NeoForge · MIT**

把机械动力的工具箱装备到胸甲栏：按 `V` 打开 8 隔间界面，或按住 `Alt` 打开与放置版一致的径向菜单，把隔间绑定到快捷栏并自动补充。Curios 为可选前置，不安装也能运行。

---

## 环境

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248（`[21.1,)`） |
| ModDevGradle | 2.0.143 |
| Gradle Wrapper | 8.8 |
| Java | 21 |
| Parchment | 2024.11.17 |
| 机械动力 Create | 6.0.10（`[6.0.10,)`） |
| Curios API | 9.2.2+1.21.1（可选，不安装也能运行） |

### 前置模组

- **机械动力 Create 6.0.10（必需）**：编译期依赖 `com.simibubi.create:create-1.21.1:6.0.10-280`，其 jarjar 已内置 Ponder、Flywheel、Registrate。
- **Curios API 9.2.2+1.21.1（可选）**：本模组不使用 Curios 槽位，也不调用 Curios API；仅将其声明为 `optional`，安装与否都不影响装备和打开工具箱。
- 本地开发环境 `run/mods` 保留了 `create-1.21.1-6.0.10.jar` 与 `curios-neoforge-9.5.1+1.21.1.jar`，后者仅用于测试与其他模组的兼容性。

## 玩法

1. 把任意颜色的 Create 工具箱放进原版 **胸甲栏**。
2. 按住 `Alt`（Create 的“工具箱”键，默认左 Alt，可在“选项 → 控制”中修改）打开径向菜单：
   - 把鼠标移到某个有物品的隔间上松开，或按对应数字键，即可把该隔间绑定到当前快捷栏。
   - 中间的槽位用于解除当前快捷栏的绑定。
   - 快捷栏物品不足时自动从隔间补充，超过半组时自动存回；换握其他物品或清空隔间会自动解绑。
3. 按 `V`（可修改）打开完整界面：8 个隔间、每格 4 槽、过滤器、快速移动与堆叠整理；关闭时内容写回物品。
4. 附近有放置版工具箱时，`Alt` 仍优先交给 Create 原逻辑，不会同时弹出两个界面。

内容保存在物品的 `create:toolbox_inventory` 组件中，掉落后不会丢失。绑定数据保存在玩家的 `WearableToolboxData` 持久化 NBT 中，换下工具箱或隔间清空时自动解除。

## 构建与运行

```sh
# Windows
gradlew.bat build
gradlew.bat runClient
gradlew.bat runServer
gradlew.bat runData

# Linux / macOS
./gradlew build
```

- 构建产物：`build/libs/wearable_toolbox-0.1.0.jar`，放入客户端 `mods/` 即可。
- 若网络无法访问 `maven.neoforged.net`，可改用 `gradlew.bat build --offline`；本机 Gradle 缓存中已有 21.1.248 全套依赖。
- 首次构建会下载 Gradle、NeoForge 并执行 NeoForm 反编译，耗时较长（网络慢时可达一小时），日志暂时没有输出并不代表卡死。
- `runServer` 首次启动后需把 `run/eula.txt` 改为 `eula=true`；局域网调试可再把 `server.properties` 的 `online-mode` 设为 `false`。
- `runData` 的输出写入 `src/generated/resources`，该目录已挂进资源源集；不要手改生成文件，改数据生成器后重跑。

## 目录结构

```
wearable_toolbox/
├─ build.gradle                     # ModDevGradle、运行配置、资源展开
├─ gradle.properties                # MC/NeoForge/Parchment 版本与模组元数据
├─ settings.gradle                  # 插件仓库与根项目名
├─ src/main/java/com/minecart/wearabletoolbox/
│  ├─ WearableToolbox.java          # @Mod 主类、菜单与网络注册
│  ├─ WearableToolboxEvents.java    # 物品提示
│  ├─ WearableToolboxServerEvents.java # 快捷栏绑定/自动补充 tick
│  ├─ client/                       # 按键、界面、径向菜单与客户端事件
│  ├─ common/                       # 菜单、网络包、绑定数据、工具箱判定
│  └─ mixin/                        # 胸甲栏限制、Create 库存/径向菜单访问器
├─ src/main/resources/
│  ├─ META-INF/neoforge.mods.toml   # 1.21.1 形态（含 modLoader / loaderVersion）
│  ├─ wearable_toolbox.mixins.json  # Mixin 配置
│  └─ assets/wearable_toolbox/      # 语言文件
├─ src/generated/resources/         # runData 输出（构建时生成）
├─ LICENSE                          # MIT
└─ README.md
```

## 元数据约定

- `mod_id` 为 `wearable_toolbox`，同时出现在 `gradle.properties`、`@Mod` 注解与 `neoforge.mods.toml` 三处，改动时必须同步。
- 1.21.1 的 `neoforge.mods.toml` **必须**保留 `modLoader="javafml"` 与 `loaderVersion`，否则客户端会以 `Missing ModLoader` 拒载。
- 前置依赖在 `neoforge.mods.toml` 的 `[[dependencies.wearable_toolbox]]` 中声明：`create [6.0.10,)` 为 `required`，`curios [9.2.2,)` 为 `optional`，均 `ordering="AFTER"`。
- 许可证为 MIT，作者 YunXian_LI。

## 作者与许可

作者：YunXian_LI

本项目以 [MIT 许可证](LICENSE) 发布。
