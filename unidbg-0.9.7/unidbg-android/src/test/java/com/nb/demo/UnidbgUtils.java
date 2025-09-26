package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import unicorn.ArmConst;

import java.io.File;


/*
* ida中首先会对传入的签名进行校验
*    if ( !is_initialised )   --->  0000A92C 13 B9       CBNZ            R3, loc_A934
        exit(1);
*   过掉此if判断，需要将hook该地址A92C，然后将R3寄存器的值改完1即可
*   result--->08165c94fc606e86613cea591592804bcd8016ec
*
* */
public class UnidbgUtils extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgUtils() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.hoge.android.app.fujian").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM();
        vm.setVerbose(true); //是否展示调用过程的细节


        vm.setJni(this);
        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libm2o_jni.so"), false);
//        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }


    public void call_signature() {

        emulator.attach().addBreakPoint(module.base + 0xA92C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("进来了---->" + Long.toHexString(address));
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R3,1);
                return true;
            }
        });


        DvmClass dvmClass = vm.resolveClass("com.hoge.android.jni.Utils");
        DvmObject<?> result = dvmClass.callStaticJniMethodObject(emulator, "signature(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", "4.0.0","1757324626928imrtsD");
        System.out.println("result--->" + result.getValue());
    }




    public static void main(String[] args) {
        UnidbgUtils utils = new UnidbgUtils();
        utils.call_signature();
    }
}

