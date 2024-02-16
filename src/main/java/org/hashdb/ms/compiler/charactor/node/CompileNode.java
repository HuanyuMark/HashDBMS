package org.hashdb.ms.compiler.charactor.node;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.charactor.NodeType;

/**
 * Date: 2023/12/10 18:52
 *
 * @author huanyuMake-pecdle
 */
public abstract class CompileNode {
    protected final CharacterCompileStream stream;

    public CompileNode(CharacterCompileStream stream) {
        this.stream = stream;
    }

    abstract public void compile() throws ArrayIndexOutOfBoundsException;

    abstract public NodeType nodeType();

}
