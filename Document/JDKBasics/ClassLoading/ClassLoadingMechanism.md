## 类加载机制
现在 我们已经了解了字节码文件的结构 以及JVM如何对内存进行管理 现在只剩下最后一个谜团等待解开了 也就是我们的类字节码文件到底是如何加载到内存中的 加载之后又会做什么事情

### 类加载过程
首先 要加载一个类 一定是出于某种目的的 比如我们要运行我们的Java程序 那么就必须要加载主类才能运行类中的主方法
又或是我们需要加载数据库驱动 那么可以通过反射来将对应的数据库驱动类进行加载

所以 一般在这些情况下 如果类没有被加载 那么会被自动加载:
- 使用new关键字创建对象时
- 使用某个类的静态成员(包括方法和字段)的时候 (当然 final类型的静态字段有可能在编译的时候被放到了当前类的常量池中 这种情况下是不会触发自动加载的)
- 使用反射对类信息进行获取的时候(之前的数据库驱动就是这样的)
- 加载一个类的子类时
- 加载接口的实现类 且接口带有`default`的方法默认实现时

比如这种情况 那么需要用到另一个类中的成员字段 所以就必须将另一个类加载之后才能访问:

```java
                        public class Main {
    
                            public static void main(String[] args) {
                                System.out.println(Test.str);
                            }
                        
                            public static class Test {
                                
                                static {
                                    System.out.println("我被初始化了");
                                }
                        
                                public static String str = "都看到这里了 不给个赞吗?";
                                
                            }
                            
                        }
```

这里我们就演示一个不太好理解的情况 我们现在将静态成员变量修改为final类型的:

```java
                        public class Main {
                        
                            public static void main(String[] args) {
                                System.out.println(Test.str);
                            }
                        
                            public static class Test {
                                
                                static {
                                    System.out.println("我被初始化了");
                                }
                        
                                public final static String str = "都看到这里了 不给个赞吗?";
                                
                            }
                            
                        }
```

可以看到 在主方法中 我们使用了Test类的静态成员变量 并且此静态成员变量是一个final类型的 也就是说不可能再发生改变 那么各位觉得 Test类会像上面一样被初始化吗?

按照正常逻辑来说 既然要用到其他类中的字段 那么肯定需要加载其他类 但是这里我们结果发现 并没有对Test类进行加载 那么这是为什么呢? 我们来看看Main类编译之后的字节码指令就知道了:

<img src="https://image.itbaima.cn/markdown/2023/03/06/JyFWfPbBvIK5zMe.png"/>

很明显 这里使用的是`ldc`指令从常量池中将字符串取出并推向操作数栈顶 也就是说 在编译阶段 整个Test.str直接被替换为了对应的字符串
(因为final不可能发生改变的 编译就会进行优化 直接来个字符串比你去加载类在获取快得多不是吗? 反正结果都一样) 所以说编译之后 实际上跟Test类半毛钱关系都没有了

所以说 当你在某些情况下疑惑为什么类加载了或是没有加载时 可以从字节码指令的角度去进行分析
一般情况下 只要遇到`new`, `getstatic`, `putstatic`, `invokestatic`这些指令时 都会进行类加载 比如:

<img src="https://image.itbaima.cn/markdown/2023/03/06/IRo9i6hntA2jQ3X.png"/>

这里很明显 是一定会将Test类进行加载的 除此之外 各位也可以试试看数组的定义会不会导致类被加载

好了 聊完了类的加载触发条件 我们接着来看一下类的详细加载流程:

<img src="https://image.itbaima.cn/markdown/2023/03/06/UIV6fJknmM4bojP.png"/>

首先类的生命周期一共有7个阶段 而首当其冲的就是加载 加载阶段需要获取此类的二进制数据流 比如我们要从硬盘中读取一个class文件 那么就可以通过文件输入流来获取类文件的`byte[]`
也可以是其他各种途径获取类文件的输入流 甚至网络传输并加载一个类也不是不可以 然后交给类加载器进行加载(类加载器可以是JDK内置的 也可以是开发者自己撸的 后面会详细介绍) 类的所有信息会被加载到方法区中
并且在堆内存中会生成一个代表当前类的Class类对象(那么思考一下 同一个Class文件加载的类 是唯一存在的吗?) 我们可以通过此对象以及反射机制来访问这个类的各种信息

数组类要稍微特殊一点 通过前面的检验 我没发现数组在创建后是不会导致类加载的 数组类型本身不会通过类加载器进行加载的 不过你既然要往里面丢对象进去 那最终依然是要加载类的

