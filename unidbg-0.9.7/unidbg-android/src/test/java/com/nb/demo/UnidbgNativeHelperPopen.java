package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;
import unicorn.Arm64Const;

import java.io.File;



public class UnidbgNativeHelperPopen implements IOResolver {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgNativeHelperPopen() {
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
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libxiaojianbangC.so"), false);
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
        emulator.getSyscallHandler().addIOResolver(this);
    }


    int count = 0;

//      v0 = getenv("PATH");
    public void call_readSomeThing() {

        //去hook getenv函数，此函数为系统函数存在于libc.so下，打印出给定字符串的环境变量
        emulator.attach().addBreakPoint(module.findSymbolByName("getenv").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                String string = emulator.getContext().getPointerArg(0).getString(0);
                System.out.println("Hooked getenv--->" + string);
                return true;
            }
        });


        /*
        * Hook popen函数打印出参数 getprop ro.product.model
        * */
        emulator.attach().addBreakPoint(module.findSymbolByName("popen").getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                String string = emulator.getContext().getPointerArg(0).getString(0);
                System.out.println("Hooked popen--->" + string);
                return true;
            }
        });

        /*
        * 跳过popen函数的执行
        * */
        emulator.attach().addBreakPoint(module.base + 0x26E4, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                //开始执行00000000000026E4 F3 FB FF 97 BL              .popen
                //result = popen("getprop ro.product.model", "r");  修改PC寄存器跳过该指令
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_PC,address + 4);
                return true;
            }
        });


        emulator.attach().addBreakPoint(module.base + 0x2744, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("0x2744进来了--->" + address);
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_PC,address + 4);
                return true;
            }
        });

        emulator.attach().addBreakPoint(module.base + 0x276C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("0x276C--->" + address);
                if (count == 0) {
                    System.out.println("0x276C count" + count);
                    count++;
                } else {
                    emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_X8,0);
                    System.out.println("0x276C count" + count);
                }
                return true;
            }
        });


        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk/NativeHelper");
        dvmClass.callStaticJniMethodObject(emulator, "readSomething()");
    }



    public static void main(String[] args) {
        UnidbgNativeHelperPopen nativeHelper = new UnidbgNativeHelperPopen();
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
