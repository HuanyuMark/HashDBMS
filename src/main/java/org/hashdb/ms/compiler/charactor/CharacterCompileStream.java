package org.hashdb.ms.compiler.charactor;

import lombok.Getter;
import org.hashdb.ms.compiler.charactor.node.keyword.supplier.GetNode;
import org.hashdb.ms.compiler.charactor.node.keyword.supplier.SupplierKeywordNode;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.UnknownTokenException;
import org.hashdb.ms.data.Database;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Date: 2023/12/8 16:36
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CharacterCompileStream {
    protected final char[] tokens;

    protected final Database database;

    @Getter
    protected final String command;

    protected int cursor = 0;

    protected SupplierKeywordNode keywordNode;

    @Contract(pure = true)
    public CharacterCompileStream(Database database, @NotNull String command) {
        Objects.requireNonNull(database);
        Objects.requireNonNull(command);
        command = command.trim();
        if (command.isEmpty()) {
            throw new CommandCompileException("command is empty or blank");
        }
        this.command = command;
        this.database = database;
        tokens = command.toCharArray();
    }

    protected SupplierKeywordNode compile() {
        if (keywordNode != null) {
            throw new IllegalStateException("command is compiled");
        }
        keywordNode = doCompile();
        return keywordNode;
    }

    /**
     * G
     * S
     * R
     * K
     * K
     * C
     * V
     * E
     * D
     * C
     * T
     * F
     * T
     * E
     * I
     * M
     */
    protected SupplierKeywordNode doCompile() {
        char token;
        try {
            token = token();
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
        return switch (token) {
            case 'g', 'G' -> new GetNode(this);
            // TODO: 2023/12/8 继续写其它Supplier编译结点
            default -> throw new UnknownTokenException("unknown token: " + token);
        };
    }

    public char token() throws ArrayIndexOutOfBoundsException {
        return tokens[cursor];
    }

    public char nextToken() throws ArrayIndexOutOfBoundsException {
        return tokens[++cursor];
    }

    public void next() {
        ++cursor;
    }

    public void prev() {
        --cursor;
    }

    public int cursor() {
        return cursor;
    }

    public Database db() {
        return database;
    }

    public boolean isDone() {
        return keywordNode != null;
    }

    public String near() {
        return String.valueOf(tokens, 0, Math.min(cursor, tokens.length));
    }
}
