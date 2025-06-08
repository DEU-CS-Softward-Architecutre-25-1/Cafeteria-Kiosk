package common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KioskLoggerFactory {
    private static final StackWalker WALKER;
    public static Logger getLogger() {
        return LoggerFactory.getLogger(WALKER.getCallerClass());
    }

    static {
        WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    }
}
