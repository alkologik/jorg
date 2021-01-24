package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;

import java.lang.reflect.Field;
import java.util.List;

public class StandardDiscoverer {

    public static Subject getAll() {
        return Suite.
                insert(Boolean.class, (Action) StandardDiscoverer::discoverBoolean).
                insert(Integer.class, (Action) StandardDiscoverer::discoverInteger).
                insert(Double.class, (Action) StandardDiscoverer::discoverDouble).
                insert(Float.class, (Action) StandardDiscoverer::discoverFloat).
                insert(Subject.class, (Action) StandardDiscoverer::discoverSubject).
                insert(String.class, (Action) StandardDiscoverer::discoverString).
                insert(Object.class, (Action) StandardDiscoverer::discoverObject).
                insert(List.class, (Action) StandardDiscoverer::discoverList)
                ;
    }

    public static Subject discoverBoolean(Subject $) {
        if($.is(String.class)) {
            String str = $.as(String.class);
            return Suite.set(Boolean.parseBoolean(str) || str.equals("+"));
        }
        if($.is(Boolean.class)) return $;
        return Suite.set(false);

    }

    public static Subject discoverInteger(Subject $) {
        if($.is(String.class)) return Suite.set(Integer.parseInt($.as(String.class)));
        if($.is(Number.class)) return Suite.set($.as(Number.class).intValue());
        return Suite.set();
    }

    public static Subject discoverDouble(Subject $) {
        if($.is(String.class)) return Suite.set(Double.parseDouble($.as(String.class)));
        if($.is(Number.class)) return Suite.set($.as(Number.class).doubleValue());
        return Suite.set();
    }

    public static Subject discoverFloat(Subject $) {
        if($.is(String.class)) return Suite.set(Float.parseFloat($.as(String.class)));
        if($.is(Number.class)) return Suite.set($.as(Number.class).floatValue());
        return Suite.set();
    }

    public static Subject discoverString(Subject $) {
        String str = $.as(String.class, "");
        boolean cutFront = str.startsWith("`"), cutBack = str.endsWith("`");
        return Suite.set(cutFront ? cutBack ? str.substring(1, str.length() - 1) : str.substring(1) :
                cutBack ? str.substring(0, str.length() - 1) : str);
    }

    public static Subject discoverObject(Subject $) {
        if($.is(String.class)) return discoverString($);

        return $.getFirst();
    }

    public static Subject discoverSubject(Subject $) {
        return Suite.alter($);
    }

    public static Subject discoverList(Subject $) {
        return Suite.set($.eachIn().eachDirect().toList());
    }

    public static void discover(Discovered reformable, Subject $) {
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

