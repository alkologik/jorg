package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class JorgReader {

    private JorgReformer reformer;
    private final Subject objects;

    public JorgReader() {
        this(new JorgReformer());
    }

    public JorgReader(JorgReformer reformer) {
        this.reformer = reformer;
        objects = Suite.set();
    }

    public JorgReformer getMainReformer() {
        return reformer;
    }

    public void setMainReformer(JorgReformer reformer) {
        this.reformer = reformer;
    }

    public JorgReader withRecipe(Function<Subject, Object> recipe) {
        reformer.setRecipe(recipe);
        return this;
    }

    public JorgReader withTypedRecipe(Class<?> type, Function<Subject, Object> recipe) {
        reformer.setTypedRecipe(type, recipe);
        return this;
    }

    public<T> JorgReader withReformer(Class<T> type, BiConsumer<T, Subject> reformer) {
        this.reformer.setReformer(type, reformer);
        return this;
    }

    public JorgReader withAdapter(String s, Object o) {
        reformer.setAdapter(s, o);
        return this;
    }

    public<T> T read(String filePath) {
        return loadWell(new File(filePath)) ? getObjects().get("0").asExpected() : null;
    }

    public<T> T read(File file) {
        return loadWell(file) ? getObjects().get("0").asExpected() : null;
    }

    public<T> T read(InputStream inputStream) {
        return loadWell(inputStream) ? getObjects().get("0").asExpected() : null;
    }

    public<T> T parse(String jorg) {
        InputStream inputStream = new ByteArrayInputStream(jorg.getBytes());
        return loadWell(inputStream) ? getObjects().get("0").asExpected() : null;
    }

    public boolean loadWell(File file) {
        try {
            load(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void load(File file) throws IOException, JorgReadException {
        load(new FileInputStream(file));
    }

    public boolean loadWell(URL url) {
        try {
            load(url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void load(URL url) throws IOException, JorgReadException {
        URLConnection connection = url.openConnection();
        load(connection.getInputStream());
    }

    public boolean loadWell(InputStream inputStream) {
        try {
            load(inputStream);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void load(InputStream inputStream) throws JorgReadException {
        Subject xkeys;
        JorgProcessor processor = new JorgProcessor();
        processor.ready();
        try (inputStream) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int code = reader.read();
            while (code != -1) {
                processor.advance(code);
                code = reader.read();
            }
            xkeys = processor.finish();

            for (Xkey xkey : xkeys.front().values().filter(Xkey.class).filter(x -> x.getObject() == null)) {
                reformer.construct(xkey);
            }
            for (Xkey xkey : xkeys.front().values().filter(Xkey.class).filter(x -> x.getLabel() instanceof Reference)) {
                reformer.reform(xkey);
                Reference ref = (Reference) xkey.getLabel();
                objects.set(ref.getId(), xkey.getObject());
            }
        }catch(Exception e) {
            throw new JorgReadException(e);
        }
    }

    public Subject getObjects() {
        return objects;
    }
}
