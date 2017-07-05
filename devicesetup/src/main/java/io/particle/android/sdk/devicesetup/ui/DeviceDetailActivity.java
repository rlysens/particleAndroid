package io.particle.android.sdk.devicesetup.ui;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.phrase.Phrase;

import io.particle.android.sdk.devicesetup.HolaDeviceData;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;

public class DeviceDetailActivity extends AppCompatActivity {

    public class SubmitWorkerParams {
        public String mBuddyName;
        public int mBuddyIdx;
        public boolean mResult;
    };

    private static final TLog log = TLog.get(DeviceDetailActivity.class);

    private TextView mDeviceDetailHeader;
    private View mDeviceDetailOnlineView;
    private TextView mBatteryChargeTextView;
    private ProgressBar  mBatteryChargeProgressBar;
    private TextView mWifiSignalTextView;
    private ProgressBar mWifiSignalProgressBar;
    private View mProgressBar;
    private EditText[] mBuddyEditText = new EditText[3];
    private HolaDeviceData mDeviceData;
    private AsyncTask<Void, Void, Boolean> mPollingTask;
    private AsyncTask<SubmitWorkerParams, Void, SubmitWorkerParams> mSubmitTask;

    private String getBatteryChargeString() {
        String batteryChargeString = String.format("Battery Charge:%d%%", mDeviceData.getBatteryCharge());
        return batteryChargeString;
    }

    private String getWifiSignalString() {
        String wifiSignalString = String.format("Wifi Signal Strength:%d%%", mDeviceData.getWifiSignal());
        return wifiSignalString;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        mDeviceDetailHeader = (TextView) findViewById(R.id.device_detail_header);
        mDeviceDetailOnlineView = findViewById(R.id.device_detail_online);
        mBatteryChargeTextView = findViewById(R.id.battery_charge_tv);
        mBatteryChargeProgressBar = (ProgressBar) findViewById(R.id.battery_charge_pb);
        mWifiSignalTextView = findViewById(R.id.wifi_signal_tv);
        mWifiSignalProgressBar = (ProgressBar) findViewById(R.id.wifi_signal_pb);
        mProgressBar = findViewById(R.id.indeterminateBar);
        mBuddyEditText[0] = (EditText) findViewById(R.id.buddy_0_edit_txt);
        mBuddyEditText[1] = (EditText) findViewById(R.id.buddy_1_edit_txt);
        mBuddyEditText[2] = (EditText) findViewById(R.id.buddy_2_edit_txt);

        mDeviceData = getIntent().getParcelableExtra("HOLA_DEVICE_DATA");

        mDeviceDetailHeader.setText(getString(R.string.msg_loading));

        mDeviceDetailOnlineView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);

        mBatteryChargeTextView.setText(getBatteryChargeString());
        mBatteryChargeProgressBar.setProgress(mDeviceData.getBatteryCharge());
        mWifiSignalTextView.setText(getWifiSignalString());
        mWifiSignalProgressBar.setProgress(mDeviceData.getWifiSignal());

