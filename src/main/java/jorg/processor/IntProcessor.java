package jorg.processor;

import suite.suite.Subject;
import suite.suite.Suite;

@FunctionalInterface
public interface IntProcessor {

    default Subject ready() {
        return Suite.set();
    }
    void advance(int i)throws ProcessorException;
    default Subject finish()throws ProcessorException {
        return Suite.set();
    }
}
