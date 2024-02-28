package org.hashdb.ms.net.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2023/11/30 17:11
 *
 * @author Huanyu Mark
 */
@StandardException
public class DatabaseInUseException extends DBClientException {
}
