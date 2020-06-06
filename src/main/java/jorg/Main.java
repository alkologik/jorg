package jorg;

import jorg.jorg.JorgReader;
import suite.suite.Subject;

public class Main {

    public static void main(String[] args) {
        Subject s = JorgReader.parse("]2]3]4[XD");
        System.out.println(s);
    }
}
