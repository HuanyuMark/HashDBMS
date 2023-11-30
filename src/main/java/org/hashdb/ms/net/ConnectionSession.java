package org.hashdb.ms.net;

import org.hashdb.ms.data.Database;

/**
 * Date: 2023/11/24 16:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ConnectionSession implements AutoCloseable {
    private Database database;

    public Database getDatabase() {
        return database;
    }
    public void setDatabase(Database database) {
        if(database == null) {
            close();
        } else {
            database.restrain(this);
            this.database = database;
        }
    }

    @Override
    public void close() {
        database.release(this);
        this.database = null;
    }
}
