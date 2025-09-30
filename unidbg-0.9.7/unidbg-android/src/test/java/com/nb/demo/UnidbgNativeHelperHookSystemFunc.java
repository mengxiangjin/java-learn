package com.nb.demo;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.Arm64Hook;
import com.github.unidbg.arm.ArmHook;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.hook.HookListener;
import com.github.unidbg.memory.SvcMemory;
import com.sun.jna.Pointer;


/*
* 去Hook相关的系统函数，然后在加载so文件的类中去add这个listener即可
*   UnidbgNativeHelperHookSystemFunc unidbgNativeHelperHookSystemFunc = new UnidbgNativeHelperHookSystemFunc(emulator);
    memory.addHookListener(unidbgNativeHelperHookSystemFunc);
*       注意添加时机：需要在loadLibrary之前
* */

public class UnidbgNativeHelperHookSystemFunc implements HookListener {

    private final Emulator<?> emulator;

    public UnidbgNativeHelperHookSystemFunc(Emulator<?> emulator) {
        this.emulator = emulator;
    }

    @Override
    public long hook(SvcMemory svcMemory, String libraryName, String symbolName, final long old) {
        if ("libc.so".equals(libraryName)) {
            if ("getenv".equals(symbolName)) {
                if (emulator.is64Bit()) {
                    return svcMemory.registerSvc(new Arm64Hook() {
                        @Override
                        protected HookStatus hook(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            int index = 0;
                            Pointer pointer = context.getPointerArg(index);
                            String key = pointer.getString(0);
                            System.out.println("UnidbgNativeHelperHookSystemFunc hook到了" + key);
                            return HookStatus.RET(emulator,old);
                        }
                    }).peer;
                } else {
                    return svcMemory.registerSvc(new ArmHook() {
                        @Override
                        protected HookStatus hook(Emulator<?> emulator) {
                            RegisterContext context = emulator.getContext();
                            int index = 0;
                            Pointer pointer = context.getPointerArg(index);
                            String key = pointer.getString(0);
                            System.out.println("UnidbgNativeHelperHookSystemFunc hook到了" + key);
                            return HookStatus.RET(emulator,old);
                        }
                    }).peer;
                }
            }

         }
        return 0;
    }




}
