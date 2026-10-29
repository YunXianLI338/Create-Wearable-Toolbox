# 可穿戴的工具箱（Wearable Toolbox）

**Minecraft 1.20.1 · Forge · MIT**

把机械动力的工具箱装备到胸甲栏或 Curios 的胸饰栏：按 `V` 打开 8 隔间界面，或按住 `Alt` 打开与放置版一致的径向菜单，把隔间绑定到快捷栏并自动补充。打开界面时展示模型会切换为打开盖子的版本。Curios 为可选前置，不安装也能运行。

> 这是 **1.20.1（Forge）** 版本；1.21.1（NeoForge）版本见 `main` 分支。

---

## 环境

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.3.0（`[47,)`） |
| ForgeGradle | 6.0.24 |
| Gradle Wrapper | 8.8 |
| Java | 17 |
| 映射 | official 1.20.1（Mojang 官方名，未加 Parchment） |
| 机械动力 Create | 6.0.8（`[6.0.8,)`，Maven 版本 `6.0.8-291`） |
| Curios API | 5.14.1+1.20.1（可选，不安装也能运行） |

### 前置模组

- **机械动力 Create 6.0.8（必需）**：运行时要装 `create-1.20.1-6.0.8-291-all.jar`（jarjar 全量版，自带 Ponder、Flywheel、Registrate）。
- **编译期**：`create-1.20.1:6.0.8-291:slim` 不含打包依赖库，因此额外声明 `Ponder-Forge-1.20.1:1.0.91`、`flywheel-forge-api-1.20.1:1.0.6-beta-266`、`Registrate:MC1.20-1.3.3`。Ponder 的 jar 内已包含 Catnip，不需要单独的 Catnip 依赖。
- **Curios API 5.14.1+1.20.1（可选）**：安装后可把工具箱放进 **胸饰（body）** 饰品栏；未安装时只使用原版胸甲栏。Curios 相关调用做了加载检查，不会因缺少前置而崩溃。
- Create 系依赖必须用 `fg.deobf(...)` 包一层：发布的 mod jar 是 SRG 名，而 ForgeGradle 6 的开发 classpath 是 official 名，不 deobf 会编译失败。

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

1.20.1 没有数据组件，内容保存在物品 NBT 的 `Inventory` 标签里，写入方式与放置版一致，掉落后不会丢失。绑定数据保存在玩家的 `WearableToolboxData` 持久化 NBT 中，换下工具箱或隔间清空时自动解除。

## 配置

客户端首次启动后生成 `config/wearable_toolbox-client.toml`。Forge 1.20.1 没有内置的模组配置界面，直接编辑该文件（或使用第三方配置模组）：

```toml
pose_arms = true
lower_head = true
```

- `pose_arms`：打开穿戴版工具箱界面时，玩家双臂向前伸出。
- `lower_head`：打开穿戴版工具箱界面时，玩家朝身体正前方低头。

两个选项互不影响，设为 `false` 即可关闭对应动作。动作与开盖模型会同步给附近玩家；配置只控制手臂与低头动作是否渲染。

## 与 1.21.1 版本的差异

| 项目 | 1.20.1（本分支） | 1.21.1（`main`） |
| --- | --- | --- |
| 加载器 / Java | Forge 47 / Java 17 | NeoForge 21.1 / Java 21 |
| 物品库存 | 物品 NBT 的 `Inventory` 标签 | `create:toolbox_inventory` 数据组件 |
| 网络层 | Forge `SimpleChannel` | `CustomPacketPayload` + `StreamCodec` |
| 配置 | `ForgeConfigSpec`，仅文件 | `ModConfigSpec`，带游戏内界面 |
| 配置键 | `pose_arms` / `lower_head` | `手臂前伸` / `低头` |

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

