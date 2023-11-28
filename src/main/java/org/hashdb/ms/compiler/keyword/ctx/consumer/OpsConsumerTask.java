package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.data.OpsTask;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Date: 2023/11/25 14:00
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface OpsConsumerTask<I, O> extends Function<I, Supplier<O>> {
}
