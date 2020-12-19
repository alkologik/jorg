package jorg.jorg;

import suite.suite.*;
import suite.suite.util.Glass;
import suite.suite.util.Wave;

import java.util.Iterator;


public class FactoryVendor implements SimpleSubject {

    ObjectFactory factory;
    Subject $local;

    public FactoryVendor(ObjectFactory factory, Subject $local) {
        this.factory = factory;
        this.$local = $local;
    }

    @Override
    public Subject getAt(Slot slot) {
        return new FactoryVendor(factory, $local.getAt(slot));
    }

    @Override
    public Object direct() {
        return factory.get($local.direct());
    }

    @Override
    public <B> B asGiven(Glass<? super B, B> requestedType) {
        return factory.get($local.direct(), requestedType.getMainClass()).asExpected();
    }

    @Override
    public <B> B asGiven(Glass<? super B, B> requestedType, B substitute) {
        return factory.get($local.direct(), requestedType.getMainClass()).orGiven(substitute);
    }

    @Override
    public Subject unset() {
        $local = Suite.set();
        return this;
    }

    @Override
    public Subject unsetAt(Slot slot) {
        return null;
    }

    @Override
    public int size() {
        return $local.size();
    }

    @Override
    public Wave<Subject> iterator(boolean reverse) {
        return new Wave<>() {
            final Iterator<Subject> source = $local.iterator(reverse);

            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public Subject next() {
                return new FactoryVendor(factory, source.next());
            }
        };
    }

    @Override
    public Subject setAt(Slot slot, Object o, Object o1) {
        return null;
    }
}
