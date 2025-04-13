package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;


public class PDD extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;



    public PDD() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.pdd").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/pdd/v6.32.0.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/pdd/libpdd_secure.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }

    private void sign() {
        DvmClass dvmClass = vm.resolveClass("com.xunmeng.pinduoduo.secure.DeviceNative");
        String method = "info2(Landroid/content/Context;J)Ljava/lang/String;";
        StringObject resultObject = dvmClass.callStaticJniMethodObject(emulator, method, vm.resolveClass("android/content/Context").newObject(null), 1744545873608L);
        System.out.println(resultObject.getValue());

    }

    public static void main(String[] args) {
        PDD pdd = new PDD();
        pdd.sign();
        System.out.println(System.getProperty("user.dir"));

    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V".equals(signature)) {
            return;
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("android/content/Context->checkSelfPermission(Ljava/lang/String;)I".equals(signature)) {
            return -1;
        }
        if ("android/telephony/TelephonyManager->getSimState()I".equals(signature)) {
            return 5;
        }
        if ("android/telephony/TelephonyManager->getNetworkType()I".equals(signature)) {
            return 13;
        }
        if ("android/telephony/TelephonyManager->getDataState()I".equals(signature)) {
            return 0;
        }
        if ("android/telephony/TelephonyManager->getDataActivity()I".equals(signature)) {
            return 4;
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;".equals(signature)) {
            Object value = varArg.getObjectArg(0).getValue();
            System.out.println(value);
            return vm.resolveClass("android/telephony/TelephonyManager").newObject(null);
        }
        if ("android/telephony/TelephonyManager->getSimOperatorName()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"中国电信");
        }
        if ("android/telephony/TelephonyManager->getSimCountryIso()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm, "cn");
        }
        if ("android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm, "46003");
        }
        if ("android/telephony/TelephonyManager->getNetworkOperatorName()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"中国电信");
        }
        if ("android/telephony/TelephonyManager->getNetworkCountryIso()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"cn");
        }
        if ("java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;".equals(signature)) {
            Throwable throwable = new Throwable();
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            return ProxyDvmObject.createObject(vm,stackTrace);
        }
        if ("java/lang/StackTraceElement->getClassName()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"");
        }
        if ("java/io/ByteArrayOutputStream->toByteArray()[B".equals(signature)) {
            ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream)dvmObject.getValue();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return ProxyDvmObject.createObject(vm,byteArray);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/xunmeng/pinduoduo/secure/EU->gad()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"5da722f0ab2d638c"); //hook得到
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("android/os/Debug->isDebuggerConnected()Z".equals(signature)) {
            return false;
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("java/lang/Throwable-><init>()V".equals(signature)) {
            Throwable throwable = new Throwable();
            return ProxyDvmObject.createObject(vm,throwable);
        }
        if ("java/io/ByteArrayOutputStream-><init>()V".equals(signature)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            return ProxyDvmObject.createObject(vm,byteArrayOutputStream);
        }
        if ("java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V".equals(signature)) {
            try {
                OutputStream chunk = (OutputStream) varArg.getObjectArg(0).getValue();
                GZIPOutputStream obj = new GZIPOutputStream(chunk);
                return vm.resolveClass("java/util/zip/GZIPOutputStream").newObject(obj);
            } catch (Exception e) {
                System.out.println("写入错误1" + e);
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }


    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(signature)) {
            String param1 = (String) vaList.getObjectArg(0).getValue();
            String param2 = (String) vaList.getObjectArg(0).getValue();
            String srcStr = (String) dvmObject.getValue();
            String result = srcStr.replace(param1, param2);
            return new StringObject(vm,result);
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("java/util/zip/GZIPOutputStream->write([B)V".equals(signature)) {
            GZIPOutputStream gzipOutputStream = (GZIPOutputStream)dvmObject.getValue();
            byte[] bytes = (byte[])varArg.getObjectArg(0).getValue();
            try {
                gzipOutputStream.write(bytes);
            } catch (Exception e) {
            }
            return;
        }
        if ("java/util/zip/GZIPOutputStream->finish()V".equals(signature)) {
            GZIPOutputStream gzipOutputStream = (GZIPOutputStream)dvmObject.getValue();
            try {
                gzipOutputStream.finish();
            } catch (Exception e) {
            }
            return;
        }
        if ("java/util/zip/GZIPOutputStream->close()V".equals(signature)) {
            GZIPOutputStream gzipOutputStream = (GZIPOutputStream)dvmObject.getValue();
            try {
                gzipOutputStream.close();
            } catch (Exception e) {
            }
            return;
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }
}

