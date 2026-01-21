@echo off
cd /d "%~dp0"

echo 正在扫描目录，请稍候...

rem 将结果输出到 ProjectTree.txt 文件中
tree /f > ProjectTree.txt

echo 扫描完成！正在打开文件...
start ProjectTree.txt