package org.hashdb.ms.net.nio.msg.v1;

import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.exception.DBSystemException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2024/1/17 14:05
 *
 * @author Huanyu Mark
 */
public class ErrorMessage extends ActMessage<ErrorMessage.Body> {
    private static final Map<String, Constructor<? extends Exception>> EXCEPTION_CONSTRUCTOR = new HashMap<>();

    public ErrorMessage(int actId, Body body) {
        super(actId, body);
    }

    public ErrorMessage(Message<?> request, DBClientException e) {
        this(request.id, e);
    }

    public ErrorMessage(int actId, DBClientException e) {
        super(actId, new Body(e.getClass().getName(), e.getCause().getMessage()));
    }

    public ErrorMessage(int actId, String cause) {
        super(actId, new Body(Exception.class.getName(), cause));
    }

    public ErrorMessage(Message<?> request, String cause) {
        this(request.id, cause);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.ERROR;
    }

    public record Body(
            String exception,
            String cause
    ) {
    }

    @SuppressWarnings("unchecked")
    public Exception toException() {
        if (body == null) {
            throw new NullPointerException("can not convert null body to exception");
        }
        var constructor = EXCEPTION_CONSTRUCTOR.get(body.exception);
        if (constructor != null) {
            try {
                return constructor.newInstance(body.cause);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new DBSystemException(e);
            }
        }
        try {
            var exceptionClazz = (Class<? extends Exception>) Class.forName(body.exception);
            try {
                var exceptionConstructor = exceptionClazz.getConstructor(String.class);
                exceptionConstructor.setAccessible(true);
                var res = exceptionConstructor.newInstance(body.cause);
                EXCEPTION_CONSTRUCTOR.put(body.exception, exceptionConstructor);
                return res;
            } catch (NoSuchMethodException e) {
                throw new DBSystemException(STR."can not find constructor with parameter type '\{String.class}' of class '\{exceptionClazz}'");
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new DBSystemException(e);
            }
        } catch (ClassNotFoundException e) {
            throw new DBSystemException(STR."can not convert body to exception: can not load class by class name '\{body.exception}'");
        }
    }
}
