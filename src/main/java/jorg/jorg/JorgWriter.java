package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class JorgWriter {

    private static final int escapeCharacter = '`';

    public static boolean write(Object object, String filePath) {
        JorgWriter writer = new JorgWriter();
        writer.objects.set("0", object);
        try {
            writer.save(new FileOutputStream(filePath));
        } catch (JorgWriteException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean write(Object object, File file) {
        JorgWriter writer = new JorgWriter();
        writer.objects.set("0", object);
        try {
            writer.save(new FileOutputStream(file));
        } catch (JorgWriteException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean write(Object object, URL url) {
        JorgWriter writer = new JorgWriter();
        writer.objects.set("0", object);
        try {
            URLConnection connection = url.openConnection();
            writer.save(connection.getOutputStream());
        } catch (IOException | JorgWriteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String encode(Object o) {
        JorgWriter writer = new JorgWriter();
        writer.objects.set("0", o);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return writer.saveWell(outputStream) ? outputStream.toString() : "";
    }

    private final Subject objects;
    private JorgPerformer performer;
    private boolean compactMode;
    private boolean rootMode;

    public JorgWriter() {
        this(new JorgPerformer());
    }

    public JorgWriter(JorgPerformer performer) {
        this.performer = performer;
        objects = Suite.set();
        compactMode = false;
        rootMode = true;
    }

    public JorgPerformer getPerformer() {
        return performer;
    }

    public void setPerformer(JorgPerformer performer) {
        this.performer = performer;
    }

    public boolean isCompactMode() {
        return compactMode;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    public boolean isRootMode() {
        return rootMode;
    }

    public void setRootMode(boolean rootMode) {
        this.rootMode = rootMode;
    }

    public void addObject(String id, Object object) {
        if(id.matches("^\\p{Alpha}.+")) {
            objects.set(id, object);
        } else {
            throw new IllegalArgumentException("Trace pattern is ^\\p{Alpha}");
        }
    }

    public boolean saveWell(File file) {
        try {
            save(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(File file) throws IOException, JorgWriteException {
        save(new FileOutputStream(file));
    }

    public boolean saveWell(URL url) {
        try {
            save(url);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(URL url) throws IOException, JorgWriteException {
        URLConnection connection = url.openConnection();
        save(connection.getOutputStream());
    }

    public boolean saveWell(OutputStream output) {
        try {
            save(output);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(OutputStream output) throws JorgWriteException, IOException {

        OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);

        Cascade<Xray> crystals = performer.perform(objects);
        boolean dartWritten;

        for(Xray c : crystals.toEnd()) {
            if(crystals.getFalls() > 1 || !"0".equals(c.getId()) || !rootMode) {
                writer.write(compactMode ? "#[" : "#[ ");
                writer.write(c.getId());
                writer.write(compactMode ? "]" : " ] ");
            }
            dartWritten = true;
            Cascade<Subject> cascade = c.getImage().front().cascade();

            for(var ref : cascade.toEnd()) {
                Xray key = ref.key().asExpected();
                Xray value = ref.asExpected();
                if(key.getObject() == Jorg.terminator || value.getObject() == Jorg.terminator) {
                    writer.write(compactMode ? "#]" : " #\n ] ");
                    dartWritten = true;
                } else {
                    if(key.getObject() instanceof Suite.Add) {
                        if(!dartWritten) {
                            writer.write(compactMode ? "]" : "\n ] ");
                        }
                    } else {
                        if (!compactMode) {
                            if (crystals.getFalls() > 1 || !"0".equals(c.getId()) || !rootMode) {
                                writer.write("\n ");
                            } else {
                                writer.write("\n");
                            }
                        }
                        writer.write(compactMode ? "[" : "[ ");
                        if (key.getId() == null) {
                            writer.write(stringify(key.getObject()));
                        } else {
                            writer.write("#");
                            writer.write(escapedHumble(key.getId(), false));
                        }
                        writer.write(compactMode ? "]" : " ] ");
                    }
                    if (value.getId() == null) {
                        writer.write(stringify(value.getObject()));
                    } else {
                        writer.write("#");
                        writer.write(escapedHumble(value.getId(), false));
                    }
                    dartWritten = false;
                }
            }
            if(!compactMode)writer.write("\n\n");
        }
        writer.flush();
        output.close();
    }

    private String stringify(Object object) throws JorgWriteException {
        if(object instanceof String) {
            return escaped((String)object);
        } else if(object instanceof Integer) {
            return "" + object;
        } else if(object instanceof Double) {
            return "" + object;
        } else if (object instanceof Boolean) {
            return (Boolean) object ? "+" : "-";
        } else if(object instanceof Reference) {
            return "#" + escapedHumble(((Reference) object).getId(), false);
        } else if(object == null || object instanceof Suite.Add) {
            return "";
        } else {
            throw new JorgWriteException("Unrecognized object type " + object.getClass());
        }
    }

    private String escaped(String str) {
        String humble = escapedHumble(str, true);
        int humbleLength = humble.length();
        if(humbleLength > 0) {
            if(humbleLength <= str.length() + 2) {
                return humble;
            }
            StringBuilder stringBuilder = new StringBuilder("\"");
            str.chars().forEach(cp -> {
                if(cp == '"' || JorgProcessor.isEscapeCharacter(cp)) {
                    stringBuilder.appendCodePoint(escapeCharacter);
                }
                stringBuilder.appendCodePoint(cp);
            });
            stringBuilder.append('"');
            String quoted = stringBuilder.toString();
            return humbleLength < quoted.length() ? humble : quoted;

        } else return "\"\"";
    }

    private String escapedHumble(String str, boolean escapeNonHumbleEntry) {
        if(str == null || str.isEmpty())return "";
        StringBuilder stringBuilder = new StringBuilder(str.length());
        if(escapeNonHumbleEntry) {
            int startCodePoint = str.codePointAt(0);
            if(!Character.isJavaIdentifierStart(startCodePoint) &&
                    !JorgProcessor.isControlCharacter(startCodePoint) &&
                    !JorgProcessor.isEscapeCharacter(startCodePoint)) {
                stringBuilder.appendCodePoint(escapeCharacter);
            }
        }
        AtomicReference<StringBuilder> endSpaces = new AtomicReference<>(new StringBuilder());
        str.chars().forEach(cp -> {
            if(Character.isWhitespace(cp)) {
                endSpaces.get().appendCodePoint(cp);
            } else {
                if(endSpaces.get().length() > 0) {
                    endSpaces.get().chars().forEach(stringBuilder::appendCodePoint);
                    endSpaces.set(new StringBuilder());
                }
                if (JorgProcessor.isControlCharacter(cp) || JorgProcessor.isEscapeCharacter(cp)) {
                    stringBuilder.appendCodePoint(escapeCharacter);
                }
                stringBuilder.appendCodePoint(cp);
            }
        });
        if(endSpaces.get().length() > 0) {
            endSpaces.get().chars().forEach(ws -> stringBuilder.appendCodePoint(escapeCharacter).appendCodePoint(ws));
        }
        return stringBuilder.toString();
    }

}
