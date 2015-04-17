package org.alexsem.mjpeg;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import org.alexsem.mjpeg.adapter.CameraConfigAdapter;
import org.alexsem.mjpeg.database.DataProvider;
import org.askerov.dynamicgrid.DynamicGridView;

import java.util.Arrays;
import java.util.List;

public class ConfigActivity extends ActionBarActivity {

    private DynamicGridView mGrid;
    private CameraConfigAdapter mAdapter;
    private int mCameraCount = 1;
    private int mOneDp = 0;

    private final List<String> CAMERA_COUNT = Arrays.asList("1", "2", "4", "9");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mCameraCount = getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("cameras", 4);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CAMERA_COUNT);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ActionBar bar = getSupportActionBar();
        bar.setTitle(R.string.config_cameras);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                mCameraCount = Integer.valueOf(CAMERA_COUNT.get(itemPosition));
                getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putInt("cameras", mCameraCount).commit();
                getSupportLoaderManager().restartLoader(0, null, mLoaderCallbacks);
                return true;
            }
        });
        bar.setSelectedNavigationItem(CAMERA_COUNT.indexOf(String.valueOf(mCameraCount)));

        mAdapter = new CameraConfigAdapter(this, null);
        mGrid = (DynamicGridView) findViewById(R.id.camera_grid);
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DialogFragment dialog = CameraDialogFragment.newInstance(id);
                dialog.show(getSupportFragmentManager(), "dialog");
            }
        });
        mGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mGrid.startEditMode(position);
                return true;
            }
        });
        mGrid.setOnDropListener(new DynamicGridView.OnDropListener() {
            @Override
            public void onActionDrop(int oldPosition, int newPosition) {
                if (oldPosition != newPosition && oldPosition > -1 && newPosition > -1) {
                    long oldId = mAdapter.getItemId(oldPosition);
                    long newId = mAdapter.getItemId(newPosition);
                    Uri uri = Uri.withAppendedPath(DataProvider.Camera.CONTENT_URI, String.valueOf(oldId));
                    ContentValues values = new ContentValues();
                    values.put(DataProvider.Camera.ORDER, newPosition + 1);
                    getContentResolver().update(uri, values, null, null);
                    uri = Uri.withAppendedPath(DataProvider.Camera.CONTENT_URI, String.valueOf(newId));
                    values.put(DataProvider.Camera.ORDER, oldPosition + 1);
                    getContentResolver().update(uri, values, null, null);
                    getSupportLoaderManager().restartLoader(0, null, mLoaderCallbacks);
                }
                mGrid.stopEditMode();
            }
        });
        mGrid.setAdapter(mAdapter);
        mOneDp = getResources().getDisplayMetrics().densityDpi / 160;

        getSupportLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play:
                startActivity(new Intent(this, FeedActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String[] projection = {
                    DataProvider.Camera.ID,
                    DataProvider.Camera.NAME,
                    DataProvider.Camera.HOST,
                    DataProvider.Camera.MODE,
                    DataProvider.Camera.ENABLED,
                    DataProvider.Camera.ORDER
            };
            return new CursorLoader(ConfigActivity.this, DataProvider.Camera.CONTENT_URI, projection, null, null, DataProvider.Camera.ORDER);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.setCount(mCameraCount);
            if (mGrid.getHeight() == 0) {
                mGrid.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int colCount = mCameraCount > 4 ? 3 : mCameraCount > 1 ? 2 : 1;
                        int rowCount = mCameraCount > 4 ? 3 : mCameraCount > 2 ? 2 : 1;
                        mAdapter.setCellSize((mGrid.getWidth() - mOneDp * (colCount - 1)) / colCount, (mGrid.getHeight() - mOneDp * (rowCount - 1)) / rowCount);
                    }
                }, 100);
            }
            int colCount = mCameraCount > 4 ? 3 : mCameraCount > 1 ? 2 : 1;
            int rowCount = mCameraCount > 4 ? 3 : mCameraCount > 2 ? 2 : 1;
            mAdapter.setCellSize((mGrid.getWidth() - mOneDp * (colCount - 1)) / colCount, (mGrid.getHeight() - mOneDp * (rowCount - 1)) / rowCount);
            mGrid.setNumColumns(colCount);
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    };
}
