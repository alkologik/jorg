package jorg.jorg;

import jorg.processor.IntProcessor;
import jorg.processor.ProcessorException;
import suite.suite.Subject;
import suite.suite.Suite;

public class JorgProcessor implements IntProcessor {

    private static final Xkey nullXkey = new Xkey(null, null, true);
    private static final Xkey terminatorXkey = new Xkey(Jorg.terminator, null, true);

    enum State {
        AT, FROM, DIRECT, POST_DIRECT, VIA, POST_VIA, TO, POST_TO
    }

    enum DataState {
        PENDING, HUMBLE_STRING, STRING, ODD_STRING, REFERENCE, NUMBER, DOUBLE, FLOAT, MINUS, PLUS
    }

    enum OddStringState {
        OPEN, CONTENT, CLOSE
    }

    private Subject keys;
    private State state;
    private DataState dataState;
    private StringBuilder dataBuilder;
    private Xkey from;
    private Xkey via;
    private Xkey to;

    private boolean lastEscapeCharacter;
    private OddStringState oddStringState;
    private String tag;
    private StringBuilder tagBuilder;

    private void resetDataBuilder() {
        dataBuilder = new StringBuilder();
        lastEscapeCharacter = false;
    }

    private void directFinish(int i) throws ProcessorException {
        from.add(to);
        dataState = DataState.PENDING;
        resetDataBuilder();
        state = switch (i) {
            case ']' -> State.DIRECT;
            case '[' -> State.VIA;
            case '#' -> State.AT;
            default -> throw new ProcessorException();
        };
    }

    private void viaFinish(int i) throws ProcessorException {
        dataState = DataState.PENDING;
        resetDataBuilder();
        state = switch (i) {
            case ']' -> State.TO;
            case '[' -> {
                from.set(via, nullXkey);
                yield State.VIA;
            }
            case '#' -> {
                from.set(via, nullXkey);
                yield State.AT;
            }
            default -> throw new ProcessorException();
        };
    }

    private void toFinish(int i) throws ProcessorException {
        from.set(via, to);
        dataState = DataState.PENDING;
        resetDataBuilder();
        state = switch (i) {
            case '[' -> State.VIA;
            case ']' -> State.DIRECT;
            case '#' -> State.AT;
            default -> throw new ProcessorException();
        };
    }


    public static boolean isControlCharacter(int codePoint) {
        return codePoint == ']' || codePoint == '#' || codePoint == '[';
    }

    public static boolean isEscapeCharacter(int codePoint) {
        return codePoint == '`';
    }

    @Override
    public Subject ready() {
        keys = Suite.set();
        Reference zero = new Reference("0", true);
        from = new Xkey(null, zero, false);
        via = to = null;
        keys.set(zero, from);
        state = State.DIRECT;
        dataState = DataState.PENDING;
        resetDataBuilder();
        return Suite.set();
    }

