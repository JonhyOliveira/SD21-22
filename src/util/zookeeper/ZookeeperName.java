package util.zookeeper;

public class ZookeeperName {

    private static String name;

    public static void set(String t) {
        name = t;
    }

    public static String get() {
        return name == null ? "" : name;
    }

}
