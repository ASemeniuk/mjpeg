package org.alexsem.mjpeg;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.camera.simplemjpeg.MjpegView;

import org.alexsem.mjpeg.R;

public class FeedActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed);
		
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.test_top_left, MjpegFragment.newInstance(MjpegView.SIZE_STANDARD));
		ft.add(R.id.test_top_right, MjpegFragment.newInstance(MjpegView.SIZE_STANDARD));
		ft.add(R.id.test_bottom_left, MjpegFragment.newInstance(MjpegView.SIZE_STANDARD));
		ft.add(R.id.test_bottom_right, MjpegFragment.newInstance(MjpegView.SIZE_STANDARD));
		ft.commit();
	}

}
