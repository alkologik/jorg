package jorg.jorg;

import jorg.processor.IntProcessor;
import jorg.processor.ProcessorException;
import suite.suite.Subject;
import suite.suite.Suite;

import java.util.ArrayList;
import java.util.List;

public class PathProcessor implements IntProcessor {

    enum State {
        STRING, INDEX
    }

    enum InputState {
        DEFAULT, BREAK, DOUBLE_BREAK
    }

    private State state;
    private InputState inputState;
    private List<Path.Token> tokens;
    private StringBuilder builder;

    private int lastBreakCodePoint;

    @Override
    public Subject ready() {
        tokens = new ArrayList<>();
        builder = new StringBuilder();
        state = State.STRING;
        inputState = InputState.DEFAULT;
        return Suite.set();
    }

    boolean breakCodePoint(int cp) {
        return cp == '/' || cp == '\\';
    }

    void closeBranch(boolean resetBuilder) {
        if(builder.length() > 0) {
            String str = builder.toString();
            if (state == State.STRING) {
                tokens.add(new Path.StringToken(str));
            } else if (state == State.INDEX) {
                tokens.add(new Path.IntToken(Integer.parseInt(str)));
            }
            if(resetBuilder)builder = new StringBuilder();
        }
    }


    public void advance(int i) {

        switch (inputState) {
            case DEFAULT:
                if(breakCodePoint(i)) {
                    lastBreakCodePoint = i;
                    inputState = InputState.BREAK;
                } else {
                    builder.appendCodePoint(i);
                }
                break;
            case BREAK:
                if(breakCodePoint(i)) {
                    if(lastBreakCodePoint == i) {
                        inputState = InputState.DOUBLE_BREAK;
                    } else {
                        builder.appendCodePoint(lastBreakCodePoint);
                        inputState = InputState.DEFAULT;
                    }
                } else {
                    closeBranch(true);
                    builder.appendCodePoint(i);
                    state = State.STRING;
                    inputState = InputState.DEFAULT;
                }
                break;
            case DOUBLE_BREAK:
                closeBranch(true);
                if(breakCodePoint(i)) {
                    if(lastBreakCodePoint != i) {
                        builder.appendCodePoint(lastBreakCodePoint);
                        state = State.STRING;
                        inputState = InputState.DEFAULT;
                    }
                } else {
                    builder.appendCodePoint(i);
                    state = State.INDEX;
                    inputState = InputState.DEFAULT;
                }
                break;
        }
    }

    @Override
    public Subject finish() {
        closeBranch(false);
        return Suite.set(new Path(tokens));
    }

    public Subject process(String str) {
        return process(() -> str.chars().iterator());
    }

    public Subject process(Iterable<Integer> it) {
        ready();
        for(int i : it) {
            advance(i);
        }
        return finish();
    }
}
