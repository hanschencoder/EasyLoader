# EasyLoader是什么
一个Android上使用的图片异步加载框架，可快速加载图片。这个库参考了Picasso的设计，API使用和Picasso很相似，开发者可以快速地切换过来。运行效果如下：

![demo1](https://github.com/shensky711/EasyLoader/blob/master/picture/example1.jpg)

![demo2](https://github.com/shensky711/EasyLoader/blob/master/picture/example2.jpg)

# EasyLoader可以干什么

 - 异步加载
 - 加载网络图片
 - 加载asset目录资源
 - 加载联系人头像
 - 加载本地图片文件
 - 设定内存缓存大小
 - 设置硬盘缓存大小
 - 设置硬盘缓存路径
 - 设置全局监听器
 - FIFO/LIFO模式切换
 - 自定义加载器

# 如何添加EasyLoader依赖
通过Gradle构建：
```
compile 'site.hanschen:easyLoader:1.0.0'
```

通过Maven构建：
```
<dependency>
  <groupId>site.hanschen</groupId>
  <artifactId>easyLoader</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

通过Ivy构建：
```
<dependency org='site.hanschen' name='easyLoader' rev='1.0.0'>
  <artifact name='$AID' ext='pom'></artifact>
</dependency>
```