        Ui.findView(this, R.id.buddy_0_submit).setOnClickListener(view -> {
            submitEdit(0);
        });
        Ui.findView(this, R.id.buddy_1_submit).setOnClickListener(view -> {
            submitEdit(1);
        });
        Ui.findView(this, R.id.buddy_2_submit).setOnClickListener(view -> {
            submitEdit(2);
        });
    }

    private void submitEdit(int buddyIdx) {
        assert(buddyIdx < mDeviceData.NUM_BUDDIES);
        assert(buddyIdx >= 0);

        SubmitWorkerParams submitWorkerParams = new SubmitWorkerParams();
        submitWorkerParams.mBuddyName = mBuddyEditText[buddyIdx].getText().toString();
        submitWorkerParams.mBuddyIdx = buddyIdx;
        submitWorkerParams.mResult = false;
        startSubmitWorker(submitWorkerParams);
    }

    private void showLoadFail() {
        mDeviceDetailHeader.setText(getString(R.string.msg_load_fail));
        mDeviceDetailOnlineView.setVisibility(View.INVISIBLE);
    }

    private void showOnlineOffline() {
        String onlineText;
        if (mDeviceData.isOnline()) {
            onlineText = getString(R.string.online);
            mDeviceDetailOnlineView.setVisibility(View.VISIBLE);
            mBatteryChargeTextView.setText(getBatteryChargeString());
            mBatteryChargeProgressBar.setProgress(mDeviceData.getBatteryCharge());
            mWifiSignalTextView.setText(getWifiSignalString());
            mWifiSignalProgressBar.setProgress(mDeviceData.getWifiSignal());
        }
        else {
            onlineText = getString(R.string.offline);
            mDeviceDetailOnlineView.setVisibility(View.INVISIBLE);
        }

        CharSequence stringToDisplay = Phrase.from(this, R.string.msg_device_onoffline)
                .put("device_name", mDeviceData.getDeviceName())
                .put("onoffline", onlineText)
                .format();

        mDeviceDetailHeader.setText(stringToDisplay);
    }

    protected void onStart() {
        super.onStart();
        mBuddyEditText[0].setText(mDeviceData.getBuddyName(0));
        mBuddyEditText[1].setText(mDeviceData.getBuddyName(1));
        mBuddyEditText[2].setText(mDeviceData.getBuddyName(2));
        startPollingWorker();
    }

    private void startPollingWorker() {
        // first, make sure we haven't actually been called twice...
        if (mPollingTask != null) {
            log.d("Already running connect worker " + mPollingTask + ", refusing to start another");
            return;
        }

        mPollingTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                // including this sleep because without it,
                // we seem to attempt a socket connection too early,
                // and it makes the process time out(!)
                log.d("Waiting a couple seconds before trying the socket connection...");
                EZ.threadSleep(2000);

                if (mDeviceData != null) {
                    mDeviceData.refresh(DeviceDetailActivity.this);
                    return true;
                }
                else {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                mPollingTask = null;

                if (!result){
                    showLoadFail();
                } else {
                    showOnlineOffline();
                }

                startPollingWorker();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void startSubmitWorker(SubmitWorkerParams submitWorkerParams) {
        // first, make sure we haven't actually been called twice...
        if (mSubmitTask != null) {
            log.d("Already running connect worker " + mSubmitTask + ", refusing to start another");
            return;
        }

        SubmitWorkerParams[] myTaskParams = { submitWorkerParams };

        mProgressBar.setVisibility(View.VISIBLE);

        // This just has doInBackground() return null on success, or if an
        // exception was thrown, it passes that along instead to indicate failure.
        mSubmitTask = new AsyncTask<SubmitWorkerParams, Void, SubmitWorkerParams>() {

            @Override
            protected SubmitWorkerParams doInBackground(DeviceDetailActivity.SubmitWorkerParams... args) {
                SubmitWorkerParams params = args[0];

                // including this sleep because without it,
                // we seem to attempt a socket connection too early,
                // and it makes the process time out(!)
                log.d("Waiting a couple seconds before trying the socket connection...");
                EZ.threadSleep(2000);

                if (mDeviceData != null) {
                    params.mResult = mDeviceData.setBuddyName(params.mBuddyIdx, params.mBuddyName);
                    if (params.mResult) {
                        int count=0;
                        String buddyId;

                        while (count++ < 10) {
                            mDeviceData.refresh(DeviceDetailActivity.this);
                            if (!mDeviceData.getBuddyId(params.mBuddyIdx).equals("")) {
                                break;
                            }
                            EZ.threadSleep(2000);
                        }
                    }
                }
                else {
                    params.mResult = false;
                }

                return params;
            }

            @Override
            protected void onPostExecute(SubmitWorkerParams params) {
                mSubmitTask = null;
                mProgressBar.setVisibility(View.INVISIBLE);

                if (!params.mResult){
                    Toaster.s(DeviceDetailActivity.this, getString(R.string.buddy_set_fail));
                }
                else {
                    if (mDeviceData.getBuddyId(params.mBuddyIdx).equals("")) {
                        String text = getString(R.string.buddy_not_found);
                        SpannableStringBuilder biggerText = new SpannableStringBuilder(text);
                        biggerText.setSpan(new RelativeSizeSpan(1.50f), 0, text.length(), 0);
                        Toast.makeText(DeviceDetailActivity.this,
                                biggerText, Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toaster.s(DeviceDetailActivity.this, getString(R.string.buddy_set_success));
                    }
                }
            }
        }.execute(myTaskParams);
    }
}
