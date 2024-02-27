package org.hashdb.ms.net;

import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.net.bio.client.CloseMessage;
import org.hashdb.ms.util.CacheMap;

import java.io.Closeable;

/**
 * Date: 2024/1/3 11:11
 *
 * @author huanyuMake-pecdle
 */
public interface ConnectionSession extends Closeable {
    /**
     * 与参数无关的命令缓存在这里
     */
    // TODO: 2024/1/13 这个缓存暂时无法实现, 因为所有的命令执行都与当前Session相关, 如果放入全局命令缓存中
    //上下文(Session)就要更换, 现在的实现是, 所有的编译流都持有一个Session, 子流也持有父流相同的Sesssion
    // 如果要更换上下文, 就会修改其它使用该缓存的线程的读写
    CacheMap<String, CompileStream<?>> globalCommandCache = null;

    CacheMap<String, CompileStream<?>> getLocalCommandCache();

    Database getDatabase();

    /**
     * @param name  '$'开头的参数名
     * @param value {@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx} 内联命令或者是
     *              {@link org.hashdb.ms.data.DataType} 支持的一个java类实例
     *              如果为null,则意为删除
     * @return 旧值
     */
    Parameter setParameter(String name, Object value);

    Parameter getParameter(String name);

    void close();

    default void close(CloseMessage closeMessage) {
        close();
    }
}
