package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class DWu extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;

    public DWu() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.dw").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/dw/du-4.74.5.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/dw/libJNIEncrypt.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }



    private void sign() {
        DvmClass dvmClass = vm.resolveClass("com.duapp.aesjni.AESEncrypt");
        String method = "getByteValues()Ljava/lang/String;";
        StringObject byteValuesObject = dvmClass.callStaticJniMethodObject(emulator, method);
        String byteValues = byteValuesObject.getValue();
        System.out.println(byteValues);
        StringBuilder sb = new StringBuilder(byteValues.length());
        for (int i = 0; i < byteValues.length(); i++) {
            if (byteValues.charAt(i) == '0') {
                sb.append("1");
            }else {
                sb.append("0");
            }
        }

        String body = "loginTokenplatformandroidtimestamp1741852561920type1uuid4d46de23c1970252v4.74.5";
        String encodeByteMethod = "encodeByte([BLjava/lang/String;)Ljava/lang/String;";
        StringObject resultObject = dvmClass.callStaticJniMethodObject(emulator,encodeByteMethod,new ByteArray(vm,body.getBytes()),new StringObject(vm,sb.toString()));
        System.out.println(resultObject.getValue());
    }

    public static void main(String[] args) {
        DWu dw = new DWu();
        dw.sign();

        //101001011101110101101101111100111000110100010101010111010001000101100101010010010101110111010011101001011101110101100101001100110000110100011101010111011011001101001101011101010100001101000011
        //knGGXR0bR7LQn4eRCvJsdZ4D96wrRcYi2zPWWxLMOs1LGifdXR7pTKgsBWd5uDIsxdUnez0HG5VIgvs3AzEA/ibOfWq5Gs0i+3tWePKGWk4=
    }
}
