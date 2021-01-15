package jorg;

import jorg.jorg.BracketTreeProcessor;
import jorg.jorg.BracketTreeReader;
import jorg.jorg.Interpreted;
import suite.suite.Suite;

public class Main {

    public static class Foo implements Interpreted {
        int a;
        int b;

        @Override
        public String toString() {
            return "Foo{" +
                    "a=" + a +
                    ", b=" + b +
                    '}';
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
//        Subject s0 = Jorg.withAdapter("a", 15).withAdapter("b", 18.5).parse("[a] #a [b] #b");
//        System.out.println(s0);
//        s0.add("le he he");
//        s0.add(null);
//        s0.set("n", null);
//        s0.set("n1", null);
//        s0.add(null);
//        JorgWriter writer = new JorgWriter();
//        writer.setCompactMode(true);
//        String jorg = writer.encode(s0);
//        System.out.println(jorg + "\n");
//        s0 = Jorg.parse(jorg);
//        System.out.println(s0);
        BracketTreeProcessor bratProcessor = new BracketTreeProcessor();
        var $ = bratProcessor.process("][`]  `a`[`xD`[d]]b[$a]c[][`$a/xD`]");
        System.out.println("RAW:");
        System.out.println(Suite.describe($));
        BracketTreeReader reader = new BracketTreeReader();
        var s1 = reader.parse("#[jorg.Main$Foo] a[1] b[2]");
        System.out.println(s1.direct());
//        System.out.println(s1);
    }
}
