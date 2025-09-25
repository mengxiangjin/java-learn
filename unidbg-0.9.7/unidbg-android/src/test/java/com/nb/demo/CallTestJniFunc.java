package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.array.IntArray;
import com.github.unidbg.memory.Memory;

import java.io.File;



/*
* 补环境 调用com.xiaojianbang.ndkdemo.MainActivity下的testJniFunc方法
*
* */
public class CallTestJniFunc extends AbstractJni{

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;

    public static NDKDemo ndkDemo;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public CallTestJniFunc() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.xiaojianbang.ndkdemo").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));
        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM();
        vm.setVerbose(true); //是否展示调用过程的细节

        //AbstractJni内部已经写好了一些常用类的常用方法，有些方法我们可以不用亲自补环境
        vm.setJni(this);

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/hookDemo/libxiaojianbangA.so"), false);

        //此函数是静态注册本来不需要调用JNI_OnLoad但是该函数内部调用了全局变量，全局变量中JNI_OnLoad中被赋值，所以需要调用JNI_OnLoad
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件


        ndkDemo = new NDKDemo("this is a ceshi");
    }

    public void call_test_jni_func() {
        DvmClass dvmClass = vm.resolveClass("com.xiaojianbang.ndkdemo.MainActivity");
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, "testJniFunc()");
        System.out.println(dvmObject.getValue());
    }


    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("java/lang/Class->getClassLoader()Ljava/lang/ClassLoader;".equals(signature)) {
            return vm.resolveClass("java/lang/ClassLoader").newObject(dvmObject.getClass().getClassLoader());
        } else if ("com/xiaojianbang/ndkdemo/NDKDemo->privateFunc(Ljava/lang/String;I)Ljava/lang/String;".equals(signature)) {
            String arg1 = ((StringObject) vaList.getObjectArg(0)).getValue();
            int arg2 = vaList.getIntArg(1);
            String result = ndkDemo.privateFunc(arg1, arg2);
            return new StringObject(vm,result);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }


    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo->privateStaticFunc([Ljava/lang/String;)[I".equals(signature)) {
            ArrayObject arrayObject = (ArrayObject)vaList.getObjectArg(0);
            DvmObject<?>[] value = arrayObject.getValue();

            String[] params = new String[3];
            for (int i = 0; i < value.length; i++) {
                params[i] = (String) value[i].getValue();
            }
            int[] result = NDKDemo.privateStaticFunc(params);
            return new IntArray(vm,result);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo->allocObject".equals(signature)) {
            return vm.resolveClass("com/xiaojianbang/ndkdemo/NDKDemo").newObject(new NDKDemo());
        }
        return super.allocObject(vm, dvmClass, signature);
    }


    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo-><init>(Ljava/lang/String;I)V".equals(signature)) {
            StringObject stringObject =(StringObject) vaList.getObjectArg(0);
            int params = vaList.getIntArg(1);
            NDKDemo demo = new NDKDemo(stringObject.getValue(), params);
            return vm.resolveClass("com/xiaojianbang/ndkdemo/NDKDemo").newObject(demo);
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo->privateStaticStringField:Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,NDKDemo.privateStaticStringField);
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }


    @Override
    public void setObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature, DvmObject<?> value) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo->privateStringField:Ljava/lang/String;".equals(signature)) {
            NDKDemo ndkDemo =(NDKDemo) dvmObject.getValue();
            ndkDemo.privateStringField = ((StringObject) value).getValue();
            return;
        }
        super.setObjectField(vm, dvmObject, signature, value);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo->privateStringField:Ljava/lang/String;".equals(signature)) {
            NDKDemo ndkDemo =(NDKDemo) dvmObject.getValue();
            return new StringObject(vm,ndkDemo.privateStringField);
        } else if ("com/xiaojianbang/ndkdemo/NDKDemo->byteArray:[B".equals(signature)) {
            NDKDemo ndkDemo =(NDKDemo) dvmObject.getValue();
            byte[] byteArray = ndkDemo.byteArray;
            return new ByteArray(vm,byteArray);
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/xiaojianbang/ndkdemo/NDKDemo->publicStaticFunc()V".equals(signature)) {
            NDKDemo.publicStaticFunc();
            return;
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    public static void main(String[] args) {
        CallTestJniFunc callTestJniFunc = new CallTestJniFunc();
        callTestJniFunc.call_test_jni_func();
    }
}
 class NDKDemo {
    public static String publicStaticStringField = "this is publicStaticStringField";
    public static String privateStaticStringField = "this is privateStaticStringField";
    public String publicStringField = "this is publicStringField";
    public String privateStringField = "this is privateStringField";
    public byte[] byteArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    public NDKDemo() {
        Log.d("xiaojianbang", "this is ReflectDemo()");
    }

    public NDKDemo(String str) {
        Log.d("xiaojianbang", "this is ReflectDemo(String str)");
    }

    public NDKDemo(String str, int i) {
        Log.d("xiaojianbang", i + " " + str);
        Log.d("xiaojianbang", "this is ReflectDemo(String str, int i)");
    }

    public static void publicStaticFunc() {
        Log.d("xiaojianbang", "this is publicStaticFunc");
    }

    public void publicFunc() {
        Log.d("xiaojianbang", "this is publicFunc");
    }

    public static int[] privateStaticFunc(String[] str) {
        StringBuilder retval = new StringBuilder();
        for (String i : str) {
            retval.append(i);
        }
        Log.d("xiaojianbang", "this is privateStaticFunc: " + retval.toString());
        return new int[]{100, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    public String privateFunc(String str, int i) {
        Log.d("xiaojianbang", i + " this is privateFunc: " + str);
        return "this is from java";
    }
}

class Log {
    public static void d(String tag,String msg) {
        System.out.println(tag + "\t" + msg);
    }
}
