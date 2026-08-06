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
