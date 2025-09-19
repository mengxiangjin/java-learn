package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class NativeHelper{

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public NativeHelper() {
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
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libxiaojianbang.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件

    }

    public void call_add() {
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");

        String method = "add(III)I";
        int result = dvmClass.callStaticJniMethodInt(emulator, method, 5, 6, 7);
        System.out.println("call_add result--->" + result);
    }


    public void call_md5() {
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndk.NativeHelper");
        String method  ="md5(Ljava/lang/String;)Ljava/lang/String;";
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, method, "123456789");
        String result  = (String) dvmObject.getValue();
        System.out.println("call_md5 result--->" + result);
    }

    public static void main(String[] args) {
        NativeHelper nativeHelper = new NativeHelper();
        nativeHelper.call_add();
        nativeHelper.call_md5();
    }
}
