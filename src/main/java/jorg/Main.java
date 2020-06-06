package jorg;

import jorg.jorg.JorgReader;

public class Main {

    public static void main(String[] args) {
        System.out.println(JorgReader.parse("1]2]3]4]0").toString());
    }
}
