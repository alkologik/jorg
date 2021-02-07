package jorg.jorg;

import suite.suite.Subject;

import java.io.*;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BracketTree {

    public static Subject read(String filePath) {
        return new BracketTreeReader().read(filePath);
    }

    public static Subject read(File file) {
        return new BracketTreeReader().read(file);
    }

    public static Subject read(InputStream inputStream) {
        return new BracketTreeReader().read(inputStream);
    }

    public static Subject parse(String jorg) {
        return new BracketTreeReader().parse(jorg);
    }

    public static BracketTreeReader reader() {
        return new BracketTreeReader();
    }

    public static boolean write(Object object, String filePath) {
        return new BracketTreeWriter().write(object, filePath);
    }

    public static boolean write(Object object, File file) {
        return new BracketTreeWriter().write(object, file);
    }

    public static boolean write(Object object, URL url) {
        return new BracketTreeWriter().write(object, url);
    }

    public static String encode(Object o) {
        return new BracketTreeWriter().encode(o);
    }

    public static BracketTreeWriter writer() {
        return new BracketTreeWriter();
    }
}
