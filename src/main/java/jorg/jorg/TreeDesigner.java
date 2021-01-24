package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;
import suite.suite.util.Series;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class TreeDesigner {

    static class Xray {
        Object o;
        int usages;
        String refId;

        public Xray(Object o) {
            this.o = o;
            usages = 0;
        }

        @Override
        public boolean equals(Object o1) {
            if(o == o1) return true;
            if (this == o1) return true;
            if (o1 == null || getClass() != o1.getClass()) return false;
            Xray xray = (Xray) o1;
            return o == xray.o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(o);
        }

        @Override
        public String toString() {
            return "$" + refId;
        }
    }

    Subject $refs = Suite.set();
    Subject $decompositions = Suite.set();

    Subject $decomposers = Suite.set();
    Subject $classAliases = Suite.set();

    public TreeDesigner() {
        setDecomposers(StandardInterpreter.getAllSupported());
        $classAliases.alter(Suite.
                insert(Integer.class, "int").
                insert(int.class, "int").
                insert(Double.class, "double").
                insert(double.class, "double").
                insert(Float.class, "float").
                insert(float.class, "float").
                insert(List.class, "list").
                insert(Subject.class, "subject").
                insert(String.class, "string")
        );
    }

    public Subject load(Object o) {
        $refs = Suite.set();
        var xray = xray(o);
        $decompositions.print();
        var $xRoot = $refs.in(xray).get();
        int id = 1;
        for(var $i : Suite.dfs($xRoot)) {
            if(isLeaf($i) && $i.is(Xray.class)) {
                Xray x = $i.as(Xray.class);
                if(x.usages < 2) {
                    $i.unset().alter($refs.in(x).get());
                } else {
                    x.refId = "" + id++;
                    $xRoot.set(x, $refs.in(x).get());
                }
            }
        }
        return $xRoot;
    }

    public void setDecomposition(Object o, Subject $) {
        $decompositions.set(o, $);
    }

    public void setDecomposer(Class<?> type, Action decomposer) {
        $decomposers.in(type).set(decomposer);
    }

    public<T> void setDecomposer(Class<T> type, BiConsumer<T, TreeDesigner> decomposer) {
        $decomposers.in(type).set(decomposer);
    }

    public void setDecomposers(Series $decomposers) {
        this.$decomposers.alter($decomposers.select(v -> v.is(Class.class) &&
                (v.in().is(Action.class) || v.in().is(BiConsumer.class))
        ));
    }

    Xray xray(Object o) {
        Xray xray = $refs.getFilled(new Xray(o)).asExpected();
        if(xray.usages++ < 1) {
            var $ = decompose(o);
            $decompositions.print();
            $refs.set(xray, $);
//            for(var $i : Suite.dfs($)) {
//                if(isLeaf($i)) {
//                    var in = $i.direct();
//                    $i.shift(in, xray(in));
//                }
//            }
        }

        return xray;
    }

    boolean isLeaf(Subject $) {
        return $.size() == 1 && $.in().absent();
    }

    Subject decompose(Object o) {
        Class<?> type = o.getClass();

        if($decompositions.present(o)) return $decompositions.in(o).get();

        var $decomposer = $decomposers.in(type).get();
        if($decomposer.present()) {
            if ($decomposer.is(Action.class)) {

                Action decomposer = $decomposer.asExpected();
                var $r = decomposer.play(Suite.set(o));
                $decompositions.set(o, $r);
                return $r;
            } else if ($decomposer.is(BiConsumer.class)) {
                BiConsumer<Object, TreeDesigner> consumer = $decomposer.asExpected();
                consumer.accept(o, this);
                return $decompositions.in(o).get();
            }
        } else {
            try {
                Method method = type.getDeclaredMethod("decompose", Subject.class);
                if(method.trySetAccessible()) {
                    int modifiers = method.getModifiers();
                    if(Subject.class.isAssignableFrom(method.getReturnType()) && Modifier.isStatic(modifiers)) {
                        var $r = (Subject)method.invoke(null, Suite.set(o));
                        $decompositions.set(o, $r);
                        return $r;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            try {
                Method method = type.getDeclaredMethod("decompose", Subject.class, TreeDesigner.class);
                if(method.trySetAccessible()) {
                    int modifiers = method.getModifiers();
                    if(Subject.class.isAssignableFrom(method.getReturnType()) && Modifier.isStatic(modifiers)) {
                        return (Subject)method.invoke(null, Suite.set(o), this);
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
            if(o instanceof Interpreted) {
                var $r = ((Interpreted)o).interpret();
                $decompositions.set(o, $r);
                return $r;
            }
        }
        return Suite.set();
    }

}
