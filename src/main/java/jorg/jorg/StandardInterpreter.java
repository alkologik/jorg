package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;

import java.lang.reflect.Field;
import java.util.List;

public class StandardInterpreter {

    public static Subject getAll() {
        return Suite.
                insert(Boolean.class, (Action)StandardInterpreter::makeBoolean).
                insert(Integer.class, (Action)StandardInterpreter::makeInteger).
                insert(Double.class, (Action)StandardInterpreter::makeDouble).
                insert(Float.class, (Action)StandardInterpreter::makeFloat).
                insert(Subject.class, (Action)StandardInterpreter::makeSubject).
                insert(String.class, (Action)StandardInterpreter::makeString).
                insert(Object.class, (Action)StandardInterpreter::makeObject).
                insert(List.class, (Action)StandardInterpreter::makeList)
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

    public static Subject makeString(Subject $) {
        String str = $.as(String.class, "");
        boolean cutFront = str.startsWith("`"), cutBack = str.endsWith("`");
        return Suite.set(cutFront ? cutBack ? str.substring(1, str.length() - 1) : str.substring(1) :
                cutBack ? str.substring(0, str.length() - 1) : str);
    }

    public static Subject makeObject(Subject $) {
        if($.is(String.class)) return makeString($);

        return $.getFirst();
    }

    public static Subject makeSubject(Subject $) {
        var $r = Suite.set();
        var c = $.cascade();
        for(var $1 : c.toEnd()) {
            if($1.is(Suite.Auto.class)) {
                Object key = $1.in().orDo(Suite.Auto::new);
                System.out.println(key);
                var $2 = c.hasNext() ? c.next() : Suite.set();
                if($2.is(Suite.Auto.class)) $r.set(key, $2.in().as(Subject.class));
                else $r.set(key);
            } else {
                $r.set($1.direct(), $1.in().as(Subject.class));
            }
        }
        return Suite.set($r);
    }

    public static Subject makeList(Subject $) {
        return Suite.set($.eachIn().eachDirect().toList());
    }

    public static void interpret(Interpreted reformable, Subject $) {
        for(Class<?> aClass = reformable.getClass(); aClass != Object.class; aClass = aClass.getSuperclass()) {
            try {
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    if ($.present(field.getName())) {
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();
                        if(fieldType.isPrimitive()) {
                            if(fieldType.equals(int.class)) {
                                field.setInt(reformable, $.in(field.getName()).as(Integer.class, 0));
                            }
                        } else {
                            field.set(reformable, $.in(field.getName()).as(fieldType, null));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

