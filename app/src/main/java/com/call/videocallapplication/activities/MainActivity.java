package com.call.videocallapplication.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.call.videocallapplication.R;
import com.call.videocallapplication.adapters.UserAdapter;
import com.call.videocallapplication.listeners.UserListener;
import com.call.videocallapplication.models.User;
import com.call.videocallapplication.utils.Constants;
import com.call.videocallapplication.utils.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UserListener {
    private final int REQUEST_CODE_BATTERY_OPTIMIZATION = 1;

    private TextView tvAccountTitle;
    private TextView tvSignOut;
    private TextView tvErrorMessage;
    private ImageView ivImageConference;
    private RecyclerView userRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PreferenceManager preferenceManager;
    private List<User> users;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init sharedPreferences
        preferenceManager = new PreferenceManager(this);

        // mapping xml
        initView();

        // handler event sign in
        processHeader();
        processTokenFCM();

        // init user list with recyclerview
        initUserList();

        // Optimization battery
        checkForBatteryOptimizations();
    }

    private void initView() {
        tvAccountTitle = findViewById(R.id.tvTextTitle);
        tvSignOut = findViewById(R.id.tvTextSignOut);
        userRecyclerView = findViewById(R.id.userRecyclerView);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        ivImageConference = findViewById(R.id.ivImageConference);
        swipeRefreshLayout.setOnRefreshListener(this::getUsers); // Call getUsers() function to load data new
    }

    private void initUserList() {
        users = new ArrayList<>();
        userAdapter = new UserAdapter(users, this);
        userRecyclerView.setAdapter(userAdapter);

        // Get user list from firebase fire store
        getUsers();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getUsers() {
        swipeRefreshLayout.setRefreshing(true); // accept load data new
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER).get().addOnCompleteListener(task -> {
            swipeRefreshLayout.setRefreshing(false);// get data new completed then unable load data continue
            String myUserId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (task.isSuccessful() && task.getResult() != null) {
                users.clear();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    // Process skip current account
                    if (myUserId.equals(documentSnapshot.getId())) {
                        continue;
                    }
                    User user = new User();
                    user.setFirstname(documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                    user.setLastName(documentSnapshot.getString(Constants.KEY_LAST_NAME));
                    user.setEmail(documentSnapshot.getString(Constants.KEY_EMAIL));
                    user.setToken(documentSnapshot.getString(Constants.KEY_FCM_TOKEN));
                    users.add(user);
                }
                if (users.size() > 0) {
                    tvErrorMessage.setVisibility(View.GONE);
                    userAdapter.notifyDataSetChanged();
                } else {
                    tvErrorMessage.setText(R.string.no_user_variable);
                    tvErrorMessage.setVisibility(View.VISIBLE);
                }
            } else {
                tvErrorMessage.setText(R.string.no_user_variable);
                tvErrorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void processHeader() {
        tvAccountTitle.setText(String.format("%s %s", preferenceManager.getString(Constants.KEY_FIRST_NAME), preferenceManager.getString(Constants.KEY_LAST_NAME)));
        tvSignOut.setOnClickListener(v -> signOutAccount());
    }

    private void processTokenFCM() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                // [TODO] Process when can't get token FCM
                return;
            }

            // Process get token FCM successful
            String token = task.getResult();
            sendFCMTokenToDatabase(token);
        });
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USER).document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                /*
                Check update FCM token when sign in completed
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Update token FCM successful", Toast.LENGTH_SHORT).show();
                })*/.addOnFailureListener(e -> Toast.makeText(this, "Unable send token(ERROR): " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void signOutAccount() {
        Toast.makeText(this, "Signing Out...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USER).document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updateToken = new HashMap<>();
        updateToken.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updateToken).addOnSuccessListener(unused -> {
            preferenceManager.clearPreference();
            startActivity(new Intent(this, SignInActivity.class));
        }).addOnFailureListener(e -> Toast.makeText(this, "Unable to sign out this account", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void initVideoMeeting(User user) {
        if (user.getToken() == null | user.getToken().trim().isEmpty()) {
            Toast.makeText(this,
                    user.getFirstname() + " " + user.getLastName()
                            + " is not available for video meeting",
                    Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutGoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);
        }
    }

    @Override
    public void initAudioMeeting(User user) {
        if (user.getToken() == null | user.getToken().trim().isEmpty()) {
            Toast.makeText(this,
                    user.getFirstname() + " " + user.getLastName()
                            + " is not available for audio meeting",
                    Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutGoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "audio");
            startActivity(intent);
        }
    }

    @Override
    public void onMultipleUserAction(Boolean isMultipleUsersSelected) {
        if (isMultipleUsersSelected) {
            ivImageConference.setVisibility(View.VISIBLE);
            ivImageConference.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), OutGoingInvitationActivity.class);
                intent.putExtra("selectedUsers", new Gson().toJson(userAdapter.getSelectedUsers()));
                intent.putExtra("type", "video");
                intent.putExtra("isMultiple", true);
                startActivity(intent);
            });
        } else {
            ivImageConference.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            checkForBatteryOptimizations();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private void checkForBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Battery optimization is enable. It can interrupt running background services");
                builder.setPositiveButton("Disable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION);
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }
        }
    }
}