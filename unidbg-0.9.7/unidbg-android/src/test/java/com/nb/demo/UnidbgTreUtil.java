package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class UnidbgTreUtil {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgTreUtil() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.maihan.tredian").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM();
        vm.setVerbose(true); //是否展示调用过程的细节


        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libtre.so"), false);
//        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }


    public void call_sign() {
        DvmClass dvmClass = vm.resolveClass("com/maihan/tredian/util/TreUtil");
        String data = "app_ver=100&nonce=cw0vd81757051310679&timestamp=1757051310&tzrd=BwzXzSGFyiPstMIVuzTZb7LzTZzbXRJOFzpbQiIaT7v3yDIfKyKLV5KaiR6PxeWBCMDpXow3A" +
                "lphz0wVj0SKvcBfvPXquz2yJmu3k8rkroG5hrXPupk7cnjBYz1Ql+z9wkyGAMZcnlV0jEYsRn3f/2Kz/ZTsL0wSE/B2HEnKg6Ul7QvsJ5XzgfMTZ4fbIp8AG6guZBfBzctfsldUtp4Uv3m5kx" +
                "Zpw+dOaFbZCuoOsJ24UvOvAuKJVCA4H8Z/XOT9qRbmwQ/H2I+Jr57zFUs0Da6iZGEmu61L/s+bH1Qc4EwFH2ap2JKF7WsGxE/M3yYhKbwXjWr4ROrqOdKgTe+TlxBTA6T743hHbZK8DCDOkgU" +
                "ly8VTTUsmDqb0p6yOQytRIBBNEAfIDSgiU3UAgwQVSzjW8+B41dgNldwTzWSuC1rv75XIABNWi8pZNOTn+qw/aRe6wKzc4m+WSA75I+nesk5qtNYvS6upSw9zUM3S8X/sITVMpUfN13+pDU693zUr";

        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, "sign(Ljava/lang/Sting;)Ljava/lang/Sting;", data);
        System.out.println("result--->" + dvmObject.getValue());
    }



    public static void main(String[] args) {
        UnidbgTreUtil unidbgTreUtil = new UnidbgTreUtil();
        unidbgTreUtil.call_sign();
    }
}

//55417a545ca9b818443a7668fb782740b3b1167b
