package jorg.jorg;

import suite.suite.Subject;

public interface Performable {
    default Subject perform() {
        return StandardPerformer.perform(this);
    }
}
