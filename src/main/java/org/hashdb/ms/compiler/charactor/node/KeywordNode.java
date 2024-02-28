package org.hashdb.ms.compiler.charactor.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hashdb.ms.compiler.charactor.CharacterCompileStream;
import org.hashdb.ms.compiler.charactor.NodeType;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.UnknownTokenException;
import org.hashdb.ms.compiler.keyword.Keyword;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2023/12/8 16:49
 *
 * @author Huanyu Mark
 */
public abstract class KeywordNode extends CompileNode {

    private final static Map<Keyword<?>, char[]> keywordCharsCache = new ConcurrentHashMap<>();

    protected KeywordNode(CharacterCompileStream stream) {
        super(stream);
        matchThisKeyword();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    abstract public Keyword<?> name();

    @Override
    public NodeType nodeType() {
        return NodeType.KEYWORD;
    }

    private void matchThisKeyword() {
        char[] keyword = keywordCharsCache.computeIfAbsent(name(), k -> k.name().toCharArray());
        for (int i = 1; i < keyword.length; i++) {
            char token;
            try {
                token = stream.nextToken();
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CommandCompileException("unknown keyword '" + stream.near() + "'");
            }
            if (Character.toUpperCase(token) != keyword[i]) {
                throw new UnknownTokenException("unknown token '" + token + "', near '" + stream.near() + "'");
            }
        }
    }

    private static boolean equalIgnoreCase(char c1, char keywordOneChar) {
        return Character.toUpperCase(c1) == keywordOneChar;
    }


    protected void compilePipe() {
    }
}