package tp1.impl.servers.common.dropbox.util;

public class Preserve {

    private static boolean preserve;

    public static void set(boolean t) {
        preserve = t;
    }

    public static boolean get() {
        return preserve;
    }

}
