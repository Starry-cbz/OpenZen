# OpenZen 项目记忆

## 项目概况

- 项目：OpenZen，面向 Minecraft 客户端的反混淆与字节码变换研究工程。
- 目标环境：Minecraft 1.20.1、Forge 47.4.20。
- 技术栈：Java 17、ForgeGradle 6、ASM、Gradle Wrapper；原生路径使用 CMake、Visual Studio 2022、Qt 6 和 vcpkg。

## 关键入口与构建

- Java Agent 入口：`src/main/java/asm/patchify/loader/PatchAgent.java`。
- 原生加载器：`native/`，最终产物为 `build/dist/OpenZenLoader.exe`。
- Java Agent 构建：`./gradlew.bat jar`，产物为 `build/libs/hey-1.0.jar`。
- 原生加载器构建：`./gradlew.bat dll`。
- 清理：`./gradlew.bat clean`。
- 构建后的类名映射：`build/rename-mapping.txt`，必须与对应产物一起保存。

## 当前会话结论

- 根目录 README 已重写为中性、技术导向的文档，移除了旧文档中的不当言论和图片引用。
- 旧 README 配套的 `img/` 目录已移除，避免不适图片和视频继续留在仓库中。
- README 现在明确说明了授权使用边界、Java Agent 与原生加载器的区别、构建前置条件和许可证现状。
- Git 提交身份为 `Starry-cbz <3663643028@qq.com>`。
- 玩家可见文本采用显示层汉化：模块、设置和模式的内部英文标识保持不变，通过 `UiText` 及 `getDisplayName()`、`getDisplayValue()` 等接口提供中文显示，避免破坏旧配置、命令参数和模式判断。
- WebUI API 同时返回原始字段和中文显示字段；读取与提交参数继续使用原始英文名称和值。
- Java 界面中文字体使用 PingFang，原生加载器使用 Microsoft YaHei UI。
- 项目要求 JDK 17，但本机 JDK 17 工具链下载失败；本机 JDK 21 可进入 `compileJava`，本次按要求中止本地完整编译，后续由 GitHub Actions 构建验证。
