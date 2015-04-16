package org.alexsem.mjpeg.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.camera.simplemjpeg.MjpegView;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "mjpeg.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Camera (_id INTEGER PRIMARY KEY, name TEXT, host TEXT, mode INTEGER, enabled INTEGER, ord INTEGER);");
        for (int i = 1; i <= 9; i++) {
            db.execSQL(String.format("INSERT INTO Camera (name, host, mode, enabled, ord) VALUES ('Camera %1$d', 'unknown', %2$d, 1, %1$d)", i, MjpegView.SIZE_STANDARD));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        boolean updated = false;
        if (oldVersion <= 1) { //Migrate from version 1 to version 2
            updated = true;
        }
        if (!updated) { //Other cases
            db.execSQL("DROP TABLE IF EXISTS Camera");
            onCreate(db);
        }
    }

}
