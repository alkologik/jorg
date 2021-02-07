package jorg;

import jorg.jorg.*;
import suite.suite.Subject;
import suite.suite.Suite;

public class Main {

    public static class Foo implements Interpreted, Discovered {
        int a;
        Foo foo = null;

        public Foo() {
        }

        public Foo(int a, int b) {
            this.a = a;
        }

        public Foo(int a, int b, Foo foo) {
            this.a = a;
            this.foo = foo;
        }

        @Override
        public String toString() {
            return super.toString() + "{" +
                    "a=" + a +
                    '}';
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Foo foo = new Foo(1,2, new Foo(3, 4));
        foo.foo.foo = foo;
        String bt = BracketTree.writer().encode(foo);
        System.out.println(bt);
        BracketTreeReader reader = new BracketTreeReader();
        var $ = reader.parse(bt);
        var $1 = $.as(Subject.class).print();
        System.out.println($1.direct());
        System.out.println($1.in().direct());
//        var $1 = $.as(Foo.class);
//        System.out.println($1);
//        System.out.println($1.foo);
//        System.out.println($1.foo.foo);
//        $1.print();

    }
}
