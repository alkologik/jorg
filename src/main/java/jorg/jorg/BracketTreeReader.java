package jorg.jorg;

import suite.suite.Subject;
import suite.suite.action.Action;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

public class BracketTreeReader {

    private ObjectFactory factory;
    private Subject $bracketTree;

    public BracketTreeReader() {
        this(new ObjectFactory(StandardInterpreter.getAll()));
    }

    public BracketTreeReader(ObjectFactory factory) {
        this.factory = factory;
    }

    public ObjectFactory getFactory() {
        return factory;
    }

    public void setFactory(ObjectFactory factory) {
        this.factory = factory;
    }

    public BracketTreeReader withRecipe(Class<?> type, Action recipe) {
        factory.setConstructor(type, recipe);
        return this;
    }

    public<T> BracketTreeReader withRecipe(Class<T> type, BiConsumer<Subject, ObjectFactory> recipe) {
        factory.setConstructor(type, recipe);
        return this;
    }

    public BracketTreeReader withParam(String ref, Object o) {
        factory.setParam(ref, o);
        return this;
    }

    public<T> T read(String filePath) {
        return loadWell(new File(filePath)) ? $bracketTree.asExpected() : null;
    }

    public<T> T read(File file) {
        return loadWell(file) ? $bracketTree.asExpected() : null;
    }

    public<T> T read(InputStream inputStream) {
        return loadWell(inputStream) ? $bracketTree.asExpected() : null;
    }

    public<T> T parse(String jorg) {
        InputStream inputStream = new ByteArrayInputStream(jorg.getBytes());
        return loadWell(inputStream) ? $bracketTree.asExpected() : null;
    }


    public<T> T read(String filePath, Class<T> type) {
        return loadWell(new File(filePath)) ? $bracketTree.as(type) : null;
    }

    public<T> T read(File file, Class<T> type) {
        return loadWell(file) ? $bracketTree.as(type) : null;
    }

    public<T> T read(InputStream inputStream, Class<T> type) {
        return loadWell(inputStream) ? $bracketTree.as(type) : null;
    }

    public<T> T parse(String jorg, Class<T> type) {
        InputStream inputStream = new ByteArrayInputStream(jorg.getBytes());
        return loadWell(inputStream) ? $bracketTree.as(type) : null;
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
        BracketTreeProcessor processor = new BracketTreeProcessor();
        processor.ready();
        try (inputStream) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int code = reader.read();
            while (code != -1) {
                processor.advance(code);
                code = reader.read();
            }
            $bracketTree = factory.load(processor.finish());
        }catch(Exception e) {
            throw new JorgReadException(e);
        }
    }

    public Subject getBracketTree() {
        return $bracketTree;
    }
}
