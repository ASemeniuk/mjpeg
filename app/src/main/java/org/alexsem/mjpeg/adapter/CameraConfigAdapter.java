package org.alexsem.mjpeg.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.alexsem.mjpeg.R;
import org.alexsem.mjpeg.database.DataProvider;

/**
 * Adapter used to represent list of news items
 * @author Semeniuk A.D.
 */
public class CameraConfigAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private AbsListView.LayoutParams mLayoutParams;
    private int mCount = 0;

    public CameraConfigAdapter(Context context, Cursor data) {
        super(context, data, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        this.mInflater = LayoutInflater.from(context);
    }

    public void setCellSize(int cellWidth, int cellHeight) {
        mLayoutParams = new AbsListView.LayoutParams(cellWidth, cellHeight);
        notifyDataSetChanged();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View row = mInflater.inflate(R.layout.cell_camera, parent, false);
        ViewHolder holder = new ViewHolder(row);
        row.setTag(holder);
        return row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        boolean enabled = cursor.getInt(cursor.getColumnIndex(DataProvider.Camera.ENABLED)) > 0;
        holder.getOrder().setHint(String.valueOf(cursor.getInt(cursor.getColumnIndex(DataProvider.Camera.ORDER))));
        holder.getName().setText(cursor.getString(cursor.getColumnIndex(DataProvider.Camera.NAME)));
        holder.getName().setVisibility(enabled ? View.VISIBLE : View.GONE);
        holder.getHost().setHint(cursor.getString(cursor.getColumnIndex(DataProvider.Camera.HOST)));
        holder.getHost().setVisibility(enabled ? View.VISIBLE : View.GONE);
        view.setActivated(enabled);
        if (mLayoutParams != null) {
            view.setLayoutParams(mLayoutParams);
        }
    }

    public void setCount(int count) {
        this.mCount = count;
    }

    @Override
    public int getCount() {
        return Math.min(mCount, super.getCount());
    }

    //--------------------------------------------------------------------------

    /**
     * Class used for temporary data storage
     * @author Semeniuk A.D.
     */
    private static class ViewHolder {
        private View base;
        private TextView name = null;
        private TextView order = null;
        private TextView host = null;

        /**
         * Constructor
         * @param base Parent view
         */
        public ViewHolder(View base) {
            this.base = base;
        }

        public TextView getOrder() {
            if (order == null) {
                order = (TextView) base.findViewById(R.id.camera_order);
            }
            return (order);
        }

        public TextView getName() {
            if (name == null) {
                name = (TextView) base.findViewById(R.id.camera_title);
            }
            return (name);
        }

        public TextView getHost() {
            if (host == null) {
                host = (TextView) base.findViewById(R.id.camera_host);
            }
            return (host);
        }

    }

}
