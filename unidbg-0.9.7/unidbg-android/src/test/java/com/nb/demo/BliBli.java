package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;


public class BliBli extends AbstractJni {

    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;



    public BliBli() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.blibli").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/blibli/v6240.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/blibli/libbili.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件
    }

    private void sign() {
        DvmClass dvmClass = vm.resolveClass("com.bilibili.nativelibrary.LibBili");
        String method = "s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;";
        SortedMap<String,String> map = new TreeMap<>();
        map.put("actual_played_time","0");
        map.put("aid","1501630152");
        map.put("appkey","1d8b6e7d45233436");
        map.put("auto_play","0");
        map.put("build","6240300");
        map.put("c_locale","zh-Hans_CN");
        map.put("channel","xxl_gdt_wm_253");
        map.put("cid","1467056786");
        map.put("epid","0");
        map.put("epid_status","");
        map.put("from","3");
        map.put("from_spmid","search.search-result.0.0");
        map.put("last_play_progress_time","0");
        map.put("list_play_time","0");
        map.put("max_play_progress_time","0");
        map.put("mid","0");
        map.put("miniplayer_play_time","0");
        map.put("mobi_app","android");
        map.put("network_type","1");
        map.put("paused_time","0");
        map.put("platform","android");
        map.put("play_status","0");
        map.put("play_type","1");
        map.put("played_time","0");
        map.put("quality","32");
        map.put("s_locale","zh-Hans_CN");
        map.put("session","a93e86ca7c337f8809a7a8e88df8f1620573b7bf");
        map.put("sid","0");
        map.put("spmid","main.ugc-video-detail.0.0");
        map.put("start_ts","0");
        map.put("statistics","{\"appId\":1,\"platform\":3,\"version\":\"6.24.0\",\"abtest\":\"\"}");
        map.put("sub_type","0");
        map.put("total_time","0");
        map.put("type","3");
        map.put("user_status","0");
        map.put("video_duration","2887");
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, method, ProxyDvmObject.createObject(vm, map));
        System.out.println(dvmObject.getValue().toString());


    }

    public static void main(String[] args) {
        BliBli wph = new BliBli();
        wph.sign();
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;".equals(signature)) {
            Map map = (Map)dvmObject.getValue();
            Object result = map.get(varArg.getObjectArg(0).getValue());
            return ProxyDvmObject.createObject(vm,result);
        }
        if ("java/util/Map->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(signature)) {
            Map map = (Map)dvmObject.getValue();
            Object result = map.put(varArg.getObjectArg(0).getValue(),varArg.getObjectArg(1).getValue());
            return ProxyDvmObject.createObject(vm,result);
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        if ("java/util/Map->isEmpty()Z".equals(signature)) {
            Map map = (Map)dvmObject.getValue();
            return map.isEmpty();
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;".equals(signature)) {
//            String sign=(String) varArg.getObjectArg(1).getValue();
            Map map=(Map) varArg.getObjectArg(0).getValue();
            String result = SignedQuery.r(map);
            return new StringObject(vm,result);
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if ("com/bilibili/nativelibrary/SignedQuery-><init>(Ljava/lang/String;Ljava/lang/String;)V".equals(signature)) {
            String str = (String) varArg.getObjectArg(0).getValue();
            String sign = (String) varArg.getObjectArg(1).getValue();
            System.out.println(str);
            System.out.println(sign);
            SignedQuery signedQuery = new SignedQuery(str, sign);
            return vm.resolveClass("com/bilibili/nativelibrary/SignedQuery").newObject(signedQuery);
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }


}

class SignedQuery {

    /* renamed from: c  reason: collision with root package name */
    private static final char[] f15567c = "0123456789ABCDEF".toCharArray();
    public final String a;
    public final String b;

    public SignedQuery(String str, String str2) {
        this.a = str;
        this.b = str2;
    }

    private static boolean a(char c3, String str) {
        return (c3 >= 'A' && c3 <= 'Z') || (c3 >= 'a' && c3 <= 'z') || !((c3 < '0' || c3 > '9') && "-_.~".indexOf(c3) == -1 && (str == null || str.indexOf(c3) == -1));
    }

    static String b(String str) {
        return c(str, null);
    }

    static String c(String str, String str2) {
        StringBuilder sb = null;
        if (str == null) {
            return null;
        }
        int length = str.length();
        int i = 0;
        while (i < length) {
            int i2 = i;
            while (i2 < length && a(str.charAt(i2), str2)) {
                i2++;
            }
            if (i2 == length) {
                if (i == 0) {
                    return str;
                }
                sb.append((CharSequence) str, i, length);
                return sb.toString();
            }
            if (sb == null) {
                sb = new StringBuilder();
            }
            if (i2 > i) {
                sb.append((CharSequence) str, i, i2);
            }
            i = i2 + 1;
            while (i < length && !a(str.charAt(i), str2)) {
                i++;
            }
            try {
                byte[] bytes = str.substring(i2, i).getBytes("UTF-8");
                int length2 = bytes.length;
                for (int i3 = 0; i3 < length2; i3++) {
                    sb.append('%');
                    sb.append(f15567c[(bytes[i3] & 240) >> 4]);
                    sb.append(f15567c[bytes[i3] & 15]);
                }
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }
        return sb == null ? str : sb.toString();
    }

    static String r(Map<String, String> map) {
        if (!(map instanceof SortedMap)) {
            map = new TreeMap(map);
        }
        StringBuilder sb = new StringBuilder(256);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!key.isEmpty()) {
                sb.append(b(key));
                sb.append("=");
                String value = entry.getValue();
                sb.append(value == null ? "" : b(value));
                sb.append("&");
            }
        }
        int length = sb.length();
        if (length > 0) {
            sb.deleteCharAt(length - 1);
        }
        if (length == 0) {
            return null;
        }
        return sb.toString();
    }

    public String toString() {
        String str = this.a;
        if (str == null) {
            return "";
        }
        if (this.b == null) {
            return str;
        }
        return this.a + "&sign=" + this.b;
    }
}
