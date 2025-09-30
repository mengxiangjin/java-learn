package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import unicorn.Arm64Const;
import unicorn.ArmConst;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/*
* 二种方式去得到返回值result
*   1、直接去补环境整个MD5方法
*
*   2、知道MD5里面的主要逻辑后，其实里面前面做了一系列的校验判断，第一种方式补环境过于麻烦，可直接挑出重点函数的地址以及参数，直接一步步调用最后得出返回值即可
* */
public class UnidbgEncryptlibUtils extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgEncryptlibUtils() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.pocket.snh48.activity").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM();
        vm.setVerbose(true); //是否展示调用过程的细节


        vm.setJni(this);
        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libencryptlib.so"), false);
//        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }


    public void call_MD5() {
        String sha1 = "F44B9F1A173564CD686D7FBF6022235EBFE49C4";
        final UnidbgPointer pointer = emulator.getMemory().malloc(sha1.length() + 1, false).getPointer();
        pointer.write(sha1.getBytes(StandardCharsets.UTF_8));
        pointer.setByte(sha1.length(),(byte) 0);

        /*
            v11 = getSha1();
        * 000000000001E670 18 93 00 94 BL              .getSha1
        *   修改PC寄存器跳过getSha1方法的执行，同时构造返回值写入到X0中,返回给v11
        * */
        emulator.attach().addBreakPoint(module.base + 0x1E670, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("进来了---->" + Long.toHexString(address));
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_PC,address + 4);
                System.out.println("跳过getSha1函数体--->开始构造getSha1返回值");
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_X0,pointer.peer);
                return true;
            }
        });

        /*
        * getSha1函数代码体的起始地址，用于判断getSha1是否真正跳过了，正确即不会打印输出
        * */
        emulator.attach().addBreakPoint(module.base + 0x1DF2C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("getSha1进来了---->" + Long.toHexString(address));
                return true;
            }
        });


        /*
        *   v11 = getSha1();  //上面返回的储存在寄存器X0里面的值
            v12 = strcmp(v11, app_sha1); //X0与X1参数，若是常规的int类型，则为w0，w1 这是ARM64架构决定的
        * 000000000001E680 FC 92 00 94 BL              .strcmp
        *
        * */
        emulator.attach().addBreakPoint(module.base + 0x1E680, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("strcmp进来了---->" + Long.toHexString(address));
                Number number = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_X0);
                Number number1 = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_X1);

                byte[] datas = new byte[100];
                memory.pointer(number.longValue()).read(0,datas,0,datas.length);
                System.out.println("读取比较值1" + Arrays.toString(datas));

                byte[] datas1 = new byte[100];
                memory.pointer(number1.longValue()).read(0,datas1,0,datas1.length);
                System.out.println("读取比较值2" + Arrays.toString(datas1));
                return true;
            }
        });



        /*
            if ( !v12 )
        *   000000000001E688 60 01 00 34 CBZ             W0, loc_1E6B4
        *   CBZ：是0即跳转到loc_1E6B4处  v12是strcmp返回的int结果，需要修改w0=0将其跳转到loc_1E6B4
        * */
        emulator.attach().addBreakPoint(module.base + 0x1E688, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("app签名比较---->" + Long.toHexString(address));
                long l = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_W0).longValue();
                emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W0,0);
                System.out.println("强行写入0到W0中---->" + Long.toHexString(address));
                return true;
            }
        });


        /*
        * MD5_CTX::MakePassMD5(&v50, v38, v39, Result);
        * 000000000001E94C 7D 92 00 94 BL              ._ZN7MD5_CTX11MakePassMD5EPhjS0_ ; MD5_CTX::MakePassMD5(uchar *,uint,uchar *)
        * hook该地址并读取该明文
        * */
        emulator.attach().addBreakPoint(module.base + 0x1E94C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("获取MD5的明文---->" + Long.toHexString(address));
                long l = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_X1).longValue();

                byte[] datas = new byte[100];
                memory.pointer(l).read(0,datas,0,datas.length);
                System.out.println("读取明文：" + bytesToAscii(datas));
                return true;
            }
        });

        DvmClass dvmClass = vm.resolveClass("com.pocket.snh48.base.net.utils.EncryptlibUtils");


        String params1 = "1758879818020";
        String params2 = "60dff660611c47c4b0d8bb6a0569817c";
        String params3 = "";

        DvmObject<?> dvmObject = vm.resolveClass("android.content.Context").newObject(null);
        DvmObject<?> result = dvmClass.callStaticJniMethodObject(emulator, "MD5(Landroid.content.Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", dvmObject, params1,params2, params3);
        System.out.println("result--->" + result.getValue());
    }


    /*
    * 直接开始手动调用相关函数获取结果
    * */
    public void call_MD5(boolean constructor) {
        byte[] datas = new byte[200];

        //手动调用MD5初始化
        UnidbgPointer md5CTX = emulator.getMemory().malloc(200, false).getPointer();
        md5CTX.read(0,datas,0,datas.length);
        System.out.println(Arrays.toString(datas));
        module.callFunction(emulator,0x1EAD0,md5CTX);
        md5CTX.read(0,datas,0,datas.length);
        System.out.println(Arrays.toString(datas));

        //手动调用MD5加密，构造函数传入参数
        String mingwen = "1758879818020" + "60dff660611c47c4b0d8bb6a0569817c";
        UnidbgPointer pointer = memory.malloc(mingwen.length(), false).getPointer();
        pointer.write(mingwen.getBytes());

        UnidbgPointer resultPointer = emulator.getMemory().malloc(200, false).getPointer();

        module.callFunction(emulator,0x1FA38,md5CTX,pointer,mingwen.length(),resultPointer);
        byte[] resultBytes = new byte[200];
        resultPointer.read(0,resultBytes,0,resultBytes.length);
        System.out.println("手动调用函数--->" + bytesToAscii(resultBytes));
    }


    public static void main(String[] args) {
        UnidbgEncryptlibUtils encryptlibUtils = new UnidbgEncryptlibUtils();
        encryptlibUtils.call_MD5();
        encryptlibUtils.call_MD5(true);
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 将byte转换为无符号int（0-255）
            int unsignedByte = b & 0xFF;
            // 转换为两位十六进制（不足两位补0）
            String hexByte = Integer.toHexString(unsignedByte);
            if (hexByte.length() == 1) {
                hex.append('0');
            }
            hex.append(hexByte);
        }
        return hex.toString();
    }

    /**
     * 字节数组→ASCII字符串（截止到第一个0）
     */
    private static String bytesToAscii(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder ascii = new StringBuilder();
        for (byte b : bytes) {
            // 遇到0（\0）则结束
            if (b == 0) {
                break;
            }
            // 转换为ASCII字符（仅显示可打印字符，0-127）
            if (b >= 32 && b <= 126) {
                ascii.append((char) b);
            } else {
                ascii.append(String.format("\\x%02x", b)); // 不可打印字符显示为\xXX
            }
        }
        return ascii.toString();
    }
}

