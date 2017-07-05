package io.particle.android.sdk.devicesetup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ui.DeviceDetailActivity;
import io.particle.android.sdk.utils.TLog;

/**
 * Created by rlysens on 7/12/2017.
 */

public class HolaDeviceData implements Parcelable {
    public static final int NUM_BUDDIES = 3;
    private static final TLog log = TLog.get(HolaDeviceData.class);

    private ParticleDevice mDevice;
    private String mDeviceName;
    private String[] mBuddyNames = {"","",""};
    private String[] mBuddyIds = {"-1", "-1", "-1"};
    private int mBatteryCharge;
    private int mWifiSignal;
    private boolean mIsOnline;

    /*returns cached data*/
    public int getWifiSignal() { return mWifiSignal; }

    /*returns cached data*/
    public int getBatteryCharge() { return mBatteryCharge; }

    /*returns cached data*/
    public ParticleDevice getDevice() {
        return mDevice;
    }

    /*returns cached data*/
    public String getDeviceName() {
        return mDeviceName;
    }

    /*returns cached data*/
    public String getBuddyName(int buddyIdx) {
        assert(buddyIdx < mBuddyNames.length);
        return mBuddyNames[buddyIdx];
    }

    /*returns cached data*/
    public String getBuddyId(int buddyIdx) {
        assert(buddyIdx < mBuddyIds.length);
        return mBuddyIds[buddyIdx];
    }

    /*returns cached data*/
    public boolean isOnline() {
        return mIsOnline;
    }

    /*returns true if successful*/
    public boolean setBuddyName(int buddyIdx, String buddyName) {
        assert(buddyIdx < mBuddyNames.length);

        try {
            List<String> args = new ArrayList<String>();

            args.add(buddyName);

            String functionName = String.format("buddy_%d_name", buddyIdx);

            /*callFunction API says 1=success, but that's not correct.
             *The value returned is from the called function.
             *In case of the buddy_name function, 0=success.
             */
            int result = mDevice.callFunction(functionName, args);

            if (result == 0) {
                mBuddyNames[buddyIdx] = buddyName;
                return true;
            }
            else {
                return false;
            }
        }
        catch (Exception e) {
            log.d("Setup exception thrown: ", e);
            return false;
        }
    }

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mDevice, flags);
        out.writeString(mDeviceName);
        out.writeStringArray(mBuddyNames);
        out.writeStringArray(mBuddyIds);
        out.writeInt(mBatteryCharge);
        out.writeInt(mWifiSignal);
        out.writeByte((byte) (mIsOnline ? 1 : 0));
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<HolaDeviceData> CREATOR = new Parcelable.Creator<HolaDeviceData>() {
        public HolaDeviceData createFromParcel(Parcel in) {
            return new HolaDeviceData(in);
        }

        public HolaDeviceData[] newArray(int size) {
            return new HolaDeviceData[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private HolaDeviceData(Parcel in) {
        mDevice = in.readParcelable(ParticleDevice.class.getClassLoader());
        mDeviceName = in.readString();
        mBuddyNames = in.createStringArray();
        mBuddyIds = in.createStringArray();
        mBatteryCharge = in.readInt();
        mWifiSignal = in.readInt();
        mIsOnline = in.readByte() != 0;
    }

    public HolaDeviceData(ParticleDevice device, Context context) {
        mDevice = device;
        mIsOnline = false;

        for (int buddyIdx=0; buddyIdx<mBuddyNames.length; buddyIdx++) {
            mBuddyNames[buddyIdx] = context.getString(R.string.buddy_not_set);
        }

        String id = device.getID();
        SharedPreferences prefs = context.getSharedPreferences(id, Context.MODE_PRIVATE);
        mDeviceName = prefs.getString("my_name", context.getString(R.string.name_unknown));
    }

    public void refresh(Context context) {
        try {
            mDevice.refresh();
            mIsOnline = mDevice.isConnected();
            if (mIsOnline) {

                String deviceName = mDevice.getStringVariable("my_name");

                /*Update name in persistent storage if found/changed*/
                if (!mDeviceName.equals(deviceName)) {
                    String id = mDevice.getID();
                    SharedPreferences prefs = context.getSharedPreferences(id, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("my_name", deviceName);
                    editor.commit();
                }

                mDeviceName = deviceName;
                mBatteryCharge = Integer.parseInt(mDevice.getStringVariable("battery_pct"));
                mWifiSignal = Integer.parseInt(mDevice.getStringVariable("wifi_pct"));

                for (int buddyIdx = 0; buddyIdx < mBuddyNames.length; buddyIdx++) {
                    String functionName = String.format("buddy_%d_name", buddyIdx);
                    String buddyName = mDevice.getStringVariable(functionName);

                    if (buddyName.isEmpty()) {
                        buddyName = context.getString(R.string.buddy_not_set);
                    }

                    mBuddyNames[buddyIdx] = buddyName;

                    functionName = String.format("buddy_%d_id", buddyIdx);
                    mBuddyIds[buddyIdx] = mDevice.getStringVariable(functionName);
                }
            }
        }
        catch (Exception e) {
            mIsOnline = false;
            log.d("Setup exception thrown: ", e);
        }
    }
}
