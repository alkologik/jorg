package jorg;

import jorg.jorg.BratProcessor;
import jorg.jorg.Linker;
import jorg.jorg.Path;
import jorg.processor.ProcessorException;

public class Main {

    public static void main(String[] args) throws ProcessorException {
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
        BratProcessor bratProcessor = new BratProcessor();
        var $ = bratProcessor.process("a[#c/xD\\od/1]1[xD[od[1]]]b[#1]c[#b]");
        System.out.println($);
        Linker linker = new Linker();
        linker.link($);
        System.out.println($);
    }
}
