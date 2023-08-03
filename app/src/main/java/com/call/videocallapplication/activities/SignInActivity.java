package com.call.videocallapplication.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.call.videocallapplication.R;
import com.call.videocallapplication.utils.Constants;
import com.call.videocallapplication.utils.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private TextView tvSignUp;
    private EditText etInputEmail, etInputPassword;
    private AppCompatButton btnSignIn;
    private ProgressBar signInProgressBar;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // init sharedPreferences
        preferenceManager = new PreferenceManager(this);

        // Check session sign in for account
        checkSessionSignIn();

        // mapping xml
        initView();

        // handler event sign in
        navSignUp();
        signInAccount();
    }

    private void initView() {
        tvSignUp = findViewById(R.id.tvTextSignUp);
        etInputEmail = findViewById(R.id.etInputEmail);
        etInputPassword = findViewById(R.id.etInputPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        signInProgressBar = findViewById(R.id.signInProgressBar);
    }

    private void signInAccount() {
        btnSignIn.setOnClickListener(v -> {
            if (validFormSignIn()) {
                btnSignIn.setVisibility(View.INVISIBLE);
                signInProgressBar.setVisibility(View.VISIBLE);

                signInWithFirebase();
            }
        });
    }

    private boolean validFormSignIn() {
        if (etInputEmail.getText().toString().trim().isEmpty()) {
            etInputEmail.setError("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(etInputEmail.getText().toString().trim()).matches()) {
            etInputPassword.setError("Email is valid");
            return false;
        } else if (etInputPassword.getText().toString().trim().isEmpty()) {
            etInputPassword.setError("Enter password");
            return false;
        }
        return true;
    }

    private void signInWithFirebase() {
        // process sign in with firebase fire store
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL, etInputEmail.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD, etInputPassword.getText().toString().trim())
                .get()
                .addOnCompleteListener(task -> {
                    // Check data response not non or null then process sign in
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, snapshot.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, snapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, snapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, snapshot.getString(Constants.KEY_EMAIL));
                        Toast.makeText(this, "SignUp Completed", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        signInProgressBar.setVisibility(View.INVISIBLE);
                        btnSignIn.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Unable to sign in with this account", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    signInProgressBar.setVisibility(View.INVISIBLE);
                    btnSignIn.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "SignIn Failed(ERROR): " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkSessionSignIn() {
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void navSignUp() {
        tvSignUp.setOnClickListener(v -> startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
    }


}