package jorg;

import jorg.jorg.*;
import suite.suite.Subject;

public class Main {

    public static class Foo implements Interpreted, Discovered {
        int a;
        int b;
        String str = "@^^";
        Foo foo = null;

        public Foo() {
        }

        public Foo(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public Foo(int a, int b, Foo foo) {
            this.a = a;
            this.b = b;
            this.foo = foo;
        }

        @Override
        public String toString() {
            return super.toString() + "{" +
                    "a=" + a +
                    ", b=" + b +
                    '}';
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        BracketTreeWriter writer = new BracketTreeWriter();
        System.out.println(writer.encode(new Foo(1,2, new Foo(3, 4)))); //TODO czemu @null na a i b w wewnetrznym foo ?
        BracketTreeReader reader = new BracketTreeReader();
        reader.getFactory().setType("foo", Foo.class);
        var $ = reader.parse("a[[@a][d]] b[x[[@a]]] @a[#[foo]a[1]b[2]]").as(Subject.class);

    }
}
