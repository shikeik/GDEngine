# 项目结构与配置

GDEngine 采用标准的 Java 项目结构，便于与 IntelliJ IDEA 等外部 IDE 兼容。

## 目录结构 (Standard Layout)

一个标准的 GDEngine 项目（如 `UserProjects/MyGame`）包含以下内容：

```text
MyGame/
├── project.json           # [核心] 项目元数据配置
├── project.index          # [自动生成] 类索引文件，用于热重载发现组件
├── src/
│   └── main/
│       └── java/          # Java 源代码存放处 (包名目录结构)
├── assets/                # 资源目录 (图片、音频、字体)
└── libs/                  # 依赖库 (引擎自动注入)
    ├── gdengine.jar       # 引擎核心 API
    ├── gdx.jar            # LibGDX 核心
    └── ...
```

## project.json 配置详解

这是项目的身份证明，引擎通过它识别入口点。

```json
{
  "name": "MyGame",
  "packageName": "com.mygame",     // Java 包名
  "mainClass": "Main",             // 入口类名 (不含包名，需实现 IGameScriptEntry)
  "engineVersion": "1.10.12.21"    // 依赖的引擎版本
}
```

## project.index 的作用

**这是 GDEngine 热重载机制的核心。**
由于 Android 环境下无法像 PC 那样扫描 Classpath，编译器 (`AndroidScriptCompiler` / `DesktopScriptCompiler`) 在每次编译成功后，会将所有用户类的全限定名写入此文件。

`ComponentRegistry` 会读取此文件来发现用户编写的新组件 (`Component`)。

> **注意：** 请勿手动修改 `project.index`，每次点击编辑器中的 **Build** 按钮都会自动重新生成它。