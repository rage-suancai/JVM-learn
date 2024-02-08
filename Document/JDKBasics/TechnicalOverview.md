## 走进JVM
JVM相对于Java应用层的学习难度更大 开篇推荐掌握的预备知识: C/C++(关键), 微机原理与接口技术, 计算机组成原理, 操作系统, 数据结构与算法, 编译原理(不推荐刚学完JavaSE的同学学习) 如果没有掌握推荐的一半以上的预备知识 可能学习起来会比较吃力

本文档中需要用到的开发工具: CLion, IDEA, Jetbrains Gateway

此阶段 我们需要深入探讨Java的底层执行原理 了解Java程序运行的本质 开始之前 推荐各位都入手一本《深入理解Java虚拟机 第三版》这本书对于JVM的讲述非常地详细

<img src="https://image.itbaima.cn/markdown/2023/03/06/b8TSFRoZayuUOlQ.jpg"/>

我们在JavaSE阶段的开篇就进行介绍了 我们的Java程序之所以能够实现跨平台 本质就是因为它是运行在虚拟机之上的 而不同平台只需要安装对应平台的Java虚拟机即可运行(在JRE中包含)
所有的Java程序都采用统一的标准 在任何平台编译出来的字节码文件(.class)也是同样的 最后实际上是将编译后的字节码交给JVM处理执行

<img src="https://image.itbaima.cn/markdown/2023/03/06/SgBybA8xC3MrD6k.jpg"/>

也正是得益于这种统一规范 除了Java以外 还有多种JVM语言 比如Kotlin, Groovy等 它们的语法虽然和Java不一样 但是最终编译得到的字节码文件和Java是同样的规范 同样可以交给JVM处理

<img src="https://image.itbaima.cn/markdown/2023/03/06/vNoWyDGsuwY23OE.jpg"/>

所以 JVM是我们需要去关注的一个部分 通过了解Java的底层运作机制 我们的技术会得到质的提升

### 技术概述
首先我们要了解虚拟机的具体定义 我们所接触过的虚拟机有安装操作系统的虚拟机 也有我们的Java虚拟机 而它们所面向的对象不同
Java虚拟机只是面向单一应用程序的虚拟机 但是它和我们接触的系统级虚拟机一样 我们也可以为其分配实际的硬件资源 比如最大内存大小等

并且Java虚拟机并没有采用传统的PC架构 比如现在的HotSpot虚拟机 实际上采用的是基于栈的指令集架构
而我们的传统程序设计一般都是基于寄存器的指令集架构 这里我们需要回顾一下计算机组成原理中的CPU结构

<img src="https://image.itbaima.cn/markdown/2023/03/06/FuCI49TPSRpaitE.png"/>

其中 **AX, BX, CX, DX称作为数据寄存器:**
- AX(Accumulator): 累加寄存器 也称之为累加器
- BX(Base): 基地址寄存器
- CX(Count): 计数器寄存器
- DX(Data): 数据寄存器

这些寄存器可以用来传送数据和暂存数据 并且它们还可以细分为一个8位的高位寄存器和一个8位的低位寄存器
除了这些通用功能 它们各自也有自己的一些专属职责 比如AX就是一个专用于累加的寄存器 用的也比较多

**SP和BP又称作为指针寄存器:**
- SP(Stack Pointer): 堆栈指针寄存器 与SS配合使用 用于访问栈顶
- BP(Base Pointer): 基指针寄存器 可用作SS的一个相对基址位置 用它可直接存取堆栈中的数据

**SI和DI又称作为变址寄存器:**
- SI(Source Index): 源变址寄存器
- DI(Destination Index): 目的变址寄存器

主要用于存放存储单元在段内的偏移量 用它们可实现多种存储器操作数的寻址方式 为以不同的地址形式访问存储单元提供方便

**控制寄存器:**
- IP(Instruction Pointer): 指令指针寄存器
- FLAG: 标志寄存器

**段寄存器**
- CS(Code Segment): 代码段寄存器
- DS(Data Segment): 数据段寄存器
- SS(Stack Segment): 堆栈段寄存器
- ES(Extra Segment): 附加段寄存器

这里我们分别比较一下在下x86架构下C语言和arm架构下编译之后的汇编指令不同之处:

```c
                        int main() { // 实现一个最简的a+b功能 并存入变量c
                            int a = 10;
                            int b = 20;
                            int c = a + b;
                            return c;
                        }
```

