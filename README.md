# 可穿戴的工具箱（Wearable Toolbox）

**Minecraft 1.21.1 · NeoForge · MIT**

把机械动力的工具箱装备到胸甲栏或 Curios 的胸饰栏：按 `V` 打开 8 隔间界面，或按住 `Alt` 打开与放置版一致的径向菜单，把隔间绑定到快捷栏并自动补充。打开界面时展示模型会切换为打开盖子的版本。Curios 为可选前置，不安装也能运行。

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
| Curios API | 9.5.1+1.21.1（可选，不安装也能运行） |

### 前置模组

- **机械动力 Create 6.0.10（必需）**：编译期依赖 `com.simibubi.create:create-1.21.1:6.0.10-280`，其 jarjar 已内置 Ponder、Flywheel、Registrate。
- **Curios API 9.5.1+1.21.1（可选）**：安装后可把工具箱放进 **胸饰（body）** 饰品栏；未安装时只使用原版胸甲栏。Curios 相关调用做了加载检查，不会因缺少前置而崩溃。
- 本地开发环境 `run/mods` 保留了 `create-1.21.1-6.0.10.jar` 与 `curios-neoforge-9.5.1+1.21.1.jar`。

## 玩法

1. 把任意颜色的 Create 工具箱放进原版 **胸甲栏**；安装 Curios 时也可以放进 **胸饰（body）** 饰品栏。
2. 在物品栏里对准工具箱 `Shift + 左键`（或 `Shift + 右键`），可快捷穿到胸甲栏。
3. 按住 `Alt`（Create 的“工具箱”键，默认左 Alt，可在“选项 → 控制”中修改）打开径向菜单：
   - 把鼠标移到某个有物品的隔间上松开，或按对应数字键，即可把该隔间绑定到当前快捷栏。
   - 中间的槽位用于解除当前快捷栏的绑定。
   - 快捷栏物品不足时自动从隔间补充，超过半组时自动存回；换握其他物品或清空隔间会自动解绑。
   - 已绑定的快捷栏槽位会显示 Create 同款黄铜色边框，选中槽位边框样式与放置版一致。
   - 玩家身上有工具箱时，即使附近有放置版工具箱也优先打开可穿戴版。
4. 按 `V`（可修改）打开完整界面：8 个隔间、每格 4 槽、过滤器、快速移动与堆叠整理；界面左侧的“把物品收回工具箱”按钮会把背包里能放入的物品全部存入。关闭时内容写回物品。
5. 身上的展示模型使用工具箱本身的颜色贴图；打开 `V` 界面或径向菜单时切换为打开盖子的版本，关闭后恢复。打开界面时双臂会向前伸出并朝正前方低头，姿势参考 WATUT 的 GUI 动作；这两个动作可在客户端配置中分别关闭。
6. 手持工具箱右键 **盔甲架**，可把它放到盔甲架的胸甲槽，原有胸甲退回背包。

内容保存在物品的 `create:toolbox_inventory` 组件中，掉落后不会丢失。绑定数据保存在玩家的 `WearableToolboxData` 持久化 NBT 中，换下工具箱或隔间清空时自动解除。

## 配置

客户端首次启动后生成 `config/wearable_toolbox-client.toml`；也可以在 **Mods → Wearable Toolbox → Config** 中打开配置界面：

- `"手臂前伸" = true`：打开穿戴版工具箱界面时，玩家双臂向前伸出。
- `"低头" = true`：打开穿戴版工具箱界面时，玩家朝身体正前方低头。

```toml
"手臂前伸" = true
"低头" = true
```

两个选项互不影响，设为 `false` 即可关闭对应动作。动作与开盖模型会同步给附近玩家；配置只控制手臂与低头动作是否渲染。

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

- 构建产物：`build/libs/wearable_toolbox-0.1.3-hotfix.jar`，放入客户端 `mods/` 即可。
- 若网络无法访问 `maven.neoforged.net`，可改用 `gradlew.bat build --offline`；本机 Gradle 缓存中已有 21.1.248 全套依赖。
- 首次构建会下载 Gradle、NeoForge 并执行 NeoForm 反编译，耗时较长（网络慢时可达一小时），日志暂时没有输出并不代表卡死。
- `runServer` 首次启动后需把 `run/eula.txt` 改为 `eula=true`；局域网调试可再把 `server.properties` 的 `online-mode` 设为 `false`。
- `runData` 的输出写入 `src/generated/resources`，该目录已挂进资源源集；不要手改生成文件，改数据生成器后重跑。

### 在 IDEA 中运行

项目已配置为使用 Gradle Wrapper 8.8，Gradle JVM 取 `#JAVA_HOME`（需为 JDK 21）。IDEA 运行配置里新增了四个 Gradle 任务，直接复用项目构建，不需要手写 `build/moddev` 下的参数文件：

- `Client (Gradle)` → `runClient`
- `Server (Gradle)` → `runServer`
- `Data (Gradle)` → `runData`
- `Build (Gradle)` → `build`

如果 Gradle 工具窗提示 `Gradle build daemon has been stopped: stop command received`，点一次 **Reload All Gradle Projects** 即可；在 IDEA 打开项目期间不要执行 `gradlew --stop`，它会连同 IDEA 使用的 daemon 一起停掉。


## 目录结构

```
wearable_toolbox/
├─ build.gradle                     # ModDevGradle、运行配置、资源展开
├─ gradle.properties                # MC/NeoForge/Parchment 版本与模组元数据
├─ settings.gradle                  # 插件仓库与根项目名
├─ src/main/java/com/minecart/wearabletoolbox/
│  ├─ WearableToolbox.java          # @Mod 主类、菜单与网络注册
│  ├─ WearableToolboxConfig.java    # 客户端配置（手臂/低头开关）
│  ├─ WearableToolboxEvents.java    # 物品提示
│  ├─ WearableToolboxServerEvents.java # 快捷栏绑定/自动补充 tick
│  ├─ client/                       # 按键、界面、径向菜单与客户端事件
│  ├─ common/                       # 菜单、网络包、绑定数据、工具箱判定
│  └─ mixin/                        # 胸甲栏限制、Create 库存/径向菜单访问器
├─ src/main/resources/
│  ├─ META-INF/neoforge.mods.toml   # 1.21.1 形态（含 modLoader / loaderVersion）
│  ├─ wearable_toolbox.mixins.json  # Mixin 配置
│  ├─ assets/wearable_toolbox/      # 语言文件与 brown / brown_on 展示模型
│  ├─ data/curios/                  # 胸饰槽物品标签（Curios 可选）
│  └─ data/wearable_toolbox/        # 胸饰槽实体配置（Curios 可选）
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
