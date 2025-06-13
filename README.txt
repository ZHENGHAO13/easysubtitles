= EasySubtitles - Minecraft 字幕模组 =
版本: 1.0.0
作者: Zhenghao123
适用于: Minecraft 1.20.1, Forge 47.4.0+
许可证: MIT

== 介绍 ==
EasySubtitles 是一个为 Minecraft 1.20.1 设计的模组，它添加了一个简易的字幕系统：
1. 可以手动播放 SRT 格式的字幕文件
2. 自动检测特定格式的音频命令并播放对应的字幕
3. 高度可定制化的字幕渲染效果

== 特性 ==
● 自动字幕功能
  - 监听格式: /playsound easysubtitles:subtitles.sound.<字幕ID> ...
  - 对应字幕文件: config/easysubtitles/<字幕ID>.srt
  
● 手动字幕播放
  - 命令: /easysub <字幕ID>
  - 停止播放: /easysub stop
  - 调试信息: /easysub debug
  
● 可配置选项
  - 自动字幕开关
  - 字幕背景设置
  - 字体颜色和大小

== 安装 ==
1. 确保已安装 Minecraft Forge 47.4.0 或更高版本
2. 将 easysubtitles-1.0.0.jar 放入 mods 文件夹
3. 启动游戏

== 使用教程 ==
1. 创建字幕文件
  在 .minecraft/config/easysubtitles/ 目录下创建 SRT 格式的字幕文件
  示例 (ce1.srt):
  1
  00:00:00,000 --> 00:00:02,000
  欢迎使用 EasySubtitles 模组

  2
  00:00:02,500 --> 00:00:04,000
  请享受自动字幕功能！