```shell
                        gcc -S main.c
```

```
                        .file	"main.c"
                            	.text
                            	.globl	main
                            	.type	main, @function
                            main:
                            .LFB0:
                            	.cfi_startproc  ;rbp寄存器是64位CPU下的基址寄存器 和8086CPU的16位bp一样
                            	pushq	%rbp    ;该函数中需要用到rbp寄存器 所以需要先把他原来的值压栈保护起来
                            	.cfi_def_cfa_offset 16
                            	.cfi_offset 6, -16
                            	movq	%rsp, %rbp  ;rsp是64位下的栈指针寄存器 这里是将rsp的值丢给rbp 因为局部变量是存放在栈中的 之后会使用rbp来访问局部变量
                            	.cfi_def_cfa_register 6
                            	movl	$10, -12(%rbp)    ;将10存入rbp所指向位置-12的位置 ->  int a = 10;
                            	movl	$20, -8(%rbp)     ;将20存入rbp所指向位置-8的位置  -> int b = 20;
                            	movl	-12(%rbp), %edx   ;将变量a的值交给DX寄存器(32位下叫edx 因为是int 这里只使用了32位)
                            	movl	-8(%rbp), %eax    ;同上 变量b的值丢给AX寄存器
                            	addl	%edx, %eax        ;将DX和AX寄存器中的值相加 并将结果存在AX中  ->  tmp = a + b
                            	movl	%eax, -4(%rbp)    ;将20存入rbp所指向位置-4的位置  -> int c = tmp;与上面合在一起就是int c = a + b;
                            	movl	-4(%rbp), %eax    ;根据约定 将函数返回值放在AX   -> return c;
                            	popq	%rbp    ;函数执行完毕 出栈
                            	.cfi_def_cfa 7, 8
                            	ret     ;函数返回
                            	.cfi_endproc
                            .LFE0:
                            	.size	main, .-main
                            	.ident	"GCC: (Ubuntu 7.5.0-6ubuntu2) 7.5.0"
                            	.section	.note.GNU-stack,"",@progbits
```

在arm架构下(Apple M1 Pro芯片) 编译的结果为:

```
                        .section   __TEXT,__text,regular,pure_instructions
                           .build_version macos, 12, 0    sdk_version 12, 1
                           .globl _main                           ; -- Begin function main
                           .p2align   2
                        _main:                                  ; @main
                           .cfi_startproc
                        ; %bb.0:
                           sub    sp, sp, #16                     ; =16
                           .cfi_def_cfa_offset 16
                           str    wzr, [sp, #12]
                           mov    w8, #10
                           str    w8, [sp, #8]
                           mov    w8, #20
                           str    w8, [sp, #4]
                           ldr    w8, [sp, #8]
                           ldr    w9, [sp, #4]
                           add    w8, w8, w9
                           str    w8, [sp]
                           ldr    w0, [sp]
                           add    sp, sp, #16                     ; =16
                           ret
                           .cfi_endproc
                                                                ; -- End function
                        .subsections_via_symbols
```

我们发现 在不同的CPU架构下 实际上得到的汇编代码也不一样 并且在arm架构下并没有和x86架构一样的寄存器结构 因此只能使用不同的汇编指令操作来实现 所以这也是为什么C语言不支持跨平台的原因
我们只能将同样的代码在不同的平台上编译之后才能在对应的平台上运行我们的程序 而Java利用了JVM 它提供了很好的平台无关性(当然 JVM本身是不跨平台的) 我们的Java程序编译之后
并不是可以由平台直接运行的程序 而是由JVM运行 同时 我们前面说了JVM(如HotSpot虚拟机)实际上采用的是基于栈的指令集架构
它并没有依赖于寄存器 而是更多的利用操作栈来完成 这样不仅设计和实现起来更简单 并且也能够更加方便地实现跨平台 不太依赖于硬件的支持

这里我们对一个类进行反编译查看:

```java
                        public class Main {
    
                            public int test() { // 和上面的例子一样
                                
                                int a = 10;
                                int b = 20;
                                int c = a + b;
                                return c;
                                
                            }
                            
                        }
```

```shell
                        javap -v target/classes/com/test/Main.class # 使用javap命令对class文件进行反编译
```

得到如下结果:

