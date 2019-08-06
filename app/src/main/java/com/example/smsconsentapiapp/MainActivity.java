package com.example.smsconsentapiapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private static final int CREDENTIAL_PICKER_REQUEST = 1;
    private static final int RESOLVE_HINT = 1;
    private static final int SMS_CONSENT_REQUEST = 2;
    private TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.textview2);

        try {
            requestHint();
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }


        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        registerReceiver(smsVerificationReceiver, intentFilter);
        final Task<Void> task = SmsRetriever.getClient(this).startSmsUserConsent(null);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("A","Task Running");
            }
        });

        Log.d("A","Value:"+task);
    }

    // Construct a request for phone numbers and show the picker
    private void requestHint() throws IntentSender.SendIntentException {
        HintRequest hintRequest = new HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build();
        PendingIntent intent = Credentials.getClient(this).getHintPickerIntent(hintRequest);
        startIntentSenderForResult(intent.getIntentSender(),
                RESOLVE_HINT, null, 0, 0, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CREDENTIAL_PICKER_REQUEST:
                // Obtain the phone number from the result
                if (resultCode == RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    credential.getId();
                    // credential.getId();  <-- will need to process phone number string

                }
                break;
            case SMS_CONSENT_REQUEST:
                if (resultCode == RESULT_OK) {

                    // Get SMS message content
                    String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                    Log.d("A","message:->" + message);
                    tv.setText(message);
                    // Extract one-time code from the message and complete verification
                    // `sms` contains the entire text of the SMS message, so you will need
                    // to parse the string.
//                  String oneTimeCode = parseOneTimeCode(message); // define this function

                    // send one time code to the server
                } else if (resultCode == RESULT_CANCELED){
                    // Consent canceled, handle the error ...
                    Task<Void> task = SmsRetriever.getClient(this).startSmsUserConsent(null);
                    Log.d("A","cancelled");
                }
                break;
        }

    }
    private BroadcastReceiver smsVerificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                switch (smsRetrieverStatus.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        // Get consent intent
                        Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
//                        Intent page = new Intent(getApplicationContext(),Main2Activity.class);
                        try {
                            // Start activity to show consent dialog to user, activity must be started in
                            // 5 minutes, otherwise you'll receive another TIMEOUT intent
                            startActivityForResult(consentIntent, SMS_CONSENT_REQUEST);


                        } catch (ActivityNotFoundException e) {
                            // Handle the exception ...
                            e.printStackTrace();
                        }
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        // Time out occurred, handle the error.
                        Toast.makeText(context, "TIME_OUT", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };
}
