package jorg.jorg;

import suite.suite.Subject;

public interface Reformable {
    default void reform(Subject sub) {
        StandardReformer.reform(this, sub);
    }
}