```
                        ...
                        public int test();
                            descriptor: ()I
                            flags: ACC_PUBLIC
                            Code:
                              stack=2, locals=4, args_size=1
                                 0: bipush        10
                                 2: istore_1
                                 3: bipush        20
                                 5: istore_2
                                 6: iload_1
                                 7: iload_2
                                 8: iadd
                                 9: istore_3
                                10: iload_3
                                11: ireturn
                              LineNumberTable:
                                line 5: 0
                                line 6: 3
                                line 7: 6
                                line 8: 10
                              LocalVariableTable:
                                Start  Length  Slot  Name   Signature
                                    0      12     0  this   Lcom/test/Main;
                                    3       9     1     a   I
                                    6       6     2     b   I
                                   10       2     3     c   I
```

我们可以看到 java文件编译之后 也会生成类似于C语言那样的汇编指令 但是这些命令都是交给JVM去执行的命令(实际上虚拟机提供了一个类似于物理机的运行环境 也有程序计数器之类的东西) 最下方存放的是本地变量(局部变量)表
表示此方法中出现的本地变量 实际上this也在其中 所以我们才能在非静态方法中使用this关键字 在最上方标记了方法的返回值类型, 访问权限等 首先介绍一下例子中出现的命令代表什么意思:
- bipush 将单字节的常量值推到栈顶
- istore_1 将栈顶的int类型数值存入到第二个本地变量
- istore_2 将栈顶的int类型数值存入到第三个本地变量
- istore_3 将栈顶的int类型数值存入到第四个本地变量
- iload_1 将第二个本地变量推向栈顶
- iload_2 将第三个本地变量推向栈顶
- iload_3 将第四个本地变量推向栈顶
- iadd 将栈顶的两个int类型变量相加 并将结果压入栈顶
- ireturn 方法的返回操作

有关详细的指令介绍列表可以参考《深入理解Java虚拟机 第三版》附录C

JVM运行字节码时 所有的操作基本都是围绕两种数据结构 一种是堆栈(本质是栈结构) 还有一种是队列 如果JVM执行某条指令时 该指令需要对数据进行操作
那么被操作的数据在指令执行前 必须要压到堆栈上 JVM会自动将栈顶数据作为操作数 如果堆栈上的数据需要暂时保存起来时 那么它就会被存储到局部变量队列上

我们从第一条指令来依次向下解读 显示方法相关属性:

    descriptor: ()I // 参数以及返回值类型 ()I就表示没有形式参数 返回值为基本类型int
    flags: ACC_PUBLIC // public访问权限
    Code:
    stack=2, locals=4, args_size=1 // stack表示要用到的最大栈深度 本地变量数 堆栈上最大对象数量(这里指的是this)

有关descriptor的详细属性介绍 我们会放在之后的类结构中进行讲解

接着我们来看指令:

    0: bipush        10 // 0是程序偏移地址 然后是指令 最后是操作数
    2: istore_1

这一步操作实际上就是使用`bipush`将10推向栈顶 接着使用`istore_1`将当前栈顶数据存放到第二个局部变量中 也就是a 所以这一步执行的是`int a = 10`操作

    3: bipush        20
    5: istore_2

同上 这里执行的是`int b = 20`操作

    6: iload_1
    7: iload_2
    8: iadd

这里是将第二和第三个局部变量放到栈中 也就是取a和b的值到栈中 最后iadd操作将栈中的两个值相加 结果依然放在栈顶

    9: istore_3
    10: iload_3
    11: ireturn

将栈顶数据存放到第四个局部变量中 也就是c 执行的是`int c = 30` 最后取出c的值放入栈顶 使用`ireturn`返回栈顶值 也就是方法的返回值

至此 方法执行完毕

实际上我们发现 JVM执行的命令基本都是入栈出栈等 而且大部分指令都是没有操作数的 传统的汇编指令有一操作数, 二操作数甚至三操作数的指令 Java相比C编译出来的汇编指令
执行起来会更加复杂 实现某个功能的指令条数也会更多 所以Java的执行效率实际上是不如C/C++的 虽然能够很方便地实现跨平台 但是性能上大打折扣 所以在性能要求比较苛刻的Android上
采用的是定制版的JVM 并且是基于寄存器的指令集架构 此外 在某些情况下 我们还可以使用JNI机制来通过Java调用C/C++编写的程序以提升性能(也就是本地方法 使用到native关键字)