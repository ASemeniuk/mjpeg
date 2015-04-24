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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.alexsem.mjpeg.adapter.CameraConfigAdapter;
import org.alexsem.mjpeg.database.DataProvider;
import org.alexsem.mjpeg.util.Utils;
import org.askerov.dynamicgrid.DynamicGridView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ConfigActivity extends ActionBarActivity {

    private static final String TAG = ConfigActivity.class.getSimpleName(); //TODO remove
    private static final String RECEIVER_ID = "C7EC4FCA";

    private DynamicGridView mGrid;
    private CameraConfigAdapter mAdapter;
    private MenuItem mPlayItem;
    private int mCameraCount = 1;
    private int mOneDp = 0;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mCastDevice;
    private GoogleApiClient mApiClient;
    private MjpegReceiverChannel mCastChannel;
    private boolean mApplicationStarted;
    private boolean mWaitingForReconnect;
    private String mSessionId;
    private MenuItem mRefreshItem;
    private View mRefreshProgress;

    private final List<String> CAMERA_COUNT = Arrays.asList("1", "2", "4", "9");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        mCameraCount = getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("cameras", 4);

        //--- Action bar ---
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CAMERA_COUNT);
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

        //--- General views ---
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

        //--- Cast-related objects ---
        mMediaRouter = MediaRouter.getInstance(this);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(RECEIVER_ID))
                .build();
        mMediaRouterCallback = new MjpegMediaRouterCallback();

        //--- Load data ---
        getSupportLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            teardown();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.config, menu);
        mPlayItem = menu.findItem(R.id.action_play);
        MenuItem media_route_menu_item = menu.findItem(R.id.action_cast);
        MediaRouteActionProvider provider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(media_route_menu_item);
        provider.setRouteSelector(mMediaRouteSelector);
        mRefreshItem = menu.findItem(R.id.action_refresh);
        mRefreshProgress = getLayoutInflater().inflate(R.layout.action_progress, null);
        if (mMediaRouter != null && mMediaRouter.getSelectedRoute() != mMediaRouter.getDefaultRoute()) {
            mPlayItem.setVisible(false);
        }
        if (mSessionId != null) {
            mRefreshItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_play:
                startActivity(new Intent(this, FeedActivity.class));
                return true;
            case R.id.action_refresh:
                sendMessage(generateCastMessage());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Callback for MediaRouter events
     */
    private class MjpegMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteSelected: info = " + info);
            mCastDevice = CastDevice.getFromBundle(info.getExtras());
            Toast.makeText(ConfigActivity.this, String.format(getString(R.string.cast_connected_to), info.getName()), Toast.LENGTH_SHORT).show();
            launchReceiver();
            if (mPlayItem != null) {
                mPlayItem.setVisible(false);
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            teardown();
            Toast.makeText(ConfigActivity.this, R.string.cast_disconnected, Toast.LENGTH_SHORT).show();
            if (mPlayItem != null) {
                mPlayItem.setVisible(true);
            }
        }
    }

    /**
     * Start the receiver app
     */
    private void launchReceiver() {
        try {
            Cast.Listener castListener = new Cast.Listener() {
                @Override
                public void onApplicationDisconnected(int errorCode) {
                    Log.d(TAG, "Application has stopped");
                    teardown();
                }
            };
            // Connect to Google Play services
            ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks();
            ConnectionFailedListener connectionFailedListener = new ConnectionFailedListener();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(mCastDevice, castListener);
            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .build();
            mApiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed launchReceiver", e);
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected");
            if (mApiClient == null) {
                // We got disconnected while this runnable was pending execution.
                return;
            }
            try {
                if (mWaitingForReconnect) {
                    mWaitingForReconnect = false;
                    if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) { //Check if the receiver app is still running
                        Log.d(TAG, "App  is no longer running");
                        teardown();
                    } else { //Re-create the custom message channel
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mCastChannel.getNamespace(), mCastChannel);
                        } catch (IOException e) {
                            Log.e(TAG, "Exception while creating channel", e);
                        }
                    }
                    if (mRefreshItem != null) {
                        mRefreshItem.setVisible(true);
                    }
                } else { //Launch the receiver app
                    Cast.CastApi.launchApplication(mApiClient, RECEIVER_ID, false).setResultCallback(new ConnectionResultCallback());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
            mWaitingForReconnect = true;
            if (mRefreshItem != null) {
                mRefreshItem.setVisible(false);
            }
        }
    }

    private class ConnectionResultCallback implements ResultCallback<Cast.ApplicationConnectionResult> {
        @Override
        public void onResult(Cast.ApplicationConnectionResult result) {
            Status status = result.getStatus();
            Log.d(TAG, "ApplicationConnectionResultCallback.onResult: statusCode " + status.getStatusCode());
            if (status.getStatusCode() == 15 && mApiClient != null) { //Timeout, try again
                Cast.CastApi.launchApplication(mApiClient, RECEIVER_ID, false).setResultCallback(new ConnectionResultCallback());
                return;
            }
            if (status.isSuccess()) {
                ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                mSessionId = result.getSessionId();
                String applicationStatus = result.getApplicationStatus();
                boolean wasLaunched = result.getWasLaunched();
                Log.d(TAG, String.format("Application name: %s, status: %s, sessionId: %s, wasLaunched: %b", applicationMetadata.getName(), applicationStatus, mSessionId, wasLaunched));
                mApplicationStarted = true;
                // Create the custom message channel
                mCastChannel = new MjpegReceiverChannel();
                try {
                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mCastChannel.getNamespace(), mCastChannel);
                } catch (IOException e) {
                    Log.e(TAG, "Exception while creating channel", e);
                }
                // Set the initial instructions on the receiver
                sendMessage(generateCastMessage());
            } else {
                Log.e(TAG, "Application could not launch: " + status.toString());
                teardown();
            }
        }

    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed");
            teardown();
        }
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Send a text message to the receiver
     * @param message Message to send
     */
    private void sendMessage(String message) {
        if (mApiClient != null && mCastChannel != null) {
            try {
                if (mRefreshItem != null) {
                    mRefreshItem.setVisible(true);
                    MenuItemCompat.setActionView(mRefreshItem, mRefreshProgress);
                }
                Cast.CastApi.sendMessage(mApiClient, mCastChannel.getNamespace(), message).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        if (!result.isSuccess()) {
                            if (mRefreshItem != null) {
                                MenuItemCompat.setActionView(mRefreshItem, null);
                            }
                            Log.e(TAG, "Sending message failed");
                        }
                    }
                });
            } catch (Exception e) {
                if (mRefreshItem != null) {
                    MenuItemCompat.setActionView(mRefreshItem, null);
                }
                Log.e(TAG, "Exception while sending message", e);
            }
        }
    }

    /**
     * Tear down the connection to the receiver
     */
    private void teardown() {
        Log.d(TAG, "Teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() /*|| mApiClient.isConnecting()*/) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        if (mCastChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mCastChannel.getNamespace());
                            mCastChannel = null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        mCastDevice = null;
        mWaitingForReconnect = false;
        mSessionId = null;
        if (mRefreshItem != null) {
            mRefreshItem.setVisible(false);
        }
    }

//----------------------------------------------------------------------------------------------

    /**
     * Custom message channel
     */
    class MjpegReceiverChannel implements Cast.MessageReceivedCallback {

        /**
         * @return custom namespace
         */
        public String getNamespace() {
            return "urn:x-cast:org.alexsem.mjpeg.receiver";
        }

        /*
         * Receive message from the receiver app
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            Log.d(TAG, "onMessageReceived: " + message);
            if (mRefreshItem != null) {
                MenuItemCompat.setActionView(mRefreshItem, null);
            }
        }

    }

    //----------------------------------------------------------------------------------------------

    /**
     * Generate message with all camera data (for casting)
     * @return Generated string
     */
    private String generateCastMessage() {
        String[] projection = {
                DataProvider.Camera.ID,
                DataProvider.Camera.NAME,
                DataProvider.Camera.HOST,
                DataProvider.Camera.MODE,
                DataProvider.Camera.ENABLED,
                DataProvider.Camera.ORDER
        };
        Cursor cursor = getContentResolver().query(DataProvider.Camera.CONTENT_URI, projection, null, null, DataProvider.Camera.ORDER);
        try {
            if (cursor.moveToFirst()) {
                StringBuilder builder = new StringBuilder("{\"cameras\":[");
                int i = 0;
                do {
                    if (i > 0) {
                        builder.append(",");
                    }
                    builder.append("{");
                    builder.append("\"order\":").append(cursor.getInt(cursor.getColumnIndex(DataProvider.Camera.ORDER))).append(",");
                    builder.append("\"name\":\"").append(cursor.getString(cursor.getColumnIndex(DataProvider.Camera.NAME))).append("\",");
                    builder.append("\"url\":\"").append(Utils.generateMpegUrl(cursor.getString(cursor.getColumnIndex(DataProvider.Camera.HOST)))).append("\",");
                    builder.append("\"visible\":").append(cursor.getInt(cursor.getColumnIndex(DataProvider.Camera.ENABLED)) > 0);
                    builder.append("}");
                } while (cursor.moveToNext() && ++i < mCameraCount);
                builder.append("]}");
                return builder.toString();
            } else {
                return "";
            }
        } finally {
            cursor.close();
        }

    }

    //----------------------------------------------------------------------------------------------

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