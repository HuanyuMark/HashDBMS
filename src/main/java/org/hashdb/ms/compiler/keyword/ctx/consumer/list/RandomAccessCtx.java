package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.CommandInterpretException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.Precompilable;
import org.hashdb.ms.compiler.keyword.ctx.consumer.PrecompileResult;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.UnmodifiableCollections;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/11/29 11:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class RandomAccessCtx extends MutableListCtx implements Precompilable {
    private final List<Object> indexOrSuppliers = new LinkedList<>();

    protected RandomAccessCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    protected void beforeCompile() {
        // 如果不为空, 说明使用了解释模式, 通过解析预编译的结果已经生成了所有所需值
        if (indexOrSuppliers.isEmpty()) {
            doCompile();
            beforeCompileInlineCommand();
        }
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsList) {
        List<Long> indexes = indexOrSuppliers.parallelStream().map(indexOrSupplier -> {
            if (indexOrSupplier instanceof SupplierCtx indexSupplier) {
                indexOrSupplier = exeSupplierCtx(indexSupplier);
            }
            Object oneValue = selectOneValue(indexOrSupplier);
            if (!(oneValue instanceof Long index)) {
                throw new StopComplieException("index must be a number");
            }
            return index < 0 ? opsList.size() + index : index;
        }).sorted().toList();
        if (indexes.getFirst() < 0) {
            throw new CommandInterpretException("index '" + indexes.getFirst() + "' out of range");
        }
        if (indexes.getLast() >= opsList.size()) {
            throw new CommandInterpretException("index '" + indexes.getLast() + "' out of range");
        }
        return access(opsList, indexes);
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsValue) {
        return operateWithMutableList(opsValue.data());
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
            try {
                indexOrSuppliers.add(Long.parseLong(token));
                stream().next();
            } catch (NumberFormatException e) {
                SupplierCtx indexSupplier;
                try {
                    indexSupplier = compileInlineCommand();
                } catch (ArrayIndexOutOfBoundsException ex) {
                    return;
                }
                if (indexSupplier != null) {
                    indexOrSuppliers.add(indexSupplier);
                }
                throw new CommandCompileException("can not parse index string '" + token + "' to number");
            }
        }
    }

    abstract protected List<Object> access(List<Object> opeList, List<Long> indexes);

    @Override
    protected void beforeCompileInlineCommand() {
        if (indexOrSuppliers.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease 1 index to access");
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public void compileWithPrecompileResult(PrecompileResult<?> result) {
        setStream(result.getPrecompileStream());
        consumerCtx = (ConsumerCtx<Object>) result.getPipeConsumer();
        /*
         *  String
         *  OptionCtx
         *  KeywordModifier
         *  org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx
         * */
        List<Object> values = ((List<Object>) result.getValues());
        for (Object unknownTypeValue : values) {
            if (unknownTypeValue instanceof String indexStr) {
                try {
                    indexOrSuppliers.add(Long.parseLong(indexStr));
                    continue;
                } catch (NumberFormatException e) {
                    throw new CommandInterpretException("can not parse index string '" + indexStr + "' to number");
                }
            }
            if (unknownTypeValue instanceof SupplierCtx indexSupplier) {
                indexOrSuppliers.add(indexSupplier);
                continue;
            }
            throw new CommandInterpretException("keyword '" + name() + "' fail to interpret token '" + unknownTypeValue + "'");
        }
    }
}
