package org.alexsem.mjpeg;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;

import org.alexsem.mjpeg.adapter.CameraConfigAdapter;

public class ConfigActivity extends Activity {

    private GridView mGrid;
    private CameraConfigAdapter mAdapter;
    private int mCameraCount = 9;
    private int mOneDp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mAdapter = new CameraConfigAdapter(this, null);
        mGrid = (GridView) findViewById(R.id.camera_grid);
        mGrid.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    return true;
                }
                return false;
            }
        });
        mGrid.setAdapter(mAdapter);
        mOneDp = getResources().getDisplayMetrics().densityDpi / 160;

        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
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

            int colCount = mCameraCount > 4 ? 3 : mCameraCount > 1 ? 2 : 1;
            int rowCount = mCameraCount / colCount;
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
