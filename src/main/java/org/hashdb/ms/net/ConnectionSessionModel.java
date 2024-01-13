package org.hashdb.ms.net;

import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.util.CacheMap;

/**
 * Date: 2024/1/3 11:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface ConnectionSessionModel {
    CacheMap<String, CompileStream<?>> getLocalCommandCache();

    Database getDatabase();

    /**
     * @param name  '$'开头的参数名
     * @param value {@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx} 内联命令或者是
     *              {@link org.hashdb.ms.data.DataType} 支持的一个java类实例
     * @return 旧值
     */
    Parameter setParameter(String name, Object value);

    Parameter getParameter(String name);

    void useParameter(Parameter parameter, String command);

//    static Parameter setParameter(CacheMap<String, ?> parameters, String name, Object value) {
//        if (parameters == null) {
//            parameters = new HashMap<>();
//        }
//        Parameter oldValue;
//        if (value == null) {
//            oldValue = parameters.remove(name);
//        } else {
//            oldValue = parameters.put(name, new Parameter(value));
//        }
//        if (oldValue != null) {
//            oldValue.usedCacheCommands.parallelStream().forEach(localCommandCache::remove);
//        }
//        return null;
//    }
}
