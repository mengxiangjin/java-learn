package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;


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
        emulator.attach().addBreakPoint(module.base + 0x976L, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("-----> 0x976L 在地址 " + Long.toHexString(address) + " 处拦截");

                // 安全的PC跳转
                try {
                    emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, module.base + 0x994L + 1);
                    System.out.println("成功跳转到: " + Long.toHexString(module.base + 0x994L));
                } catch (Exception e) {
                    System.err.println("跳转失败: " + e.getMessage());
                }
                return true;
            }
        });

        //跳过release
        emulator.attach().addBreakPoint(module.base + 0xA62, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("-----> 0xA62 在地址 " + Long.toHexString(address) + " 处拦截");
                // 安全的PC跳转
                try {
                    emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, module.base + 0xAA4 + 1);
                    System.out.println("成功跳转到: " + Long.toHexString(module.base + 0xAA4));
                } catch (Exception e) {
                    System.err.println("跳转失败: " + e.getMessage());
                }
                return true;
            }
        });
//
        emulator.attach().addBreakPoint(module.base + 0x9d5, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("-----> 0x994 在地址 " + Long.toHexString(address) + " 处拦截");

                String sign = "A1B2C3D4E5F67890A1B2C3D4E5F67890";
                byte[] signBytes = sign.getBytes(StandardCharsets.UTF_8);
                System.out.println("字符串字节长度：" + signBytes.length); // 确认是32

                int mallocSize = signBytes.length + 1;
                MemoryBlock block = emulator.getMemory().malloc(mallocSize, true);
                UnidbgPointer pointer = block.getPointer();
                long strAddr32 = pointer.peer & 0xFFFFFFFFL; // 32位有效地址
                System.out.println("分配的地址：0x" + Long.toHexString(strAddr32));

                // 写入字符串内容
                pointer.write(signBytes);
                // 写入结束符
                pointer.setByte(signBytes.length, (byte) 0);

                // 关键步骤：将地址设置到R11寄存器（v12 = R11）
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R11, strAddr32);
                System.out.println("已将R11设置为：0x" + Long.toHexString(strAddr32));

                // 验证R11设置是否成功
                long r11Value = emulator.getBackend().reg_read(ArmConst.UC_ARM_REG_R11).longValue() & 0xFFFFFFFFL;
                System.out.println("R11当前值：0x" + Long.toHexString(r11Value)); // 应与分配的地址一致

                // 验证内存内容
                byte[] checkBytes = new byte[32];
                pointer.read(0, checkBytes, 0, 32);
                String checkStr = new String(checkBytes, StandardCharsets.UTF_8);
                System.out.println("内存中的字符串：[" + checkStr + "]"); // 应显示完整字符串
                return true;
            }
        });

        DvmClass dvmClass = vm.resolveClass("com/sichuanol/cbgc/util/SignManager");
//        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, "getSign(Ljava/lang/Sting;Ljava/lang/Sting;Ljava/lang/Sting;)Ljava/lang/Sting;",  new StringObject(vm, ""), new StringObject(vm, ""), new StringObject(vm, "1636221462621"));
//        System.out.println("result--->" + dvmObject.getValue());

        String data = "1636221462621";
        StringObject strResult = dvmClass.callStaticJniMethodObject(emulator, "getSign(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", new StringObject(vm, "12"), new StringObject(vm, "34"), new StringObject(vm, data)); // 执行Jni方法
        System.out.println(strResult);
    }



    public static void main(String[] args) {
        UnidbgSignManager signManager = new UnidbgSignManager();
        signManager.call_sign();
    }
}

