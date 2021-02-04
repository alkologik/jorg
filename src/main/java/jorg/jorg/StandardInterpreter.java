package jorg.jorg;

import suite.suite.SolidSubject;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

public class StandardInterpreter {

    public static Subject getAllSupported() {
        return Suite.
                insert(Integer.class, (Action)StandardInterpreter::interpretPrimitive).
                insert(ArrayList.class, (Action)StandardInterpreter::interpretCollection).
                insert(HashSet.class, (Action)StandardInterpreter::interpretCollection).
                insert(HashMap.class, (Action)StandardInterpreter::interpretMap).
                insert(File.class, (Action)StandardInterpreter::interpretFile).
                insert(SolidSubject.class, (Action)StandardInterpreter::interpretSubject)
                ;
    }

    public static Subject interpret(Interpreted interpreted) {
        var $ = Suite.set();
        for(Class<?> aClass = interpreted.getClass(); aClass != Object.class; aClass = aClass.getSuperclass()) {
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                try {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) {
                        field.setAccessible(true);
                        $.setIf(field.getName(), Suite.set(field.get(interpreted)), $::absent);
                    }
                } catch (Exception ex) {
                    System.err.println("Cant get '" + field.getName() + "' from " + aClass);
                }
            }
        }
        return $;
    }

    public static Subject interpretPrimitive(Subject $in) {
        return Suite.set($in.direct().toString());
    }

    public static Subject interpretCollection(Subject $in) {
        Collection<?> collection = $in.asExpected();
        var $ = Suite.set();
        collection.forEach(o -> $.in().set(o));
        return $;
    }

    public static Subject interpretMap(Subject $in) {
        Map<?, ?> map = $in.asExpected();
        var $ = Suite.set();
        map.forEach((key, value) -> $.in().set(key).set(value));
        return $;
    }

    public static Subject interpretSubject(Subject $in) {
        var $ = Suite.set();
        $in.forEach($i -> $.in().set($i.direct()).set($i.in().get()));
        return $;
    }

    public static Subject interpretFile(Subject $in) {
        File file = $in.asExpected();
        return Suite.set(file.getPath());
    }

}
