package io.particle.android.sdk.devicesetup;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.Tuple;

/**
 * Created by rlysens on 7/1/2017.
 */

public class OnlineDeviceListAdapter extends RecyclerView.Adapter<OnlineDeviceListAdapter.NumberViewHolder> {

    final private ListItemClickListener mOnClickListener;

    private static final TLog log = TLog.get(OnlineDeviceListAdapter.class);

    private List<HolaDeviceData> mDeviceData;
    private final Context mContext;

    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex);
    }

    /**
     * Constructor for adapter that accepts a number of items to display and the specification
     * for the ListItemClickListener.
     *
     * @param numberOfItems Number of items to display in list
     */
    public OnlineDeviceListAdapter(ListItemClickListener listener, Context context) {
        mOnClickListener = listener;
        mContext = context;
    }

    /**
     *
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *                  {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new NumberViewHolder that holds the View for each list item
     */
    @Override
    public NumberViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.online_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        NumberViewHolder viewHolder = new NumberViewHolder(view);

        return viewHolder;
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(NumberViewHolder viewHolder, int position) {
        log.d("#" + position);
        HolaDeviceData deviceData = mDeviceData.get(position);
        String deviceName = deviceData.getDeviceName();

        String statusString;

        if (deviceData.isOnline()) {
            statusString = mContext.getString(R.string.online);
        }
        else {
            statusString = mContext.getString(R.string.offline);
        }

        viewHolder.mListItemNameButton.setText(deviceName);
        viewHolder.mListItemStatusView.setText(statusString);
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (null == mDeviceData)
            return 0;

        return mDeviceData.size();
    }

    public void setDeviceData(List<HolaDeviceData> deviceData) {
        mDeviceData = deviceData;

        notifyDataSetChanged();
    }

    /**
     * Cache of the children views for a list item.
     */
    class NumberViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // Will display the position in the list, ie 0 through getItemCount() - 1
        Button mListItemNameButton;
        TextView mListItemStatusView;

        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
         * onClick method below.
         * @param itemView The View that you inflated in
         *                 onCreateViewHolder(ViewGroup, int)
         */
        public NumberViewHolder(View itemView) {
            // COMPLETED (15) Within the constructor, call super(itemView) and then find listItemNumberView by ID
            super(itemView);

            mListItemNameButton = (Button) itemView.findViewById(R.id.item_name);
            mListItemStatusView = (TextView) itemView.findViewById(R.id.item_status);

            mListItemNameButton.setOnClickListener(this);
        }

        public void onClick(View v) {
            int clickedPosition = getAdapterPosition();
            mOnClickListener.onListItemClick(clickedPosition);
        }
    }
}
