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
import java.util.function.Function;

public class ObjectFactory {

    Subject $references = Suite.set();
    Subject $inferredTypes = Suite.set();

    Subject $externalReferences = Suite.set();

    Subject $compositions = Suite.set();
    Subject $composers = Suite.set();
    Subject $classAliases = Suite.set();
    Function<String, Subject> elementaryComposer;

    public ObjectFactory(Series $composers) {
        setComposers(StandardDiscoverer.getAll());
        setComposers($composers);
        $classAliases.alter(Suite.
                insert("int", Integer.class).
                insert("double", Double.class).
                insert("float", Float.class).
                insert("list", List.class).
                insert("subject", Subject.class).
                insert("string", String.class)
        );
        elementaryComposer = str -> Suite.set();
    }

    public FactoryVendor load(Subject $root) {
        $references = Suite.set();
        $inferredTypes = Suite.set();
        for(var $1 : Suite.postDfs(Suite.add($root)).eachIn()) {
            var $hash = $1.take("#");
            if($hash.present()) $inferredTypes.set($1, inferType($hash.in().get()));
            var $at = $1.take("@");
            if($at.present()) $references.set($at.in().direct(), $1);
            $1.unset("@/");
        }
        $references.alter($externalReferences);
        return prepare($root);
    }

    public void setComposition(String ref, Object param) {
        if(ref.startsWith("@")) ref = ref.substring(1);
        var $s = Suite.set();
        $externalReferences.set(ref, $s);
        $compositions.in($s).set(param);
    }

    public void setClassAlias(String alias, Class<?> aClass) {
        $classAliases.in(alias).set(aClass);
    }

    public void setComposer(Class<?> type, Action constructor) {
        $composers.in(type).set(constructor);
    }

    public void setComposer(Class<?> type, BiConsumer<Subject, ObjectFactory> constructor) {
        $composers.in(type).set(constructor);
    }

    public void setComposers(Series composers) {
        $composers.alter(composers.select(v -> v.is(Class.class) &&
                (v.in().is(Action.class) || v.in().is(BiConsumer.class))
        ));
    }

    public void setElementaryComposer(Function<String, Subject> elementaryComposer) {
        this.elementaryComposer = elementaryComposer;
    }

    public Subject get(Subject $, Class<?> expectedType) {

        if (isReference($)) {
            $ = findReferred($);
        }

        var $v = $compositions.in($).get();
        if($v.present()) {
            if($v.is(expectedType) || $v.direct() == null) return $v;
            else  System.err.println("Expected type (" + expectedType +
                    ") is not supertype of backed object type (" + $v.direct().getClass() + ")");
        }

        var $inferredType = $inferredTypes.in($).get();

        if($inferredType.is(Class.class)) {
            Class<?> inferredType = $inferredType.asExpected();
            if(expectedType.isAssignableFrom(inferredType)) {
                $v = compose($, inferredType);
            } else {
                System.err.println("Expected type (" + expectedType +
                        ") is not assignable from inferred type (" + inferredType + ")");
                $v = compose($, expectedType);
            }
        } else {
            $v = compose($, expectedType);
        }

        return $v;
    }

    boolean isReference(Subject $) {
        return $.size() == 1 && $.in().absent() && $.as(String.class, "").startsWith("@");
    }

    Subject findReferred(Subject $) {
        do {
            String str = $.asExpected();
            $ = $references.in(str.substring(1)).get();
        } while (isReference($));
        return $;
    }

    Subject inferType(Subject $) {
        if($.is(String.class)) {
            String type = $.asExpected();
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

    Subject compose(Subject $, Class<?> type) {
        var $composer = $composers.in(type).get();
        if($composer.present()) {
            if ($composer.is(Action.class)) {

                Action constructor = $composer.asExpected();
                var $r = constructor.play(factoryVendorRoot($));
                if ($r.present()) {
                    $compositions.in($).set($r.direct());
                }
                return $r;
            } else if ($composer.is(BiConsumer.class)) {
                BiConsumer<Vendor, ObjectFactory> consumer = $composer.asExpected();
                consumer.accept(factoryVendorRoot($), this);
                return $compositions.in($).get();
            }
        } else {
            try {
                Method method = type.getDeclaredMethod("generate", Subject.class);
                if(method.trySetAccessible()) {
                    int modifiers = method.getModifiers();
                    if(Subject.class.isAssignableFrom(method.getReturnType()) && Modifier.isStatic(modifiers)) {
                        var $r = (Subject)method.invoke(null, factoryVendorRoot($));
                        if ($r.present()) $compositions.in($).set($r.direct());
                        return $r;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            try {
                Method method = type.getDeclaredMethod("generate", Subject.class, ObjectFactory.class);
                if(method.trySetAccessible()) {
                    int modifiers = method.getModifiers();
                    if(Modifier.isStatic(modifiers)) {
                        method.invoke(null, factoryVendorRoot($), this);
                        return $compositions.in($).get();
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            if(Discovered.class.isAssignableFrom(type)) {
                try {
                    Constructor<?> constructor = type.getDeclaredConstructor();
                    Discovered reformable = (Discovered)constructor.newInstance();
                    $compositions.in($).set(reformable);
                    reformable.discover(factoryVendorRoot($));
                    return Suite.set(reformable);
                } catch (NoSuchMethodException | IllegalAccessException |
                        InstantiationException | InvocationTargetException ignored) {
                    System.err.println("Can't create object. Check access modifiers");
                }
            }
        }
        return Suite.set();
    }

    FactoryVendorRoot factoryVendorRoot(Subject $sub) {
        for(var $ : $sub) {
            if($.is(String.class)) {
                String str = $.asExpected();
                if(str.startsWith("@")) {
                    $sub.shift($.direct(), get(findReferred($), Object.class).asExpected());
                } else {
                    var $prim = elementaryComposer.apply(str);
                    if($prim.present()) $sub.shift(str, $prim.direct());
                }
            }
        }
        return new FactoryVendorRoot(this, $sub);
    }

    FactoryVendor prepare(Subject $sub) {
        if(isReference($sub)) $sub = findReferred($sub);
        return new FactoryVendor(this, $sub);
    }
}
