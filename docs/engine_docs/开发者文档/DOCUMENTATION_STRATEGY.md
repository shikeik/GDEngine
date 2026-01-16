# GDEngine 文档化开发规范 (Documentation Strategy)

## 1. 核心宗旨 (Core Philosophy)
打造一套 **"API 契约 + 开发者手册"** 的双层文档体系，旨在降低新开发者的上手难度，同时保持代码库的整洁与专业。

**核心目标**：
1.  **IDE 友好**：编码时通过悬浮提示获取参数、返回值与核心用法。
2.  **教程详尽**：提供独立的文档站，包含架构图、原理讲解与 Step-by-Step 教程。
3.  **脚本优先**：为脚本开发者（Script Developers）提供类似 Unity 文档的舒适体验。

## 2. 文档架构 (Architecture)

我们采用 **"混合双链策略" (Hybrid Strategy)**：

### Layer 1: API Reference (源码 Javadoc)
*   **位置**：`.java` 源文件。
*   **内容**：
    *   **What**: 类/方法的功能摘要。
    *   **Contract**: 参数范围、Nullability、抛出异常、返回值含义。
    *   **Link**: 使用 `@see` 或文本指引指向详细的 Markdown 教程。
*   **规范**：
    *   保持克制，避免长篇大论。
    *   关键类必须包含简短的 `Usage Example`。

### Layer 2: Developer Manual (指南文档)
*   **位置**：`docs/manual/` 目录。
*   **格式**：Markdown (`.md`)。
*   **内容**：
    *   **Why**: 设计原理与架构图。
    *   **Guide**: 完整功能的实现教程。
    *   **Best Practice**: 最佳实践与常见坑点。

### Layer 3: Visualization (展示层)
*   **当前方案**：基于 Docsify 的静态文档站。
*   **特点**：无需构建，无需后端，双击 `index.html` (或配合极简脚本) 即可查看。
*   **未来规划**：开发引擎内置的文档浏览器，直接在游戏内渲染这些 Markdown。

## 3. 目录结构 (Directory Structure)

```text
GDEngine/
├── core/src/.../gdengine/  (源码 + Javadoc)
└── docs/
    └── manual/
        ├── index.html      (文档站入口)
        ├── README.md       (文档首页)
        ├── _sidebar.md     (侧边栏导航)
        ├── core/           (核心模块: ECS, FSM, Context)
        ├── graphics/       (渲染: NeonBatch, Effects)
        ├── scripting/      (脚本: API, Lua/Java)
        └── tutorials/      (新手教程)
```

## 4. 工作流 (Workflow)

1.  **编写 Markdown**: 在 `docs/manual` 下创建对应模块的解释文档。
2.  **编写 Javadoc**: 在源码中添加注释，并链接到上述 Markdown。
3.  **验证**:
    *   IDE 检查：悬浮提示是否清晰。
    *   浏览器检查：打开 `docs/manual/index.html` 确认文档站渲染正常。

## 5. 试验阶段 (Pilot Phase)
*   **范围**: 引擎核心上下文 (`Gd.java`) 与 基础实体 (`GObject.java`)。
*   **目的**: 验证双链结构的可行性与文档站的浏览体验。
