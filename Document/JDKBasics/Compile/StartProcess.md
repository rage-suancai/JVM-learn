## JVM启动流程探究
前面我们完成了JDK8的编译 也了解了如何进行断点调试 现在我们就可以来研究一下JVM的启动流程了 首先我们要明确
虚拟机的启动入口位于`jdk/src/share/bin/java.c`的`JLI_Launch`函数 整个流程分为如下几个步骤:
1. 配置JVM装载环境
2. 解析虚拟机参数
3. 设置线程栈大小
4. 执行JavaMain方法

首先我们来看看`JLI_Launch`函数是如何定义的:

```c
                        int
                        JLI_Launch(int argc, char ** argv,              /* main argc, argc */
                                int jargc, const char** jargv,          /* java args */
                                int appclassc, const char** appclassv,  /* app classpath */
                                const char* fullversion,                /* full version defined */
                                const char* dotversion,                 /* dot version defined */
                                const char* pname,                      /* program name */
                                const char* lname,                      /* launcher name */
                                jboolean javaargs,                      /* JAVA_ARGS */
                                jboolean cpwildcard,                    /* classpath wildcard */
                                jboolean javaw,                         /* windows-only javaw */
                                jint     ergo_class                     /* ergnomics policy */
                        );
```

可以看到在入口点的参数有很多个 其中包括当前的完整版本名称, 简短版本名称, 运行参数, 程序名称, 启动器名称等

首先会进行一些初始化操作以及Debug信息打印配置等:

```c
                        InitLauncher(javaw);
                        DumpState();
                        if (JLI_IsTraceLauncher()) {
                            int i;
                            printf("Command line args:\n");
                            for (i = 0; i < argc ; i++) {
                                printf("argv[%d] = %s\n", i, argv[i]);
                            }
                            AddOption("-Dsun.java.launcher.diag=true", NULL);
                        }
```

接着就是选择一个合适的JRE版本:

```c
                        /*
                         * Make sure the specified version of the JRE is running.
                         *
                         * There are three things to note about the SelectVersion() routine:
                         *  1) If the version running isn't correct, this routine doesn't
                         *     return (either the correct version has been exec'd or an error
                         *     was issued).
                         *  2) Argc and Argv in this scope are *not* altered by this routine.
                         *     It is the responsibility of subsequent code to ignore the
                         *     arguments handled by this routine.
                         *  3) As a side-effect, the variable "main_class" is guaranteed to
                         *     be set (if it should ever be set).  This isn't exactly the
                         *     poster child for structured programming, but it is a small
                         *     price to pay for not processing a jar file operand twice.
                         *     (Note: This side effect has been disabled.  See comment on
                         *     bugid 5030265 below.)
                         */
                        SelectVersion(argc, argv, &main_class);
```

接着是创建JVM执行环境 例如需要确定数据模型 是32位还是64位 以及jvm本身的一些配置在jvm.cfg文件中读取和解析:

```c
                        CreateExecutionEnvironment(&argc, &argv,
                                                        jrepath, sizeof(jrepath),
                                                        jvmpath, sizeof(jvmpath),
                                                        jvmcfg,  sizeof(jvmcfg));
```

此函数只在头文件中定义 具体的实现是根据不同平台而定的 接着会动态加载jvm.so这个共享库 并把jvm.so中的相关函数导出并且初始化 而启动JVM的函数也在其中:

```c
                        if (!LoadJavaVM(jvmpath, &ifn)) {
                            return(6);
                        }
```

比如mac平台下的实现:

