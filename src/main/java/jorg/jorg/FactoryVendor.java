package jorg.jorg;


import suite.suite.Subject;
import suite.suite.Vendor;

public class FactoryVendor extends Vendor {

    ObjectFactory factory;

    public FactoryVendor(ObjectFactory factory, Subject $local) {
        super($local);
        this.factory = factory;
    }

    @Override
    protected Subject wrap(Subject subject) {
        return new FactoryVendor(factory, subject);
    }

    @Override
    protected Subject factor(Subject subject) {
        return factory.get(subject);
    }

    @Override
    protected Subject factor(Subject subject, Class<?> aClass) {
        return factory.get(subject, aClass);
    }
}
