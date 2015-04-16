package org.alexsem.mjpeg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import org.alexsem.mjpeg.database.DataProvider;

public class CameraDialogFragment extends DialogFragment {

    public static CameraDialogFragment newInstance(long id) {
        CameraDialogFragment f = new CameraDialogFragment();
        Bundle args = new Bundle();
        args.putLong("id", id);
        f.setArguments(args);
        return f;
    }

    private Switch mEnabled;
    private EditText mName;
    private EditText mHost;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_camera, null);
        mEnabled = (Switch) view.findViewById(R.id.camera_enabled);
        mName = (EditText) view.findViewById(R.id.camera_name);
        mHost = (EditText) view.findViewById(R.id.camera_host);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues values = new ContentValues();
                        values.put(DataProvider.Camera.ENABLED, mEnabled.isChecked() ? 1 : 0);
                        values.put(DataProvider.Camera.NAME, mName.getText().toString());
                        values.put(DataProvider.Camera.HOST, mHost.getText().toString());
                        Uri uri = Uri.withAppendedPath(DataProvider.Camera.CONTENT_URI, String.valueOf(getArguments().getLong("id")));
                        getActivity().getContentResolver().update(uri, values, null,null);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mEnabled.setChecked(savedInstanceState.getBoolean("enabled"));
            mName.setText(savedInstanceState.getString("name"));
            mHost.setText(savedInstanceState.getString("host"));
        } else {
            getLoaderManager().initLoader(0, getArguments(), mLoaderCallbacks);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("enabled", mEnabled.isChecked());
        outState.putString("name", mName.getText().toString());
        outState.putString("host", mHost.getText().toString());
        super.onSaveInstanceState(outState);
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri uri = Uri.withAppendedPath(DataProvider.Camera.CONTENT_URI, String.valueOf(args.getLong("id")));
            String[] projection = {
                    DataProvider.Camera.ID,
                    DataProvider.Camera.NAME,
                    DataProvider.Camera.HOST,
                    DataProvider.Camera.ENABLED
            };
            return new CursorLoader(getActivity(), uri, projection, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                try {
                    if (data.moveToFirst()) {
                        mEnabled.setChecked(data.getInt(data.getColumnIndex(DataProvider.Camera.ENABLED)) > 0);
                        mName.setText(data.getString(data.getColumnIndex(DataProvider.Camera.NAME)));
                        mHost.setText(data.getString(data.getColumnIndex(DataProvider.Camera.HOST)));
                    }
                } finally {
                    data.close();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };


}
