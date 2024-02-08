## JVM内存管理
在之前 我们了解了JVM的大致运作原理以及相关特性 这一章 我们首先会从内存管理说起

在传统的C/C++开发中 我们经常通过使用申请内存的方式来创建对象或是存放某些数据 但是这样也带来了一些额外的问题
我们要在何时释放这些内存 怎么才能使得内存的使用高效 因此 内存管理是一个非常严肃的问题

比如我们就可以通过C语言动态申请内存 并用于存放数据:

```c
                        #include <stdlib.h>
                        #include <stdio.h>
                        
                        int main() {
                        
                            // 动态申请4个int大小的内存空间
                            int* memory = malloc(sizeof(int) * 4);
                            // 修改第一个int空间的值
                            memory[0] = 10;
                            //修改第二个int空间的值
                            memory[1] = 2;
                            // 遍历内存区域中所有的值
                            for (int i = 0;i < 4;i++){
                                printf("%d ", memory[i]);
                            }
                            // 释放指针所指向的内存区域
                            free(memory);
                            // 最后将指针赋值为NULL
                            memory = NULL;
                            
                        }
```

而在Java中 这种操作实际上是不允许的 Java只支持直接使用基本数据类型和对象类型 至于内存到底如何分配 并不是由我们来处理 而是JVM帮助我们进行控制 
这样就帮助我们节省很多内存上的工作 虽然带来了很大的便利 但是 一旦出现内存问题 我们就无法像C/C++那样对所管理的内存进行合理地处理
因为所有的内存操作都是由JVM在进行 只有了解了JVM的内存管理机制 我们才能够在出现内存相关问题时找到解决方案

### 内存区域划分
既然要管理内存 那么肯定不会是杂乱无章的 JVM对内存的管理采用的是分区治理 不同的内存区域有着各自的职责所在 在虚拟机运行时 内存区域如下划分:

<img src="https://image.itbaima.cn/markdown/2023/03/06/CP4yv1iqrfjmXzW.jpg"/>

我们可以看到 内存区域一共分为5个区域 其中方法区和堆是所有线程共享的区域 随着虚拟机的创建而创建 虚拟机的结束而销毁 而虚拟机栈, 本地方法栈, 程序计数器都是线程之间相互隔离的
每个线程都有一个自己的区域 并且线程启动时会自动创建 结束之后会自动销毁 内存划分完成之后 我们的JVM执行引擎和本地库接口 也就是Java程序开始运行之后就会根据分区合理地使用对应区域的内存了

### 大致划分

#### 程序计数器
首先我们来介绍一下程序计数器 它和我们的传统8085 CPU中PC寄存器的工作差不多 因为JVM虚拟机目的就是实现物理机那样的程序执行 在8086 CPU中 PC作为程序计数器 负责储存内存地址
该地址指向下一条即将执行的指令 每解释执行完一条指令 PC寄存器的值就会自动被更新为下一条指令的地址 进入下一个指令周期时 就会根据当前地址所指向的指令 进行执行

而JVM中的程序计数器可以看做是当前线程所执行字节码的行号指示器 而行号正好就指的是某一条指令 字节码解释器在工作时也会改变这个值 来指定下一条即将执行的指令

因为Java的多线程也是依靠时间片轮转算法进行的 因此一个CPU同一时间也只会处理一个线程 当某个线程的时间片消耗完成后 会自动切换到下一个线程继续执行
而当前线程的执行位置会被保存到当前线程的程序计数器中 当下次轮转到此线程时 又继续根据之前的执行位置继续向下执行

程序计数器因为只需要记录很少的信息 所以只占用很少一部分内存

#### 虚拟机栈
虚拟机栈就是一个非常关键的部分 看名字就知道它是一个栈结构 每个方法被执行的时候 Java虚拟机都会同步创建一个栈帧(其实就是栈里面的一个元素) 栈帧中包括了当前方法的一些信息 比如局部变量表, 操作数栈, 动态链接, 方法出口等

<img src="https://image.itbaima.cn/markdown/2023/03/06/As1NGy6BwhKJ9kY.png"/>

其中局部变量表就是我们方法中的局部变量 之前我们也进行过演示 实际上局部变量表在class文件中就已经定义好了 操作数栈就是我们之前字节码执行时使用到的栈结构
每个栈帧还保存了一个**可以指向当前方法所在类**的运行时常量池 目的是: 当前方法中如果需要调用其他方法的时候 能够从运行时常量池中找到对应的符号引用 然后将符号引用转换为直接引用
然后就能直接调用对应方法 这就是动态链接(我们还没讲到常量池 暂时记住即可 建议之后再回顾一下) 最后是方法出口 也就是方法该如何结束 是抛出异常还是正常返回

