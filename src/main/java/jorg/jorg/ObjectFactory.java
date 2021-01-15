package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.Vendor;
import suite.suite.action.Action;
import suite.suite.util.Series;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.BiConsumer;

public class ObjectFactory {

    Subject $refs = Suite.set();
    Subject $inferredTypes = Suite.set();

    Subject $backedRefs = Suite.set();

    Subject $backed = Suite.set();
    Subject $constructors = Suite.set();
    Subject $classAliases = Suite.set();

    public ObjectFactory(Series $constructors) {
        setConstructors(StandardInterpreter.getAll());
        setConstructors($constructors);
        $classAliases.alter(Suite.
                insert("int", Integer.class).
                insert("double", Double.class).
                insert("float", Float.class).
                insert("list", List.class).
                insert("subject", Subject.class).
                insert("string", String.class)
        );
    }

    public FactoryVendor load(Subject $root) {
        $refs = Suite.set();
        $inferredTypes = Suite.set();
        for(var $1 : Suite.dfs($root)) {
            for(var $ : $1) {
                if($.in().present()) {
                    var str = $.as(String.class, "");
                    if(str.startsWith("$")) {
                        $refs.alter($);
                        $1.unset(str);
                    } else if(str.equals("#")) {
                        $inferredTypes.set($1, inferType($));
                        $1.unset(str);
                    }
                }
            }
        }
        $refs.set("$", $root);
        $refs.alter($backedRefs);
        return new FactoryVendor(this, $root);
    }

    public void setParam(String ref, Object param) {
        if(!ref.startsWith("$")) ref = "$" + ref;
        var $s = Suite.set();
        $backedRefs.set(ref, $s);
        $backed.in($s).set(param);
    }

    public void setConstructor(Class<?> type, Action constructor) {
        $constructors.in(type).set(constructor);
    }

    public void setConstructor(Class<?> type, BiConsumer<Subject, ObjectFactory> constructor) {
        $constructors.in(type).set(constructor);
    }

    public void setConstructors(Series constructors) {
        $constructors.alter(constructors.select(v -> v.is(Class.class) &&
                (v.in().is(Action.class) || v.in().is(BiConsumer.class))
        ));
    }

    public Subject get(Subject $, Class<?> expectedType) {

        if(isReference($)) {
            $ = findReferred($);
        }

        var $v = $backed.in($).get();
        if($v.present()) {
            if($v.is(expectedType) || $v.direct() == null) return $v;
            else  System.err.println("Expected type (" + expectedType +
                    ") is not supertype of backed object type (" + $v.direct().getClass() + ")");
        }

        var $inferredType = $inferredTypes.in($).get();

        if($inferredType.is(Class.class)) {
            Class<?> inferredType = $inferredType.asExpected();
            if(expectedType.isAssignableFrom(inferredType)) {
                $v = construct($, inferredType);
            } else {
                System.err.println("Expected type (" + expectedType +
                        ") is not assignable from inferred type (" + inferredType + ")");
                $v = construct($, expectedType);
            }
        } else {
            $v = construct($, expectedType);
        }

        return $v;
    }

    boolean isReference(Subject $) {
        return $.size() == 1 && $.in().absent() && $.as(String.class, "").startsWith("$");
    }

    Subject findReferred(Subject $) {
        do {
            $ = $refs.in($.asExpected()).get();
        } while (isReference($));
        return $;
    }

    Subject inferType(Subject $) {
        var $type = $.take("#").in().get();
        if($type.is(String.class)) {
            String type = $type.asExpected();
            if($classAliases.in(type).is(Class.class))
                return $classAliases.in(type).get();
            else try {
                return Suite.set(Class.forName(type));
            } catch (ClassNotFoundException e) {
                System.err.println("ObjectFactory: class '" + type + "' not found");
            }
        }
        return Suite.set();
    }

    Subject construct(Subject $, Class<?> type) {
        var $constructor = $constructors.in(type).get();
        if($constructor.present()) {
            if ($constructor.is(Action.class)) {

                Action constructor = $constructor.asExpected();
                var $r = constructor.play(new FactoryVendorRoot(this, $));
                if ($r.present()) {
                    $backed.in($).set($r.direct());
                }
                return $r;
            } else if ($constructor.is(BiConsumer.class)) {
                BiConsumer<Vendor, ObjectFactory> consumer = $constructor.asExpected();
                consumer.accept(new FactoryVendorRoot(this, $), this);
                return $backed.get($);
            }
        } else {
            try {
                Method method = type.getDeclaredMethod("generate", Subject.class);
                if(method.trySetAccessible()) {
                    int modifiers = method.getModifiers();
                    if(Subject.class.isAssignableFrom(method.getReturnType()) && Modifier.isStatic(modifiers)) {
                        var $r = (Subject)method.invoke(null, new FactoryVendorRoot(this, $));
                        if ($r.present()) $backed.in($).set($r.direct());
                        return $r;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            try {
                Method method = type.getDeclaredMethod("generate", Subject.class, ObjectFactory.class);
                if(method.trySetAccessible()) {
                    int modifiers = method.getModifiers();
                    if(Subject.class.isAssignableFrom(method.getReturnType()) && Modifier.isStatic(modifiers)) {
                        var $r = (Subject)method.invoke(null, new FactoryVendorRoot(this, $), this);
                        if ($r.present()) $backed.in($).set($r.direct());
                        return $r;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            if(Interpreted.class.isAssignableFrom(type)) {
                try {
                    Constructor<?> constructor = type.getDeclaredConstructor();
                    Interpreted reformable = (Interpreted)constructor.newInstance();
                    $backed.in($).set(reformable);
                    reformable.interpret(new FactoryVendorRoot(this, $));
                    return Suite.set(reformable);
                } catch (NoSuchMethodException | IllegalAccessException |
                        InstantiationException | InvocationTargetException ignored) {
                    ignored.printStackTrace();
                }
            }
        }
        return Suite.set();
    }

}
