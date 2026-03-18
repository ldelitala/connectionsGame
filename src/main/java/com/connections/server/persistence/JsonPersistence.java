package com.connections.server.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;

/**
 * Common utilities for DAO persistence.
 * Centralizes directory management and Gson configuration.
 */
class JsonPersistence {
    static final Gson GSON = new GsonBuilder().create();

    /** Ensures the parent directory of the given path exists. */
    static void ensureDirectory(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /** Returns true if the file exists and is not empty. */
    static boolean isReadable(String path) {
        File file = new File(path);
        return file.exists() && file.length() > 0;
    }
}
