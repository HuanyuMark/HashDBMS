package org.hashdb.ms.handler;

import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.exception.WorkerInterruptedException;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.Runners;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Date: 2023/11/21 1:17
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class OpsEventQueue {
    private final BlockingQueue<OpsEvent<?>> eventQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Object> resultQueue = new LinkedBlockingQueue<>();
    private final OpsConsumerDispatcher consumerDispatcher = HashDBMSApp.ctx().getBean(OpsConsumerDispatcher.class);
    public OpsEventQueue() {
        consume();
    }
    public OpsEventQueue(List<OpsEvent<?>> initialEvents) {
        eventQueue.addAll(initialEvents);
        consume();
    }
    /**
     * 启用一个子线程， 专门用来消费该数据库的操作事件
     */
    public void consume(){
        AsyncService.submit(()-> Runners.everlasting(()->{
            OpsEvent<?> event;
            try {
                event = eventQueue.take();
            } catch (InterruptedException e) {
                throw new WorkerInterruptedException(e);
            }
            Object result = consumerDispatcher.dispatch(event);
            resultQueue.add(result);
        }));
    }
    public Object publish(OpsEvent<?> event) {
        eventQueue.add(event);
        try {
            return resultQueue.take();
        } catch (InterruptedException e) {
            throw new WorkerInterruptedException(e);
        }
    }

    /**
     * @param events 一连串事件，一般只在开启事务时使用。
     *               在同一段时间内，这些事件的执行顺序同其被发布的顺序
     *               不会被其它线程打乱， 确保了在同一个事务内，数据的一致性
     * @return 事件执行结果
     */
    synchronized public List<Object> publish(List<OpsEvent<?>> events) {
        eventQueue.addAll(events);
        return events.stream().map(this::publish).toList();
    }
}
