package org.hashdb.ms.compiler.keyword.ctx.sys;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.exception.CommandInterpretException;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.net.ConnectionSession;

/**
 * Date: 2023/11/30 14:46
 * CREATE $ID $NAME
 *
 * @author huanyuMake-pecdle
 */
public class DBCreateCtx extends SystemCompileCtx<Boolean> {

    @JsonProperty
    protected Integer id;

    @JsonProperty
    protected String name;

    @Override
    OpsTask<Boolean> doCompile(SystemCompileStream stream) {
        while (true) {
            String token;
            try {
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
            if (id != null) {
                throw new CommandInterpretException("redundancy param: 'database id'");
            }
            try {
                id = Integer.valueOf(token);
                stream.next();
            } catch (NumberFormatException e) {
                try {
                    Double.parseDouble(token);
                    throw new CommandInterpretException("keyword '" + name() + "' require integer param 'database id'");
                } catch (NumberFormatException ignore) {
                }
                if (name != null) {
                    throw new CommandInterpretException("redundancy param: 'database id'");
                }
                name = token;
                stream.next();
            }
        }
        if (name == null) {
            throw new CommandInterpretException("keyword '" + name() + "' require param 'database name'");
        }
        return executor(stream.session());
    }

    @Override
    public OpsTask<Boolean> executor(ConnectionSession session) {
        return OpsTask.of(() -> {
            system().newDatabase(id, name);
            return Boolean.TRUE;
        });
    }

    @Override
    public SystemKeyword name() {
        return SystemKeyword.DBCREATE;
    }
}
