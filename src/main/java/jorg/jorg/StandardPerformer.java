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

public class StandardPerformer {

    public static Subject getAllSupported() {
        return Suite.
                set(ArrayList.class, (Function<Collection<?>, Subject>)StandardPerformer::performCollection).
                set(HashSet.class, (Function<Collection<?>, Subject>)StandardPerformer::performCollection).
                set(HashMap.class, (Function<Map<?, ?>, Subject>)StandardPerformer::performMap).
                set(File.class, (Function<File, Subject>)StandardPerformer::performFile).
                set(SolidSubject.class, (Function<Subject, Subject>)s -> s)
                ;
    }

    public static Subject perform(Performable performable) {
        Subject sub = Suite.add(performable.getClass());
        for(Class<?> aClass = performable.getClass(); aClass != Object.class; aClass = aClass.getSuperclass()) {
            Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                try {
                    int modifiers = field.getModifiers();
                    if (!Modifier.isTransient(modifiers)) {
                        field.setAccessible(true);
                        sub.put(field.getName(), field.get(performable));
                    }
                } catch (Exception ex) {
                    System.err.println("Cant get '" + field.getName() + "' from " + field.getDeclaringClass());
                }
            }
        }
        return sub;
    }

    public static Subject performCollection(Collection<?> collection) {
        Subject sub = Suite.add(collection.getClass()).add(Jorg.terminator);
        collection.forEach(sub::add);
        return sub;
    }

    public static Subject performMap(Map<?, ?> map) {
        Subject sub = Suite.add(map.getClass());
        for(var entry : map.entrySet()) {
            sub.set(entry.getKey(), entry.getValue());
        }
        return sub;
    }

    public static Subject performFile(File file) {
        return Suite.add(File.class).add(file.getPath());
    }

}
