package io.frinx.utils.bump.transformer.util;

public class LoggingUtil {


    public static final Error fatal(String message) {
        System.err.println("FATAL: " + message);
        System.exit(1);
        throw new IllegalStateException();
    }

}
