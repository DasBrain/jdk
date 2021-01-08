package java.lang.invoke;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

class PrintHelper {
    private static List<String> messages = null;
    
    static void println(String msg) {
        if (messages == null) {
            PrintStream out = System.out;
            if (out == null) {
                messages = new ArrayList<>();
            } else {
                out.println(msg);
                return;
            }
        }
        messages.add(msg);
    }
    
    static void dump() {
        if (messages != null) {
            for (String msg : messages) {
                System.out.println(msg);
            }
            messages = null;
        }
    }
}
