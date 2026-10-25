package com.sum.ladybuddy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;

public class DiscreetModeActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult;
    private StringBuilder inputBuffer = new StringBuilder();
    private double firstOperand = Double.NaN;
    private String pendingOperator = "";
    private boolean isNewNumber = true;

    private SafetyHistoryDbHelper dbHelper;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discreet_mode);

        dbHelper = new SafetyHistoryDbHelper(this);

        tvExpression = findViewById(R.id.tv_calc_expression);
        tvResult = findViewById(R.id.tv_calc_result);

        findViewById(R.id.btn_calc_exit).setOnClickListener(v -> finish());
        findViewById(R.id.btn_calc_info).setOnClickListener(v -> showDecoyInfoDialog());

        setupKeypad();
    }

    private void setupKeypad() {
        int[] numIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : numIds) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> appendDigit(btn.getText().toString()));
        }

        findViewById(R.id.btn_dot).setOnClickListener(v -> appendDot());

        findViewById(R.id.btn_plus).setOnClickListener(v -> setOperator("+"));
        findViewById(R.id.btn_minus).setOnClickListener(v -> setOperator("−"));
        findViewById(R.id.btn_multiply).setOnClickListener(v -> setOperator("×"));
        findViewById(R.id.btn_divide).setOnClickListener(v -> setOperator("÷"));

        findViewById(R.id.btn_plus_minus).setOnClickListener(v -> toggleSign());
        findViewById(R.id.btn_percent).setOnClickListener(v -> applyPercent());

        Button btnC = findViewById(R.id.btn_c);
        btnC.setOnClickListener(v -> clearAll());
        btnC.setOnLongClickListener(v -> {
            triggerSecretSos("Clear Button Long-Press");
            return true;
        });

        findViewById(R.id.btn_equals).setOnClickListener(v -> calculateResult());
    }

    private void appendDigit(String digit) {
        if (isNewNumber) {
            inputBuffer.setLength(0);
            isNewNumber = false;
        }
        inputBuffer.append(digit);
        tvResult.setText(inputBuffer.toString());
    }

    private void appendDot() {
        if (isNewNumber) {
            inputBuffer.setLength(0);
            inputBuffer.append("0");
            isNewNumber = false;
        }
        if (!inputBuffer.toString().contains(".")) {
            inputBuffer.append(".");
            tvResult.setText(inputBuffer.toString());
        }
    }

    private void toggleSign() {
        if (inputBuffer.length() > 0) {
            try {
                double val = Double.parseDouble(inputBuffer.toString());
                val = -val;
                inputBuffer.setLength(0);
                inputBuffer.append(decimalFormat.format(val));
                tvResult.setText(inputBuffer.toString());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void applyPercent() {
        if (inputBuffer.length() > 0) {
            try {
                double val = Double.parseDouble(inputBuffer.toString()) / 100.0;
                inputBuffer.setLength(0);
                inputBuffer.append(decimalFormat.format(val));
                tvResult.setText(inputBuffer.toString());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void setOperator(String op) {
        if (inputBuffer.length() > 0) {
            try {
                firstOperand = Double.parseDouble(inputBuffer.toString());
                pendingOperator = op;
                tvExpression.setText(decimalFormat.format(firstOperand) + " " + op);
                isNewNumber = true;
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void calculateResult() {
        String enteredString = inputBuffer.toString().trim();

        // 1. Check for secret SOS PIN triggers
        if ("911".equals(enteredString) || "0000".equals(enteredString) || "9999".equals(enteredString)) {
            triggerSecretSos("Emergency PIN Entered: " + enteredString);
            return;
        }

        // 2. Normal arithmetic execution
        if (Double.isNaN(firstOperand) || pendingOperator.isEmpty()) {
            return;
        }

        if (inputBuffer.length() == 0) return;

        try {
            double secondOperand = Double.parseDouble(inputBuffer.toString());
            double result = 0;

            switch (pendingOperator) {
                case "+":
                    result = firstOperand + secondOperand;
                    break;
                case "−":
                    result = firstOperand - secondOperand;
                    break;
                case "×":
                    result = firstOperand * secondOperand;
                    break;
                case "÷":
                    if (secondOperand == 0) {
                        tvResult.setText("Error");
                        firstOperand = Double.NaN;
                        pendingOperator = "";
                        isNewNumber = true;
                        return;
                    }
                    result = firstOperand / secondOperand;
                    break;
            }

            tvExpression.setText(decimalFormat.format(firstOperand) + " " + pendingOperator + " " + decimalFormat.format(secondOperand) + " =");
            tvResult.setText(decimalFormat.format(result));
            inputBuffer.setLength(0);
            inputBuffer.append(decimalFormat.format(result));
            firstOperand = Double.NaN;
            pendingOperator = "";
            isNewNumber = true;

        } catch (NumberFormatException e) {
            tvResult.setText("Error");
        }
    }

    private void clearAll() {
        inputBuffer.setLength(0);
        firstOperand = Double.NaN;
        pendingOperator = "";
        isNewNumber = true;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void triggerSecretSos(String triggerSource) {
        // Haptic feedback pulse
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(400);
            }
        }

        // Record in safety history
        dbHelper.insertSafetyEvent(
                "Discreet SOS",
                "Discreet Emergency Activated",
                "Triggered via calculator decoy (" + triggerSource + ").",
                "Dispatched"
        );

        Toast.makeText(this, "Emergency Protocol Initiated", Toast.LENGTH_SHORT).show();

        // Launch full SOS dispatch activity
        Intent intent = new Intent(this, SosEmergencyActivity.class);
        intent.putExtra("DIRECT_EMERGENCY", true);
        startActivity(intent);
        finish();
    }

    private void showDecoyInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Discreet Emergency Mode")
                .setMessage("• Secret Emergency PINs:\nType 911= or 0000= on the calculator to immediately launch SOS emergency dispatch.\n\n" +
                        "• Panic Trigger:\nLong-press the 'C' button for 3 seconds.\n\n" +
                        "• Transparent Device Disclaimer:\nWhile this screen disguises AtmaRaksha visually as a calculator, modern Android system privacy indicators (e.g. green microphone or location dots in the status bar) and permission audit logs remain governed by Android OS. If your device is monitored by stalkerware or corporate management tools, OS-level permission logs cannot be hidden.")
                .setPositiveButton("Understood", null)
                .show();
    }
}
