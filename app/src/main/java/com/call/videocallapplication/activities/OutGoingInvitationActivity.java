package com.call.videocallapplication.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.call.videocallapplication.R;
import com.call.videocallapplication.models.User;
import com.call.videocallapplication.network.ApiClient;
import com.call.videocallapplication.network.ApiService;
import com.call.videocallapplication.utils.Constants;
import com.call.videocallapplication.utils.PreferenceManager;
import com.google.common.reflect.TypeToken;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutGoingInvitationActivity extends AppCompatActivity {
    private ImageView ivMeetingType;
    private ImageView ivStopInvitation;
    private TextView tvTextFirstChar;
    private TextView tvTextUserName;
    private TextView tvTextEmail;
    private PreferenceManager preferenceManager;
    private String inviterToken;
    private String meetingRoom;
    private String meetingType;
    private int rejectionCount = 0;
    private int totalReceivers = 0;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_out_going_invitation);

        // init sharedPreferences
        preferenceManager = new PreferenceManager(this);

        // mapping xml
        initView();

        // handler event
        getUserData();
    }

    private void initView() {
        ivMeetingType = findViewById(R.id.ivMeetingType);
        tvTextFirstChar = findViewById(R.id.tvTextFirstChar);
        tvTextUserName = findViewById(R.id.tvTextUserName);
        tvTextEmail = findViewById(R.id.tvTextEmail);
        ivStopInvitation = findViewById(R.id.ivStopInvitation);
    }

    private void getUserData() {
        meetingType = getIntent().getStringExtra("type");
        if (meetingType != null && meetingType.equals("video")) {
            ivMeetingType.setImageResource(R.drawable.ic_video);
        } else {
            ivMeetingType.setImageResource(R.drawable.ic_call);
        }
        user = (User) getIntent().getSerializableExtra("user");
        if (user != null) {
            tvTextFirstChar.setText(user.getFirstname().substring(0, 1));
            tvTextUserName.setText(String.format("%s %s", user.getFirstname(), user.getLastName()));
            tvTextEmail.setText(user.getEmail());
        }
        ivStopInvitation.setOnClickListener(v -> {
            if (getIntent().getBooleanExtra("isMultiple", false)) {
                Type type = new TypeToken<ArrayList<User>>() {
                }.getType();
                ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                cancelInvitation(null, receivers);
            } else {
                if (user != null) {
                    cancelInvitation(user.getToken(), null);
                }
            }
        });

        processInviterToken(meetingType, user);
    }

    private void processInviterToken(String meetingType, User user) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() && task.getResult() == null) {
                // [TODO] Process when can't get inviter token
                return;
            }

            // Process get inviter token successful
            inviterToken = task.getResult();
            // check meeting type and user data to send remote message
            if (meetingType != null) {
                if (getIntent().getBooleanExtra("isMultiple", false)) {
                    Type type = new TypeToken<ArrayList<User>>() {
                    }.getType();
                    ArrayList<User> receivers = new Gson().fromJson(getIntent().getStringExtra("selectedUsers"), type);
                    if (receivers != null) {
                        totalReceivers = receivers.size();
                    }
                    initMeeting(meetingType, null, receivers);
                } else {
                    if (user != null) {
                        totalReceivers = 1;
                        initMeeting(meetingType, user.getToken(), null);
                    }
                }
            }
        });
    }

    /*
     * Send remote message using Firebase Cloud Messaging
     * - URL: https://fcm.googleapis.com/fcm/send
     * - Headers:
     * (1) Authorization: "key=your_server_key"
     * (2) Content-Type: "application/json
     * - Body:
     * {
     *      "data" : {
     *          "type":"invitation",
     *          "meetingType":"video",
     *          "first_name":"your_first_name",
     *          "last_name:"your_last_name"
     *          "email":"your_email"
     *      },
     *      "registration_ids":["receiver_token"]
     * }
     */
    private void initMeeting(String meetingType, String receiverToken, ArrayList<User> receivers) {
        try {
            JSONArray tokens = new JSONArray();
            if (receivers != null && receivers.size() > 0) {
                tokens.put(receiverToken);
                StringBuilder usernames = new StringBuilder();
                for (int i = 0; i < receivers.size(); i++) {
                    tokens.put(receivers.get(i).getToken());
                    usernames.append(receivers.get(i).getFirstname()).append(" ").append(receivers.get(i).getLastName()).append("\n");
                }
                tvTextFirstChar.setVisibility(View.GONE);
                tvTextEmail.setVisibility(View.GONE);
                tvTextUserName.setText(usernames.toString());
            } else {
                tokens.put(receiverToken);
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
            data.put(Constants.KEY_FIRST_NAME, preferenceManager.getString(Constants.KEY_FIRST_NAME));
            data.put(Constants.KEY_LAST_NAME, preferenceManager.getString(Constants.KEY_LAST_NAME));
            data.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);

            meetingRoom = preferenceManager.getString(Constants.KEY_USER_ID) + "_" + UUID.randomUUID().toString().substring(0, 5);
            data.put(Constants.REMOTE_MSG_MEETING_ROOM, meetingRoom);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);
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
                    if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                        Toast.makeText(OutGoingInvitationActivity.this, "Invitation send successful", Toast.LENGTH_SHORT).show();
                    } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                        Toast.makeText(OutGoingInvitationActivity.this, "Invitation cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(OutGoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(OutGoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void cancelInvitation(String receiverToken, ArrayList<User> receivers) {
        try {
            JSONArray tokens = new JSONArray();

            if (receivers != null && receivers.size() > 0) {
                for (User user : receivers) {
                    tokens.put(user.getToken());
                }
            } else if (receiverToken != null) {
                tokens.put(receiverToken);
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private final BroadcastReceiver initResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
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
                        userInfo.setDisplayName(user.getFirstname() + " " + user.getLastName());
                        userInfo.setEmail(user.getEmail());
                        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                                .setRoom(meetingRoom)
                                .setVideoMuted(meetingType.equals("audio"))
                                .setUserInfo(userInfo)
                                .build();
                        JitsiMeetActivity.launch(OutGoingInvitationActivity.this, options);
                        finish();
                    } catch (Exception ex) {
                        Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    rejectionCount += 1;
                    if (rejectionCount == totalReceivers) {
                        Toast.makeText(context, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }
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