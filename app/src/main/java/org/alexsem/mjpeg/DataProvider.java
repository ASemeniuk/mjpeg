package org.alexsem.mjpeg;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.ArrayList;

public class DataProvider extends ContentProvider {

    public static final String AUTHORITY = "org.alexsem.mjpeg";

    //--------------------------------------------------------------------------------------------------------------------

    public static final class Camera {
        public static final String _T = "Camera";
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String HOST = "host";
        public static final String MODE = "mode";
        public static final String ENABLED = "enabled";
        public static final String ORDER = "ord";

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/camera");
    }

    //--------------------------------------------------------------------------------------------------------------------

    private DatabaseHelper mHelper;

    private static final int CAMERA_ALL = 1;
    private static final int CAMERA_SINGLE = 2;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(AUTHORITY, "camera", CAMERA_ALL);
        uriMatcher.addURI(AUTHORITY, "camera/#", CAMERA_SINGLE);
    }

    @Override
    public boolean onCreate() {
        mHelper = new DatabaseHelper(getContext());
        return false;
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CAMERA_ALL:
                return String.format("vnd.android.cursor.dir/vnd.%s.camera", AUTHORITY);
            case CAMERA_SINGLE:
                return String.format("vnd.android.cursor.item/vnd.%s.camera", AUTHORITY);
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {

            case CAMERA_ALL:
                queryBuilder.setTables(Camera._T);
                selection = null;
                selectionArgs = null;
                sortOrder = Camera.ORDER;
                break;
            case CAMERA_SINGLE:
                queryBuilder.setTables(Camera._T);
                selection = (String.format("%s = ?", Camera.ID));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                sortOrder = null;
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String table;

        switch (uriMatcher.match(uri)) {
            case CAMERA_ALL:
                table = Camera._T;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        long id = db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(String.format("%s/%d", uri.toString(), id));
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String table;

        switch (uriMatcher.match(uri)) {
            case CAMERA_SINGLE:
                table = Camera._T;
                selection = (String.format("%s = ?", Camera.ID));
                selectionArgs = new String[]{uri.getLastPathSegment()};
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        int count = db.update(table, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

    //--------------------------------------------------------------------------------------------------------------------

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        ContentProviderResult[] result = new ContentProviderResult[operations.size()];
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int i = 0;
            for (ContentProviderOperation operation : operations) {
                result[i] = operation.apply(this, result, i);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return result;
    }

    //--------------------------------------------------------------------------------------------------------------------


}
