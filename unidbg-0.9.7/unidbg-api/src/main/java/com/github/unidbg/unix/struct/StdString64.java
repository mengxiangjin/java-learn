package com.github.unidbg.unix.struct;

import com.github.unidbg.Emulator;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;

import java.util.Arrays;
import java.util.List;

/**
 * Note: Only compatible with libc++, though libstdc++'s std::string is a lot simpler.
 */
public class StdString64 extends StdString {

    StdString64(Pointer p) {
        super(p);
        unpack();
    }

    public byte isTiny;
    public long size;
    public long value;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("isTiny", "size", "value");
    }

    @Override
    public Pointer getDataPointer(Emulator<?> emulator) {
        boolean isTiny = (this.isTiny & 1) == 0;
        if (isTiny) {
            return getPointer().share(1);
        } else {
            return UnidbgPointer.pointer(emulator, value);
        }
    }

    @Override
    public long getDataSize() {
        boolean isTiny = (this.isTiny & 1) == 0;
        if (isTiny) {
            return (this.isTiny & 0xff) >> 1;
        } else {
            return size;
        }
    }
}
