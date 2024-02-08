## 手动编译JDK8
学习JVM最关键的是研究底层C/C++源码 我们首先需要搭建一个测试环境 方便我们之后对底层源码进行调试 但是编译这一步的坑特别多
请务必保证跟教程中的环境一致 尤其是编译环境 版本不能太高 因为JDK8属于比较早期的版本了 否则会遇到各种各样奇奇怪怪的问题

### 环境配置
- 操作系统: Ubuntu 20.04 Server
- 硬件配置: i7-4790 4C8T / 16G内存 / 128G硬盘(不能用树莓派或是arm芯片Mac的虚拟机 配置越高越好 不然卡爆)
- 调试工具: Jetbrains Gateway(服务器运行CLion Backend程序 界面在Mac上显示)
- OpenJDK源码: https://codeload.github.com/openjdk/jdk/zip/refs/tags/jdk8-b120
- 编译环境: 
  - gcc-4.8
  - g++-4.8
  - make-3.81
  - openjdk-8

### 开始折腾
首选需要在我们的测试服务器上安装Ubuntu 20.04 Server系统 并通过ssh登录到服务器:

```shell
                        Welcome to Ubuntu 20.04.3 LTS (GNU/Linux 5.4.0-96-generic x86_64)

                         * Documentation:  https://help.ubuntu.com
                         * Management:     https://landscape.canonical.com
                         * Support:        https://ubuntu.com/advantage
                        
                          System information as of Sat 29 Jan 2022 10:33:03 AM UTC
                        
                          System load:  0.08               Processes:               156
                          Usage of /:   5.5% of 108.05GB   Users logged in:         0
                          Memory usage: 5%                 IPv4 address for enp2s0: 192.168.10.66
                          Swap usage:   0%                 IPv4 address for enp2s0: 192.168.10.75
                          Temperature:  32.0 C
                        
                        
                        37 updates can be applied immediately.
                        To see these additional updates run: apt list --upgradable
                        
                        
                        Last login: Sat Jan 29 10:27:06 2022
                        nagocoler@ubuntu-server:~$
```

先安装一些基本的依赖:

```shell
                        sudo apt install build-essential libxrender-dev xorg-dev libasound2-dev libcups2-dev gawk zip libxtst-dev libxi-dev libxt-dev gobjc
```

接着我们先将JDK的编译环境配置好 首先是安装gcc和g++的4.8版本 但是最新的源没有这个版本了 我们先导入旧版软件源:

```shell
                        sudo vim /etc/apt/sources.list
```

在最下方添加旧版源地址并保存:

```shell
                        deb http://archive.ubuntu.com/ubuntu xenial main
                        deb http://archive.ubuntu.com/ubuntu xenial universe
```

接着更新一下apt源信息 并安装gcc和g++:

```shell
                        sudo apt update
                        sudo apt install gcc-4.8 g++-4.8
```

接着配置:

```shell
                        sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.8 100
                        sudo update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-4.8 100
```

最后查看版本是否为4.8版本:

```shell
                        nagocoler@ubuntu-server:~$ gcc --version
                        gcc (Ubuntu 4.8.5-4ubuntu2) 4.8.5
                        Copyright (C) 2015 Free Software Foundation, Inc.
                        This is free software; see the source for copying conditions.  There is NO
                        warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
                        
                        nagocoler@ubuntu-server:~$ g++ --version
                        g++ (Ubuntu 4.8.5-4ubuntu2) 4.8.5
                        Copyright (C) 2015 Free Software Foundation, Inc.
                        This is free software; see the source for copying conditions.  There is NO
                        warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
```

接着安装make 3.81版本 需要从官方下载:

```shell
                        wget https://ftp.gnu.org/gnu/make/make-3.81.tar.gz
```

下载好之后进行解压 并进入目录:

```shell
                        tar -zxvf make-3.81.tar.gz 
                        cd make-3.81/
```

