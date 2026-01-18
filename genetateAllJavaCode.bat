@echo off
setlocal enabledelayedexpansion
REM ================= 配置区域 =================

REM 1. 设置目标文件后缀 (例如: *.java, *.txt, *.*)
REM 如果想要所有文件，请设置为 *.*
set "TARGET_EXT=*.java"

REM 2. 设置排除的目录或文件名关键词 (使用空格分隔)
REM 修改点：
REM 1. 添加了 \assets\
REM 2. 将 \GDEngine\ 修改为 \GDEngine\GDEngine\ (如果是为了排除子目录)
REM    或者如果不改名，必须小心根目录重名问题。
REM 3. 删除了整个字符串末尾的反斜杠，或者在末尾加个空格，防止转义最后一个引号。

set "EXCLUDE_PATTERN=\GDEngine\GDEngine\ \build\ \bin\ \.git\ \target\ \.idea\ \out\ \assets\ \ig这个会被忽略是个bug\"

REM 3. 输出文件名
set "OUTPUT_FILE=ProjectCode.txt"

REM ===========================================

REM 设置控制台编码为UTF-8，防止中文乱码
chcp 65001 >nul

REM 获取当前脚本的文件名（带后缀）
set "CURRENT_SCRIPT=%~nx0"

echo 正在处理...
echo 目标后缀: %TARGET_EXT%
echo 排除模式: %EXCLUDE_PATTERN%
echo 忽略脚本: %CURRENT_SCRIPT%
echo.

(
    REM 遍历指定后缀的文件 (/r 表示递归子目录)
    for /r %%f in (%TARGET_EXT%) do (

        REM 初始化标记：0=处理，1=跳过
        set "SKIP=0"

        REM --- 检查 1: 是否为脚本本身 ---
        if /i "%%~nxf"=="%CURRENT_SCRIPT%" set "SKIP=1"

        REM --- 检查 2: 是否为输出文件 (防止死循环) ---
        if /i "%%~nxf"=="%OUTPUT_FILE%" set "SKIP=1"

        REM --- 检查 3: 是否包含排除的关键词 ---
        if "!SKIP!"=="0" (
            REM 使用 findstr 查找路径中是否包含排除关键词
            REM /i 忽略大小写, /c 逐字搜索(这里改用默认空格分割搜索多个词)
            echo "%%f" | findstr /i "%EXCLUDE_PATTERN%" >nul
            REM 如果 errorlevel 为 0，说明找到了排除关键词，需要跳过
            if not errorlevel 1 set "SKIP=1"
        )

        REM --- 执行输出 ---
        if "!SKIP!"=="0" (
            echo 文件: %%f
            echo ------------------------------------------------------------------------
            type "%%f"
            echo.
            echo.
        )
    )
) > "%OUTPUT_FILE%"

echo.
echo ==========================================
echo 处理完成!
echo 代码已导出到: %OUTPUT_FILE%
echo ==========================================

pause
