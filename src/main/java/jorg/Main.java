package jorg;

import jorg.jorg.BracketTreeProcessor;
import jorg.jorg.BracketTreeReader;
import jorg.jorg.BracketTreeWriter;
import jorg.jorg.Discovered;
import suite.suite.Subject;
import suite.suite.Suite;

public class Main {

    public static class Foo implements Discovered {
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
        var $ = bratProcessor.process("][']  'a'['xD'[d]]b[$a]c[]['$a[xD]']");
        System.out.println("RAW:");
        System.out.println(Suite.describe($));
        BracketTreeReader reader = new BracketTreeReader();
        reader.withParam("a", 123.99);
        var s1 = reader.parse("#[jorg.Main$Foo] a[#a] b[12]");
        s1.print();
        Foo foo = s1.asExpected();
        System.out.println(foo.a);
//        BracketTreeWriter writer = new BracketTreeWriter();
//        System.out.println("?");
//        System.out.println(writer.encode("ok"));

//        System.out.println(s1);
    }
}