可能听起来有点懵逼 这里我们来模拟一下整个虚拟机栈的运作流程 我们先编写一个测试类:

```java
                        public class Main {
    
                            public static void main(String[] args) {
                                
                                int res = a();
                                System.out.println(res);
                                
                            }
                        
                            public static int a() {
                                return b();
                            }
                        
                            public static int b() {
                                return c();
                            }
                        
                            public static int c() {
                                
                                int a = 10;
                                int b = 20;
                                return a + b;
                                
                            }
                            
                        }
```

当我们的主方法执行后 会依次执行三个方法`a() -> b() -> c() -> 返回` 我们首先来观察一下反编译之后的结果:

```
                        {
                          public com.test.Main();   # 这个是构造方法
                            descriptor: ()V
                            flags: ACC_PUBLIC
                            Code:
                              stack=1, locals=1, args_size=1
                                 0: aload_0
                                 1: invokespecial #1                  // Method java/lang/Object."<init>":()V
                                 4: return
                              LineNumberTable:
                                line 3: 0
                              LocalVariableTable:
                                Start  Length  Slot  Name   Signature
                                    0       5     0  this   Lcom/test/Main;
                        
                          public static void main(java.lang.String[]);  # 主方法
                            descriptor: ([Ljava/lang/String;)V
                            flags: ACC_PUBLIC, ACC_STATIC
                            Code:
                              stack=2, locals=2, args_size=1
                                 0: invokestatic  #2                  // Method a:()I
                                 3: istore_1
                                 4: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
                                 7: iload_1
                                 8: invokevirtual #4                  // Method java/io/PrintStream.println:(I)V
                                11: return
                              LineNumberTable:
                                line 5: 0
                                line 6: 4
                                line 7: 11
                              LocalVariableTable:
                                Start  Length  Slot  Name   Signature
                                    0      12     0  args   [Ljava/lang/String;
                                    4       8     1   res   I
                        
                          public static int a();
                            descriptor: ()I
                            flags: ACC_PUBLIC, ACC_STATIC
                            Code:
                              stack=1, locals=0, args_size=0
                                 0: invokestatic  #5                  // Method b:()I
                                 3: ireturn
                              LineNumberTable:
                                line 10: 0
                        
                          public static int b();
                            descriptor: ()I
                            flags: ACC_PUBLIC, ACC_STATIC
                            Code:
                              stack=1, locals=0, args_size=0
                                 0: invokestatic  #6                  // Method c:()I
                                 3: ireturn
                              LineNumberTable:
                                line 14: 0
                        
                          public static int c();
                            descriptor: ()I
                            flags: ACC_PUBLIC, ACC_STATIC
                            Code:
                              stack=2, locals=2, args_size=0
                                 0: bipush        10
                                 2: istore_0
                                 3: bipush        20
                                 5: istore_1
                                 6: iload_0
                                 7: iload_1
                                 8: iadd
                                 9: ireturn
                              LineNumberTable:
                                line 18: 0
                                line 19: 3
                                line 20: 6
                              LocalVariableTable:
                                Start  Length  Slot  Name   Signature
                                    3       7     0     a   I
                                    6       4     1     b   I
                        }
```

可以看到在编译之后 我们整个方法的最大操作数栈深度, 局部变量表都是已经确定好的 当我们程序开始执行时 会根据这些信息封装为对应的栈帧 我们从main方法开始看起:

<img src="https://image.itbaima.cn/markdown/2023/03/06/rRtxbFZXDkmGci4.png"/>

接着我们继续往下 到了`0: invokestatic #2 // Method a:()I`时 需要调用方法`a()` 这时当前方法就不会继续向下运行了 而是去执行方法`a()`
那么同样的 将此方法也入栈 注意是放入到栈顶位置 `main`方法的栈帧会被压下去:

<img src="https://image.itbaima.cn/markdown/2023/03/06/SZJWH8l5xByTjr7.png"/>

这时 进入方法a之后 又继而进入到方法b 最后在进入c 因此 到达方法c的时候 我们的虚拟机栈变成了:

<img src="https://image.itbaima.cn/markdown/2023/03/06/FdS8U4lHVjCLKwD.png"/>