接着我们来看验证阶段 验证阶段相当于是对加载的类进行一次规范校验(因为一个类并不一定是由我们使用IDEA编译出来的 有可能是像我们之前那样直接用ASM框架写的一个)
如果说类的任何地方不符合虚拟机规范 那么这个类是不会验证通过的 如果没有验证机制 那么一旦出现危害虚拟机的操作 整个程序会出现无法预料的后果

验证阶段 首先是文件格式的验证:
- 是否魔数为CAFEBABE开头
- 主, 次版本号是否可以由当前Java虚拟机运行
- Class文件各个部分的完整性如何
- ...

有关类验证的详细过程 可以参考《深入理解Java虚拟机 第三版》268页

接下来就是准备阶段了 这个阶段会为类变量分配内存 并为一些字段设定初始值 注意是系统规定的初始值 不是我们手动指定的初始值

再往下就是解析阶段 此阶段是将常量池内的符号引用替换为直接引用的过程 也就是说 到这个时候 所有引用变量的指向都是已经切切实实地指向了内存中的对象了

到这里 链接过程就结束了 也就是说这个时候类基本上已经完成大部分内容的初始化了

最后就是真正的初始化阶段了 从这里开始 类中的Java代码部分 才会开始执行 还记得我们之前介绍的`<clinit>`方法吗 它就是在这个时候执行的
比如我们的类中存在一个静态成员变量 并且赋值为10 或是存在一个静态代码块 那么就会自动生成一个`<clinit>`方法来进行赋值操作 但是这个方法是自动生成的

全部完成之后 我们的类就算是加载完成了

### 类加载器
Java提供了类加载器 以使我们自己可以更好地控制类加载 我们可以自定义类加载 也可以使用官方自带的类加载器去加载类
对于任意一个类 都必须由加载它的类加载器和这个类本身一起共同确立其在Java虚拟机中的唯一性

也就是说 一个类可以由不同的类加载器加载 并且 不同的类加载器加载的出来的类 即使来自同一个Class文件 也是不同的
只有两个类来自同一个Class文件并且是由同一个类加载器加载的 才能判断为是同一个 默认情况下 所有的类都是由JDK自带的类加载器进行加载

比如 我们先创建一个Test类用于测试:

```java
                        package com.test;

                        public class Test {
                            
                        }
```

接着我们自己实现一个ClassLoader来加载我们的Test类 同时使用官方默认的类加载器来加载:

```java
                        public class Main {
    
                            public static void main(String[] args) throws ReflectiveOperationException {
                                Class<?> testClass1 = Main.class.getClassLoader().loadClass("com.test.Test");
                                CustomClassLoader customClassLoader = new CustomClassLoader();
                                Class<?> testClass2 = customClassLoader.loadClass("com.test.Test");
                        
                             	// 看看两个类的类加载器是不是同一个
                                System.out.println(testClass1.getClassLoader());
                                System.out.println(testClass2.getClassLoader());
                        				
                              	// 看看两个类是不是长得一模一样
                                System.out.println(testClass1);
                                System.out.println(testClass2);
                        
                              	// 两个类是同一个吗?
                                System.out.println(testClass1 == testClass2);
                              
                              	// 能成功实现类型转换吗?
                                Test test = (Test) testClass2.newInstance();
                                
                            }
                        
                            static class CustomClassLoader extends ClassLoader {
                                
                                @Override
                                public Class<?> loadClass(String name) throws ClassNotFoundException {
                                    try (FileInputStream stream = new FileInputStream("./target/classes/"+name.replace(".", "/")+".class")){
                                        byte[] data = new byte[stream.available()];
                                        stream.read(data);
                                        if(data.length == 0) return super.loadClass(name);
                                        return defineClass(name, data, 0, data.length);
                                    } catch (IOException e) {
                                        return super.loadClass(name);
                                    }
                                }
                                
                            }
                        }            
```

通过结果我们发现 即使两个类是同一个Class文件加载的 只要类加载器不同 那么这两个类就是不同的两个类

所以说 我们当时在JavaSE阶段讲解的每个类都在堆中有一个唯一的Class对象放在这里来看 并不完全正确 只是当前为了防止各位初学者搞混

实际上 JDK内部提供的类加载器一共有三个 比如上面我们的Main类 其实是被AppClassLoader加载的 而JDK内部的类 都是由BootstrapClassLoader加载的 这其实就是为了实现双亲委派机制而做的:

<img src="https://image.itbaima.cn/markdown/2023/03/06/RFaE7s5CnmylgkT.png"/>

有关双亲委派机制 我们在JavaSE阶段反射板块已经讲解过了 所以说这就不多做介绍了