接着我们修改一下代码 打开`glob/glob.c`文件:

                        ...
                        #ifdef  HAVE_CONFIG_H
                        # include <config.h>
                        #endif
                        
                        #define __alloca alloca   <- 添加这一句
                        /* Enable GNU extensions
                        ...

接着进行配置并完成编译和安装:

```shell
                        bash configure
                        sudo make install
```

安装完成后 将make已经变成3.81版本了:

```shell
                        nagocoler@ubuntu-server:~/make-3.81$ make -verison
                        GNU Make 3.81
                        Copyright (C) 2006  Free Software Foundation, Inc.
                        This is free software; see the source for copying conditions.
                        There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
                        PARTICULAR PURPOSE.
```

由于JDK中某些代码是Java编写的 所以我们还需要安装一个启动JDK 启动JDK可以是当前版本或低一版本 比如我们要编译JDK8的源码
那么就可以使用JDK7, JDK8作为启动JDK 对源码中的一些java文件进行编译 这里我们选择安装OpenJDK8作为启动JDK:

```shell
                        sudo apt install openjdk-8-jdk
```

这样 我们的系统环境就准备完成了 接着我们需要下载OpenJDK8的源码 解压:

```shell
                        unzip jdk-jdk8-b120.zip
```

接着我们需要安装JetBrains Gateway在我们的服务器上导入项目 这里我们使用CLion后端 等待下载远程后端 这样我们的Linux服务器上虽然没有图形化界面 但是依然可以使用IDEA, CLion等工具
只是服务器上只有后端程序 而界面由我们电脑上的前端程序提供(目前此功能还在Beta阶段 暂不支持arm架构的Linux服务器) 整个过程根据服务器配置决定可能需要5-20分钟

完成之后 我们操作起来就很方便了 界面和IDEA其实差不多 我们打开终端 开始进行配置:

```shell
                        bash configure --with-debug-level=slowdebug --enable-debug-symbols ZIP_DEBUGINFO_FIELS=0
```

配置完成后 再次确认是否和教程中的配置信息一致:

                        Configuration summary:
                        * Debug level:    slowdebug
                        * JDK variant:    normal
                        * JVM variants:   server
                        * OpenJDK target: OS: linux, CPU architecture: x86, address length: 64
                        
                        Tools summary:
                        * Boot JDK:       openjdk version "1.8.0_312" OpenJDK Runtime Environment (build 1.8.0_312-8u312-b07-0ubuntu1~20.04-b07) OpenJDK 64-Bit Server VM (build 25.312-b07, mixed mode)  (at /usr/lib/jvm/java-8-openjdk-amd64)
                        * C Compiler:     gcc-4.8 (Ubuntu 4.8.5-4ubuntu2) version 4.8.5 (at /usr/bin/gcc-4.8)
                        * C++ Compiler:   g++-4.8 (Ubuntu 4.8.5-4ubuntu2) version 4.8.5 (at /usr/bin/g++-4.8)
                        
                        Build performance summary:
                        * Cores to use:   3
                        * Memory limit:   3824 MB
                        * ccache status:  not installed (consider installing)
                        
                        WARNING: The result of this configuration has overridden an older
                        configuration. You *should* run 'make clean' to make sure you get a
                        proper build. Failure to do so might result in strange build problems.

接着我们需要修改几个文件 不然一会会编译失败 首先是`hotspot/make/linux/Makefile`文件:

                        原有的 SUPPORTED_OS_VERSION = 2.4% 2.5% 2.6% 3%
                        修改为 SUPPORTED_OS_VERSION = 2.4% 2.5% 2.6% 3% 4% 5%

接着是`hotspot/make/linux/makefiles/gcc.make`文件:

                        原有的 WARNINGS_ARE_ERRORS = -Werror
                        修改为 #WARNINGS_ARE_ERRORS = -Werror

接着是`nashorn/make/BuildNashorn.gmk`文件:

                          $(CP) -R -p $(NASHORN_OUTPUTDIR)/nashorn_classes/* $(@D)/
                          $(FIXPATH) $(JAVA) \
                        原有的 -cp "$(NASHORN_OUTPUTDIR)/nasgen_classes$(PATH_SEP)$(NASHORN_OUTPUTDIR)/nashorn_classes" \
                        修改为  -Xbootclasspath/p:"$(NASHORN_OUTPUTDIR)/nasgen_classes$(PATH_SEP)$(NASHORN_OUTPUTDIR)/nashorn_classes" \
                          jdk.nashorn.internal.tools.nasgen.Main $(@D) jdk.nashorn.internal.objects $(@D)

OK 修改完成 接着我们就可以开始编译了:

                        make all

整个编译过程大概需要持续10-20分钟 请耐心等待 构建完成后提示:

                        ----- Build times -------
                        Start 2022-01-29 11:36:35
                        End   2022-01-29 11:48:20
                        00:00:30 corba
                        00:00:25 demos
                        00:02:39 docs
                        00:03:05 hotspot
                        00:00:27 images
                        00:00:17 jaxp
                        00:00:31 jaxws
                        00:03:02 jdk
                        00:00:38 langtools
                        00:00:11 nashorn
                        00:11:45 TOTAL
                        -------------------------
                        Finished building OpenJDK for target 'all'

只要按照我的操作一步步走 别漏了 应该是直接可以完成的 当然难免可能有的同学出现了奇奇怪怪的问题 加油 慢慢折腾 总会成功的~

接着我们就可以创建一个测试配置了 首先打开设置页面 找到`自定义构建目标`:

<img src="https://image.itbaima.cn/markdown/2023/03/06/TAcqg1Sx3KwOQZz.png"/>

点击`应用`即可 接着打开运行配置 添加一个新的自定义配置:

<img src="https://image.itbaima.cn/markdown/2023/03/06/FbEYsV1zvIf9TWl.png"/>

选择我们编译完成的java程序 然后测试-version查看版本信息 去掉下方的构建

接着直接运行即可:

                        /home/nagocoler/jdk-jdk8-b120/build/linux-x86_64-normal-server-slowdebug/jdk/bin/java -version
                        openjdk version "1.8.0-internal-debug"
                        OpenJDK Runtime Environment (build 1.8.0-internal-debug-nagocoler_2022_01_29_11_36-b00)
                        OpenJDK 64-Bit Server VM (build 25.0-b62-debug, mixed mode)
                        
                        Process finished with exit code 0

我们可以将工作目录修改到其他地方 接着我们创建一个Java文件并完成编译 然后测试能否使用我们编译的JDK运行:

<img src="https://image.itbaima.cn/markdown/2023/03/06/YZcxklCK7hvnapV.png"/>

在此目录下编写一个Java程序 然后编译:

```java
                          public class Main {
                                  public static void main(String[] args){
                                          System.out.println("Hello World!");
                                  }       
                          }
```

```shell
                          nagocoler@ubuntu-server:~$ cd JavaHelloWorld/
                          nagocoler@ubuntu-server:~/JavaHelloWorld$ vim Main.java
                          nagocoler@ubuntu-server:~/JavaHelloWorld$ javac Main.java 
                          nagocoler@ubuntu-server:~/JavaHelloWorld$ ls
                          Main.class  Main.java
```

点击运行 成功得到结果:

                          /home/nagocoler/jdk-jdk8-b120/build/linux-x86_64-normal-server-slowdebug/jdk/bin/java Main
                          Hello World!
                          
                          Process finished with exit code 0

我们还可以在CLion前端页面中进行断点调试 比如我们测试一个入口点JavaMain 在`jdk/src/share/bin/java.c`中的JavaMain方法:

<img src="https://image.itbaima.cn/markdown/2023/03/06/AcdjJWy8QnxlTa4.png"/>

点击右上角调试按钮 可以成功进行调试:

<img src="https://image.itbaima.cn/markdown/2023/03/06/tZzqg2GD3LSbn9o.png"/>

至此 在Ubuntu系统上手动编译OpenJDK8完成