# OpenZen

OpenZen 是面向 Minecraft 1.20.1 和 Forge 47.4.20 的客户端研究工程，内容来自 Zen 客户端的反混淆与工程化整理。仓库包含 Java Agent、ASM 字节码变换代码，以及可选的 Windows 原生加载器。

本项目的目标是便于学习客户端字节码变换、类加载、混淆分析和 Forge 开发流程。反混淆结果并不等同于原作者的源代码，部分类名、字段名和方法名是根据上下文重建的。

## 安全与使用边界

- 仅在你拥有或明确获准测试的环境中使用，并遵守服务器、游戏和当地法律的规则。
- 这是一个会修改 Minecraft 客户端行为的研究项目，不是经过安全审计的发行版。运行前请审查源码、构建脚本和依赖。
- 原生加载器会对 Java 进程执行注入和类加载操作。请只在可信的本地测试环境中运行，不要运行来源不明的预编译文件。
- 本项目不提供任何服务器兼容性、安全性或持续可用性的保证。

## 目标环境

- Minecraft：`1.20.1`
- Forge：`47.4.20`
- Java：JDK 17
- 构建系统：Gradle Wrapper + ForgeGradle 6
- 原生构建：CMake、Visual Studio 2022、Qt 6（通过 vcpkg 提供）

## 交付形式

OpenZen 支持两种构建产物：

1. Java Agent：`build/libs/hey-1.0.jar`
2. Windows 原生加载器：`build/dist/OpenZenLoader.exe`

Java Agent 产物不能作为普通 Forge Mod 使用。不要把它复制到 `.minecraft/mods/`；它应当作为 JVM 启动参数传入。

## 构建

### Java Agent

准备 JDK 17，并将 `JAVA_HOME` 指向 JDK 安装目录。仓库自带 Gradle Wrapper，不需要另行安装 Gradle。

```powershell
.\gradlew.bat jar
```

构建完成后，在 Forge 启动器的 JVM 参数中加入：

```text
-javaagent:"完整\路径\到\hey-1.0.jar" -Djdk.attach.allowAttachSelf=true
```

`-Djdk.attach.allowAttachSelf=true` 用于在需要时允许 Agent 通过 Attach API 重新转换已加载的类。

### Windows 原生加载器

除 JDK 17 外，还需要：

- Visual Studio 2022，并安装“使用 C++ 的桌面开发”和 CMake 工具；
- CMake（Visual Studio 自带版本或 PATH 中的独立安装）；
- vcpkg，并设置 `VCPKG_ROOT`，或安装到 `C:\vcpkg`、`D:\vcpkg` 或 `%USERPROFILE%\vcpkg`。

一次性安装 vcpkg：

```powershell
git clone https://github.com/microsoft/vcpkg.git C:\vcpkg
C:\vcpkg\bootstrap-vcpkg.bat
```

构建单文件加载器：

```powershell
.\gradlew.bat dll
```

产物为 `build/dist/OpenZenLoader.exe`。如果已安装 UPX，可以执行以下命令压缩产物；未安装 UPX 时任务会跳过压缩，不影响构建：

```powershell
.\gradlew.bat upxCompress
```

清理 Java 和原生构建目录：

```powershell
.\gradlew.bat clean
```

首次构建会从 Forge Maven 和 vcpkg 下载依赖，耗时取决于网络和本机性能。

## 构建时类名重命名

在 `reobf` 完成后，构建脚本会使用 ASM 对项目自有类进行重命名：

- 项目自有 Java 包下的类会生成随机的不透明名称；
- 每次构建生成的名称都不同；
- 方法名和字段名保持不变，以避免破坏反射、JNI 和序列化配置；
- 原名称到新名称的映射写入 `build/rename-mapping.txt`。

请将 `rename-mapping.txt` 与对应的 jar 或 EXE 一起保存。它是阅读构建产物堆栈信息和定位问题的必要资料。GitHub Actions 产物也会单独上传这份映射文件。

## 使用原生加载器

1. 使用 Forge 启动器正常启动 Minecraft 1.20.1。
2. 运行 `OpenZenLoader.exe`。
3. 在列表中选择目标 Java 进程。
4. 仅在确认进程和构建来源无误后执行注入。

原生端日志默认写入 `%TEMP%\openzen.log`，Java 端日志位于 Minecraft 的 `logs/latest.log`。

## 项目结构

```text
src/main/java/       Java 客户端、模块、GUI、Agent 和 ASM 代码
src/main/resources/  Forge 元数据、资源和映射文件
native/               CMake 原生 DLL、加载器和 Qt 配置
mapping/              原始 Jar 与映射文件
gradle/               Gradle Wrapper 文件
.github/workflows/    GitHub Actions 构建流程
```

原始文件名按仓库现状保留：

- [原始 Jar](mapping/zen-orignial.jar)
- [Mapping](mapping/zen.mapping)

## 状态

这是一个持续整理中的反混淆工程。当前代码可以通过 Gradle 构建，但部分行为、命名和兼容性仍需要在目标环境中验证。提交问题时，请附上：

- 操作系统、JDK 和 Forge 版本；
- 使用的构建命令；
- 对应的 `build/rename-mapping.txt`；
- 相关日志和最小复现步骤。

## 许可与第三方项目

仓库当前没有声明统一的开源许可证。原始客户端、原始 Jar、内置资源及第三方依赖的权利归各自权利人所有；在复制、修改或再分发前，请自行确认授权范围。

构建和分析过程使用了以下项目：

- [Minecraft Forge](https://files.minecraftforge.net/)
- [ASM](https://asm.ow2.io/)
- [Java Deobfuscator](https://github.com/java-deobfuscator/deobfuscator)
- [Enigma MCP](https://github.com/Margele/Enigma-MCP)
- [CMake](https://cmake.org/)
- [vcpkg](https://github.com/microsoft/vcpkg)

欢迎通过 Issue 或 Pull Request 提交可复现的构建问题、修复和文档改进。请不要在 Issue 中发布他人的私人信息、攻击性内容或未经授权的文件。
