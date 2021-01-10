package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;

public class StandardInterpreter {

    public static Subject getAll() {
        return Suite.
                insert(Boolean.class, (Action)StandardInterpreter::makeBoolean).
                insert(Integer.class, (Action)StandardInterpreter::makeInteger).
                insert(Double.class, (Action)StandardInterpreter::makeDouble).
                insert(Float.class, (Action)StandardInterpreter::makeFloat).
                insert(Subject.class, (Action)StandardInterpreter::makeSubject)
                ;
    }

    public static Subject makeBoolean(Subject $) {
        if($.absent()) return Suite.set(false);
        String str = $.as(String.class);

        return Suite.set(Boolean.parseBoolean(str) || str.equals("+"));
    }

    public static Subject makeInteger(Subject $) {
        if($.absent()) return Suite.set();
        String str = $.asExpected();

        return Suite.set(Integer.parseInt(str));
    }

    public static Subject makeDouble(Subject $) {
        if($.absent()) return Suite.set();
        String str = $.asExpected();

        return Suite.set(Double.parseDouble(str));
    }

    public static Subject makeFloat(Subject $) {
        if($.absent()) return Suite.set();
        String str = $.asExpected();

        return Suite.set(Float.parseFloat(str));
    }

    public static Subject makeSubject(Subject $) {
        return Suite.set(Suite.alter($));
    }
}

