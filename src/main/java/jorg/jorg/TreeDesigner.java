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

    interface Xray {
        boolean escaped();
    }

    static class ObjectXray implements Xray {
        Object o;
        int usages;
        String refId;

        public ObjectXray(Object o) {
            this.o = o;
            usages = 0;
        }

        @Override
        public boolean equals(Object o1) {
            if(o == o1) return true;
            if (this == o1) return true;
            if (o1 == null || getClass() != o1.getClass()) return false;
            ObjectXray xray = (ObjectXray) o1;
            return o == xray.o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(o);
        }

        @Override
        public String toString() {
            return "@" + refId;
        }

        public boolean escaped() {
            return false;
        }
    }

    static class StringXray implements Xray {
        String str;

        public StringXray(String str) {
            this.str = str;
        }

        @Override
        public boolean equals(Object o1) {
            return false;
        }

        @Override
        public int hashCode() {
            return str.hashCode();
        }

        @Override
        public String toString() {
            return str;
        }

        public boolean escaped() {
            return true;
        }
    }

    static class SpecialXray implements Xray {
        String str;

        public SpecialXray(String str) {
            this.str = str;
        }

        @Override
        public boolean equals(Object o1) {
            return false;
        }

        @Override
        public int hashCode() {
            return str.hashCode();
        }

        @Override
        public String toString() {
            return str;
        }

        public boolean escaped() {
            return false;
        }
    }

    static final Xray hashXray = new SpecialXray("#");

    Subject $refs = Suite.set();
    Subject $decompositions = Suite.set();

    Subject $decomposers = Suite.set();
    boolean attachingTypes;
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
        $decompositions.alter(Suite.
                set(null, Suite.set(new SpecialXray("@null")))
        );
        attachingTypes = true;
    }

    public boolean isAttachingTypes() {
        return attachingTypes;
    }

    public void setAttachingTypes(boolean attachingTypes) {
        this.attachingTypes = attachingTypes;
    }

    public Subject load(Object o) {
        $refs = Suite.set();
        var xray = xray(o);
        var $xRoot = xray instanceof StringXray ? Suite.set(xray) : $refs.in(xray).get();
        int id = 1;
        for(var $i : Suite.preDfs(Suite.add($xRoot)).eachIn()) {
            for(var $i1 : $i) {
                if($i1.is(ObjectXray.class)) {
                    ObjectXray x = $i1.asExpected();
                    if (x.usages < 2 && $i.size() == 1 && $i1.in().absent()) {
                        $i.unset().alter($refs.in(x).get());
                    } else {
                        if (x.refId == null) x.refId = "" + id++;
                        $xRoot.set(x, $refs.in(x).get());
                    }
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
        if(o instanceof Xray) return (Xray) o;
        if(o instanceof String) return new StringXray((String)o);
        ObjectXray xray = $refs.getFilled(new ObjectXray(o)).asExpected();
        if(xray.usages++ < 1) {
            var $ = decompose(o);
            $refs.set(xray, $);
            for(var $i : Suite.preDfs(Suite.add($)).eachIn()) {
                for(var i : $i.eachDirect()) {
                    $i.shift(i, xray(i));
                }
            }
        }

        return xray;
    }

    boolean isLeaf(Subject $) {
        return $.size() == 1 && $.in().absent();
    }

    Subject decompose(Object o) {

        if($decompositions.present(o)) return $decompositions.in(o).get();

        Class<?> type = o.getClass();

        var $decomposer = $decomposers.in(type).get();
        if($decomposer.present()) {
            if ($decomposer.is(Action.class)) {

                Action decomposer = $decomposer.asExpected();
                var $r = decomposer.play(Suite.set(o));
                if(isAttachingTypes()) attachType($r, type);
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
                        if(attachingTypes) attachType($r, type);
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
                $r.setBefore($r.direct(), hashXray, Suite.set($classAliases.in(type).orGiven(type.toString())));
                $decompositions.set(o, $r);
                return $r;
            }
        }
        return Suite.set();
    }

    void attachType(Subject $, Class<?> type) {
        $.setBefore($.direct(), hashXray, Suite.set($classAliases.in(type).orGiven(type.toString())));
    }

}
