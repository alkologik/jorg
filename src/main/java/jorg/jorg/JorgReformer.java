package jorg.jorg;

import jorg.jorg.util.PortableList;
import suite.suite.Slot;
import suite.suite.Subject;
import suite.suite.Suite;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static jorg.jorg.Jorg.terminator;

public class JorgReformer {

    private final Subject recipes = Suite.set();
    private final Subject typedRecipes = Suite.set();
    private final Subject reformers = Suite.set();
    private final Subject adapters = Suite.set();
    private final Function<Subject, Object> typedRecipesMethod = this::reformTyped;

    public JorgReformer() {
        this(true, true, true);
    }

    public JorgReformer(boolean enableStandardReformers, boolean enableDefaultRecipes, boolean enableDefaultAdapters) {

        if(enableStandardReformers) {
            reformers.insetAll(StandardReformer.getAllSupported().front());
        }

        if(enableDefaultRecipes) {
            setRecipe(s -> {
                var sub = s.at(0);
                if(sub.assigned(Class.class) && sub.key().assigned(Suite.Add.class)) {
                    Class<?> type = sub.asExpected();
                    try {
                        return type.getMethod("form", Subject.class).invoke(null, sub.frontFrom(Slot.in(1)).toSubject());
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            });
            setRecipe(typedRecipesMethod);
        }

        if(enableDefaultAdapters) {
            setAdapter("bool", boolean.class);
            setAdapter("int", int.class);
            setAdapter("char", char.class);
            setAdapter("byte", byte.class);
            setAdapter("short", short.class);
            setAdapter("long", long.class);
            setAdapter("float", float.class);
            setAdapter("double", double.class);
            setAdapter("string", String.class);
            setAdapter("list", PortableList.class);
        }
    }


    public void setRecipe(Function<Subject, Object> recipe) {
        recipes.set(recipe);
    }

    public void setTypedRecipe(Class<?> type, Function<Subject, Object> recipe) {
        typedRecipes.set(type, recipe);
    }

    public<T> void setReformer(Class<T> type, BiConsumer<T, Subject> reformer) {
        reformers.set(type, reformer);
    }

    public void setAdapter(String s, Object o) {
        adapters.set(s, o);
    }

    public void enableTypedRecipes(boolean enable) {
        if(enable) {
            recipes.setAt(Slot.RECENT, typedRecipesMethod);
        } else {
            recipes.unset(typedRecipesMethod);
        }
    }

    protected void construct(Xkey xkey) throws JorgReadException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(xkey.isConstructed())return;
        Object o;

        if(xkey.getLabel() instanceof Reference) {
            Reference reference = (Reference) xkey.getLabel();
            Subject sub = adapters.get(reference.getId());
            if(sub.settled()) {
                xkey.setObject(sub.direct());
                xkey.setConstructed(true);
                sub.direct();
                return;

            } else if(reference.isDeclared()) {
                //  Konstrukcja obiektu
                xkey.setUnderConstruction(true);

                Subject image = xkey.getImage();
                Subject params = Suite.set();
                // Parametry konstrukcyjne to parametry przed termiantorem
                for(var s : image.front()) {
                    Xkey key = s.key().asExpected();
                    image.take(key);
                    if(key.isUnderConstruction()) throw new JorgReadException("Construction loop");
                    if(!key.isConstructed()) construct(key);
                    if(key.getObject() == terminator) break;
                    Xkey value = s.asExpected();
                    if(value.isUnderConstruction()) throw new JorgReadException("Construction loop");
                    if(!value.isConstructed()) construct(value);
                    if(value.getObject() == terminator) break;
                    params.set(key.getObject(), value.getObject());
                }

                xkey.setUnderConstruction(false);

                for(var s : recipes.reverse()) {
                    Function<Subject, Object> constructor = s.asExpected();
                    o = constructor.apply(params);
                    if(o != null) {
                        xkey.setObject(o);
                        xkey.setConstructed(true);
                        return;
                    }
                }

                if(params.size() == 1) {
                    if (params.assigned(Class.class) && params.key().assigned(Suite.Add.class)) {
                        Class<?> type = params.asExpected();
                        Constructor<?> c = type.getDeclaredConstructor();
                        o = c.newInstance();
                    } else if (image.size() == 0) {
                        o = params.direct();
                    } else {
                        o = Suite.insetAll(params.front());
                    }
                } else if(params.size() == 2) {
                    var p0 = params.at(0);
                    var p1 = params.at(1);
                    if(p0.assigned(Class.class) && p0.key().assigned(Suite.Add.class) &&
                            p1.assigned(Integer.class) && p1.key().assigned(Suite.Add.class)) {
                        o = Array.newInstance(p0.asExpected(), p1.asInt());
                    } else if(p0.assigned(Class.class) && p0.key().assigned(Suite.Add.class) &&
                            p1.assigned(Boolean.class) && p1.key().assigned(Suite.Add.class)) {
                        o = Array.newInstance(p0.asExpected(), image.size());
                    } else {
                        o = Suite.insetAll(params.front());
                    }
                } else {
                    o = Suite.insetAll(params.front());
                }

                xkey.setObject(o);
                xkey.setConstructed(true);
                return;

            } else try { //Odwołania do referencji spoza przestrzeni, która nie została rozpoznana jako adapter
                xkey.setObject(Class.forName(reference.getId()));
                xkey.setConstructed(true);
                xkey.setReformed(true);
                return;
            } catch (ClassNotFoundException e) {
                throw new JorgReadException("Cant create object for reference " + reference);
            }
        }

        throw new JorgReadException("Cant create object for " + xkey.getLabel());
    }

    protected void reform(Xkey xkey) throws JorgReadException {

        if(xkey.isReformed())return;
        Object o = xkey.getObject();
        if(o == null || o instanceof Boolean || o instanceof Character || o instanceof Byte || o instanceof Short ||
                o instanceof Integer || o instanceof Long || o instanceof Float || o instanceof Double ||
                o instanceof String || o instanceof Class) return;

        Subject params = xkey.getImage().front().advance(
                s -> Suite.set(s.key().asGiven(Xkey.class).getObject(), s.asGiven(Xkey.class).getObject())).toSubject();

        if(o instanceof Reformable) {
            ((Reformable) o).reform(params);
        } else if(o.getClass().isArray()) {
            reformArray(o, params);
        } else {
            Subject sub = reformers.get(o.getClass());
            if(sub.settled()) {
                BiConsumer<Object, Subject> reformer = sub.asExpected();
                reformer.accept(o, params);
            } else throw new JorgReadException("Reformer for " + xkey.getObject() + " #" + xkey.getObject().getClass() + " not found");
        }
    }

    private void reformArray(Object o, Subject s) {
        Class<?> type = o.getClass().getComponentType();
        int i = 0;

        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                int[] a = (int[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Byte.TYPE) {
                byte[] a = (byte[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Long.TYPE) {
                long[] a = (long[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Float.TYPE) {
                float[] a = (float[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Double.TYPE) {
                double[] a = (double[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Short.TYPE) {
                short[] a = (short[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Character.TYPE) {
                char[] a = (char[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else if (type == Boolean.TYPE) {
                boolean[] a = (boolean[]) o;
                for(var sub : s.front()) {
                    if(sub.key().assigned(Integer.class)) {
                        i = sub.key().asExpected();
                    }
                    if(i < a.length)a[i++] = sub.asExpected();
                }
            } else {
                throw new InternalError();
            }
        } else {
            Object[] a = (Object[]) o;
            for(var sub : s.front()) {
                if(sub.key().assigned(Integer.class)) {
                    i = sub.key().asExpected();
                }
                if(i < a.length)a[i++] = sub.asExpected();
            }
        }
    }

    private Object reformTyped(Subject s) {
        if(s.get(0).assigned(Class.class)) {
            Class<?> type = s.get(0).asExpected();
            var recipe = typedRecipes.get(type);
            if(recipe.settled()) {
                Function<Subject, ?> function = recipe.asExpected();
                return function.apply(s.frontFrom(Slot.in(1)).toSubject());
            }
        }
        return null;
    }
}
