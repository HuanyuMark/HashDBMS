package org.hashdb.ms.handler.ops;

import org.hashdb.ms.handler.AsyncOpsConsumer;
import org.hashdb.ms.handler.OpsConsumer;
import org.hashdb.ms.handler.OpsEvent;
import org.hashdb.ms.util.AsyncService;

import java.util.concurrent.CompletableFuture;

/**
 * Date: 2023/11/21 1:43
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ReadKeyConsumer implements OpsConsumer<Object, Object> {
    @Override
    public Object consume(OpsEvent<Object> event) {
        return AsyncService.submit(()->{
//            event.getDatabase()
            return null;
        });
    }
}
