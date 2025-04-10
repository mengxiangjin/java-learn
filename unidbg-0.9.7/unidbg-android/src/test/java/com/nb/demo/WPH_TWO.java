package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WPH_TWO extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;

    public WPH_TWO() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.wph").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/wph/v7.83.3.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/wph/libkeyinfo.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }

    private void sign() {
        DvmClass dvmClass = vm.resolveClass("com.vip.vcsp.KeyInfo");
        String method = "gsNav(Landroid/content/Context;Ljava.util/Map;Ljava/lang/String;Ljava/lang/Boolean;)Ljava/lang/String;";
        TreeMap<String,String> map = new TreeMap<>();
        map.put("app_name","achievo_ad");
        map.put("app_version","7.83.3");
        map.put("channel","oziq7dxw:::");
        map.put("device","Pixel 2 XL");
        map.put("device_token","1f3fb32d-9f6a-3cb8-86d2-4c8dc359650b");
        map.put("manufacturer","Google");
        map.put("os_version","30");
        map.put("regPlat","0");
        map.put("regid",null);
        map.put("rom","Dalvik/2.1.0 (Linux; U; Android 11; Pixel 2 XL Build/RP1A.201005.004.A1)");
        map.put("skey","6692c461c3810ab150c9a980d0c275ec");
        map.put("status","1");
        map.put("vipruid","");
        map.put("warehouse",null);

        DvmObject contextObject = vm.resolveClass("android/content/Context").newObject(null);
        DvmObject<?> mapObject = ProxyDvmObject.createObject(vm, map);
        StringObject stringObject = new StringObject(vm, "");
        StringObject resultObject = dvmClass.callStaticJniMethodObject(emulator,method,contextObject,mapObject,stringObject,false);
        System.out.println(resultObject.getValue());
    }

    public static void main(String[] args) {
        WPH_TWO wph = new WPH_TWO();
        wph.sign();
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("java/util/TreeMap->entrySet()Ljava/util/Set;".equals(signature)) {
            TreeMap map =(TreeMap) dvmObject.getValue();
            Set set = map.entrySet();
            return ProxyDvmObject.createObject(vm,set);
        }
        if ("java/util/Set->iterator()Ljava/util/Iterator;".equals(signature)) {
            Set set =(Set) dvmObject.getValue();
            Iterator iterator = set.iterator();
            return ProxyDvmObject.createObject(vm,iterator);
        }
        if ("java/util/Iterator->next()Ljava/lang/Object;".equals(signature)) {
            Iterator iterator = (Iterator)dvmObject.getValue();
            Object next = iterator.next();
            return ProxyDvmObject.createObject(vm,next);
        }
        if ("java/util/Map$Entry->getKey()Ljava/lang/Object;".equals(signature)) {
            Map.Entry entry = (Map.Entry)dvmObject.getValue();
            Object key = entry.getKey();
            return ProxyDvmObject.createObject(vm,key);
        }
        if ("java/util/Map$Entry->getValue()Ljava/lang/Object;".equals(signature)) {
            Map.Entry entry = (Map.Entry)dvmObject.getValue();
            Object value = entry.getValue();
            return ProxyDvmObject.createObject(vm,value);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("java/util/Iterator->hasNext()Z".equals(signature)) {
            Iterator iterator =(Iterator) dvmObject.getValue();
            return iterator.hasNext();
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }
}
