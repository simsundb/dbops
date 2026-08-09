package com.sunzh.sync;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Ensures child sync processes write redirected logs in UTF-8. */
final class ConsoleEncoding {
    private ConsoleEncoding() {
    }

    static void configureUtf8() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
    }
}