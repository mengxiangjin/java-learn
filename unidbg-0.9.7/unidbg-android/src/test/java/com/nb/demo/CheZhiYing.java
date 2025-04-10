package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class CheZhiYing extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public CheZhiYing() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.che168.autotradercloud").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/che/che3.32.1.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/che/libnative-lib.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件

    }



    public void sign() {
        // 1 找到java中 jni的类 native 类，必须用固定的写法写
        // 只要拿类，就要使用这个方法写，使用resolveClass把它包裹起来，中间用 /  区分
        DvmClass CheckSignUtil = vm.resolveClass("com/autohome/ahkit/jni/CheckSignUtil");

        // 2 找到类中的方法--》固定写法
        // 方法名(参数签名)返回值签名
        String method = "get3desKey(Landroid/content/Context;)Ljava/lang/String;";
        // 3 执行这个方法，传入参数
        //第一个参数是：设备对象
        // 第二个参数是：方法
        // 第三个参数往后的是：方法要传的参数,传参数的具体方式，下小结讲

        StringObject stringObject = new StringObject(vm,"我是字符串类型参数");

        StringObject obj = CheckSignUtil.callStaticJniMethodObject(
                emulator,
                method,
                // 可能会出错--》如果so语言中使用了传入的这个context参数，而我们传的是空，就会报错，但是如果so中只是传了，没有使用，它就不会报错，先尝试传null试试
                vm.resolveClass("android/content/Context").newObject(null)
        );
        // 4 得到结果，打印出来
        String result = obj.getValue();
        System.out.println(result);

    }

    public static void main(String[] args) {
        CheZhiYing cheZhiYing = new CheZhiYing();
        cheZhiYing.sign();
        //appapiche168comappapiche168comap
    }
}
