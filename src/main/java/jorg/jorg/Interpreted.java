package jorg.jorg;

import suite.suite.Subject;

public interface Interpreted {
    default void interpret(Subject sub) {
        StandardInterpreter.interpret(this, sub);
    }
}
