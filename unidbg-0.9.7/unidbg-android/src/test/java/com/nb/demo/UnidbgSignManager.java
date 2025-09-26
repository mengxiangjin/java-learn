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
* ida中日志输出打印尝试访问已经release的变量导致错误
*   通过跳过改日志打印汇编
*       1、修改PC寄存器的值为下一行地址
*       2、修改日志输出的机器码FF F7 A4 EE 为NOP机器码 即00 BF 00 BF 什么都不做指令
*
* 79EE68C4C011B923EB2DA905553EA045
* */
public class UnidbgSignManager extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgSignManager() {
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


        vm.setJni(this);
        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libwtf.so"), false);
//        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }


    public void call_sign() {
        //写入字符串到内存中
//        String str = "Hello, Unidbg!";
//        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8); // 转为 UTF-8 字节
//        UnidbgPointer pointer = memory.malloc(strBytes.length + 1, false).getPointer();
//        pointer.write(strBytes);
//        emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R11,pointer.peer);

        /*
        * 方式一
        * */
        emulator.attach().addBreakPoint(module.base + 0xABE, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println(Long.toHexString(address));
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, address + 4 + 1);
                return true;
            }
        });


        DvmClass dvmClass = vm.resolveClass("com/sichuanol/cbgc/util/SignManager");

        String data = "1636221462621";
        StringObject strResult = dvmClass.callStaticJniMethodObject(emulator, "getSign(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", new StringObject(vm, "12"), new StringObject(vm, "34"), new StringObject(vm, data)); // 执行Jni方法
        System.out.println(strResult);
    }


    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/sichuanol/cbgc/util/LogShutDown->getAppSign()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"0093CB6721DAF15D31CFBC9BBE3A2B79");
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    public static void main(String[] args) {
        UnidbgSignManager signManager = new UnidbgSignManager();

        /*
        * 方式二
        * */
//        UnidbgPointer pointer = UnidbgPointer.pointer(emulator, module.base + 0xABE);
//        // 读取 4 字节（原指令是 32 位 Thumb 指令，占 4 字节）
//        byte[] originalBytes = new byte[4];
//        pointer.read(0, originalBytes, 0, originalBytes.length); // 从偏移 0 开始读取 4 字节到数组
//        // 转换为无符号十六进制显示
//        System.out.print("原始机器码: ");
//        for (byte b : originalBytes) {
//            System.out.printf("%02X ", b & 0xFF);
//        }
//
//        System.out.println(Arrays.toString(originalBytes));
//
//        byte[] nopBytes = {0x00, (byte)0xBF, 0x00, (byte)0xBF};
//        pointer.write(nopBytes);


        signManager.call_sign();
    }
}

