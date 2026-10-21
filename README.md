# 可穿戴的工具箱（Wearable Toolbox）

**Minecraft 1.21.1 · NeoForge · MIT**

一个把工具箱穿在身上的 NeoForge 模组。当前仓库是项目骨架：构建、加载与元数据已经就绪，玩法内容尚未加入。

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
| Curios API | 9.2.2+1.21.1（`[9.2.2,)`） |

### 前置模组

- **机械动力 Create 6.0.10**：编译期依赖 `com.simibubi.create:create-1.21.1:6.0.10-280`，其 jarjar 已内置 Ponder、Flywheel、Registrate。
- **Curios API 9.2.2+1.21.1**：编译期使用 `:api` 构件；运行时需要完整模组。
- 本地开发环境 `run/mods` 已放入 `create-1.21.1-6.0.10.jar` 与 `curios-neoforge-9.5.1+1.21.1.jar`；正式发布后玩家需自行安装这两个前置。

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
│  └─ WearableToolbox.java          # @Mod 主类
├─ src/main/resources/META-INF/
│  └─ neoforge.mods.toml            # 1.21.1 形态（含 modLoader / loaderVersion）
├─ src/generated/resources/         # runData 输出（构建时生成）
├─ LICENSE                          # MIT
└─ README.md
```

## 元数据约定

- `mod_id` 为 `wearable_toolbox`，同时出现在 `gradle.properties`、`@Mod` 注解与 `neoforge.mods.toml` 三处，改动时必须同步。
- 1.21.1 的 `neoforge.mods.toml` **必须**保留 `modLoader="javafml"` 与 `loaderVersion`，否则客户端会以 `Missing ModLoader` 拒载。
- 前置依赖在 `neoforge.mods.toml` 的 `[[dependencies.wearable_toolbox]]` 中声明：`create [6.0.10,)`、`curios [9.2.2,)`，均为 `required` 且 `ordering="AFTER"`。
- 许可证为 MIT，作者 YunXian_LI。

## 作者与许可

作者：YunXian_LI

本项目以 [MIT 许可证](LICENSE) 发布。
