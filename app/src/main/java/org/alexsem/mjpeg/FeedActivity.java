package org.alexsem.mjpeg;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.alexsem.mjpeg.database.DataProvider;

import java.util.concurrent.atomic.AtomicInteger;

public class FeedActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
						| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
						| View.SYSTEM_UI_FLAG_IMMERSIVE);

		generateLayout(getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("cameras", 4));
	}

	//----------------------------------------------------------------------------------------------

	private void generateLayout(int cameraCount) {
		LinearLayout global = new LinearLayout(this);
		global.setOrientation(LinearLayout.VERTICAL);
		global.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setContentView(global);

		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		int columns = cameraCount > 4 ? 3 : cameraCount > 1? 2 : 1;
		LinearLayout row = null;
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		params.weight = 1;
		String[] projection = {
				DataProvider.Camera.ID,
				DataProvider.Camera.NAME,
				DataProvider.Camera.HOST,
				DataProvider.Camera.MODE,
				DataProvider.Camera.ENABLED
		};
		Cursor cursor = getContentResolver().query(DataProvider.Camera.CONTENT_URI, projection, null, null, DataProvider.Camera.ORDER);
		try {
			if (cursor.moveToFirst()) {
				cursor.moveToPrevious();
				for (int i = 0; i < cameraCount; i++) {
					if (i % columns == 0) {
						if (row != null) {
							global.addView(row);
						}
						row = new LinearLayout(this);
						row.setOrientation(LinearLayout.HORIZONTAL);
						row.setLayoutParams(params);
					}
					FrameLayout cell = new FrameLayout(this);
					cell.setId(generateViewId());
					cell.setLayoutParams(params);
					row.addView(cell);
					if (cursor.moveToNext() && cursor.getInt(cursor.getColumnIndex(DataProvider.Camera.ENABLED)) > 0) {
						String name = cursor.getString(cursor.getColumnIndex(DataProvider.Camera.NAME));
						String url = String.format("http://%s:8080/videofeed", cursor.getString(cursor.getColumnIndex(DataProvider.Camera.HOST)));
						int mode = cursor.getInt(cursor.getColumnIndex(DataProvider.Camera.MODE));
						ft.add(cell.getId(), MjpegFragment.newInstance(name, url, mode));
					} else {
						cell.setVisibility(View.INVISIBLE);
					}
				}
				global.addView(row);
				ft.commit();
			}
		} finally {
			cursor.close();
		}
	}

	//----------------------------------------------------------------------------------------------

	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

	public static int generateViewId() {
		for (;;) {
			final int result = sNextGeneratedId.get();
			// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
			int newValue = result + 1;
			if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
			if (sNextGeneratedId.compareAndSet(result, newValue)) {
				return result;
			}
		}
	}

}
