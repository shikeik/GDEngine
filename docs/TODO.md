ignored: indentFix codeFormatFix

# TODO   

PC libs为0个, 没有打包libs到运行程序中?应该是gradle任务的问题
Editor的New Script上下文菜单还在用硬编码脚本模板, 这里需要和创建项目的统一下逻辑, 新脚本文件模板也改为从assets/script_project_templates/NewScript.java获取的方式
脚本项目结构的规范化进阶
现在世界相机的视口大小和ui视口缩放绑定的, 按理应该基于基准视口大小而不是和ui视口耦合, 有空改

限制变换组件为等比缩放

现在编辑器只是渲染，而没有跑真的gameworld逻辑或者gameworld没有自己的逻辑
gameworld内部逻辑自己完成摇杆绘制与相机跟随和玩家移动
GScreen的输入多路复用要接上

LogErr得做了

BioCodeEditor更新: 现在行数变了不刷新布局高度导致内容被截掉




## bug修复与功能优化改善(不改善严重影响使用才算): 






# 增加内容: 







## 已完成: 








## 已搁置
做ui布局编辑器: 可调整控件和布局的对齐方式，调整布局位置...

