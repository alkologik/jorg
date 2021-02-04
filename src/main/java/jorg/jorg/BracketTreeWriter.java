package jorg.jorg;

import suite.suite.Suite;
import suite.suite.action.Action;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class BracketTreeWriter {

    private TreeDesigner designer;
    private boolean compact;
    private boolean root;
    private int extendSign = '[';
    private int closeSign = ']';
    private int fenceSign = '"';


    public BracketTreeWriter() {
        this(new TreeDesigner());
    }

    public BracketTreeWriter(TreeDesigner designer) {
        this.designer = designer;
        compact = false;
        root = false;
    }

    public boolean write(Object object, String filePath) {
        try {
            save(object, new FileOutputStream(filePath));
        } catch (JorgWriteException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean write(Object object, File file) {
        try {
            save(object, new FileOutputStream(file));
        } catch (JorgWriteException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean write(Object object, URL url) {
        try {
            URLConnection connection = url.openConnection();
            save(object, connection.getOutputStream());
        } catch (IOException | JorgWriteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String encode(Object o) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return saveWell(o, outputStream) ? outputStream.toString() : "";
    }

    public TreeDesigner getDesigner() {
        return designer;
    }

    public void setDesigner(TreeDesigner designer) {
        this.designer = designer;
    }

    public<T> BracketTreeWriter withDecomposer(Class<T> type, Action decomposer) {
        this.designer.setDecomposer(type, decomposer);
        return this;
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public boolean saveWell(Object o, File file) {
        try {
            save(o, file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(Object o, File file) throws IOException, JorgWriteException {
        save(o, new FileOutputStream(file));
    }

    public boolean saveWell(Object o, URL url) {
        try {
            save(o, url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(Object o, URL url) throws IOException, JorgWriteException {
        URLConnection connection = url.openConnection();
        save(o, connection.getOutputStream());
    }

    public boolean saveWell(Object o, OutputStream output) {
        try {
            save(o, output);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(Object o, OutputStream output) throws JorgWriteException, IOException {

        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);

        var $ = designer.load(o);

        String str = Suite.describe($, root, this::stringify, compact);

        writer.write(str);

        writer.flush();
        output.close();
    }

    private String stringify(Object object) {
        TreeDesigner.Xray x = (TreeDesigner.Xray)object;
        return x.escaped() ? escaped(x.toString()) : x.toString();
    }

    private String escaped(String str) {
        if(str.startsWith("@") || str.startsWith("#") || str.trim().length() < str.length() ||
                str.contains("" + extendSign) || str.contains("" + closeSign)) {
            int i = 0;
            while(str.contains(fenceSign + "^".repeat(i)))++i;
            return "^".repeat(i) + "\"" + str + "\"" + "^".repeat(i);
        }
        return str;
    }
}
