package jorg.jorg;

import suite.suite.Subject;

public interface Interpreted {
    default Subject interpret() {
        return StandardInterpreter.interpret(this);
    }
}
