## JNI调用本地方法
Java还有一个JNI机制 它的全称: Java Native Interface 即Java本地接口 它允许在Java虚拟机内运行的Java代码与其它编程语言(如C/C++和汇编语言) 
编写的程序和库进行交互(在Android开发得比较多) 比如我们现在想要让C语言程序帮助我们的Java程序实现a+b的运算 首先我们需要创建一个本地方法:

```java
                        public class Main {
                        
                            public static void main(String[] args) {
                                System.out.println(sum(1, 2));
                            }
                            
                            public static native int sum(int a, int b);
                            
                        }
```

创建好后 接着点击构建按钮 会出现一个out文件夹 也就是生成的class文件在其中 接着我们直接生成对应的C头文件:

```shell
                        javah -classpath out/production/SimpleHelloWorld -d ./jni com.test.Main
```

生成的头文件位于jni文件夹下:

```c
                        /* DO NOT EDIT THIS FILE - it is machine generated */
                        #include <jni.h>
                        /* Header for class com_test_Main */
                        
                        #ifndef _Included_com_test_Main
                        #define _Included_com_test_Main
                        #ifdef __cplusplus
                        extern "C" {
                        #endif
                        /*
                         * Class:     com_test_Main
                         * Method:    sum
                         * Signature: (II)V
                         */
                        JNIEXPORT void JNICALL Java_com_test_Main_sum
                          (JNIEnv *, jclass, jint, jint);
                        
                        #ifdef __cplusplus
                        }
                        #endif
                        #endif
```

接着我们在CLion中新建一个C++项目 并引入刚刚生成的头文件 并导入jni相关头文件(在JDK文件夹中) 首先修改CMake文件:

```
                        cmake_minimum_required(VERSION 3.21)
                        project(JNITest)
                        
                        include_directories(/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/include)
                        include_directories(/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/include/darwin)
                        set(CMAKE_CXX_STANDARD 14)
                        
                        add_executable(JNITest com_test_Main.cpp com_test_Main.h)
```

接着就可以编写实现了 首先认识一下引用类型对照表:

<img src="https://image.itbaima.cn/markdown/2023/03/06/QS9FxGhdsMRBCcm.png"/>

所以我们这里直接返回a+b即可:

```cpp
                        #include "com_test_Main.h"

                        JNIEXPORT jint JNICALL Java_com_test_Main_sum
                                (JNIEnv * env, jclass clazz, jint a, jint b){
                            return a + b;
                        }
```

接着我们就可以将cpp编译为动态链接库 在MacOS下会生成`.dylib`文件 Windows下会生成`.dll`文件 我们这里就只以MacOS为例 命令有点长 因为还需要包含JDK目录下的头文件:

```shell
                        gcc com_test_Main.cpp -I /Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/include -I /Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/include/darwin -fPIC -shared -o test.dylib -lstdc++
```

编译完成后 得到`test.dylib`文件 这就是动态链接库了

最后我们再将其放到桌面 然后在Java程序中加载:

```java
                        public class Main {
    
                            static {
                                System.load("/Users/nagocoler/Desktop/test.dylib");
                            }
                        
                            public static void main(String[] args) {
                                System.out.println(sum(1, 2));
                            }
                        
                            public static native int sum(int a, int b);
                            
                        }
```

运行 成功得到结果:

<img src="https://image.itbaima.cn/markdown/2023/03/06/AdKbxHjlGwDfUY2.png"/>

通过了解JVM的一些基础知识 我们心目中大致有了一个JVM的模型 接下来 我们将继续深入学习JVM的内存管理机制和垃圾收集器机制 以及一些实用工具