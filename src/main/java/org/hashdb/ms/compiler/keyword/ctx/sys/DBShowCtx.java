package org.hashdb.ms.compiler.keyword.ctx.sys;

import org.hashdb.ms.compiler.SystemCompileStream;
import org.hashdb.ms.compiler.keyword.SystemKeyword;
import org.hashdb.ms.data.DatabaseInfos;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.CommandInterpretException;

import java.util.Collection;

/**
 * Date: 2023/11/30 15:53
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DBShowCtx extends SystemCompileCtx<Collection<DatabaseInfos>> {
    @Override
    Collection<DatabaseInfos> doInterpret(SystemCompileStream stream) {
        try {
            String token = stream.token();
            throw new CommandInterpretException("unknown token '" + token + "'");
        } catch (ArrayIndexOutOfBoundsException ignore) {
        }
        return system().submitOpsTaskSync(OpsTask.of(() -> system().getSystemInfo().getDatabaseInfosMap().keySet()));
    }

    @Override
    public SystemKeyword name() {
        return SystemKeyword.DBSHOW;
    }
}
