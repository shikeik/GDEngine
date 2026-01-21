#!/bin/bash

# ================= 配置区域 =================
# 目标文件后缀
FILE_PATTERN="*.java"

# 被替换的内容 (支持正则，点号 . 建议转义为 \.)
SEARCH_STR="com\.goldsprite\.solofight\.ui\.widget\.ToastUI"

# 替换后的内容 (普通字符串，不需要转义)
REPLACE_STR="com.goldsprite.gdengine.ui.widget.ToastUI"
# ===========================================

# 查找当前目录下所有的指定文件
find . -name "$FILE_PATTERN" -type f | while read file; do
    echo "正在处理: $file"

    # 1. 【普通替换】
    # s/查找/替换/g
    # g 代表 global (全局替换)，即一行如果有多个也能全部替换
    sed -i "s/$SEARCH_STR/$REPLACE_STR/g" "$file"

done

echo "✅ 所有 $FILE_PATTERN 文件内容替换已处理完毕！"