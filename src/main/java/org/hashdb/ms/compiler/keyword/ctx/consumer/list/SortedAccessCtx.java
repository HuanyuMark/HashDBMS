package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.exception.CommandExecuteException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/29 12:27
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class SortedAccessCtx extends MutableListCtx {
    protected SortedAccessCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    protected static  <T extends Comparable<T>> @NotNull List<T> sort(@NotNull List<T> target, @NotNull Function<T, Long> indexMapper) {
        List<T> indexes = target.stream().sorted().toList();
        Long smallestIndex = indexMapper.apply(indexes.getFirst());
        Long largestIndex = indexMapper.apply(indexes.getLast());
        if (smallestIndex < 0) {
            throw new CommandExecuteException("can not access by index '" + smallestIndex + "'");
        }
        if (largestIndex >= target.size()) {
            throw new CommandExecuteException("can not access by index '" + largestIndex + "'");
        }
        return indexes;
    }
}
