package com.call.videocallapplication.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.call.videocallapplication.R;
import com.call.videocallapplication.network.ApiClient;
import com.call.videocallapplication.network.ApiService;
import com.call.videocallapplication.utils.Constants;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {
    private ImageView ivMeetingType;
    private ImageView ivAcceptInvitation;
    private ImageView ivRejectInvitation;
    private TextView tvTextFirstChar;
    private TextView tvTextUserName;
    private TextView tvTextEmail;
    private String meetingType;
    private String firstname;
    private String lastName;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_invitation);

        // mapping xml
        initView();

        // handler event
        getUserData();
        processEventButton();
    }

    private void initView() {
        ivMeetingType = findViewById(R.id.ivMeetingType);
        tvTextFirstChar = findViewById(R.id.tvTextFirstChar);
        tvTextUserName = findViewById(R.id.tvTextUserName);
        tvTextEmail = findViewById(R.id.tvTextEmail);
        ivAcceptInvitation = findViewById(R.id.ivAcceptInvitation);
        ivRejectInvitation = findViewById(R.id.ivRejectInvitation);
    }

    private void getUserData() {
        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if (meetingType != null) {
            if (meetingType.equals("video")) {
                ivMeetingType.setImageResource(R.drawable.ic_video);
            } else {
                ivMeetingType.setImageResource(R.drawable.ic_call);
            }
        }
        firstname = getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        if (firstname != null) {
            tvTextFirstChar.setText(firstname.substring(0, 1));
        }
        lastName = getIntent().getStringExtra(Constants.KEY_LAST_NAME);
        tvTextUserName.setText(String.format("%s %s", firstname, lastName));
        email = getIntent().getStringExtra(Constants.KEY_EMAIL);
        tvTextEmail.setText(email);
    }

    private void processEventButton() {
        ivAcceptInvitation.setOnClickListener(v -> sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_ACCEPTED, getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)));
        ivRejectInvitation.setOnClickListener(v -> sendInvitationResponse(Constants.REMOTE_MSG_INVITATION_REJECTED, getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)));
    }

    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), type);
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(Constants.getRemoteMessageHeaders(), remoteMessageBody).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                        try {
                            URL serverUrl = new URL(Constants.BASE_URL_JITSI_MEETING);
                            // Set default options
                            JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                                    .setFeatureFlag("welcomepage.enabled", false)
                                    .setServerURL(serverUrl)
                                    .build();
                            JitsiMeet.setDefaultConferenceOptions(defaultOptions);
                            // Create user info
                            JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
                            userInfo.setDisplayName(firstname + " " + lastName);
                            userInfo.setEmail(email);
                            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                                    .setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM))
                                    .setVideoMuted(meetingType.equals("audio"))
                                    .setUserInfo(userInfo)
                                    .build();
                            JitsiMeetActivity.launch(IncomingInvitationActivity.this, options);
                        } catch (Exception ex) {
                            Toast.makeText(IncomingInvitationActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(IncomingInvitationActivity.this, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                }
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private final BroadcastReceiver initResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                    Toast.makeText(context, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(initResponseReceiver, new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(initResponseReceiver);
    }
}