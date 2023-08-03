package com.call.videocallapplication.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.call.videocallapplication.R;
import com.call.videocallapplication.utils.Constants;
import com.call.videocallapplication.utils.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {
    private ImageView ivBackSignIn;
    private TextView tvBackSignIn;
    private EditText etFistName;
    private EditText etLastName;
    private EditText etInputEmail;
    private EditText etInputPassword;
    private EditText etInputConfirmPassword;
    private AppCompatButton btnSignUp;
    private ProgressBar signUpProgressBar;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // init sharedPreferences
        preferenceManager = new PreferenceManager(this);

        // mapping xml
        initView();

        // handler event sign up
        navSignIn();
        signUpAccount();
    }

    private void initView() {
        ivBackSignIn = findViewById(R.id.ivImageBack);
        tvBackSignIn = findViewById(R.id.tvTextSignIn);
        etFistName = findViewById(R.id.etInputFirstName);
        etLastName = findViewById(R.id.etInputLastName);
        etInputEmail = findViewById(R.id.etInputEmail);
        etInputPassword = findViewById(R.id.etInputPassword);
        etInputConfirmPassword = findViewById(R.id.etInputConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        signUpProgressBar = findViewById(R.id.signUpProgressBar);
    }

    private void signUpAccount() {
        btnSignUp.setOnClickListener(v -> {
            if (validFormSignUp()) {
                btnSignUp.setVisibility(View.INVISIBLE);
                signUpProgressBar.setVisibility(View.VISIBLE);

                signUpWithFirebase();
            }
        });
    }

    private void signUpWithFirebase() {
        // process sign up with firebase fire store
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, etFistName.getText().toString().trim());
        user.put(Constants.KEY_LAST_NAME, etLastName.getText().toString().trim());
        user.put(Constants.KEY_EMAIL, etInputEmail.getText().toString().trim());
        user.put(Constants.KEY_PASSWORD, etInputPassword.getText().toString().trim());

        database.collection(Constants.KEY_COLLECTION_USER)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, etFistName.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_LAST_NAME, etLastName.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_EMAIL, etInputEmail.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_PASSWORD, etInputPassword.getText().toString().trim());

                    Toast.makeText(this, "SignUp Completed", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    signUpProgressBar.setVisibility(View.INVISIBLE);
                    btnSignUp.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "SignUp Failed(ERROR): " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validFormSignUp() {
        if (etFistName.getText().toString().trim().isEmpty()) {
            etFistName.setError("Enter first name");
            return false;
        } else if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Enter last name");
            return false;
        } else if (etInputEmail.getText().toString().trim().isEmpty()) {
            etInputEmail.setError("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(etInputEmail.getText().toString().trim()).matches()) {
            etInputPassword.setError("Email is valid");
            return false;
        } else if (etInputPassword.getText().toString().trim().isEmpty()) {
            etInputPassword.setError("Enter password");
            return false;
        } else if (!etInputPassword.getText().toString().equals(etInputConfirmPassword.getText().toString())) {
            etInputConfirmPassword.setError("Password and confirm password must be same");
            return false;
        }
        return true;
    }

    private void navSignIn() {
        ivBackSignIn.setOnClickListener(v -> onBackPressed());
        tvBackSignIn.setOnClickListener(v -> onBackPressed());
    }
}