```c
                        jboolean
                        LoadJavaVM(const char *jvmpath, InvocationFunctions *ifn)
                        {
                            Dl_info dlinfo;
                            void *libjvm;
                        
                            JLI_TraceLauncher("JVM path is %s\n", jvmpath);
                        
                            libjvm = dlopen(jvmpath, RTLD_NOW + RTLD_GLOBAL);
                            if (libjvm == NULL) {
                                JLI_ReportErrorMessage(DLL_ERROR1, __LINE__);
                                JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
                                return JNI_FALSE;
                            }
                        
                            ifn->CreateJavaVM = (CreateJavaVM_t)
                                dlsym(libjvm, "JNI_CreateJavaVM");
                            if (ifn->CreateJavaVM == NULL) {
                                JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
                                return JNI_FALSE;
                            }
                        
                            ifn->GetDefaultJavaVMInitArgs = (GetDefaultJavaVMInitArgs_t)
                                dlsym(libjvm, "JNI_GetDefaultJavaVMInitArgs");
                            if (ifn->GetDefaultJavaVMInitArgs == NULL) {
                                JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
                                return JNI_FALSE;
                            }
                        
                            ifn->GetCreatedJavaVMs = (GetCreatedJavaVMs_t)
                            dlsym(libjvm, "JNI_GetCreatedJavaVMs");
                            if (ifn->GetCreatedJavaVMs == NULL) {
                                JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
                                return JNI_FALSE;
                            }
                        
                            return JNI_TRUE;
                        }
```

最后就是对JVM进行初始化了:

```c
                        return JVMInit(&ifn, threadStackSize, argc, argv, mode, what, ret);
```

这也是由平台决定的 比如Mac下的实现为:

```c
                        int
                        JVMInit(InvocationFunctions* ifn, jlong threadStackSize,
                                         int argc, char **argv,
                                         int mode, char *what, int ret) {
                            if (sameThread) {
                                // 无需关心....
                            } else {
                              	// 正常情况下走这个
                                return ContinueInNewThread(ifn, threadStackSize, argc, argv, mode, what, ret);
                            }
                        }
```

可以看到最后进入了一个`ContinueInNewThread`函数(在刚刚的java.c中实现) 这个函数会创建一个新的线程来执行:

```c
                        int
                        ContinueInNewThread(InvocationFunctions* ifn, jlong threadStackSize,
                                            int argc, char **argv,
                                            int mode, char *what, int ret)
                        {
                        
                            ...
                        
                              rslt = ContinueInNewThread0(JavaMain, threadStackSize, (void*)&args);
                              /* If the caller has deemed there is an error we
                               * simply return that, otherwise we return the value of
                               * the callee
                               */
                              return (ret != 0) ? ret : rslt;
                            }
                        }
```

接着进入了一个名为`ContinueInNewThread0`的函数 可以看到它将`JavaMain`函数传入作为参数 而此函数定义的第一个参数类型是一个函数指针:

```c
                        int
                        ContinueInNewThread0(int (JNICALL *continuation)(void *), jlong stack_size, void * args) {
                            int rslt;
                            pthread_t tid;
                            pthread_attr_t attr;
                            pthread_attr_init(&attr);
                            pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);
                        
                            if (stack_size > 0) {
                              pthread_attr_setstacksize(&attr, stack_size);
                            }
                        
                            if (pthread_create(&tid, &attr, (void *(*)(void*))continuation, (void*)args) == 0) {
                              void * tmp;
                              pthread_join(tid, &tmp);
                              rslt = (int)tmp;
                            } else {
                             /*
                              * Continue execution in current thread if for some reason (e.g. out of
                              * memory/LWP)  a new thread can't be created. This will likely fail
                              * later in continuation as JNI_CreateJavaVM needs to create quite a
                              * few new threads, anyway, just give it a try..
                              */
                              rslt = continuation(args);
                            }
                        
                            pthread_attr_destroy(&attr);
                            return rslt;
                        }
```

最后实际上是在新的线程中执行JavaMain函数 最后我们再来看看此函数里面做了什么事情:

```c
                        /* Initialize the virtual machine */
                        start = CounterGet();
                        if (!InitializeJVM(&vm, &env, &ifn)) {
                            JLI_ReportErrorMessage(JVM_ERROR1);
                            exit(1);
                        }
```

第一步初始化虚拟机 如果报错直接退出 接着就是加载主类(至于具体如何加载一个类 我们会放在后面进行讲解) 因为主类是我们Java程序的入口点:

