package jorg.jorg;

import suite.suite.Sub;
import suite.suite.Subject;
import suite.suite.Suite;

public class Linker {

    Subject $root;
    Subject $linked;

    public Subject link(Subject $tree) {
        $linked = Suite.set();
        return rqLink($root = $tree);
    }

    Subject rqLink(Subject $tree) {
        if($linked.get($tree).notEmpty()) return $tree;
        $linked.set($tree);
        if(isReference($tree)) {
            Subject $t = rqLink(find($tree));
            return $tree.unset().inset($t);
        }
        for(var $s : $tree) {
            if($s.instanceOf(Subject.class)) {
                rqLink($s.asExpected());
            }
        }
        return $tree;
    }

    boolean isReference(Subject $s) {
        return $s.size() == 1 && $s.instanceOf(String.class) && $s.asString().startsWith("#");
    }

    Subject find(Subject $path) {
        Path path = Path.parse($path.asString().substring(1));
        var $node = $root;
        Subject $t;
        for(Path.Token token : path.tokens) {
            if(token instanceof Path.StringToken) {
                $node = $node.at(((Path.StringToken)token).token);
            } else if(token instanceof Path.IntToken) {
                $t = $node.getAt(((Path.IntToken)token).token);
                if($t.isEmpty()) return Suite.set();
                else if($t.instanceOf(Subject.class)) {
                    $node = $t.asExpected();
                } else $node = Suite.set($t.direct());
            }
            rqLink($node);
        }
        return $node;
    }
}
