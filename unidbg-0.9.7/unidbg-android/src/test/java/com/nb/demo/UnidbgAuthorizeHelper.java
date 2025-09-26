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
*   if ( ((unsigned __int8)signatureChecked((int)a1, a2, a3, a6) ^ 1) & 0xFF )
        return _JNIEnv::NewStringUTF(v13, "Illegal signature");
*   找到signatureChecked的机器码地址，修改PC寄存器跳转到下一个地址，不执行此方法
*   同时需要把返回值寄存器R0置为1，让其不走到if判断里面去
*   result--->Cl7ihG9LlYOBQgbsDiFoa6diVoI=
* */
public class UnidbgAuthorizeHelper extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public UnidbgAuthorizeHelper() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.mfw.roadbook").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM();
        vm.setVerbose(true); //是否展示调用过程的细节


        vm.setJni(this);
        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libmfw.so"), false);
//        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }


    public void call_xAuthencode() {
        DvmClass dvmClass = vm.resolveClass("com.mfw.tnative.AuthorizeHelper");


        emulator.attach().addBreakPoint(module.base + 0x914C, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                System.out.println("进来了---->" + Long.toHexString(address));
                //不去执行 0000914C FF F7 C8 FE BL      signatureChecked 此方法 同时把寄存器置为0
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC,address + 4 + 1);
                //函数执行的返回结果寄存器UC_ARM_REG_R0 置为1
                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0,1);
                return true;
            }
        });




        DvmObject<?> dvmObject = vm.resolveClass("android.content.Context").newObject(null);
        String params2 = "PUT&https%3A%2F%2Fmapi.mafengwo.cn%2Frest%2Fapp%2Fuser%2Flogin%2F&after_style%3Ddefault%26app_code%3Dcom.mfw.roadbook%26app_ver%3D8.1.6%26app_version_code%3D535%26brand%3Dgoogle%26channel_id%3DGROWTH-WAP-LC-3%26device_id%3D9E%253AAB%253A9F%253AAC%253A40%253A41%26device_type%3Dandroid%26hardware_model%3DPixel%25202%2520XL%26mfwsdk_ver%3D20140507%26o_lat%3D31.837912%26o_lng%3D117.134873%26oauth_consumer_key%3D5%26oauth_nonce%3D7be11328-cc8b-48dd-b6f4-bcdf2f56cb23%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1757064754%26oauth_version%3D1.0%26open_udid%3D9E%253AAB%253A9F%253AAC%253A40%253A41%26put_style%3Ddefault%26screen_height%3D2712%26screen_scale%3D3.5%26screen_width%3D1440%26sys_ver%3D11%26time_offset%3D480%26x_auth_mode%3Dclient_auth%26x_auth_password%3D123456%26x_auth_username%3D15655549599";
        DvmObject<?> result = dvmClass.callStaticJniMethodObject(emulator, "xAuthencode(Landroid.content.Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;", dvmObject, params2, "", "com.mfw.roadbook", true);
        System.out.println("result--->" + result.getValue());
    }




    public static void main(String[] args) {
        UnidbgAuthorizeHelper authorizeHelper = new UnidbgAuthorizeHelper();
        authorizeHelper.call_xAuthencode();
    }
}

