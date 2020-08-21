package jorg.jorg;

import jorg.jorg.util.PortableList;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;

import java.util.function.Function;

public class JorgPerformer {

    private final Subject performers = Suite.set();
    private final Subject ports = Suite.set();

    private Subject solution;
    private int automaticIndex;

    public JorgPerformer() {
        this(true, true);
    }

    public JorgPerformer(boolean enableStandardPerformers, boolean enableDefaultPorts) {
        if(enableStandardPerformers) {
            performers.insetAll(StandardPerformer.getAllSupported());
        }

        if(enableDefaultPorts) {
            setPort(PortableList.class, "list");
            setPort(Boolean.class, "bool");
            setPort(Integer.class, "int");
            setPort(Character.class, "char");
            setPort(Byte.class, "byte");
            setPort(Short.class, "short");
            setPort(Long.class, "long");
            setPort(Float.class, "float");
            setPort(Double.class, "double");
            setPort(String.class, "string");
        }
    }

    public<T> void setPerformer(Class<T> type, Function<T, Subject> performer) {
        performers.set(type, performer);
    }

    public void setPort(Object object, String id) {
        ports.set(object, Xray.image(new Reference(id)));
    }

    public Cascade<Xray> perform(Subject solid) throws JorgWriteException {
        solution = Suite.set();
        automaticIndex = 0;

        for(var s : solid) {

            Xray xray = new Xray(s.key().asExpected(), s.direct());
            Subject sub = solution.get(Xray.image(s.direct()));
            if(sub.settled()) {
                Xray x = sub.asExpected();
                addXray(xray.getImage(), x);
                xray.setReady(true);
            }

            solution.set(xray);
        }

        for(Xray xray : solution.keys(Xray.class).filter(x -> !x.isReady())) {
            Object o = xray.getObject();
            Xray x = directImage(o);
            if (x != null) {
                addXray(xray.getImage(), x);
                xray.setReady(true);
            } else if (o instanceof Performable) {
                Subject sub = ((Performable) o).perform();
                if(sub == null) throw new NullPointerException();
                xray.getImage().insetAll(subjectImage(sub));
                xray.setReady(true);
            } else if(o.getClass().isArray()) {
                Subject sub = arrayImage(o);
                if (sub != null) {
                    xray.getImage().insetAll(sub);
                    xray.setReady(true);
                } else throw new JorgWriteException("Cant perform array of " + o.getClass().getComponentType());
            } else {
                Subject sub = performers.get(o.getClass());
                if(sub.settled()) {
                    Function<Object, Subject> performer = sub.asExpected();
                    sub = performer.apply(o);
                    if(sub == null) throw new NullPointerException();
                    xray.getImage().insetAll(subjectImage(sub));
                    xray.setReady(true);
                } else throw new JorgWriteException("Cant perform object of " + o.getClass());
            }
        }

        return solution.keys(Xray.class).cascade();
    }

    private Xray directImage(Object o) {
        Subject sub = ports.get(o);
        if(sub.settled()) return sub.asExpected();
        if(o == null) return Xray.image(null);
        if(o instanceof Class) return Xray.image(new Reference(((Class<?>) o).getName()));
        if(o instanceof Boolean || o instanceof Character || o instanceof Byte || o instanceof Short ||
                o instanceof Integer || o instanceof Long || o instanceof Float || o instanceof Double ||
                o instanceof String || o instanceof Suite.AutoKey || o == Jorg.terminator) {
            return Xray.image(o);
        }
        return null;
    }

    private Subject subjectImage(Subject subject) {
        Subject sub = Suite.set();
        for (var s : subject) {
            Xray keyXray = directImage(s.key().direct());
            if (keyXray == null) {
                Subject s1 = solution.get(Xray.image(s.key().direct()));
                if (s1.settled()) {
                    keyXray = s1.asExpected();
                } else {
                    keyXray = new Xray("" + ++automaticIndex, s.key().direct());
                    solution.set(keyXray);
                }
            }

            Xray valueXray = directImage(s.direct());
            if (valueXray == null) {
                Subject s1 = solution.get(Xray.image(s.direct()));
                if (s1.settled()) {
                    valueXray = s1.asExpected();
                } else {
                    valueXray = new Xray("" + ++automaticIndex, s.direct());
                    solution.set(valueXray);
                }
            }
            sub.set(keyXray, valueXray);
        }
        return sub;
    }

    private Subject arrayImage(Object o) {
        Class<?> type = o.getClass().getComponentType();
        Xray typeXray = directImage(type);
        if(typeXray == null) return null;
        Subject s = addXray(Suite.set(), typeXray);

        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                int[] a = (int[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Byte.TYPE) {
                byte[] a = (byte[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Long.TYPE) {
                long[] a = (long[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Float.TYPE) {
                float[] a = (float[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Double.TYPE) {
                double[] a = (double[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Short.TYPE) {
                short[] a = (short[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Character.TYPE) {
                char[] a = (char[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else if (type == Boolean.TYPE) {
                boolean[] a = (boolean[]) o;
                addXray(s, directImage(a.length));
                addXray(s, directImage(Jorg.terminator));
                for(var i : a) addXray(s, directImage(i));
            } else {
                throw new InternalError();
            }
        } else {
            Object[] a = (Object[]) o;
            addXray(s, directImage(a.length));
            addXray(s, directImage(Jorg.terminator));
            for(var i : a) {
                Xray x = directImage(i);
                if (x == null) {
                    Subject sub = solution.get(Xray.image(i));
                    if (sub.settled()) {
                        x = sub.asExpected();
                    } else {
                        x = new Xray("" + ++automaticIndex, s.key().direct());
                        solution.set(x);
                    }
                }
                addXray(s, x);
            }
        }
        return s;
    }

    private Subject addXray(Subject s, Xray x) {
        return s.set(Xray.image(new Suite.AutoKey()), x);
    }
}
