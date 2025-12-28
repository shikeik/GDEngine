find . -name "*.java" -type f ! -path "*/build/*" -exec sh -c '
    echo "文件: $1"
    cat -- "$1"
    echo ""
' sh {} \; > ProjectCode.txt