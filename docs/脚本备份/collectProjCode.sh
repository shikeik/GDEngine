#!/bin/sh

# ============================
# 1. 设置要包含的文件后缀 (使用正则表达式)
# ============================
# 写法说明：
#   \.java$  表示以 .java 结尾
#   |        表示 "或者"
#   .*       表示所有文件
#
# 例子 A: 只想要 java 和 xml 和 txt
# INCLUDE_PATTERN="\.java$|\.xml$|\.txt$"
#
# 例子 B: 想要所有文件 (慎用！会包含图片和二进制文件)
# INCLUDE_PATTERN=".*"

INCLUDE_PATTERN="\.java$"

# ============================
# 2. 设置排除的目录关键词 (使用正则表达式)
# ============================
# 这里的配置和之前一样：
#   /build/  表示排除包含 /build/ 的路径
EXCLUDE_PATTERN="/GDEngine/GDEngine/|/build/|/bin/|/\.git/|/target/|/\.idea/|/out/|/assets/"

# ============================
# 3. 输出文件名
# ============================
OUTPUT_FILE="ProjectCode.txt"

# ============================
# 4. 执行逻辑
# ============================
echo "正在搜索..."
echo "包含规则: $INCLUDE_PATTERN"
echo "排除规则: $EXCLUDE_PATTERN"

# 逻辑解释：
# 1. find . -type f       -> 找到当前目录下所有的文件
# 2. grep -E "$INCLUDE.." -> 筛选出符合后缀的文件
# 3. grep -vE "$EXCLUDE.."-> 剔除掉在排除目录里的文件
# 4. while read ...       -> 循环输出内容

find . -type f | grep -E "$INCLUDE_PATTERN" | grep -vE "$EXCLUDE_PATTERN" | while read -r file; do
    echo "文件: $file"
    # 注意：如果 INCLUDE_PATTERN 设置为 .*，这里可能会打印出乱码的二进制文件(如图片)
    # 如果系统支持，可以加 -I 参数忽略二进制文件： grep -I "" "$file" 2>/dev/null
    cat -- "$file"
    echo ""
done > "$OUTPUT_FILE"

echo "完成！已保存至 $OUTPUT_FILE"