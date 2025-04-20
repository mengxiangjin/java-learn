package com.nb.demo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import okhttp3.*;
import okio.Buffer;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.nio.charset.Charset;

public class XHS extends AbstractJni {


    public static AndroidEmulator emulator;  // 静态属性，以后对象和类都可以直接使用
    public static Memory memory;
    public static VM vm;
    public static Module module;


    public static Request request;


    // 构造方法,以后这个代码，基本是固定的，只需要改app位置即可，其他不用动
    public XHS() {
        // 1.创建设备（32位或64位模拟器）， 具体看so文件在哪个目录。 在armeabi-v7a就选择32位
        // 传进设备时，如果是32位，后面so文件就要用32位，同理需要用64位的
        // 这个名字可以随便写,一般写成app的包名    以后可能会动
        emulator = AndroidEmulatorBuilder.for32Bit().setProcessName("com.xhs").build();
        // 2.获取内存对象（可以操作内存）
        memory = emulator.getMemory();
        // 3.设置安卓sdk版本（只支持19、23）
        memory.setLibraryResolver(new AndroidResolver(23));

        // 4.创建虚拟机（运行安卓代码需要虚拟机，就想运行py代码需要python解释器一样）    以后会动
        vm = emulator.createDalvikVM(new File("unidbg-0.9.7/apks/xhs/v6.73.0.apk"));
        vm.setJni(this); // 后期补环境会用，把要补的环境，写在当前这个类中，执行这个代码即可，但是必须继承AbstractJni
        //vm.setVerbose(true); //是否展示调用过程的细节

        // 5.加载so文件
        DalvikModule dm = vm.loadLibrary(new File("unidbg-0.9.7/apks/xhs/libshield.so"), false);   // 以后会动
        dm.callJNI_OnLoad(emulator); // jni开发动态注册，会执行JNI_OnLoad，如果是动态注册，需要执行一下这个，如果静态注册，这个不需要执行，车智赢案例是静态注册

        // 6.dm代表so文件，dm.getModule()得到module对象，基于module对象可以访问so中的成员。
        module = dm.getModule(); // 把so文件加载到内存后，后期可以获取基地址，偏移量等，该变量代指so文件




    }



    public void sign() {
        DvmClass dvmClass = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        String method = "initializeNative()V";
        dvmClass.callStaticJniMethod(emulator,method);
    }

    public long initialize(String str) {
        DvmClass dvmClass = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        String method = "initialize(Ljava/lang/String;)J";
        long result = dvmClass.callStaticJniMethodLong(emulator, method, new StringObject(vm, str));
//        System.out.println(result);
        return result;
    }

    public void intercept(long j2) {
        DvmClass dvmClass = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        String method = "intercept(Lokhttp3/Interceptor$Chain;J)Lokhttp3/Response;";
        DvmObject<?> dvmObject = dvmClass.callStaticJniMethodObject(emulator, method, vm.resolveClass("okhttp3/Interceptor$Chain").newObject(null), j2);
    }

