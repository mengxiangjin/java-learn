package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class HNHK extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;

    public HNHK() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.hnhk").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/hnhk/v9.0.0.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hnhk/libsignature.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }



    private void sign() {
        DvmClass dvmClass = vm.resolveClass("com.rytong.hnair.HNASignature");
        String method = "getHNASignature(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";

        StringObject str = new StringObject(vm,"{}");
        StringObject str2 = new StringObject(vm,"{}");
        StringObject str3 = new StringObject(vm,"{\"akey\":\"184C5F04D8BE43DCBD2EE3ABC928F616\",\"aname\":\"com.rytong.hnair\",\"atarget\":\"standard\",\"aver\":\"9.0.0\",\"did\":\"12a6a371aba0f791\",\"dname\":\"Google_Pixel 2 XL\",\"gtcid\":\"815f8252cd8c6ea58efa58988d7dae17\",\"mchannel\":\"huawei\",\"schannel\":\"AD\",\"slang\":\"zh-CN\",\"sname\":\"google\\/taimen\\/taimen:11\\/RP1A.201005.004.A1\\/6934943:user\\/release-keys\",\"stime\":\"1744274996209\",\"sver\":\"11\",\"system\":\"AD\",\"szone\":\"+0800\",\"abuild\":\"64249\",\"riskToken\":\"67f78617Xv17x7AHF5xE3wgBNBjfQig8oeq5xn03\",\"hver\":\"9.0.0.35417.7ac793f2e.standard\",\"style\":\"roll\",\"type\":\"8\"}");
        StringObject str4 = new StringObject(vm,"21047C596EAD45209346AE29F0350491");
        StringObject str5 = new StringObject(vm,"F6B15ABD66F91951036C955CB25B069F");

        StringObject resultObject = dvmClass.callStaticJniMethodObject(emulator, method, str, str2, str3, str4, str5);
        String result = resultObject.getValue();
        System.out.println(result);
    }

    public static void main(String[] args) {
        HNHK hnhk = new HNHK();
        hnhk.sign();
        //4B8078EF539CC94069F68D79017DAD558350FD54>>64249184C5F04D8BE43DCBD2EE3ABC928F616com.rytong.hnairstandard9.0.012a6a371aba0f791Google_Pixel 2 XL815f8252cd8c6ea58efa58988d7dae179.0.0.35417.7ac793f2e.standardhuawei67f78617Xv17x7AHF5xE3wgBNBjfQig8oeq5xn03ADzh-CNgoogle/taimen/taimen:11/RP1A.201005.004.A1/6934943:user/release-keys1744274996209roll11AD+08008>>F6B15ABD66F91951036C955CB25B069F
    }
}
