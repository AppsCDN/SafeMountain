package com.sovworks.safemountain.db;

import android.provider.BaseColumns;

public final class Log_DB {
    private Log_DB() {}
    public static final String SQL_CREATE_LOG_TABLE = String.format("create table if not exists %s (%s integer PRIMARY KEY , %s text)",
            Log_DB_Entry.TABLE_NAME,
            Log_DB_Entry.COLUMN_NAME_ID,
            Log_DB_Entry.COLUMN_NAME_PATH);
    public static class Log_DB_Entry implements BaseColumns {
        public static final String TABLE_NAME = "Files_To_Transfer";
        public static final String COLUMN_NAME_ID = "ID";
        public static final String COLUMN_NAME_PATH = "PATH";
    }
}