现在我们依次执行方法c中的指令 最后返回a+b的结果 在方法c返回之后 也就代表方法c已经执行结束了 栈帧4会自动出栈
这时栈帧3就得到了上一栈帧返回的结果 并继续执行 但是由于紧接着马上就返回 所以继续重复栈帧4的操作
此时栈帧3也出栈并继续将结果交给下一个栈帧2 最后栈帧2再将结果返回给栈帧1 然后栈帧1就可以继续向下运行了 最后输出结果

<img src="https://image.itbaima.cn/markdown/2023/03/06/FBpxKWIbuY7ftq4.png"/>

#### 本地方法栈
本地方法栈与虚拟机栈作用差不多 但是它 备的 这里不多做介绍

#### 堆
堆是整个Java应用程序共享的区域 也是整个虚拟机最大的一块内存空间 而此区域的职责就是存放和管理对象和数组 而我们马上要提到的垃圾回收机制也是主要作用于这一部分内存区域

#### 方法区
方法区也是整个Java应用程序共享的区域 它用于存储所有的类信息, 常量, 静态变量, 动态编译缓存等数据 可以大致分为两个部分 一个是类信息表 一个是运行时常量池 方法区也是我们要重点介绍的部分

<img src="https://image.itbaima.cn/markdown/2023/03/06/vYL53rtIDAWcQhb.png"/>

首先类信息表中存放的是当前应用程序加载的所有类信息 包括类的版本, 字段, 方法, 接口等信息 同时会将编译时生成的常量池数据全部存放到运行时常量池中
当然 常量也并不是只能从类信息中获取 在程序运行时 也有可能会有新的常量进入到常量池

其实我们的String类正是利用了常量池进行优化 这里我们编写一个测试用例:

```java
                        public static void main(String[] args) {
    
                            String str1 = new String("abc");
                            String str2 = new String("abc");
                        
                            System.out.println(str1 == str2);
                            System.out.println(str1.equals(str2));
                            
                        }
```

得到的结果也是显而易见的 由于`str1`和`str2`是单独创建的两个对象 那么这两个对象实际上会在堆中存放 保存在不同的地址:

<img src="https://image.itbaima.cn/markdown/2023/03/06/tnqFSxzEcB4U9WL.png"/>

所以当我们使用`==`判断时 得到的结果`false` 而使用`equals`时因为比较的是值 所以得到`true` 现在我们来稍微修改一下:

```java
                        public static void main(String[] args) {
    
                            String str1 = "abc";
                            String str2 = "abc";
                        
                            System.out.println(str1 == str2);
                            System.out.println(str1.equals(str2));
                            
                        }
```

现在我们没有使用new的形式 而是直接使用双引号创建 那么这时得到的结果就变成了两个true 这是为什么呢? 这其实是因为我们直接使用双引号赋值
会先在常量池中查找是否存在相同的字符串 若存在 则将引用直接指向该字符串 若不存在 则在常量池中生成一个字符串 再将引用指向该字符串

<img src="https://image.itbaima.cn/markdown/2023/03/06/GTgbpIYdCyK3RLj.png"/>

实际上两次调用String类的intern()方法 和上面的效果差不多 也是第一次调用会将堆中字符串复制并放入常量池中
第二次通过此方法获取字符串时 会查看常量池中是否包含 如果包含那么会直接返回常量池中字符串的地址:

```java
                        public static void main(String[] args) {
    
                            // 不能直接写"abc" 双引号的形式 写了就直接在常量池里面吧abc创好了
                            String str1 = new String("ab") + new String("c");
                            String str2 = new String("ab") + new String("c");
                        
                            System.out.println(str1.intern() == str2.intern());
                            System.out.println(str1.equals(str2));
                            
                        }
```

<img src="https://image.itbaima.cn/markdown/2023/03/06/EOC5ilkr396BHNy.png"/>

所以上述结果中得到的依然是两个`true` 在JDK1.7之后 稍微有一些区别 在调用`intern()`方法时 当常量池中没有对应的字符串时 不会再进行复制操作 而是将其直接修改为指向当前字符串堆中的的引用:

<img src="https://image.itbaima.cn/markdown/2023/03/06/2fXENphit4OZvVk.png"/>

```java
                        public static void main(String[] args) {
    
                          	// 不能直接写"abc" 双引号的形式 写了就直接在常量池里面吧abc创好了
                            String str1 = new String("ab") + new String("c");
                            System.out.println(str1.intern() == str1);
                            
                        }
```

```java
                        public static void main(String[] args) {
    
                            String str1 = new String("ab") + new String("c");
                            String str2 = new String("ab") + new String("c");
                        
                            System.out.println(str1 == str1.intern());
                            System.out.println(str2.intern() == str1);
                            
                        }
```

