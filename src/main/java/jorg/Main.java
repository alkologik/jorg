package jorg;

import jorg.jorg.BracketTreeProcessor;
import jorg.jorg.BracketTreeReader;
import jorg.processor.ProcessorException;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.selector.Index;

public class Main {

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
        var s1 = reader.parse("[#[int]10] [#[int]22] [#[list[int]][1][2][3]]", Subject.class);
        System.out.println(s1.in().as(Integer.class) + s1.select(1).in().as(Integer.class));
    }
}
