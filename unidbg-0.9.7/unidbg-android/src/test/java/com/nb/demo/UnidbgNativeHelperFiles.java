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
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.HookEntryInfo;
import com.github.unidbg.hook.hookzz.HookZz;
import com.github.unidbg.hook.hookzz.InstrumentCallback;
import com.github.unidbg.hook.hookzz.WrapCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.SimpleFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;


/*
* ida中关于对访问文件的相关
* v3 = fopen("/proc/self/maps", "r");  访问自身设备的进程相关信息map文件，其中self是动态的，进程的pid
*      ps -ef | grep 包名 查看pid
*   此时unidbg存在默认的maps文件，会正常输出，我们也可进行模拟读取文件操作
*       1、implements IOResolver 重写resolve方法
*       2、emulator.getSyscallHandler().addIOResolver(this); 进行注册文件系统
* */
public class UnidbgNativeHelperFiles implements IOResolver {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgNativeHelperFiles() {
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

        //注意添加时机需要在loadLibrary之前
        UnidbgNativeHelperHookSystemFunc unidbgNativeHelperHookSystemFunc = new UnidbgNativeHelperHookSystemFunc(emulator);
        memory.addHookListener(unidbgNativeHelperHookSystemFunc);

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libxiaojianbangC.so"), false);
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件



        emulator.getSyscallHandler().addIOResolver(this);
    }



//      v0 = getenv("PATH");
    public void call_readSomeThing() {

        //去hook getenv函数，此函数为系统函数存在于libc.so下，打印出给定字符串的环境变量
        emulator.attach().addBreakPoint(module.findSymbolByName("getenv").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                String string = emulator.getContext().getPointerArg(0).getString(0);
                System.out.println("Hooked--->" + string);
                return true;
            }
        });


        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk/NativeHelper");
        dvmClass.callStaticJniMethodObject(emulator, "readSomething()");
    }



    public static void main(String[] args) {
        UnidbgNativeHelperFiles nativeHelper = new UnidbgNativeHelperFiles();
        nativeHelper.call_readSomeThing();
    }

    /*
    * v3 = fopen("/proc/self/maps", "r");
    * */
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        if ("/proc/self/maps".equals(pathname)) {
            //自己构造
//            return FileResult.success(new SimpleFileIO(oflags,new File("unidbg-0.9.7/apks/hookDemo/mymaps"),pathname));
            return FileResult.success(new ByteArrayFileIO(oflags,pathname,"我是自己构造的字符串".getBytes()));
        }
        return null;
    }
}