    public void advance(int i) throws ProcessorException {
        switch (state) {
            case AT:
                if(i == '[') {
                    state = State.FROM;
                    dataState = DataState.PENDING;
                    resetDataBuilder();
                } else if(i == ']') {
                    to = terminatorXkey;
                    directFinish(i);
                } else if(!Character.isWhitespace(i)) {
                    throw new ProcessorException("Invalid input '" + new StringBuilder().appendCodePoint(i) + "'");
                }
                break;

            case FROM:
                if(lastEscapeCharacter) {
                    dataBuilder.appendCodePoint(i);
                    lastEscapeCharacter = false;
                } else if(isControlCharacter(i)) {
                    String string = dataBuilder.toString().trim();
                    if(string.isEmpty()) throw new ProcessorException("Empty reference");
                    Reference reference = new Reference(string);
                    from = keys.getSaved(reference, new Xkey(null, reference, false)).asExpected();
                    ((Reference)from.getLabel()).setDeclared(true);
                    dataState = DataState.PENDING;
                    resetDataBuilder();
                    state = switch (i) {
                        case ']' -> State.DIRECT;
                        case '[' -> State.VIA;
                        case '#' -> State.AT;
                        default -> throw new ProcessorException();
                    };
                } else if(isEscapeCharacter(i)) {
                    lastEscapeCharacter = true;
                } else {
                    dataBuilder.appendCodePoint(i);
                }
                break;

            case DIRECT:
                switch (dataState) {
                    case PENDING:
                        if(Character.isJavaIdentifierStart(i)) {
                            dataState = DataState.HUMBLE_STRING;
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '#') {
                            dataState = DataState.REFERENCE;
                        } else if(i == '"') {
                            dataState = DataState.STRING;
                        } else if(Character.isDigit(i)) {
                            dataState = DataState.NUMBER;
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("0.");
                        } else if(i == '-') {
                            dataState = DataState.MINUS;
                        } else if(i == '+') {
                            dataState = DataState.PLUS;
                        } else if(isEscapeCharacter(i)) {
                            dataState = DataState.HUMBLE_STRING;
                            lastEscapeCharacter = true;
                        } else if(i == '@') {
                            dataState = DataState.ODD_STRING;
                            oddStringState = OddStringState.OPEN;
                            tagBuilder = new StringBuilder();
                        } else if(i == '[') {
                            state = State.VIA;
                        } else if(i == ']') {
                            if(to != null || via != null) {
                                from.add(nullXkey);
                            } else {
                                to = nullXkey;
                            }
                        } else if(!Character.isWhitespace(i)) {
                                throw new ProcessorException("Invalid input");
                        }
                        break;

                    case HUMBLE_STRING:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(isControlCharacter(i)) {
                            String string = dataBuilder.toString().trim();
                            to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                            directFinish(i);
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case REFERENCE:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(isControlCharacter(i)) {
                            String string = dataBuilder.toString().trim();
                            if (string.isEmpty()) {
                                if (i == '[') {
                                    state = State.FROM;
                                } else if(i == ']') {
                                    from.add(nullXkey);
                                    to = terminatorXkey;
                                    directFinish(i);
                                } else {
                                    throw new ProcessorException();
                                }
                            } else {
                                Reference reference = new Reference(string);
                                to = keys.getSaved(reference, new Xkey(null, reference, false)).asExpected();
                                directFinish(i);
                            }
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case STRING:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(i == '"') {
                            String string = dataBuilder.toString();
                            to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                            from.add(to);
                            dataState = DataState.PENDING;
                            resetDataBuilder();
                            state = State.POST_DIRECT;
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case ODD_STRING:
                        switch (oddStringState) {
                            case OPEN -> {
                                if(i == '"') {
                                    tag = tagBuilder.toString();
                                    oddStringState = OddStringState.CONTENT;
                                } else {
                                    tagBuilder.appendCodePoint(i);
                                }
                            }
                            case CONTENT -> {
                                if(i == '"') {
                                    if(tag.isEmpty()) {
                                        String string = dataBuilder.toString();
                                        to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                                        from.add(to);
                                        dataState = DataState.PENDING;
                                        resetDataBuilder();
                                        state = State.POST_DIRECT;
                                    } else {
                                        oddStringState = OddStringState.CLOSE;
                                        tagBuilder = new StringBuilder();
                                    }
                                } else {
                                    dataBuilder.appendCodePoint(i);
                                }
                            }
                            case CLOSE -> {
                                tagBuilder.appendCodePoint(i);
                                if(tagBuilder.length() == tag.length()) {
                                    if(tagBuilder.toString().equals(tag)) {
                                        String string = dataBuilder.toString();
                                        to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                                        from.add(to);
                                        dataState = DataState.PENDING;
                                        resetDataBuilder();
                                        state = State.POST_DIRECT;
                                    } else {
                                        dataBuilder.append(tagBuilder);
                                        oddStringState = OddStringState.CONTENT;
                                    }
                                }
                            }
                        }
                        break;

                    case NUMBER:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append(".");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append(".");
                        } else if(isControlCharacter(i)) {
                            int integer = Integer.parseInt(dataBuilder.toString());
                            to = keys.getSaved(integer, new Xkey(integer, null, true)).asExpected();
                            directFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case DOUBLE:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(isControlCharacter(i)) {
                            double d = Double.parseDouble(dataBuilder.toString());
                            to = keys.getSaved(d, new Xkey(d, null, true)).asExpected();
                            directFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case FLOAT:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(isControlCharacter(i)) {
                            float f = Float.parseFloat(dataBuilder.toString());
                            to = keys.getSaved(f, new Xkey(f, null, true)).asExpected();
                            directFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case MINUS:
                        if(Character.isDigit(i)) {
                            dataBuilder.append('-').appendCodePoint(i);
                            dataState = DataState.NUMBER;
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("-0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("-0.");
                        } else if(isControlCharacter(i)) {
                            to = keys.getSaved(false, new Xkey(false, null, true)).asExpected();
                            directFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case PLUS:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                            dataState = DataState.NUMBER;
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("0.");
                        } else if(isControlCharacter(i)) {
                            to = keys.getSaved(true, new Xkey(true, null, true)).asExpected();
                            directFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;
                }
                break;

            case POST_DIRECT:
                state = switch (i) {
                    case ']' -> State.DIRECT;
                    case '[' -> State.VIA;
                    case '#' -> State.AT;
                    default -> {
                        if(Character.isWhitespace(i)){
                            yield State.POST_DIRECT;
                        }
                        throw new ProcessorException();
                    }
                };
                break;

            case VIA:
                switch (dataState) {
                    case PENDING:
                        if(Character.isJavaIdentifierStart(i)) {
                            dataState = DataState.HUMBLE_STRING;
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '#') {
                            dataState = DataState.REFERENCE;
                        } else if(i == '"') {
                            dataState = DataState.STRING;
                        } else if(Character.isDigit(i)) {
                            dataState = DataState.NUMBER;
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("0.");
                        } else if(i == '-') {
                            dataState = DataState.MINUS;
                        } else if(i == '+') {
                            dataState = DataState.PLUS;
                        } else if(isEscapeCharacter(i)) {
                            dataState = DataState.HUMBLE_STRING;
                            lastEscapeCharacter = true;
                        } else if(i == '@') {
                            dataState = DataState.ODD_STRING;
                            oddStringState = OddStringState.OPEN;
                            tagBuilder = new StringBuilder();
                        } else if(i == ']') {
                            via = nullXkey;
                            viaFinish(i);
                        } else if(!Character.isWhitespace(i)){
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case HUMBLE_STRING:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(isControlCharacter(i)) {
                            String string = dataBuilder.toString().trim();
                            via = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                            viaFinish(i);
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case REFERENCE:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(isControlCharacter(i)) {
                            String string = dataBuilder.toString().trim();
                            if(string.isEmpty()) {
                                if (i == '[') {
                                    from.set(nullXkey, nullXkey);
                                    state = State.FROM;
                                } else if(i == ']') {
                                    to = terminatorXkey;
                                    directFinish(i);
                                } else {
                                    throw new ProcessorException();
                                }
                            } else {
                                Reference reference = new Reference(string);
                                via = keys.getSaved(reference, new Xkey(null, reference, false)).asExpected();
                                viaFinish(i);
                            }
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case STRING:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(i == '"') {
                            String string = dataBuilder.toString();
                            via = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                            dataState = DataState.PENDING;
                            resetDataBuilder();
                            state = State.POST_VIA;
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case ODD_STRING:
                        switch (oddStringState) {
                            case OPEN -> {
                                if(i == '"') {
                                    tag = tagBuilder.toString();
                                    oddStringState = OddStringState.CONTENT;
                                } else {
                                    tagBuilder.appendCodePoint(i);
                                }
                            }
                            case CONTENT -> {
                                if(i == '"') {
                                    if(tag.isEmpty()) {
                                        String string = dataBuilder.toString();
                                        via = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                                        dataState = DataState.PENDING;
                                        resetDataBuilder();
                                        state = State.POST_VIA;
                                    } else {
                                        oddStringState = OddStringState.CLOSE;
                                        tagBuilder = new StringBuilder();
                                    }
                                } else {
                                    dataBuilder.appendCodePoint(i);
                                }
                            }
                            case CLOSE -> {
                                tagBuilder.appendCodePoint(i);
                                if(tagBuilder.length() == tag.length()) {
                                    if(tagBuilder.toString().equals(tag)) {
                                        String string = dataBuilder.toString();
                                        via = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                                        dataState = DataState.PENDING;
                                        resetDataBuilder();
                                        state = State.POST_VIA;
                                    } else {
                                        dataBuilder.append(tagBuilder);
                                        oddStringState = OddStringState.CONTENT;
                                    }
                                }
                            }
                        }
                        break;

                    case NUMBER:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append(".");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append(".");
                        } else if(isControlCharacter(i)) {
                            int integer = Integer.parseInt(dataBuilder.toString());
                            via = keys.getSaved(integer, new Xkey(integer, null, true)).asExpected();
                            viaFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case DOUBLE:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(isControlCharacter(i)) {
                            double d = Double.parseDouble(dataBuilder.toString());
                            via = keys.getSaved(d, new Xkey(d, null, true)).asExpected();
                            viaFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case FLOAT:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(isControlCharacter(i)) {
                            float f = Float.parseFloat(dataBuilder.toString());
                            via = keys.getSaved(f, new Xkey(f, null, true)).asExpected();
                            viaFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case MINUS:
                        if(Character.isDigit(i)) {
                            dataBuilder.append('-').appendCodePoint(i);
                            dataState = DataState.NUMBER;
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("-0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("-0.");
                        } else if(isControlCharacter(i)) {
                            via = keys.getSaved(false, new Xkey(false, null, true)).asExpected();
                            viaFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case PLUS:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                            dataState = DataState.NUMBER;
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("0.");
                        } else if(isControlCharacter(i)) {
                            via = keys.getSaved(true, new Xkey(true, null, true)).asExpected();
                            viaFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                }
                break;

            case POST_VIA:
                if(!Character.isWhitespace(i)) {
                    viaFinish(i);
                }
                break;

            case TO:
                switch (dataState) {
                    case PENDING:
                        if(Character.isJavaIdentifierStart(i)) {
                            dataState = DataState.HUMBLE_STRING;
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '#') {
                            dataState = DataState.REFERENCE;
                        } else if(i == '"') {
                            dataState = DataState.STRING;
                        } else if(Character.isDigit(i)) {
                            dataState = DataState.NUMBER;
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("0.");
                        } else if(i == '-') {
                            dataState = DataState.MINUS;
                        } else if(i == '+') {
                            dataState = DataState.PLUS;
                        } else if(i == '@') {
                            dataState = DataState.ODD_STRING;
                            oddStringState = OddStringState.OPEN;
                            tagBuilder = new StringBuilder();
                        } else if(i == '[' || i == ']') {
                            to = nullXkey;
                            toFinish(i);
                        } else if(isEscapeCharacter(i)) {
                            dataState = DataState.HUMBLE_STRING;
                            lastEscapeCharacter = true;
                        } else if(!Character.isWhitespace(i)){
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case HUMBLE_STRING:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(isControlCharacter(i)) {
                            String string = dataBuilder.toString().trim();
                            to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                            toFinish(i);
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case REFERENCE:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(isControlCharacter(i)) {
                            String string = dataBuilder.toString().trim();
                            if(string.isEmpty()) {
                                if (i == '[') {
                                    from.set(via, nullXkey);
                                    state = State.FROM;
                                } else if(i == ']') {
                                    from.set(via, nullXkey);
                                    to = terminatorXkey;
                                    directFinish(i);
                                } else {
                                    throw new ProcessorException();
                                }
                            } else {
                                Reference reference = new Reference(string);
                                to = keys.getSaved(reference, new Xkey(null, reference, false)).asExpected();
                                toFinish(i);
                            }
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case STRING:
                        if(lastEscapeCharacter) {
                            dataBuilder.appendCodePoint(i);
                            lastEscapeCharacter = false;
                        } else if(i == '"') {
                            String string = dataBuilder.toString();
                            to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                            state = State.POST_TO;
                        } else if(isEscapeCharacter(i)) {
                            lastEscapeCharacter = true;
                        } else {
                            dataBuilder.appendCodePoint(i);
                        }
                        break;

                    case ODD_STRING:
                        switch (oddStringState) {
                            case OPEN -> {
                                if(i == '"') {
                                    tag = tagBuilder.toString();
                                    oddStringState = OddStringState.CONTENT;
                                } else {
                                    tagBuilder.appendCodePoint(i);
                                }
                            }
                            case CONTENT -> {
                                if(i == '"') {
                                    if(tag.isEmpty()) {
                                        String string = dataBuilder.toString();
                                        to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                                        state = State.POST_TO;
                                    } else {
                                        oddStringState = OddStringState.CLOSE;
                                        tagBuilder = new StringBuilder();
                                    }
                                } else {
                                    dataBuilder.appendCodePoint(i);
                                }
                            }
                            case CLOSE -> {
                                tagBuilder.appendCodePoint(i);
                                if(tagBuilder.length() == tag.length()) {
                                    if(tagBuilder.toString().equals(tag)) {
                                        String string = dataBuilder.toString();
                                        to = keys.getSaved(string, new Xkey(string, null, true)).asExpected();
                                        state = State.POST_TO;
                                    } else {
                                        dataBuilder.append(tagBuilder);
                                        oddStringState = OddStringState.CONTENT;
                                    }
                                }
                            }
                        }
                        break;

                    case NUMBER:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append(".");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append(".");
                        } else if(isControlCharacter(i)) {
                            int integer = Integer.parseInt(dataBuilder.toString());
                            to = keys.getSaved(integer, new Xkey(integer, null, true)).asExpected();
                            toFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case DOUBLE:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(isControlCharacter(i)) {
                            double d = Double.parseDouble(dataBuilder.toString());
                            to = keys.getSaved(d, new Xkey(d, null, true)).asExpected();
                            toFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case FLOAT:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                        } else if(isControlCharacter(i)) {
                            float f = Float.parseFloat(dataBuilder.toString());
                            to = keys.getSaved(f, new Xkey(f, null, true)).asExpected();
                            toFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case MINUS:
                        if(Character.isDigit(i)) {
                            dataBuilder.append('-').appendCodePoint(i);
                            dataState = DataState.NUMBER;
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("-0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("-0.");
                        } else if(isControlCharacter(i)) {
                            to = keys.getSaved(false, new Xkey(false, null, true)).asExpected();
                            toFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                    case PLUS:
                        if(Character.isDigit(i)) {
                            dataBuilder.appendCodePoint(i);
                            dataState = DataState.NUMBER;
                        } else if(i == '.') {
                            dataState = DataState.DOUBLE;
                            dataBuilder.append("0.");
                        } else if(i == ',') {
                            dataState = DataState.FLOAT;
                            dataBuilder.append("0.");
                        } else if(isControlCharacter(i)) {
                            to = keys.getSaved(true, new Xkey(true, null, true)).asExpected();
                            toFinish(i);
                        } else if(!Character.isWhitespace(i)) {
                            throw new ProcessorException("Invalid input");
                        }
                        break;

                }
                break;

            case POST_TO:
                if(!Character.isWhitespace(i)){
                    toFinish(i);
                }
                break;
        }
    }

    @Override
    public Subject finish() throws ProcessorException {
        String str;
        switch (state) {
            case FROM -> {
                str = dataBuilder.toString().trim();
                if (str.isEmpty()) {
                    throw new ProcessorException();
                } else {
                    Reference reference = new Reference(str);
                    from = keys.getSaved(reference, new Xkey(null, reference, false)).asExpected();
                    ((Reference) from.getLabel()).setDeclared(true);
                }
            }
            case DIRECT, TO, POST_DIRECT, POST_TO, VIA, POST_VIA -> {
                switch (dataState) {
                    case HUMBLE_STRING -> {
                        str = dataBuilder.toString().trim();
                        to = keys.getSaved(str, new Xkey(str, null, true)).asExpected();
                    }

                    case REFERENCE -> {
                        str = dataBuilder.toString().trim();
                        if (str.isEmpty()) {
                            throw new ProcessorException();
                        } else {
                            Reference reference = new Reference(str);
                            to = keys.getSaved(reference, new Xkey(null, reference, false)).asExpected();
                        }
                    }

                    case STRING, ODD_STRING -> {
                        str = dataBuilder.toString();
                        to = keys.getSaved(str, new Xkey(str, null, true)).asExpected();
                    }

                    case NUMBER -> {
                        str = dataBuilder.toString();
                        int integer = Integer.parseInt(str);
                        to = keys.getSaved(integer, new Xkey(integer, null, true)).asExpected();
                    }

                    case DOUBLE -> {
                        str = dataBuilder.toString();
                        double d = Double.parseDouble(str);
                        to = keys.getSaved(d, new Xkey(d, null, true)).asExpected();
                    }

                    case FLOAT -> {
                        str = dataBuilder.toString();
                        float f = Float.parseFloat(str);
                        to = keys.getSaved(f, new Xkey(f, null, true)).asExpected();
                    }

                    case MINUS -> to = keys.getSaved(false, new Xkey(false, null, true)).asExpected();

                    case PLUS -> to = keys.getSaved(true, new Xkey(true, null, true)).asExpected();

                    case PENDING -> to = nullXkey;

                }

                    if (state == State.DIRECT || state == State.POST_DIRECT) {
                        if (dataState != DataState.PENDING) {
                            from.add(to);
                        }
                    } else if(state == State.VIA || state == State.POST_VIA) {
                        from.set(to, nullXkey);
                    } else from.set(via, to);
            }
        }
        return keys;
    }
}
