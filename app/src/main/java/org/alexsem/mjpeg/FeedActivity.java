package org.alexsem.mjpeg;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.camera.simplemjpeg.MjpegView;

import org.alexsem.mjpeg.R;

import java.util.concurrent.atomic.AtomicInteger;

public class FeedActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int CAMERA_COUNT = 9;

		LinearLayout global = new LinearLayout(this);
		global.setOrientation(LinearLayout.VERTICAL);
		global.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setContentView(global);

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		int columns = CAMERA_COUNT > 4 ? 3 : CAMERA_COUNT > 1? 2 : 1;
		LinearLayout row = null;
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		params.weight = 1;
		for (int i = 0; i < CAMERA_COUNT; i++) {
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
			ft.add(cell.getId(), MjpegFragment.newInstance(MjpegView.SIZE_STANDARD));
		}
		global.addView(row);
		ft.commit();
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
