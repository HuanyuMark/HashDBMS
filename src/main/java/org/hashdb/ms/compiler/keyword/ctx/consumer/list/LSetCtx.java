package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.*;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.exception.CommandInterpretException;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Date: 2023/11/29 9:58
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LSetCtx extends MutableListCtx implements Precompilable {

    private final List<IndexValuePair> indexValuePairs = new LinkedList<>();

    private long listSize;

    public LSetCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LSET;
    }

    @Override
    protected void beforeCompile() {
        if (indexValuePairs.isEmpty()) {
            doCompile();
            beforeCompileInlineCommand();
        }
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        listSize = opsTarget.size();
        List<IndexValuePair> sortedIndexValuePairs = indexValuePairs.parallelStream().sorted().toList();
        Long smallestIndex = (Long) sortedIndexValuePairs.getFirst().indexOrSupplier;
        if (smallestIndex < 0) {
            throw new CommandExecuteException("index '" + smallestIndex + "' out of range." + stream().errToken(""));
        }
        Long largestIndex = (Long) sortedIndexValuePairs.getLast().indexOrSupplier;
        if (largestIndex >= opsTarget.size()) {
            throw new CommandExecuteException("index '" + largestIndex + "' out of range" + stream().errToken(""));
        }

        if (opsTarget instanceof RandomAccess) {
            return indexValuePairs.stream().map(pair -> {
                if (pair.valueOrSupplier instanceof SupplierCtx vs) {
                    pair.valueOrSupplier = selectOne(getSuppliedValue(vs));
                }
                return opsTarget.set(((Number) (pair.indexOrSupplier)).intValue(), pair.valueOrSupplier);
            }).toList();
        }

        List<Object> result = new ArrayList<>();
        int elIndex;
        //尾遍历
        if (opsTarget.size() - smallestIndex < largestIndex) {
            elIndex = opsTarget.size() - 1;
            var elIter = (ListIterator<Object>) (opsTarget.reversed().listIterator());
            var indexIter = sortedIndexValuePairs.reversed().iterator();
            var pair = indexIter.next();
            while (elIter.hasNext()) {
                Object el = elIter.next();
                if (elIndex != (Long) pair.indexOrSupplier) {
                    --elIndex;
                    continue;
                }
                replaceValueByItr(result, elIter, pair, el);
                try {
                    pair = indexIter.next();
                } catch (NoSuchElementException e) {
                    break;
                }
                --elIndex;
            }
            return Collections.unmodifiableList(result);
        }

        // 头遍历
        elIndex = 0;
        @SuppressWarnings("unchecked")
        var elIter = (ListIterator<Object>) (opsTarget.listIterator());
        var indexIter = sortedIndexValuePairs.iterator();
        var pair = indexIter.next();
        while (elIter.hasNext()) {
            Object el = elIter.next();
            if (elIndex != (Long) pair.indexOrSupplier) {
                ++elIndex;
                continue;
            }
            replaceValueByItr(result, elIter, pair, el);
            try {
                pair = indexIter.next();
            } catch (NoSuchElementException e) {
                break;
            }
            ++elIndex;
        }
        return Collections.unmodifiableList(result);
    }

    private void replaceValueByItr(List<Object> result, ListIterator<Object> elIter, IndexValuePair pair, Object el) {
        result.add(el);
        if (pair.valueOrSupplier instanceof SupplierCtx vs) {
            pair.valueOrSupplier = selectOne(getSuppliedValue(vs));
        }
        elIter.set(pair.valueOrSupplier);
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsTarget) {
        return operateWithMutableList(opsTarget.data());
    }

    private void doCompile() {
        while (true) {
            String token;
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllKeywords();
                filterAllOptions();
                token = stream().token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            IndexValuePair pair = new IndexValuePair();
            try {
                pair.indexOrSupplier = Long.parseLong(token);
                stream().next();
            } catch (NumberFormatException e) {
                SupplierCtx indexSupplier = compileInlineCommand();
                if (indexSupplier == null) {
                    throw new CommandCompileException("can not parse string '" + token + "' to number." + stream().errToken(token));
                }
            }
            try {
                compileJsonValues((dataType, value) -> {
                    if (value instanceof SupplierCtx v) {
                        if (ImmutableChecker.isUnmodifiableCollection(v.supplyType())) {
                            throw new CommandCompileException("keyword '" + name() + "' can not set immutable return value of supplier command '" + v.command() + "'");
                        }
                    }
                    pair.valueOrSupplier = value;
                    return false;
                });
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CommandCompileException("keyword '" + name() + "' require index-value pair to operate");
            }
            indexValuePairs.add(pair);
        }
    }

    @Override
    protected void beforeCompileInlineCommand() {
        if (indexValuePairs.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one index-value pair to operate");
        }
    }

    @Override
    public void compileWithPrecompileResult(PrecompileResult<?> result_) {
        if (!(result_ instanceof KeyValuePairPrecompileResult result)) {
            throw new UnsupportedOperationException();
        }
        setStream(result.getPrecompileStream());
        consumerCtx = (ConsumerCtx<Object>) result.getPipeConsumer();
        var values = result.getValues();
        for (SetCtx.Pair pair : values) {
            IndexValuePair indexValuePair = new IndexValuePair();
            if (pair.keyOrSupplier instanceof String keyStr) {
                try {
                    indexValuePair.indexOrSupplier = Long.parseLong(keyStr);
                } catch (NumberFormatException e) {
                    throw new CommandInterpretException("can not parse string '" + keyStr + "' to number." + stream().errToken(keyStr));
                }
                indexValuePair.valueOrSupplier = pair.valueOrSupplier;
            }
            indexValuePairs.add(indexValuePair);
        }
    }

    protected class IndexValuePair implements Comparable<IndexValuePair> {
        Object indexOrSupplier;
        Object valueOrSupplier;

        private long normalizeIndex() {
            if (indexOrSupplier instanceof SupplierCtx indexSupplier) {
                indexOrSupplier = getSuppliedValue(indexSupplier);
            }
            Object indexKey = selectOneKeyOrElseThrow(indexOrSupplier);
            if (!(indexKey instanceof Long index)) {
                throw new StopComplieException("index must be a number");
            }
            indexOrSupplier = index;
            return index < 0 ? listSize + index : index;
        }

        @Override
        public int compareTo(@NotNull LSetCtx.IndexValuePair o) {
            return (int) (normalizeIndex() - o.normalizeIndex());
        }
    }
}