所以最后我们会发现 `str1.intern()`和`str1`都是同一个对象 结果为`true`

值得注意的是 在JDK7之后 字符串常量池从方法区移动到了堆中:

最后我们再来进行一个总结 各个内存区域的用途:
- (线程独有)程序计数器: 保存当前程序的执行位置
- (线程独有)虚拟机栈: 通过栈帧来维持方法调用顺序 帮助控制程序有序运行
- (线程独有)本地方法栈: 同上 作用与本地方法
- 堆: 所有的对象和数组都在这里保存
- 方法区: 类信息, 即时编译器的代码缓存, 运行时常量池

当然 这些内存区域划分仅仅是概念上的 具体的实现过程我们后面还会提到

### 爆内存和爆栈
实际上 在Java程序运行时 内存容量不可能是无限制的 当我们的对象创建过多或是数组容量过大时 就会导致我们的堆内存不足以存放更多新的对象或是数组 这时就会出现错误 比如:

```java
                        public static void main(String[] args) {
                            int[] a = new int[Integer.MAX_VALUE];
                        }
```

这里我们申请了一个容量为21亿多的int型数组 显然 如此之大的数组不可能放在我们的堆内存中 所以程序运行时就会这样:

```java
                        Exception in thread "main" java.lang.OutOfMemoryError: Requested array size exceeds VM limit
	                            at com.test.Main.main(Main.java:5)
```

这里得到了一个`OutOfMemoryError`错误 也就是我们常说的内存溢出错误 我们可以通过参数来控制堆内存的最大值和最小值:

                        -Xms最小值 -Xmx最大值

比如我们现在限制堆内存为固定值1M大小 并且在抛出内存溢出异常时保存当前的内存堆转储快照:

<img src="https://image.itbaima.cn/markdown/2023/03/06/r5IsmTk3DZfXA26.png"/>

注意堆内存不要设置太小 不然连虚拟机都不足以启动 接着我们编写一个一定会导致内存溢出的程序:

```java
                        public class Main {
    
                            public static void main(String[] args) {
                                
                                List<Test> list = new ArrayList<>();
                                while (true){
                                    list.add(new Test()); // 无限创建Test对象并丢进List中
                                }
                                
                            }
                        
                            static class Test{ }
    
                        }
```

在程序运行之后:

                        java.lang.OutOfMemoryError: Java heap space
                        Dumping heap to java_pid35172.hprof ...
                        Heap dump file created [12895344 bytes in 0.028 secs]
                        Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
                                at java.util.Arrays.copyOf(Arrays.java:3210)
                                at java.util.Arrays.copyOf(Arrays.java:3181)
                                at java.util.ArrayList.grow(ArrayList.java:267)
                                at java.util.ArrayList.ensureExplicitCapacity(ArrayList.java:241)
                                at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:233)
                                at java.util.ArrayList.add(ArrayList.java:464)
                                at com.test.Main.main(Main.java:10)

可以看到错误出现原因正是`Java heap space` 也就是堆内存满了 并且根据我们设定的VM参数 堆内存保存了快照信息 我们可以在IDEA内置的Profiler中进行查看:

<img src="https://image.itbaima.cn/markdown/2023/03/06/C71bdyIY2JVpLKz.png"/>

可以很明显地看到 在创建了360146个Test对象之后 堆内存蚌埠住了 于是就抛出了内存溢出错误

我们接着来看栈溢出 我们知道 虚拟机栈会在方法调用时插入栈帧 那么 设想如果出现无限递归的情况呢?

```java
                        public class Main {
    
                            public static void main(String[] args) {
                                test();
                            }
                        
                            public static void test() {
                                test();
                            }
                            
                        }
```

这很明显是一个永无休止的程序 并且会不断继续向下调用test方法本身 那么按照我们之前的逻辑推导 无限地插入栈帧那么一定会将虚拟机栈塞满 所以 当栈的深度已经不足以继续插入栈帧时 就会这样:

                        Exception in thread "main" java.lang.StackOverflowError
	                            at com.test.Main.test(Main.java:12)
	                            at com.test.Main.test(Main.java:12)
	                            at com.test.Main.test(Main.java:12)
	                            at com.test.Main.test(Main.java:12)
	                            at com.test.Main.test(Main.java:12)
	                            at com.test.Main.test(Main.java:12)
	                            ....以下省略很多行

这也是我们常说的栈溢出 它和堆溢出比较类似 也是由于容纳不下才导致的 我们可以使用-Xss来设定栈容量

