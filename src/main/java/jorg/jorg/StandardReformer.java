package jorg.jorg;

import suite.suite.SolidSubject;
import suite.suite.Subject;
import suite.suite.Suite;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;

public class StandardReformer {

    /*public static Subject getAllSupported() {
        return Suite.
                set(SolidSubject.class, (BiConsumer<Subject, Subject>)StandardReformer::reformSubject).
                set(ArrayList.class, (BiConsumer<Collection<?>, Subject>)StandardReformer::reformCollection).
                set(HashSet.class, (BiConsumer<Collection<?>, Subject>)StandardReformer::reformCollection).
                set(HashMap.class, (BiConsumer<Map<?, ?>, Subject>)StandardReformer::reformMap).
                set(File.class, (BiConsumer<File, Subject>)StandardReformer::reformFile)
                ;
    }

    public static void reform(Reformable reformable, Subject sub) {
        for(Class<?> aClass = reformable.getClass(); aClass != Object.class; aClass = aClass.getSuperclass()) {
            try {
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    if (sub.get(field.getName()).settled()) {
                        field.setAccessible(true);
                        field.set(reformable, sub.get(field.getName()).direct());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void reformSubject(Subject subject, Subject sub) {
        subject.inset(sub);
    }

    public static void reformCollection(Collection<?> collection, Subject sub) {
        sub.forEach(s -> collection.add(s.asExpected()));
    }

    public static void reformMap(Map<?, ?> map, Subject sub) {
        sub.forEach(s -> map.put(s.key().asExpected(), s.asExpected()));
    }

    public static void reformFile(File file, Subject sub) {}

*/
}
