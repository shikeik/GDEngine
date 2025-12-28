@echo off
REM 设置控制台编码为UTF-8
chcp 65001 >nul

(
REM 遍历所有Java文件，但排除build目录
for /r %%f in (*.java) do (
    REM 检查文件路径是否包含\build\
    echo %%f | findstr /i "\\build\\" >nul
    if errorlevel 1 (
        echo 文件: %%f
        type "%%f"
        echo.
    )
)
) > ProjectCode.txt

echo 代码已导出到 ProjectCode.txt

pause