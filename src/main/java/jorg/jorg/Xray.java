package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;

import java.util.Objects;

public class Xray {

    public static Xray image(Object germ) {
        return new Xray(germ);
    }

    private String id;
    private final Object object;
    private final Subject image;
    private boolean ready = false;

    Xray(Object object) {
        this.object = object;
        image = Suite.set();
    }

    public Xray(String id, Object object) {
        this(object);
        this.id = id;
    }

    public Object getObject() {
        return object;
    }

    public Subject getImage() {
        return image;
    }

    public String getId() {
        return id;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public boolean equals(Object o1) {
        return (o1 instanceof Xray && object == ((Xray) o1).object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }

    @Override
    public String toString() {
        return "(" + id + ")" + object;
    }

}