    public static void main(String[] args) {
        request = new Request.Builder()
                .url("https://www.xiaohongshu.com/api/sns/v1/system_service/check_code?zone=86&phone=15655549539&code=112233")
                .addHeader("xy-common-params", "fid=174489558010907696d9426c0a9d4592be034529d16a&device_fingerprint=2025041721124389b728ba32dde6c23ba49fda9098452401946d56dff2f2cb&device_fingerprint1=2025041721124389b728ba32dde6c23ba49fda9098452401946d56dff2f2cb&launch_id=1744897972&tz=Asia%2FShanghai&channel=YingYongBao&versionName=6.73.0&deviceId=cbd4f703-1198-3bb3-8edf-5f8b14a338f4&platform=android&sid=session.1744896751298845707272&identifier_flag=0&t=1744898921&project_id=ECFAAF&build=6730157&x_trace_page_current=welcome_page&lang=zh-Hans&app_id=ECFAAF01&uis=light")
                .build();
        if (args.length > 0 ) {
            String method = args[0];
            String url = args[1];
            String commonParams = args[2];
            String content = args[3];
            if (method.equalsIgnoreCase("post")) {
                MediaType mediaType = MediaType.parse("text/plain;charset=utf-8");
                RequestBody requestBody = RequestBody.create(mediaType, content);
                request = new Request.Builder()
                        .url(url)
                        .addHeader("xy-common-params",commonParams)
                        .addHeader("content-type","application/x-www-form-urlencode")
                        .post(requestBody)
                        .build();
            } else {
                request = new Request.Builder()
                        .url(url)
                        .addHeader("xy-common-params",commonParams)
                        .build();
            }
        }

        XHS xhs = new XHS();
        xhs.sign();
        long result = xhs.initialize("main");
        xhs.intercept(result);

    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("java/nio/charset/Charset->defaultCharset()Ljava/nio/charset/Charset;".equals(signature)) {
            Charset charset = Charset.defaultCharset();
            return ProxyDvmObject.createObject(vm,charset);
        }
        if ("com/xingin/shield/http/Base64Helper->decode(Ljava/lang/String;)[B".equals(signature)) {
            String params1 = (String) vaList.getObjectArg(0).getValue();
            byte[] bytes = Base64.decodeBase64(params1);
            return ProxyDvmObject.createObject(vm,bytes);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("com/xingin/shield/http/ContextHolder->sDeviceId:Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm,"cbd4f703-1198-3bb3-8edf-5f8b14a338f4");
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("com/xingin/shield/http/ContextHolder->sAppId:I".equals(signature)) {
            return -319115519;
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public boolean getStaticBooleanField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("com/xingin/shield/http/ContextHolder->sExperiment:Z".equals(signature)) {
            return true;
        }
        return super.getStaticBooleanField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("android/content/Context->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;".equals(signature)) {
            String params1 = (String) vaList.getObjectArg(0).getValue();
//            System.out.println("xml文件名称：" + params1);
            return vm.resolveClass("android/content/SharedPreferences").newObject(null);
        }
        if ("android/content/SharedPreferences->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(signature)) {
            String params1 = (String) vaList.getObjectArg(0).getValue();
//            System.out.println("参数1：" + params1);
            String params2 = (String) vaList.getObjectArg(1).getValue();
//            System.out.println("参数2：" + params2);
            if (params1.equals("main")) {
                return new StringObject(vm,params2);
            }
            if (params1.equals("main_hmac")) {
                return new StringObject(vm,"piP0w7sPh/Z5ZRGSQnh3Lrt8/YrZj5r/Aq2wYUFwSjPzLxSEha86aDRvmT8i17+ozROelXjXAdxWmCXTqjk4tx27DRmTCWKrRjfb0rAFB8PQ6vmNriISvPAb8/6P2lA7");
            }
        }
        if ("okhttp3/Interceptor$Chain->request()Lokhttp3/Request;".equals(signature)) {
            return ProxyDvmObject.createObject(vm,request);
        }
        if ("okhttp3/Request->url()Lokhttp3/HttpUrl;".equals(signature)) {
            Request request1 = (Request)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,request1.url());
        }
        if ("okhttp3/HttpUrl->encodedPath()Ljava/lang/String;".equals(signature)) {
            HttpUrl httpUrl = (HttpUrl)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,httpUrl.encodedPath());
        }
        if ("okhttp3/HttpUrl->encodedQuery()Ljava/lang/String;".equals(signature)) {
            HttpUrl httpUrl = (HttpUrl)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,httpUrl.encodedQuery());
        }
        if ("okhttp3/Request->body()Lokhttp3/RequestBody;".equals(signature)) {
            Request request1 = (Request)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,request1.body());
        }
        if ("okhttp3/Request->headers()Lokhttp3/Headers;".equals(signature)) {
            Request request1 = (Request)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,request1.headers());
        }
        if ("okio/Buffer->writeString(Ljava/lang/String;Ljava/nio/charset/Charset;)Lokio/Buffer;".equals(signature)) {
            Buffer buffer = (Buffer)dvmObject.getValue();
            String content = (String)vaList.getObjectArg(0).getValue();
            Charset charset = (Charset)vaList.getObjectArg(1).getValue();
            Buffer resultBuffer = buffer.writeString(content, charset);
            return ProxyDvmObject.createObject(vm,resultBuffer);
        }
        if ("okhttp3/Headers->name(I)Ljava/lang/String;".equals(signature)) {
            Headers headers = (Headers)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,headers.name(vaList.getIntArg(0)));
        }
        if ("okhttp3/Headers->value(I)Ljava/lang/String;".equals(signature)) {
            Headers headers = (Headers)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,headers.value(vaList.getIntArg(0)));
        }
        if ("okio/Buffer->clone()Lokio/Buffer;".equals(signature)) {
            Buffer buffer = (Buffer)dvmObject.getValue();
            Buffer resultBuffer = buffer.clone();
            return ProxyDvmObject.createObject(vm,resultBuffer);
        }
        if ("okhttp3/Request->newBuilder()Lokhttp3/Request$Builder;".equals(signature)) {
            Request request1 = (Request)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,request1.newBuilder());
        }
        if ("okhttp3/Request$Builder->header(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;".equals(signature)) {
            Request.Builder builder = (Request.Builder)dvmObject.getValue();
            String params1 = (String) vaList.getObjectArg(0).getValue();
            String params2 = (String) vaList.getObjectArg(1).getValue();
            if (params1.equals("shield")) {
                System.out.println(params2);
            }
//            System.out.println("params1:" + params1 + " params2: " + params2);
            return ProxyDvmObject.createObject(vm,builder.header(params1,params2));
        }
        if ("okhttp3/Request$Builder->build()Lokhttp3/Request;".equals(signature)) {
            Request.Builder builder = (Request.Builder)dvmObject.getValue();
            return ProxyDvmObject.createObject(vm,builder.build());
        }
        if ("okhttp3/Interceptor$Chain->proceed(Lokhttp3/Request;)Lokhttp3/Response;".equals(signature)) {
            Interceptor.Chain chain = (Interceptor.Chain)dvmObject.getValue();
            Request request1 = (Request) vaList.getObjectArg(0).getValue();
            try {
                return ProxyDvmObject.createObject(vm,chain.proceed(request1));
            } catch (Exception e) {
                return vm.resolveClass("okhttp3/Response").newObject(null);
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("okhttp3/Headers->size()I".equals(signature)) {
            Headers headers = (Headers)dvmObject.getValue();
            return headers.size();
        }
        if ("okio/Buffer->read([B)I".equals(signature)) {
            Buffer buffer = (Buffer)dvmObject.getValue();
            byte[] bytes = (byte[])vaList.getObjectArg(0).getValue();
            return buffer.read(bytes);
        }
        if ("okhttp3/Response->code()I".equals(signature)) {
            return 200;
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("okio/Buffer-><init>()V".equals(signature)) {
            Buffer buffer = new Buffer();
            return ProxyDvmObject.createObject(vm,buffer);
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }
}
