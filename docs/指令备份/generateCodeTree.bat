@echo off
setlocal enabledelayedexpansion
REM 设置编码为UTF-8，确保树状图符号显示正常
chcp 65001 >nul

REM ================= 配置区域 =================

REM 1. 输出文件名
set "OUTPUT_FILE=ProjectTree.txt"

REM 2. 要排除的关键词 (空格分隔)
REM 原理：只要文件名或文件夹名包含这些词，就会被排除
REM 建议：排除编译目录、版本控制、IDE配置、二进制文件后缀
set "EXCLUDE_KEYWORDS=target build .git .idea out node_modules .class .jar .exe"

REM ===========================================

REM 获取脚本自身的文件名，防止把自己算进去
set "SCRIPT_NAME=%~nx0"

echo 正在生成目录结构...
echo 排除关键词: %EXCLUDE_KEYWORDS%
echo 输出文件: %OUTPUT_FILE%
echo.
echo 正在扫描，文件多时可能需要几秒钟，请稍候...

(
    echo 项目目录结构: %CD%
    echo.
    REM 从当前目录 "." 开始递归，初始缩进为空
    call :TraverseDir "." ""
) > "%OUTPUT_FILE%"

echo.
echo ==========================================
echo 生成完成!
echo 代码已导出到: %OUTPUT_FILE%
echo ==========================================
pause
goto :eof


REM ==============================================================================
REM  递归遍历子程序 (函数)
REM  参数1: 当前路径
REM  参数2: 当前行的缩进前缀
REM ==============================================================================
:TraverseDir
setlocal enabledelayedexpansion
set "currentPath=%~1"
set "indent=%~2"

REM 定义临时数组计数器
set "idx=0"

REM 第一步：收集当前目录下所有需要显示的项，存入数组
REM dir /b 仅列出名称 (包括文件和文件夹)
REM 这里的逻辑稍微复杂是为了确定哪一个是"最后一个"，以便绘制 └── 
for /f "delims=" %%i in ('dir /b "%currentPath%"') do (
    set "itemName=%%i"
    set "skip=0"

    REM --- 排除逻辑开始 ---
    
    REM 1. 排除脚本自身
    if /i "!itemName!"=="%SCRIPT_NAME%" set "skip=1"
    
    REM 2. 排除输出文件
    if /i "!itemName!"=="%OUTPUT_FILE%" set "skip=1"

    REM 3. 排除配置的关键词
    REM 遍历排除列表，检查文件名是否包含关键词
    if "!skip!"=="0" (
        for %%k in (%EXCLUDE_KEYWORDS%) do (
            echo "!itemName!" | findstr /i /c:"%%k" >nul
            if not errorlevel 1 set "skip=1"
        )
    )
    REM --- 排除逻辑结束 ---

    REM 如果未被排除，则加入待处理列表
    if "!skip!"=="0" (
        set /a idx+=1
        set "item[!idx!]=!itemName!"
    )
)

REM 保存总数量
set "totalItems=!idx!"

REM 如果该目录下没有有效文件，直接退出该层递归
if !totalItems! equ 0 (
    endlocal
    exit /b
)

REM 第二步：遍历数组，输出树状图
for /l %%n in (1, 1, !totalItems!) do (
    set "thisItem=!item[%%n]!"
    set "fullPath=!currentPath!\!thisItem!"
    
    REM 判断是否为当前层级的最后一个元素
    if %%n equ !totalItems! (
        set "isLast=1"
        set "prefix=└── "
        set "subIndent=!indent!    "
    ) else (
        set "isLast=0"
        set "prefix=├── "
        set "subIndent=!indent!│   "
    )

    REM 输出当前行 (缩进 + 前缀 + 名称)
    echo !indent!!prefix!!thisItem!

    REM 判断是否为文件夹，如果是则递归
    REM 使用 exist "path\*" 是判断文件夹的经典技巧
    if exist "!fullPath!\" (
        call :TraverseDir "!fullPath!" "!subIndent!"
    )
)

endlocal
exit /b