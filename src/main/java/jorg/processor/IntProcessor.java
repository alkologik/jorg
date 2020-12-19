package jorg.processor;

import suite.suite.Subject;
import suite.suite.Suite;

@FunctionalInterface
public interface IntProcessor {

    default Subject ready() {
        return Suite.set();
    }
    void advance(int i);
    default Subject finish() {
        return Suite.set();
    }

    default Subject process(String str) {
        return process(() -> str.chars().iterator());
    }

    default Subject process(Iterable<Integer> it) {
        ready();
        for(int i : it) {
            advance(i);
        }
        return finish();
    }
}
