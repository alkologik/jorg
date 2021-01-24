package jorg.jorg;


import suite.suite.Subject;
import suite.suite.Vendor;

public class FactoryVendorRoot extends Vendor {

    ObjectFactory factory;

    public FactoryVendorRoot(ObjectFactory factory, Subject $local) {
        super($local);
        this.factory = factory;
    }

    @Override
    protected Subject wrap(Subject subject) {
        return factory.prepare(subject);
    }

    @Override
    protected Subject factor(Subject subject) {
        return subject;
    }

    @Override
    protected Subject factor(Subject subject, Class<?> aClass) {
        return subject;
    }
}
