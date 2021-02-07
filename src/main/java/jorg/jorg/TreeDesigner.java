package jorg.jorg;

import suite.suite.SolidSubject;
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
import java.util.function.Function;

public class TreeDesigner {

    interface Xray {
        String toString(BracketTreeWriter writer);
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
            if (this == o1) return true;
            if (o1 == null || getClass() != o1.getClass()) return false;
            ObjectXray that = (ObjectXray) o1;
            return o == that.o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(o);
        }

        @Override
        public String toString(BracketTreeWriter writer) {
            return "@" + refId;
        }

        @Override
        public String toString() {
            return super.toString() + "{" + o + "}";
        }
    }

    static class AutoXray implements Xray {

        @Override
        public String toString(BracketTreeWriter writer) {
            return "";
        }

        @Override
        public String toString() {
            return super.toString() + "{}";
        }
    }

    static class StringXray implements Xray {
        String str;

        public StringXray(String str) {
            this.str = str;
        }

        @Override
        public String toString(BracketTreeWriter writer) {
            return writer.escaped(str);
        }

        @Override
        public String toString() {
            return super.toString() + "{" + str + "}";
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
        public String toString(BracketTreeWriter writer) {
            return str;
        }

        @Override
        public String toString() {
            return super.toString() + "{" + str + "}";
        }
    }

    static class PrimitiveXray implements Xray {
        Subject $data;


        public PrimitiveXray(Subject $data) {
            this.$data = $data;
        }

        @Override
        public boolean equals(Object o1) {
            return false;
        }

        @Override
        public int hashCode() {
            return $data.hashCode();
        }

        @Override
        public String toString(BracketTreeWriter writer) {
            return Suite.describe($data, false, writer::stringify, true);
        }

        @Override
        public String toString() {
            return super.toString() + "{" + $data + "}";
        }
    }

    static final Xray hashXray = new SpecialXray("#");
    static final Xray atXray = new SpecialXray("@");
    static final Xray slimeXray = new SpecialXray("@/");

    Subject $refs = Suite.set();
    Subject $decompositions = Suite.set();

    Subject $decomposers = Suite.set();
    boolean attachingTypes;
    Function<Object, Subject> elementaryDecomposer;
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
                insert(SolidSubject.class, "subject").
                insert(String.class, "string")
        );
        $decompositions.alter(Suite.
                set(null, Suite.set(new SpecialXray("@null")))
        );
        attachingTypes = true;
        elementaryDecomposer = o -> {
            if(o instanceof String) return Suite.set(o);
            if(o instanceof Integer) return Suite.set(o.toString());
            if(o instanceof Double) return Suite.set(o.toString());
            if(o instanceof Float) return Suite.set(o.toString());
            if(o instanceof Boolean) return Suite.set(o.toString());
            return Suite.set();
        };
    }

    public boolean isAttachingTypes() {
        return attachingTypes;
    }

    public void setAttachingTypes(boolean attachingTypes) {
        this.attachingTypes = attachingTypes;
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

    public void setElementaryDecomposer(Function<Object, Subject> elementaryDecomposer) {
        this.elementaryDecomposer = elementaryDecomposer;
    }

    public void setClassAlias(Class<?> aClass, String alias) {
        $classAliases.in(aClass).set(alias);
    }

    public Subject load(Object o) {
        $refs = Suite.set();
        var xray = xray(o);
        var $xRoot = Suite.set(xray);
        int id = 0;
        for(var $i : Suite.preDfs(Suite.add($xRoot)).eachIn()) {
            for(var $i1 : $i) {
                if($i1.is(ObjectXray.class)) {
                    ObjectXray x = $i1.asExpected();
                    if (x.usages < 2 && $i.size() == 1 && $i1.in().absent()) {
                        $i.unset().alter($refs.in(x).get());
                    } else {
                        if (x.refId == null) {
                            x.refId = "" + id++;
                            var $r = $refs.in(x).get();
                            $r.setBefore($r.getFirst().direct(), atXray, Suite.set(new StringXray(x.refId)));
                            $i.in(slimeXray).set(new AutoXray(), $r);
                        }
                    }
                }
            }
        }
        if(xray instanceof ObjectXray && ((ObjectXray) xray).usages > 1) return $xRoot.at(1);
        else return $xRoot;
    }

    Xray xray(Object o) {
        if(o instanceof Xray) return (Xray) o;
        var $prim = elementaryDecomposer.apply(o);
        if($prim.present()) return new StringXray($prim.asExpected());
        if(o instanceof String) return new StringXray($prim.asExpected());

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
                if(attachingTypes) attachType($r, type);
                $decompositions.set(o, $r);
                return $r;
            }
        }
        return Suite.set();
    }

    void attachType(Subject $, Class<?> type) {
        $.setBefore($.direct(), hashXray, Suite.set(new StringXray($classAliases.in(type).orGiven(type.getName()))));
    }

}
