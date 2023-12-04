package org.hashdb.ms.exception;

import com.sun.jdi.connect.spi.ClosedConnectionException;
import lombok.Getter;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Date: 2023/12/3 14:39
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Getter
public class ClosedConnectionWrapper extends RuntimeException {
    private final ClosedConnectionException underlay;

    protected ClosedConnectionWrapper(ClosedConnectionException underlay) {
        this.underlay = underlay;
    }

    public static ClosedConnectionWrapper wrap(ClosedConnectionException e) {
        return new ClosedConnectionWrapper(e);
    }

    @Override
    public String getMessage() {
        return underlay.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return underlay.getLocalizedMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return underlay.getCause();
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return underlay.initCause(cause);
    }

    @Override
    public void printStackTrace() {
        underlay.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        underlay.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        underlay.printStackTrace(s);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return underlay.fillInStackTrace();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return underlay.getStackTrace();
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        underlay.setStackTrace(stackTrace);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + super.toString() + ")";
    }
}
