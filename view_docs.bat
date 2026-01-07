@echo off
setlocal

REM ==========================================
REM GDEngine 文档启动脚本 (修复版)
REM ==========================================

echo [GDEngine] Checking environment...

REM 1. 检查 java 命令
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [Error] 'java' command not found! 
    echo Please make sure JDK/JRE is installed and added to PATH.
    pause
    exit /b 1
)

REM 2. 检查 javac 命令 (编译需要 JDK)
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo [Error] 'javac' command not found!
    echo You might be running with JRE only. JDK is required to compile the server tool.
    echo.
    echo Attempting to find JAVA_HOME...
    if defined JAVA_HOME (
        echo Found JAVA_HOME: %JAVA_HOME%
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    ) else (
        echo JAVA_HOME is not set.
    )
    
    REM 再次尝试检查
    where javac >nul 2>nul
    if %errorlevel% neq 0 (
        echo [Fatal] Still cannot find 'javac'. Please install JDK.
        pause
        exit /b 1
    )
)

REM 3. 准备编译
if not exist tools\bin mkdir tools\bin

echo [GDEngine] Compiling documentation server...
javac -d tools\bin tools\SimpleDocServer.java
if %errorlevel% neq 0 (
    echo [Error] Compilation failed!
    pause
    exit /b 1
)

echo [GDEngine] Server compiled successfully.
echo [GDEngine] Starting server at http://localhost:8899/ ...

REM 4. 启动浏览器 (异步)
start http://localhost:8899/

REM 5. 启动服务器 (阻塞运行)
java -cp tools\bin tools.SimpleDocServer

REM 如果服务器异常退出，暂停显示错误
if %errorlevel% neq 0 (
    echo [Error] Server crashed with exit code %errorlevel%
    pause
)

pause
