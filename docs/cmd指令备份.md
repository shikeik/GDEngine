解决>重定向到文件时乱码
```
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; git log --pretty=format:"%h - %an, %ar : %s" -n 50 | Out-File -Encoding utf8 logs/git_log.txt
```