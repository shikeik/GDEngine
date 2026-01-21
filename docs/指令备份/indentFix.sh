# 查找当前目录下所有的 .java 文件
find . -name "*.java" -type f | while read file; do
    echo "正在处理: $file"

    # 1. 【循环】将行首所有的 4个空格 替换为 1个Tab
    # :a 是标签，t a 表示如果发生了替换，就跳回标签a继续匹配（处理多层缩进）
    sed -i ':a; s/^\(\t*\)    /\1\t/; t a' "$file"

    # 2. 【循环】将行首剩余的 3个空格 替换为 1个Tab
    # 这一步是为了处理比如 7个空格的情况 (4+3 -> Tab+Tab)
    sed -i ':a; s/^\(\t*\)   /\1\t/; t a' "$file"

    # 3. 【循环】将行首剩余的 2个空格 替换为 1个Tab
    # 这一步是为了处理比如 2个空格或 6个空格(4+2) 的情况
    sed -i ':a; s/^\(\t*\)  /\1\t/; t a' "$file"

done

echo "✅ 所有 Java 文件缩进已转换为 Tab！"