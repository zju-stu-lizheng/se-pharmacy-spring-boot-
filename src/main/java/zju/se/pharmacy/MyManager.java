package zju.se.pharmacy;

import java.util.Vector;

public class MyManager {
    public static Vector<String> aid = new Vector<String>();
    public static Vector<String> passwd = new Vector<String>();
    static {
        for (int i = 0; i < 10000; i++) {
            aid.add(String.valueOf(i));
            passwd.add("liuyiyuan123");
        }
        aid.add("2333");
        passwd.add("liuyiyuan123");
        aid.add("888");
        passwd.add("yjj");
    }
}
