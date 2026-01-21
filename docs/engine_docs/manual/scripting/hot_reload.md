# 编译与热重载原理(未审查)

GDEngine 的核心特性是在运行时编译 Java 代码并热加载。

## 跨平台编译器实现

GDEngine 包含一个抽象接口 `IScriptCompiler`，在不同平台有不同实现：

### 1. PC (DesktopScriptCompiler)
*   **编译核心**: 使用 **ECJ** (Eclipse Compiler for Java) 库。
*   **依赖处理**: 利用 `System.getProperty("java.class.path")` 获取当前运行时的所有 Jar 包作为编译依赖。
*   **加载**: 编译生成 `.class` 文件到 `build/script_cache/`，然后使用 `URLClassLoader` 动态加载。

### 2. Android (AndroidScriptCompiler)
Android 无法直接运行 `.class` 文件，因此流程更复杂：
1.  **编译**: 使用 ECJ 将 `.java` 编译为 `.class`。
2.  **转换**: 使用 Android 工具链中的 **D8** (R8) 将 `.class` 转换为 `.dex` (Dalvik Executable)。
3.  **加载**:
    *   **Android 8.0+**: 直接从内存字节数组加载 (`InMemoryDexClassLoader`)，速度极快。
    *   **旧版本**: 写入临时文件后使用 `DexClassLoader` 加载。

## 热重载限制

虽然热重载很强大，但 Java 虚拟机的机制决定了它有以下限制：

1.  **静态变量**: 重新编译后，旧的类被丢弃，新的类被加载。静态变量会重置为初始值。
2.  **匿名内部类**: 大量修改代码可能导致匿名内部类编号变化 ($1, $2)，导致序列化数据无法正确对应（GDEngine 的 JSON 序列化器已针对此做了部分防护，跳过匿名类序列化）。
3.  **对象引用**: 如果你在 `GameWorld` 中持有了一个旧类的对象引用，热重载后这个对象依然是旧类的实例。只有新创建的对象才是新类。
    *   *解决方案*: 编辑器在 Build 成功后，通常会触发场景刷新或建议重启游戏逻辑。