package org.hashdb.ms.compiler.keyword.ctx.sys;

import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.exception.CommandInterpretException;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.net.ConnectionSession;

import java.util.Collection;

/**
 * Date: 2023/11/30 15:53
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DBShowCtx extends SystemCompileCtx<Collection<DatabaseInfos>> {
    @Override
    OpsTask<Collection<DatabaseInfos>> doCompile(SystemCompileStream stream) {
        stream.toWrite();
        try {
            String token = stream.token();
            throw new CommandInterpretException("unknown token '" + token + "'");
        } catch (ArrayIndexOutOfBoundsException ignore) {
        }
        return executor(stream.session());
    }

    @Override
    public OpsTask<Collection<DatabaseInfos>> executor(ConnectionSession session) {
        return OpsTask.of(() -> system().getSystemInfo().getDatabaseInfosMap().keySet());
    }

    @Override
    public SystemKeyword name() {
        return SystemKeyword.DBSHOW;
    }
}
