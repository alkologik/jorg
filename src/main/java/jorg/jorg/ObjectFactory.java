package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;
import suite.suite.util.Fluid;

import java.util.function.BiConsumer;

public class ObjectFactory {

    Subject $instances = Suite.set();
    Subject $constructors = Suite.set();
    Subject $implementers = Suite.set();
    Action wizard = (v) -> Suite.set();
    Linker linker = new Linker();

    Subject $root;

    public void setRoot(Subject $root) {
        this.$root = $root;
    }

    public void setConstructor(Class<?> type, Action constructor) {
        $constructors.set(type, constructor);
    }

    public void setConstructors(Fluid constructors) {
        $constructors.inset(constructors.select(
                v -> v.instanceOf(Action.class) && v.key() instanceof Class
        ));
    }

    public<T> void setImplementer(Class<T> type, BiConsumer<T, Subject> implementer) {
        $implementers.set(type, implementer);
    }

    public void setImplementers(Fluid implementers) {
        $implementers.inset(implementers.select(
                v -> v.instanceOf(BiConsumer.class) && v.key() instanceof Class
        ));
    }

    public void setWizard(Action wizard) {
        this.wizard = wizard;
    }

    public Object get(Object o) {
        var $v = $instances.get(o);
        if($v.notEmpty())return $v.asExpected();
        $v = o instanceof Subject ? (Subject)o : Suite.set(o);
        $v = new FactoryVendor(this, $v);

        var $r = wizard.play($v);
        Object r = $r.direct();
        if($r.notEmpty()) $instances.set(o, r);
        return r;
    }

    public Subject get(Object o, Class<?> type) {
        var $v = $instances.get(o);
        if($v.notEmpty())return $v.asExpected();
        $v = o instanceof Subject ? (Subject)o : Suite.set(o);
        $v = new FactoryVendor(this, $v);

        var $r = construct($v, type);
        if($r.notEmpty()) {
            $instances.set(o, $r.direct());
            return $r;
        }

        $v = wizard.play($v);
        if($r.notEmpty()) {
            $instances.set(o, $r.direct());
        }
        return $r;
    }


    Subject construct(Subject $vendor, Class<?> type) {
        var $constructor = $constructors.get(type);
        if($constructor.notEmpty()) {
            Action constructor = $constructor.asExpected();
            var $r = constructor.play($vendor);
            if($r.notEmpty()) {
                var $implementer = $implementers.get(type);
                if($implementer.notEmpty()) {
                    BiConsumer<?, Subject> implementer = $implementer.asExpected();
                    implementer.accept($r.asExpected(), $vendor);
                }
                return $r;
            }
        }
        return Suite.set();
    }
}
