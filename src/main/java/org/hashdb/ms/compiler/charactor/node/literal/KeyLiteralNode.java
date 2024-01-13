package org.hashdb.ms.compiler.charactor.node.literal;

import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;

import java.util.LinkedList;

/**
 * Date: 2023/12/8 17:37
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class KeyLiteralNode extends LiteralNode<String> {
    public KeyLiteralNode(CharacterCompileStream stream) {
        super(stream);
    }

    @Override
    public void compile() throws ArrayIndexOutOfBoundsException {
        LinkedList<Character> symbols = new LinkedList<>();
        while (true) {
            char token;
            try {
                token = stream.nextToken();
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
            switch (token) {
                case '(' -> {
                    if (!symbols.isEmpty() && symbols.getLast().equals(')')) {
                        throw new CommandCompileException();
                    }
                    symbols.add(token);
                }
                case ')' -> {

                }
            }
        }
    }

    @Override
    public String get() {
        return null;
    }
}
