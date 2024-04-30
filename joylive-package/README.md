# joylive-package

## How to use

1、首先需要对根目录joylive-agent进行maven install(命令: mvn install)

2、接着使用joylive-package 进行maven package(命令: mvn package)

3、安装完成后会在target目录下面出现live-版本信息.tar.gz的包
如图所示：![img.png](pic%2Fimg.png)

## How to debug

1、将live.jar所在的位置复制出来，例如/Users/zhongzunfa/xxxx/xxxx/joylive-package/target/live.jar

2、在同目录下的要调试的项目启动类vm参数加上-agent的执行，例如：
-javaagent:/Users/zhongzunfa/xxx/joylive-agent-debug/joylive-agent/joylive-package/target/live-1.0.0-SNAPSHOT/live.jar
具体如图所示：
![img_1.png](pic%2Fimg_1.png)