```c
                        /*
                         * Get the application's main class.
                         *
                         * See bugid 5030265.  The Main-Class name has already been parsed
                         * from the manifest, but not parsed properly for UTF-8 support.
                         * Hence the code here ignores the value previously extracted and
                         * uses the pre-existing code to reextract the value.  This is
                         * possibly an end of release cycle expedient.  However, it has
                         * also been discovered that passing some character sets through
                         * the environment has "strange" behavior on some variants of
                         * Windows.  Hence, maybe the manifest parsing code local to the
                         * launcher should never be enhanced.
                         *
                         * Hence, future work should either:
                         *     1)   Correct the local parsing code and verify that the
                         *          Main-Class attribute gets properly passed through
                         *          all environments,
                         *     2)   Remove the vestages of maintaining main_class through
                         *          the environment (and remove these comments).
                         *
                         * This method also correctly handles launching existing JavaFX
                         * applications that may or may not have a Main-Class manifest entry.
                         */
                        mainClass = LoadMainClass(env, mode, what);
```

某些没有主方法的Java程序比如JavaFX应用 会获取ApplicationMainClass:

```c
                        /*
                         * In some cases when launching an application that needs a helper, e.g., a
                         * JavaFX application with no main method, the mainClass will not be the
                         * applications own main class but rather a helper class. To keep things
                         * consistent in the UI we need to track and report the application main class.
                         */
                        appClass = GetApplicationClass(env);
```

初始化完成:

```c
                        /*
                         * PostJVMInit uses the class name as the application name for GUI purposes,
                         * for example, on OSX this sets the application name in the menu bar for
                         * both SWT and JavaFX. So we'll pass the actual application class here
                         * instead of mainClass as that may be a launcher or helper class instead
                         * of the application class.
                         */
                        PostJVMInit(env, appClass, vm);
```

接着就是获取主类中的主方法:

```c
                        /*
                         * The LoadMainClass not only loads the main class, it will also ensure
                         * that the main method's signature is correct, therefore further checking
                         * is not required. The main method is invoked here so that extraneous java
                         * stacks are not in the application stack trace.
                         */
                        mainID = (*env)->GetStaticMethodID(env, mainClass, "main",
                                                           "([Ljava/lang/String;)V");
```

没错 在字节码中`void main(String[] args)`表示为`([Ljava/lang/String;)V`我们之后会详细介绍 接着就是调用主方法了:

```c
                        /* Invoke main method. */
                        (*env)->CallStaticVoidMethod(env, mainClass, mainID, mainArgs);
```

调用后 我们的Java程序就开飞速运行起来 直到走到主方法的最后一行返回:

```c
                        /*
                         * The launcher's exit code (in the absence of calls to
                         * System.exit) will be non-zero if main threw an exception.
                         */
                        ret = (*env)->ExceptionOccurred(env) == NULL ? 0 : 1;
                        LEAVE();
```

至此 一个Java程序的运行流程结束 在最后LEAVE函数中会销毁JVM 我们可以进行断点调试来查看是否和我们推出的结论一致:

<img src="https://image.itbaima.cn/markdown/2023/03/06/DgkhOvWYfAiB1yq.png"/>

还是以我们之前编写的测试类进行 首先来到调用之前 我们看到主方法执行之前 控制台没有输出任何内容 接着我们执行此函数 再来观察控制台的变化:

<img src="https://image.itbaima.cn/markdown/2023/03/06/X3F2Hjvplnm17UJ.png"/>

可以看到 主方法执行完成之后 控制台也成功输出了Hello World!

继续下一步 整个Java程序执行完成 得到退出状态码`0`:

<img src="https://image.itbaima.cn/markdown/2023/03/06/SoP1fVekqM4R8sd.png"/>

成功验证 最后总结一下整个执行过程:

<img src="https://image.itbaima.cn/markdown/2023/03/06/c4IKjgrhtw3ak9p.png"/>