package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class JorgReader {

    public static<T> T read(String filePath) {
        JorgReader reader = new JorgReader();
        return reader.loadWell(new File(filePath)) ? reader.getObjects().get("0").asExpected() : null;
    }

    public static<T> T read(File file) {
        JorgReader reader = new JorgReader();
        return reader.loadWell(file) ? reader.getObjects().get("0").asExpected() : null;
    }

    public static<T> T read(InputStream inputStream) {
        JorgReader reader = new JorgReader();
        return reader.loadWell(inputStream) ? reader.getObjects().get("0").asExpected() : null;
    }

    public static<T> T read(String filePath, Function<Subject, Object> recipe) {
        JorgReader reader = new JorgReader();
        reader.reformer.setRecipe(recipe);
        return reader.loadWell(new File(filePath)) ? reader.getObjects().get("0").asExpected() : null;
    }

    public static<T> T read(File file, Function<Subject, Object> recipe) {
        JorgReader reader = new JorgReader();
        reader.reformer.setRecipe(recipe);
        return reader.loadWell(file) ? reader.getObjects().get("0").asExpected() : null;
    }

    public static<T> T read(InputStream inputStream, Function<Subject, Object> recipe) {
        JorgReader reader = new JorgReader();
        reader.reformer.setRecipe(recipe);
        return reader.loadWell(inputStream) ? reader.getObjects().get("0").asExpected() : null;
    }

    public static<T> T parse(String jorg) {
        JorgReader reader = new JorgReader();
        InputStream inputStream = new ByteArrayInputStream(jorg.getBytes());
        return reader.loadWell(inputStream) ? reader.getObjects().get("0").asExpected() : null;
    }

    private JorgReformer reformer;
    private final Subject objects;

    public JorgReader() {
        this(new JorgReformer());
    }

    public JorgReformer getReformer() {
        return reformer;
    }

    public void setReformer(JorgReformer reformer) {
        this.reformer = reformer;
    }

    public JorgReader(JorgReformer reformer) {
        this.reformer = reformer;
        objects = Suite.set();
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
