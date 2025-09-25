package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.arm.context.Arm64RegisterContext;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookEntryInfo;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.hookzz.InstrumentCallback;
import com.github.unidbg.hook.hookzz.WrapCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;

public class UnidbgNativeHelper {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgNativeHelper() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.xiaojianbang.app").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM();
        vm.setVerbose(true); //是否展示调用过程的细节

        //AbstractJni内部已经写好了一些常用类的常用方法，有些方法我们可以不用亲自补环境
        vm.setJni(new AbstractJni() {});

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libxiaojianbang.so"), false);
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }

    /*
    * callStaticJniMethodInt调用native方法 静态注册的函数
    * */
    public void call_add() {
        System.out.println("------------------- call_add begin -------------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");

        String method = "add(III)I";
        int result = dvmClass.callStaticJniMethodInt(emulator, method, 5, 6, 7);
        System.out.println("call_add result--->" + result);
        System.out.println("------------------- call_add end -------------------------");
    }


    /*
    * callStaticJniMethodObject  静态注册的native函数
    * */
    public void call_md5() {
        System.out.println("------------------- call_md5 begin -------------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        String method  ="md5(Ljava/lang/String;)Ljava/lang/String;";
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, method, "123456789");
        String result  = (String) dvmObject.getValue();
        System.out.println("call_md5 result--->" + result);
        System.out.println("------------------- call_md5 end -------------------");
    }


    /*
    * callStaticJniMethodObject 调用动态注册的函数，需注意：
    *      动态注册的native函数通常是在JNI_OnLoad函数中调用registerNative(),所以当加载so文件的时我们需要显示调用JNI_Onload函数
    *               dm.callJNI_OnLoad(emulator);   否则会报错找不到该函数
    * */
    public void call_encode() {
        System.out.println("------------------- call_encode begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        String method  ="encode()Ljava/lang/String;";
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, method);
        String result  = (String) dvmObject.getValue();
        System.out.println("call_encode result--->" + result);
        System.out.println("------------------- call_encode end ------------------");
    }

    /*
    * findSymbolByName通过在IDA中的函数名称去查找符合Symbol，然后调用call传入对应的参数获取结果Number
    *   call中参数对于基本数据类型可直接进行传，对于引用数据类型，需要通过vm.addLocalObject(DvmObject)进行包装传递
    *       返回值Number，对于基本数据类型，返回的即为结果.intValue,.longValue等
    *                    对于引用数据类型，返回的即为一个hashCode，需要通过vm.getObject(hashCod)将其转换为DvmObject，然后getValue即可得到真正返回值
    *                   倘若返回结果是一个指针地址，可以通过Memory.pointer(Number).getByteArray()
    * */
    public void call_findSymbolByName() {
        System.out.println("----------------- call_findSymbolByName begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");

        Symbol symbol = module.findSymbolByName("Java_com_xiaojianbang_ndk_NativeHelper_add");
        System.out.println(symbol.toString());
        Number call = symbol.call(emulator, vm.getJNIEnv(), vm.addLocalObject(dvmClass), 100, 200, 300);
        System.out.println(call);

        Symbol nativeSymbol = module.findSymbolByName("_Z7_strcatP7_JNIEnvP7_jclass");
        Number native_call = nativeSymbol.call(emulator, vm.getJNIEnv(), vm.addLocalObject(dvmClass));
        System.out.println(native_call);
        DvmObject<?> object = vm.getObject(native_call.intValue());
        System.out.println(object.getValue());

        System.out.println("----------------- call_findSymbolByName end ---------------");
    }

    /*
    * callFunction 通过函数的相对偏移地址去调用
    *
    * */
    public void call_function() {
        System.out.println("----------------- call_function begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        Number number = module.callFunction(emulator, 0x1B4C, vm.getJNIEnv(), vm.addLocalObject(dvmClass));
        System.out.println("encode-------->hashCode:" + number.intValue());
        Object value = vm.getObject(number.intValue()).getValue();
        System.out.println("encode-------->result:" + value);
        System.out.println("----------------- call_function end ---------------");


        //MD5 Init 构造上下文md5CTX
        UnidbgPointer md5CTX = emulator.getMemory().malloc(200, false).getPointer();
        System.out.println(md5CTX);
        module.callFunction(emulator,0x2230,md5CTX);
        System.out.println(md5CTX);

        //MD5 update 通过init初始化上下文进行传递，对于native非jstring的参数，不可使用vm.addLocalObject(new StringObjece(vm,"123456"))去进行传递参数
        //需要申请内存空间写入内容后通过指针传递UnidbgPointer
        UnidbgPointer pointer = emulator.getMemory().malloc(200, false).getPointer();
        byte[] datas = "xiaojianbang_unidbg".getBytes();
        pointer.write(datas);
        module.callFunction(emulator, 0x22A0, md5CTX, pointer,datas.length);

        //MD5 Final
        UnidbgPointer resultPointer = emulator.getMemory().malloc(200, false).getPointer();
        module.callFunction(emulator,0x3A78,md5CTX,resultPointer);
        byte[] byteArray = resultPointer.getByteArray(0, 16);
        Inspector.inspect(byteArray, "MD5Result");
        System.out.println("----------------- call_function end ---------------");
    }


    /*
    * hook MD5中的update函数
    * */
    public void hook_zz() {
        System.out.println("----------------- hook_zz begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        HookZz hookZz = HookZz.getInstance(emulator);
        Symbol symbol = module.findSymbolByName("MD5Update");
        System.out.println(symbol);
        hookZz.wrap(module.findSymbolByName("_Z9MD5UpdateP7MD5_CTXPhj"), new WrapCallback<RegisterContext>() {
            @Override
            public void preCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
                //获取参数
                UnidbgPointer pointerArg = ctx.getPointerArg(1);
                int dataLength = ctx.getIntArg(2);
                Inspector.inspect(pointerArg.getByteArray(0,dataLength),"plainText");
                System.out.println("preCall");
            }

            @Override
            public void postCall(Emulator<?> emulator, RegisterContext ctx, HookEntryInfo info) {
                super.postCall(emulator, ctx, info);
                System.out.println("postCall");
            }
        });
        System.out.println("----------------- hook_zz end ---------------");

        //主动调用去触发hookZz
        StringObject md5Result = dvmClass.callStaticJniMethodObject(emulator, "md5(Ljava/lang/String;)Ljava/lang/String;", new StringObject(vm, "xiaojianbang")); // 执行Jni方法
        System.out.println("----------------- hook_zz end ---------------");
    }


    /*
    * hook具体行的汇编代码
    * */
    public void hook_zz_inline() {
        System.out.println("----------------- hook_zz_inline begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        HookZz hookZz = HookZz.getInstance(emulator);
        hookZz.instrument(module.base + 0x1AEC, new InstrumentCallback<Arm64RegisterContext>() {
            @Override
            public void dbiCall(Emulator<?> emulator, Arm64RegisterContext ctx, HookEntryInfo info) {
                //ida中汇编代码：08 01 09 0B ADD             W8, W8, W9
                System.out.println("w8:" + ctx.getXInt(8));
                System.out.println("w9:" + ctx.getXInt(9));
            }
        });
        //主动调用
        dvmClass.callStaticJniMethodInt(emulator, "add(III)I", 5, 6, 7);
        System.out.println("---------------- hook_zz_inline end --------------------");
    }


    /*
    * 获取参数Jsting格式 需要用vm.getObject(hashCode)进行
    * 获取参数c语言格式字符串，需要用指针UnidbgPointer去读写操作
    * */
    public void hook_zz_two() {
        System.out.println("----------------- hook_zz_two begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        HookZz hookZz = HookZz.getInstance(emulator);
        hookZz.wrap(module.findSymbolByName("_Z12jstring2cstrP7_JNIEnvP8_jstring"), new WrapCallback<Arm64RegisterContext>() {
            @Override
            public void preCall(Emulator<?> emulator, Arm64RegisterContext ctx, HookEntryInfo info) {
                int argHashCode = ctx.getIntArg(1);
                DvmObject<?> object = vm.getObject(argHashCode);
                System.out.println("参数1：" + object.getValue());
            }

            @Override
            public void postCall(Emulator<?> emulator, Arm64RegisterContext ctx, HookEntryInfo info) {
                super.postCall(emulator, ctx, info);
                UnidbgPointer xPointer = ctx.getXPointer(0);
                byte[] byteArray = xPointer.getByteArray(0, 16);
                Inspector.inspect(byteArray,"cString");
            }
        });
        dvmClass.callStaticJniMethodObject(emulator, "md5(Ljava/lang/String;)Ljava/lang/String;", new StringObject(vm, "xiaojianbang")); // 执行Jni方法
        System.out.println("------------------ hook_zz_two end ----------------");
    }


    /*
    * 替换函数 return super即指向原函数的逻辑
    * 传递过去的jstring参数可通过vm.getObject(hashCode)进行获取
    * 返回值：
    *   HookStatus.LR(emulator,100) ----> 返回100
    * */
    public void hook_replace() {
        System.out.println("----------------- hook_replace begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");

        HookZz hookZz = HookZz.getInstance(emulator);
        hookZz.replace(module.findSymbolByName("Java_com_xiaojianbang_ndk_NativeHelper_md5"), new ReplaceCallback() {

            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                int hashCode = context.getIntArg(2);
                DvmObject<?> object = vm.getObject(hashCode);
                System.out.println("onCall----->" + object.getValue());
//                return super.onCall(emulator, context, originFunction);
                return HookStatus.LR(emulator,100);
            }
        });
        int res = dvmClass.callStaticJniMethodInt(emulator, "md5(Ljava/lang/String;)Ljava/lang/String;", new StringObject(vm, "xiaojianbang")); // 执行Jni方法
        System.out.println("res替换后的结果--->" + res);

        System.out.println("------------------ hook_replace end ----------------");
    }


    /*
    * 具体汇编行级别的Hook
    *   context.getPointerArg 获取参数
    *   context.getXPointer 获取具体的寄存器里面相关的值
    *   注意hook时的起始地址需要绝对地址即module.base+offset
    * */
    public void hook_unicorn() {
        System.out.println("----------------- hook_unicorn begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        emulator.getBackend().hook_add_new(new CodeHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                System.out.println(address);
                if (address == module.base + 0x1FF4) {
                    System.out.println("偏移地址0x1FF4进来了");
                    Arm64RegisterContext context = emulator.getContext();
                    UnidbgPointer pointerArg = context.getPointerArg(0);
                    Inspector.inspect(pointerArg.getByteArray(0,32),"MD5CTX");
                    UnidbgPointer plainTxt = context.getPointerArg(1);
                    int length = context.getIntArg(2);
                    Inspector.inspect(plainTxt.getByteArray(0,length),"明文");
                } else if (address == module.base + 0x2004) {
                    System.out.println("偏移地址0x2004进来了");
                    Arm64RegisterContext context = emulator.getContext();
                    UnidbgPointer cipherTxt = context.getPointerArg(1);
                    int length = context.getIntArg(2);
                    Inspector.inspect(cipherTxt.getByteArray(0,length),"密文");
                }
            }
            @Override
            public void onAttach(UnHook unHook) {
            }

            @Override
            public void detach() {
            }
        },module.base + 0x1FE8,module.base + 0x2004,"");
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, "md5(Ljava/lang/String;)Ljava/lang/String;", "987654321");
        String result  = (String) dvmObject.getValue();
        System.out.println("hook_unicorn result--->" + result);
        System.out.println("----------------- hook_unicorn end ---------------");
    }

    /*
    * 打印函数调用栈情况
    * */
    public void hook_print_call_stack() {
        System.out.println("----------------- hook_print_call_stack begin ---------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");

        emulator.getBackend().hook_add_new(new CodeHook() {
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                emulator.getUnwinder().unwind();
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        },module.base + 0x22A0,module.base + 0x22A0,"");

        dvmClass.callStaticJniMethodObject(emulator, "md5(Ljava/lang/String;)Ljava/lang/String;", "987654321");
        System.out.println("----------------- hook_print_call_stack end ---------------");
    }


    /*
    * 添加断点 可按二次回车查看命令行帮助
    * */
    private void add_debugger() {
        System.out.println("------------------- add_debugger begin -------------------");
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");

        Debugger debugger = emulator.attach();
        debugger.addBreakPoint(module.base + 0x1AEC);
        debugger.addBreakPoint(module.base + 0x1AF4);

        int result = dvmClass.callStaticJniMethodInt(emulator, "add(III)I", 5, 6, 7);
        System.out.println("call_add result--->" + result);
        System.out.println("------------------- add_debugger end -------------------------");
    }

    /*
    * 监控内存读写情况
    * */
    public void trace_read_write() {
        System.out.println("------------------- trace_read_write begin -------------------");
        File file = new File("log.txt");
        try {
            PrintStream printStream = new PrintStream(Files.newOutputStream(file.toPath()),true);
            emulator.traceRead(module.base,module.base + module.size).setRedirect(printStream);
            emulator.traceWrite(module.base,module.base + module.size).setRedirect(printStream);
        } catch (Exception e) {
            System.out.println(e);
        }

        //主动调用
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        String method  ="md5(Ljava/lang/String;)Ljava/lang/String;";
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, method, "123456789");
        String result  = (String) dvmObject.getValue();
        System.out.println("trace_read_write call_md5 result--->" + result);
        System.out.println("------------------- trace_read_write end -------------------");
    }


    public void trace_code() {
        System.out.println("------------------- trace_code begin -------------------");
        File file = new File("traceCode.txt");
        try {
            PrintStream printStream = new PrintStream(Files.newOutputStream(file.toPath()),true);
            emulator.traceCode(module.base,module.base + module.size).setRedirect(printStream);
        } catch (Exception e) {
            System.out.println(e);
        }
        //主动调用
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        String method = "add(III)I";
        int result = dvmClass.callStaticJniMethodInt(emulator, method, 500, 300, 700);
        System.out.println("trace_code call_add result--->" + result);
        System.out.println("------------------- trace_code end -------------------");
    }



    public static void main(String[] args) {
        UnidbgNativeHelper nativeHelper = new UnidbgNativeHelper();
//        nativeHelper.call_add();
//        nativeHelper.call_md5();
//        nativeHelper.call_encode();
//        nativeHelper.call_findSymbolByName();
//        nativeHelper.call_function();
//        nativeHelper.hook_zz();
//        nativeHelper.hook_zz_inline();
//        nativeHelper.hook_zz_two();
//        nativeHelper.hook_replace();
//        nativeHelper.hook_unicorn();
//        nativeHelper.hook_print_call_stack();
//        nativeHelper.add_debugger();
//        nativeHelper.trace_read_write();
//        nativeHelper.trace_code();
    }
}
