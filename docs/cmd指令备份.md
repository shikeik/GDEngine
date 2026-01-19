解决>重定向到文件时乱码
```
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; git log --pretty=format:"%h - %an, %ar : %s" -n 50 | Out-File -Encoding utf8 logs/git_log.txt
```

高度密集显示logs
```
git log --graph --oneline --decorate --all
```

删除tag包括远程
```
git tag -d 0.1.0
git push origin --delete 0.1.0
```
紧接上面: 同步tag信息，
将云端tag信息同步到本地，
否则推送tags将再次推送已删除tag！！
```
git fetch origin --prune --prune-tags
```