- 构建产物：`build/libs/wearable_toolbox-0.1.3.jar`，放入客户端 `mods/` 即可。
- `libs/maven` 是本地预置依赖仓库（构建时优先命中，缺失时自动回落远程仓库）。网络慢时可先用浏览器或 `curl` 把依赖 jar 放进 `libs/maven/<group>/<artifact>/<version>/`，并在同目录手写一个 `<packaging>jar</packaging>` 的最小 pom。该目录不进版本库。
- 首次构建会下载 Forge MDK 并执行反编译（ForgeGradle 的 `setup` 阶段），耗时较长；日志暂时没有输出并不代表卡死。
- `runClient` / `runServer` 不需要手动往 `run/mods` 放前置：Create、Ponder、Flywheel、Registrate 以 `implementation` + `fg.deobf` 声明，ForgeGradle 会把它们（已重映射为 official 名）放进开发运行时的 classpath，FML 在开发环境从 classpath 发现模组。若想测试 Curios，把它也加成 `runtimeOnly fg.deobf(...)`，或直接把 jar 丢进 `run/mods/`。
- 缺失前置时客户端会在启动阶段直接退出，日志（`run/logs/latest.log`）末尾是 `Missing or unsupported mandatory dependencies: Mod ID: 'create' ...`。
- `runServer` 首次启动后需把 `run/eula.txt` 改为 `eula=true`；局域网调试可再把 `server.properties` 的 `online-mode` 设为 `false`。
- `runData` 的输出写入 `src/generated/resources`，该目录已挂进资源源集；不要手改生成文件，改数据生成器后重跑。

### 在 IDEA 中运行

项目使用 Gradle Wrapper 8.8，Gradle JVM 取 `#JAVA_HOME`；编译工具链固定为 **Java 17**，本机需装有 JDK 17（可在 `~/.gradle/gradle.properties` 里用 `org.gradle.java.installations.paths` 指向它）。运行配置可直接复用 Gradle 任务：

- `Client (Gradle)` → `runClient`
- `Server (Gradle)` → `runServer`
- `Data (Gradle)` → `runData`
- `Build (Gradle)` → `build`

## 目录结构

```
wearable_toolbox/
├─ build.gradle                     # ForgeGradle 6、Mixin、依赖与资源展开
├─ gradle.properties                # MC/Forge/Create 版本与模组元数据
├─ settings.gradle                  # 插件仓库与根项目名
├─ libs/maven/                      # 本地预置依赖（可选，不进版本库）
├─ src/main/java/com/minecart/wearabletoolbox/
│  ├─ WearableToolbox.java          # @Mod 主类、菜单注册
│  ├─ WearableToolboxConfig.java    # ForgeConfigSpec 客户端配置
│  ├─ WearableToolboxEvents.java    # 物品提示、盔甲架交互
│  ├─ WearableToolboxServerEvents.java # 快捷栏绑定/自动补充 tick
│  ├─ client/                       # 按键、界面、径向菜单、客户端事件与渲染
│  ├─ common/                       # 菜单、网络、存储、绑定数据、工具箱判定
│  └─ mixin/                        # 胸甲栏限制、Create 库存/径向菜单访问器
├─ src/main/resources/
│  ├─ META-INF/mods.toml            # Forge 元数据（含 [[mixins]] 与依赖声明）
│  ├─ pack.mcmeta                   # pack_format 15
│  ├─ wearable_toolbox.mixins.json  # Mixin 配置（JAVA_17 + refmap）
│  ├─ assets/wearable_toolbox/      # 语言文件与 brown / brown_on 展示模型
│  ├─ data/curios/                  # 胸饰槽物品标签（Curios 可选）
│  └─ data/wearable_toolbox/        # 胸饰槽实体配置（Curios 可选）
├─ LICENSE                          # MIT
└─ README.md
```

## 元数据约定

- `mod_id` 为 `wearable_toolbox`，同时出现在 `gradle.properties`、`@Mod` 注解与 `META-INF/mods.toml` 三处，改动时必须同步。
- `mods.toml` 必须保留 `modLoader="javafml"` 与 `loaderVersion="[47,)"`，否则客户端会以 `Missing ModLoader` 拒载。
- 前置依赖在 `[[dependencies.wearable_toolbox]]` 中声明，用 `mandatory=true/false`（不是 NeoForge 的 `type=`）：`create [6.0.8,)` 必需，`curios [5.2.0,)` 可选，均 `ordering="AFTER"`。
- Mixin 需要 refmap：针对原版方法的注入由注解处理器生成映射（`setupAnim`、`getEquipmentSlotForItem`），针对 Create 的注入要写 `remap = false`（Create 的方法名不混淆）。
- 许可证为 MIT，作者 YunXian_LI。

## 作者与许可

作者：YunXian_LI

本项目以 [MIT 许可证](LICENSE) 发布。
