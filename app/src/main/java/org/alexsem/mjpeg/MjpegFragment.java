package org.alexsem.mjpeg;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpConnection;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camera.simplemjpeg.MjpegInputStream;
import com.camera.simplemjpeg.MjpegView;

public class MjpegFragment extends Fragment {

	private static final int CONNECT_TIMEOUT = 10000;

	private MjpegView mPlayer;
	private TextView mTitle;
	private View mProgress;
	private View mError;
	private boolean isSuspended = false;

	public static MjpegFragment newInstance(int mode) {
		MjpegFragment fragment = new MjpegFragment();
		Bundle args = new Bundle();
		args.putInt("mode", mode);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_mjpeg, container, false);
		mPlayer = (MjpegView) view.findViewById(R.id.mjpeg_player);
		mPlayer.showFps(false);
		mPlayer.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				load();
				return false;
			}
		});
		mTitle = (TextView) view.findViewById(R.id.mjpeg_title);
		mProgress = view.findViewById(R.id.mjpeg_progress);
		mError = view.findViewById(R.id.mjpeg_error);
		view.findViewById(R.id.mjpeg_retry).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				load();
			}
		});
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPlayer.setDisplayMode(getArguments().getInt("mode"));
		load();
	}

	private void load() {
		String url = "http://172.24.25.118:8080/videofeed"; //TODO
		mPlayer.setResolution(640, 480); //TODO
		mTitle.setHint(url); //TODO
		isSuspended = false;
		new DataLoadingTask().execute(url);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPlayer != null && mPlayer.isStreaming()) {
			mPlayer.stopPlayback();
			isSuspended = true;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mPlayer != null && isSuspended) {
			load();
			isSuspended = false;
		}
	}

	//--------------------------------------------------------------------------------------------
	public class DataLoadingTask extends AsyncTask<String, Void, MjpegInputStream> {

		@Override
		protected void onPreExecute() {
			mProgress.setVisibility(View.VISIBLE);
			mError.setVisibility(View.GONE);
			if (mPlayer.isStreaming()) {
				mPlayer.stopPlayback();
			}
		}

		protected MjpegInputStream doInBackground(String... url) {
			//TODO: if camera has authentication deal with it and don't just not work
			try {
				DefaultHttpClient client = new DefaultHttpClient();
				HttpParams params = client.getParams();
				HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT);
				HttpConnectionParams.setSoTimeout(params, CONNECT_TIMEOUT);
				HttpGet method = new HttpGet(URI.create(url[0]));
				HttpResponse response = client.execute(method);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					//You must turn off camera User Access Control before this will work
					return null;
				}
				return new MjpegInputStream(response.getEntity().getContent());
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		protected void onPostExecute(MjpegInputStream result) {
			mProgress.setVisibility(View.GONE);
			if (result != null) {
				result.setSkip(1);
				mError.setVisibility(View.GONE);
				mPlayer.setSource(result);
			} else {
				mError.setVisibility(View.VISIBLE);
			}
		}
	}

}
