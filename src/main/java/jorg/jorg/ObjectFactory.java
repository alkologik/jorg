package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.Vendor;
import suite.suite.action.Action;
import suite.suite.util.Series;

import java.util.function.BiConsumer;

public class ObjectFactory {

    Subject $refs = Suite.set();

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
                insert("float", Float.class)
        );
    }

    public FactoryVendor load(Subject $root) {
        $refs = Suite.set();
        for(var $1 : Suite.dfs($root)) {
            for(var $ : $1) {
                if($.in().present() && $.as(String.class, "").startsWith("$")) {
                    $refs.alter($);
                    $1.unset($.direct());
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

    public Subject get(Subject $) {
        if(isReference($)) {
            $ = findReferred($.asExpected());
        }

        var $v = $backed.get($);
        if($v.present()) return $v;

        Class<?> inferredType = inferType($);

        if(inferredType != null) {
            $v = construct($, inferredType);
            if ($v.present()) return $v;
        }

        return $;
    }

    public Subject get(Subject $, Class<?> expectedType) {

        if(isReference($)) {
            $ = findReferred($.asExpected());
        }

        var $v = $backed.get($);
        if($v.present()) return $v;

        Class<?> inferredType = inferType($);

        if(inferredType != null) {
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

        if($v.present()) return $v;

        return $;
    }

    boolean isReference(Subject $) {
        return $.size() == 1 && $.in().absent() && $.as(String.class, "").startsWith("$");
    }

    Subject findReferred(Subject $) {
        do {
            $ = $refs.get($.asExpected());
        } while (isReference($));
        return $;
    }

    Class<?> inferType(Subject $) {
        var $type = $.take("#").in();
        if($type.is(String.class)) {
            String type = $type.asExpected();
            if($classAliases.in(type).is(Class.class))
                return $classAliases.in(type).asExpected();
            try {
                return Class.forName(type);
            } catch (ClassNotFoundException e) {
                System.err.println("ObjectFactory: class '" + type + "' not found");
            }
        }
        return null;
    }

    Subject construct(Subject $, Class<?> type) {
        var $constructor = $constructors.in(type);
        if($constructor.present()) {
            if($constructor.is(Action.class)) {

                Action constructor = $constructor.asExpected();
                var $r = constructor.play(new FactoryVendor(this, $));
                if($r.present()) {
                    $backed.in($).set($r.direct());
                }
                return $r;
            } else if($constructor.is(BiConsumer.class)) {
                BiConsumer<Vendor, ObjectFactory> consumer = $constructor.asExpected();
                consumer.accept(new FactoryVendor(this, $), this);
                return $backed.get($);
            }
        }
        return Suite.set();
    }

}
