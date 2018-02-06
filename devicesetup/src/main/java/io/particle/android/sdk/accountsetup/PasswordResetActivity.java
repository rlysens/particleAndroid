package io.particle.android.sdk.accountsetup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.View;
import android.widget.EditText;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.truthy;


public class PasswordResetActivity extends BaseActivity {

    private static final TLog log = TLog.get(PasswordResetActivity.class);


    public static final String EXTRA_EMAIL = "EXTRA_EMAIL";


    private ParticleCloud sparkCloud;
    private EditText emailView;
    private Async.AsyncApiWorker<ParticleCloud, Void> resetTask = null;


    public static Intent buildIntent(Context context, String email) {
        Intent i = new Intent(context, PasswordResetActivity.class);
        if (truthy(email)) {
            i.putExtra(EXTRA_EMAIL, email);
        }
        return i;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);
        SEGAnalytics.screen("Auth: Forgot password screen");
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);

        sparkCloud = ParticleCloudSDK.getCloud();

        Ui.findView(this, R.id.action_cancel).setOnClickListener(view -> finish());
        emailView = Ui.findView(this, R.id.email);
        emailView.setText(getIntent().getStringExtra(EXTRA_EMAIL));
    }

    public void onPasswordResetClicked(View v) {
        SEGAnalytics.track("Auth: Request password reset");
        final String email = emailView.getText().toString();
        if (isEmailValid(email)) {
            performReset();

        } else {
            new Builder(this)
                    .setTitle(getString(R.string.reset_password_dialog_title))
                    .setMessage(getString(R.string.reset_paassword_dialog_please_enter_a_valid_email))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        emailView.requestFocus();
                    })
                    .show();
        }
    }

    private void performReset() {
        ParticleUi.showParticleButtonProgress(this, R.id.action_reset_password, true);

        resetTask = Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException {
                sparkCloud.requestPasswordReset(emailView.getText().toString());
                return null;
            }

            @Override
            public void onTaskFinished() {
                resetTask = null;
                ParticleUi.showParticleButtonProgress(PasswordResetActivity.this, R.id.action_reset_password, false);
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                onResetAttemptFinished("Instructions for how to reset your password will be sent " +
                        "to the provided email address.  Please check your email and continue " +
                        "according to instructions.");
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                log.d("onFailed(): " + error.getMessage());
                onResetAttemptFinished("Could not find a user with supplied email address, please " +
                        " check the address supplied or create a new user via the signup screen");
            }
        });
    }

    private void onResetAttemptFinished(String content) {
        new AlertDialog.Builder(this)
                .setMessage(content)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    private boolean isEmailValid(String email) {
        return truthy(email) && email.contains("@");
    }

}
