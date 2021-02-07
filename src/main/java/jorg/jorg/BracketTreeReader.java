package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BracketTreeReader {

    private ObjectFactory factory;

    public BracketTreeReader() {
        this(new ObjectFactory(StandardDiscoverer.getAll()));
    }

    public BracketTreeReader(ObjectFactory factory) {
        this.factory = factory;
    }

    public Subject read(String filePath) {
        return loadWell(new File(filePath));
    }

    public Subject read(File file) {
        return loadWell(file);
    }

    public Subject read(InputStream inputStream) {
        return loadWell(inputStream);
    }

    public Subject parse(String jorg) {
        InputStream inputStream = new ByteArrayInputStream(jorg.getBytes());
        return loadWell(inputStream);
    }



    public ObjectFactory getFactory() {
        return factory;
    }

    public void setFactory(ObjectFactory factory) {
        this.factory = factory;
    }

    public BracketTreeReader withComposer(Class<?> type, Action recipe) {
        factory.setComposer(type, recipe);
        return this;
    }

    public<T> BracketTreeReader withComposer(Class<T> type, BiConsumer<Subject, ObjectFactory> recipe) {
        factory.setComposer(type, recipe);
        return this;
    }

    public BracketTreeReader withComposition(String reference, Object composition) {
        factory.setComposition(reference, composition);
        return this;
    }

    public BracketTreeReader withElementaryComposer(Function<String, Subject> composer) {
        factory.setElementaryComposer(composer);
        return this;
    }

    public BracketTreeReader withAlias(Class<?> aClass, String alias) {
        this.factory.setClassAlias(alias, aClass);
        return this;
    }

    public Subject loadWell(File file) {
        try {
            return load(file);
        } catch (Exception e) {
            e.printStackTrace();
            return Suite.set();
        }
    }

    public Subject load(File file) throws IOException, JorgReadException {
        return load(new FileInputStream(file));
    }

    public Subject loadWell(URL url) {
        try {
            return load(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Suite.set();
        }
    }

    public Subject load(URL url) throws IOException, JorgReadException {
        URLConnection connection = url.openConnection();
        return load(connection.getInputStream());
    }

    public Subject loadWell(InputStream inputStream) {
        try {
            return load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return Suite.set();
        }
    }

    public Subject load(InputStream inputStream) throws JorgReadException {
        BracketTreeProcessor processor = new BracketTreeProcessor();
        processor.ready();
        try (inputStream) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int code = reader.read();
            while (code != -1) {
                processor.advance(code);
                code = reader.read();
            }
            return factory.load(processor.finish());
        }catch(Exception e) {
            throw new JorgReadException(e);
        }
    }
}
