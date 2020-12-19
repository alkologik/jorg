package jorg.jorg;

import suite.suite.Subject;
import suite.suite.Suite;

import java.util.List;

public class Path {

    public interface Token {}

    public static class StringToken implements Token {
        String token;

        public StringToken(String token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return "StringToken{" +
                    "token='" + token + '\'' +
                    '}';
        }
    }

    public static class IntToken implements Token {
        int token;

        public IntToken(int token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return "IntToken{" +
                    "token=" + token +
                    '}';
        }
    }

    public static Path parse(String path) {
        return new PathProcessor().process(path).asExpected();
    }

    List<Token> tokens;

    public Path(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Subject follow(Subject $node) {
        Subject $t;
        for(Token token : tokens) {
            if(token instanceof StringToken) {
                $node = $node.at(((StringToken)token).token);
            } else if(token instanceof IntToken) {
                $t = $node.getAt(((IntToken)token).token);
                if($t.isEmpty()) return Suite.set();
                else if($t.instanceOf(Subject.class)) {
                    $node = $t.asExpected();
                } else $node = Suite.set($t.direct());
            }
        }
        return $node;
    }
}
