package org.alexsem.mjpeg;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.SpinnerAdapter;

import org.alexsem.mjpeg.adapter.CameraConfigAdapter;
import org.alexsem.mjpeg.database.DataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigActivity extends Activity {

    private GridView mGrid;
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
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.config_cameras);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                mCameraCount = Integer.valueOf(CAMERA_COUNT.get(itemPosition));
                getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putInt("cameras", mCameraCount).commit();
                getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
                return true;
            }
        });
        bar.setSelectedNavigationItem(CAMERA_COUNT.indexOf(String.valueOf(mCameraCount)));

        mAdapter = new CameraConfigAdapter(this, null);
        mGrid = (GridView) findViewById(R.id.camera_grid);
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DialogFragment dialog = CameraDialogFragment.newInstance(id);
                dialog.show(getFragmentManager(), "dialog");
            }
        });
        mGrid.setAdapter(mAdapter);
        mOneDp = getResources().getDisplayMetrics().densityDpi / 160;

        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
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
