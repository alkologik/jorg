package jorg.jorg;

import jorg.processor.IntProcessor;
import suite.suite.Subject;
import suite.suite.Suite;

import java.util.Stack;

public class BratProcessor implements IntProcessor {

    enum State {
        DEFAULT, FENCE, FENCED
    }

    private int extendSign = '[';
    private int closeSign = ']';
    private int fenceSign = '~';
    private Stack<Subject> branch;
    private Subject work;
    private State state;
    private StringBuilder primaryBuilder;
    private StringBuilder secondaryBuilder;
    private String fence;

    public BratProcessor() {
    }

    void updateSecondaryBuilder(String extension) {
        if(secondaryBuilder == null) {
            secondaryBuilder = new StringBuilder(extension);
        } else secondaryBuilder.append(extension);
    }

    @Override
    public Subject ready() {
        branch = new Stack<>();
        work = Suite.set();
        state = State.DEFAULT;
        primaryBuilder = new StringBuilder();
        secondaryBuilder = new StringBuilder();
        return Suite.set();
    }

    public void advance(int i) {
        switch (state) {
            case DEFAULT:
                if (i == extendSign) {
                    Subject newWork = Suite.set();
                    updateSecondaryBuilder(primaryBuilder.toString().trim());
                    if(secondaryBuilder.length() > 0) {
                        work.set(secondaryBuilder.toString(), newWork);
                    } else {
                        work.add(newWork);
                    }
                    branch.add(work);
                    work = newWork;
                    primaryBuilder = new StringBuilder();
                    secondaryBuilder = new StringBuilder();
                 } else if (i == closeSign) {
                    updateSecondaryBuilder(primaryBuilder.toString().trim());
                    if(work.isEmpty()) {
                        work.set(secondaryBuilder.toString());
                    }
                    work = branch.pop();
                    primaryBuilder = new StringBuilder();
                    secondaryBuilder = new StringBuilder();
                } else if(i == fenceSign) {
                    updateSecondaryBuilder(primaryBuilder.toString().trim());
                    primaryBuilder = new StringBuilder();
                    state = State.FENCE;
                } else {
                    primaryBuilder.appendCodePoint(i);
                }
                break;

            case FENCE:
                if (i == extendSign) {
                    fence = closeSign + primaryBuilder.toString() + fenceSign;
                    primaryBuilder = new StringBuilder();
                    state = State.FENCED;
                } else {
                    primaryBuilder.appendCodePoint(i);
                }
                break;

            case FENCED:
                primaryBuilder.appendCodePoint(i);
                if (i == fenceSign) {
                    int fenceStartIndex = primaryBuilder.length() - fence.length();
                    if (fenceStartIndex >= 0 && primaryBuilder.indexOf(fence, fenceStartIndex) != -1) {
                        updateSecondaryBuilder(primaryBuilder.substring(0, fenceStartIndex));
                        primaryBuilder = new StringBuilder();
                        state = State.DEFAULT;
                    }
                }
                break;
        }
    }

    @Override
    public Subject finish() {
        if(state == State.DEFAULT) {
            updateSecondaryBuilder(primaryBuilder.toString().trim());
            if(work.isEmpty()) work.set(secondaryBuilder.toString());
        } else if(state == State.FENCED) {
            updateSecondaryBuilder(primaryBuilder.toString());
            if(work.isEmpty()) work.set(secondaryBuilder.toString());
        }
        while (!branch.empty()) work = branch.pop();
        return work;
    }
}
