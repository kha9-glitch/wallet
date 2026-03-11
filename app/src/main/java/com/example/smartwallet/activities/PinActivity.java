package com.example.smartwallet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.smartwallet.R;
import com.example.smartwallet.utils.SecurityUtils;

import java.util.concurrent.Executor;

public class PinActivity extends AppCompatActivity {

    private StringBuilder inputPin = new StringBuilder();
    private View[] dots = new View[4];
    private String mode; // "SET", "CONFIRM", "ENTER", "CHANGE", "DISABLE"
    private String tempPin;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "ENTER";

        TextView tvTitle = findViewById(R.id.tv_pin_title);
        if (tvTitle != null) {
            if (mode.equals("SET")) tvTitle.setText("Set 4-Digit PIN");
            else if (mode.equals("ENTER")) tvTitle.setText("Enter App PIN");
            else if (mode.equals("CHANGE")) tvTitle.setText("Enter Current PIN");
            else if (mode.equals("DISABLE")) tvTitle.setText("Enter PIN to Disable Lock");
        }

        setupDots();
        setupKeypad();

        if (mode.equals("ENTER") && SecurityUtils.isBiometricEnabled(this)) {
            setupBiometric();
            biometricPrompt.authenticate(promptInfo);
        }
    }

    private void setupBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(PinActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                MainActivity.isPinVerified = true;
                startActivity(new Intent(PinActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for Smart Wallet")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account PIN")
                .build();
    }

    private void setupDots() {
        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);
    }

    private void setupKeypad() {
        int[] buttonIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int id : buttonIds) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    if (inputPin.length() < 4) {
                        if (v instanceof Button) {
                            inputPin.append(((Button) v).getText());
                        } else if (v instanceof com.google.android.material.button.MaterialButton) {
                            inputPin.append(((com.google.android.material.button.MaterialButton) v).getText());
                        }
                        updateDots();
                        if (inputPin.length() == 4) {
                            handlePinComplete();
                        }
                    }
                });
            }
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (inputPin.length() > 0) {
                    inputPin.deleteCharAt(inputPin.length() - 1);
                    updateDots();
                }
            });
        }

        View btnClear = findViewById(R.id.btn_clear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                inputPin.setLength(0);
                updateDots();
            });
        }
    }

    private void updateDots() {
        for (int i = 0; i < 4; i++) {
            if (dots[i] != null) {
                if (i < inputPin.length()) {
                    dots[i].setBackgroundResource(R.drawable.pin_dot_on);
                } else {
                    dots[i].setBackgroundResource(R.drawable.pin_dot_off);
                }
            }
        }
    }

    private void handlePinComplete() {
        String enteredPin = inputPin.toString();
        String savedPin = SecurityUtils.getPin(this);

        if (mode.equals("ENTER")) {
            if (enteredPin.equals(savedPin)) {
                MainActivity.isPinVerified = true;
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                clearPin();
            }
        } else if (mode.equals("SET")) {
            tempPin = enteredPin;
            mode = "CONFIRM";
            TextView tvTitle = findViewById(R.id.tv_pin_title);
            if (tvTitle != null) tvTitle.setText("Confirm PIN");
            clearPin();
        } else if (mode.equals("CONFIRM")) {
            if (enteredPin.equals(tempPin)) {
                SecurityUtils.setPin(this, enteredPin);
                Toast.makeText(this, "PIN Set Successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "PIN Mismatch. Try again.", Toast.LENGTH_SHORT).show();
                mode = "SET";
                TextView tvTitle = findViewById(R.id.tv_pin_title);
                if (tvTitle != null) tvTitle.setText("Set 4-Digit PIN");
                clearPin();
            }
        } else if (mode.equals("CHANGE")) {
            if (enteredPin.equals(savedPin)) {
                mode = "SET";
                TextView tvTitle = findViewById(R.id.tv_pin_title);
                if (tvTitle != null) tvTitle.setText("Set New PIN");
                clearPin();
            } else {
                Toast.makeText(this, "Incorrect Current PIN", Toast.LENGTH_SHORT).show();
                clearPin();
            }
        } else if (mode.equals("DISABLE")) {
            if (enteredPin.equals(savedPin)) {
                SecurityUtils.disableLock(this);
                Toast.makeText(this, "App Lock Disabled", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                clearPin();
            }
        }
    }

    private void clearPin() {
        inputPin.setLength(0);
        updateDots();
    }
}
