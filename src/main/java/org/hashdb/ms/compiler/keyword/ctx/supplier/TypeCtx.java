package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 * TYPE $KEY … $KEY
 *
 * @author Huanyu Mark
 */
public class TypeCtx extends SupplierCtx {
    private final List<Object> keyOrSupplier = new LinkedList<>();

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.TYPE;
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    public Supplier<?> compile() {
        doCompile();
        beforeCompilePipe();
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> keyOrSupplier.stream().map(keyOrSupplier -> {
            Object toQuery;
            String key;
            if (keyOrSupplier instanceof SupplierCtx keySupplierCtx) {
                toQuery = exeSupplierCtx(keySupplierCtx);
                try {
                    key = normalizeToQueryKey(toQuery);
                } catch (UnsupportedQueryKey e) {
                    throw UnsupportedQueryKey.of(name(), keySupplierCtx);
                }
            } else {
                key = ((String) keyOrSupplier);
            }
            return stream().db().type(key);
        }).toList();
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
            SupplierCtx supplierCtx = compileInlineCommand();
            if (supplierCtx != null) {
                keyOrSupplier.add(supplierCtx);
                continue;
            }
            keyOrSupplier.add(token);
            stream().next();
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (keyOrSupplier.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one key to query");
        }
    }
}