### 申请堆外内存
除了堆外内存可以存放对象数据以外 外面也可以申请堆外内存(直接内存) 也就是不受JVM管控的内存区域 这部分区域的内存需要外面自行去申请和释放 实际上本质就是JVM通过C/C++调用`malloc`函数申请的内存
当然得我们自己去释放了 不过虽然是直接内存 不会受到堆内存容量限制 但是依然会受到本机最大内存的限制 所以还是有可能抛出`OutOfMemoryError`异常

这里我们需要提到一个堆外内存操作类: `Unsafe` 就像它的名字一样 虽然Java提供堆外内存的操作类 但是实际上它是不安全的 只有你完全了解底层原理并且能够合理控制堆外内存 才能安全地使用堆外内存

注意这个类不让我们new 也没有直接获取方式(压根就没想让我们用):

```java
                        public final class Unsafe {

                            private static native void registerNatives();
                            static {
                                registerNatives();
                                sun.reflect.Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
                            }
                        
                            private Unsafe() {}
                        
                            private static final Unsafe theUnsafe = new Unsafe();
                        
                            @CallerSensitive
                            public static Unsafe getUnsafe() {
                                Class<?> caller = Reflection.getCallerClass();
                                if (!VM.isSystemDomainLoader(caller.getClassLoader()))
                                    throw new SecurityException("Unsafe"); // 不是JDK的类 不让用
                                return theUnsafe;
                            }
```

所以我们这里就通过反射给他giao出来:

```java
                            public static void main(String[] args) throws IllegalAccessException {
    
                                Field unsafeField = Unsafe.class.getDeclaredFields()[0];
                                unsafeField.setAccessible(true);
                                Unsafe unsafe = (Unsafe) unsafeField.get(null);
                                
                            }
```

成功拿到Unsafe类之后 我们就可以开始申请堆外内存了 比如我们现在想要申请一个int大小的内存空间 并在此空间中存放一个int类型的数据:

```java
                            public static void main(String[] args) throws IllegalAccessException {
    
                                Field unsafeField = Unsafe.class.getDeclaredFields()[0];
                                unsafeField.setAccessible(true);
                                Unsafe unsafe = (Unsafe) unsafeField.get(null);
                            
                                // 申请4字节大小的内存空间 并得到对应位置的地址
                                long address = unsafe.allocateMemory(4);
                                // 在对应的地址上设定int的值
                                unsafe.putInt(address, 6666666);
                                // 获取对应地址上的Int型数值
                                System.out.println(unsafe.getInt(address));
                                // 释放申请到的内容
                                unsafe.freeMemory(address);
                            
                                // 由于内存已经释放 这时数据就没了
                                System.out.println(unsafe.getInt(address));
                                
                            }
```

我们可以来看一下`allocateMemory`底层是如何调用的 这是一个native方法 我们来看C++源码:

```cpp
                            UNSAFE_ENTRY(jlong, Unsafe_AllocateMemory0(JNIEnv *env, jobject unsafe, jlong size)) {
                              size_t sz = (size_t)size;
                            
                              sz = align_up(sz, HeapWordSize);
                              void* x = os::malloc(sz, mtOther); // 这里调用了os::malloc方法
                            
                              return addr_to_java(x);
                            } UNSAFE_END
```

接着来看:

```cpp
                            void* os::malloc(size_t size, MEMFLAGS flags) {
                              return os::malloc(size, flags, CALLER_PC);
                            }
                            
                            void* os::malloc(size_t size, MEMFLAGS memflags, const NativeCallStack& stack) {
                            	...
                              u_char* ptr;
                              ptr = (u_char*)::malloc(alloc_size); // 调用C++标准库函数 malloc(size)
                            	....
                              // we do not track guard memory
                              return MemTracker::record_malloc((address)ptr, size, memflags, stack, level);
                            }
```

所以 我们上面的Java代码转换为C代码 差不多就是这个意思:

```c
                            #include <stdlib.h>
                            #include <stdio.h>
                            
                            int main() {
                            
                                int * a = malloc(sizeof(int));
                                *a = 6666666;
                                printf("%d\n", *a);
                                free(a);
                                printf("%d\n", *a);
                                
                            }
```

所以说 直接内存实际上就是JVM申请的一块额外的内存空间 但是它并不在受管控的几种内存空间中 当然这些内存依然属于是JVM的
由于JVM提供的堆内存会进行垃圾回收等工作 效率不如直接申请和操作内存来得快 一些比较追求极致性能的框架会用到堆外内存来提升运行速度 如nio框架

当然 Unsafe类不仅仅只是这些功能 在其他系列中 我们还会讲到它