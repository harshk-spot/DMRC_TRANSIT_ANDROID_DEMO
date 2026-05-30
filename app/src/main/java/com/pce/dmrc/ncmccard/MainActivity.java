package com.pce.dmrc.ncmccard;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NCMC_CARD";

    private Button btnStartCardService;
    private Button btnOfflineTopUp;

    private TextView tvServicePan;
    private TextView tvServiceBalance;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        initVariables();

        updateUi();

        // =====================================================
        // CREATE CARD SERVICE
        // =====================================================
        btnStartCardService.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        SharedPreferences.Editor editor =
                                sharedPreferences.edit();

                        int status =
                                sharedPreferences.getInt(
                                        Constants.SERVICE_STATUS,
                                        0
                                );

                        if (status == 0) {

                            String deviceId =
                                    DeviceUtils.getDeviceId(
                                            MainActivity.this
                                    );

                            String fixedDeviceId =
                                    getFixed16Hex(deviceId);

                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("yyMMdd", Locale.getDefault());

                            String dateTime = sdf.format(new Date());   // e.g. 260530

                            Random random = new Random();
                            int random4Digit = 1000 + random.nextInt(9000);

                            String randomNumber = String.format(Locale.getDefault(), "%04d", random4Digit);

                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.YEAR, 10);

                            String expDate =
                                    new SimpleDateFormat("yyMMdd", Locale.getDefault())
                                            .format(cal.getTime());

                            Log.e(TAG,
                                    "DEVICE ID: " + fixedDeviceId);

                            editor.putString(
                                    Constants.SERVICE_BALANCE,
                                    "000000000000"
                            );

                            editor.putString(
                                    Constants.SERVICE_PAN,
                                    fixedDeviceId
                            );

                            editor.putString(
                                    Constants.SERVICE_DATE,
                                    dateTime
                            );

                            editor.putString(
                                    Constants.SERVICE_EXPIRY,
                                    expDate
                            );

                            editor.putString(
                                    Constants.SERVICE_CVV,
                                    randomNumber
                            );

                            editor.putString(
                                    Constants.SERVICE_DATA,
                                    Constants.TEMP_SERVICE_DATA
                            );

                            editor.putInt(
                                    Constants.SERVICE_STATUS,
                                    1
                            );

                            editor.apply();

                            updateUi();

                            Toast.makeText(
                                    MainActivity.this,
                                    "Service created successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Service already exists!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );

        // =====================================================
        // OFFLINE TOPUP
        // =====================================================
        btnOfflineTopUp.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        SharedPreferences.Editor editor =
                                sharedPreferences.edit();

                        int status =
                                sharedPreferences.getInt(
                                        Constants.SERVICE_STATUS,
                                        0
                                );

                        if (status == 1) {

                            String serviceBalance =
                                    sharedPreferences.getString(
                                            Constants.SERVICE_BALANCE,
                                            "000000000000"
                                    );

                            long currentBalance =
                                    Long.parseLong(serviceBalance);

                            // Add 100 rupees
                            long finalBalance =
                                    currentBalance + (100 * 100L);

                            String storedValue =
                                    String.format(
                                            "%012d",
                                            finalBalance
                                    );

                            editor.putString(
                                    Constants.SERVICE_BALANCE,
                                    storedValue
                            );

                            editor.apply();

                            updateUi();

                            Toast.makeText(
                                    MainActivity.this,
                                    "Top-Up Success!",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Service not created!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );
    }

    // =====================================================
    // INITIALIZE VARIABLES
    // =====================================================
    private void initVariables() {

        btnStartCardService =
                findViewById(R.id.btnStartCardService);

        btnOfflineTopUp =
                findViewById(R.id.btnOfflineTopUp);

        tvServicePan =
                findViewById(R.id.tvServicePan);

        tvServiceBalance =
                findViewById(R.id.tvServiceBalance);

        sharedPreferences =
                getSharedPreferences(
                        Constants.SERVICE_PREF,
                        Context.MODE_PRIVATE
                );

        if (!sharedPreferences.contains(
                Constants.SERVICE_STATUS
        )) {

            sharedPreferences.edit()
                    .putInt(Constants.SERVICE_STATUS, 0)
                    .apply();
        }
    }

    // =====================================================
    // UPDATE UI REALTIME
    // =====================================================
    private void updateUi() {

        int status =
                sharedPreferences.getInt(
                        Constants.SERVICE_STATUS,
                        0
                );

        if (status == 1) {

            String pan =
                    sharedPreferences.getString(
                            Constants.SERVICE_PAN,
                            "0000000000000000"
                    );

            tvServicePan.setText(
                    "**** Card Number ****\n" + pan
            );

            String balance =
                    sharedPreferences.getString(
                            Constants.SERVICE_BALANCE,
                            "000000000000"
                    );

            long value =
                    Long.parseLong(balance);

            String formatted =
                    String.format(
                            "%.2f",
                            value / 100.0
                    );

            tvServiceBalance.setText(
                    "**** Card Balance ****\n₹" +
                            formatted
            );

        } else {

            tvServicePan.setText(
                    "**** Card Number ****\nNOT CREATED"
            );

            tvServiceBalance.setText(
                    "**** Card Balance ****\n₹0.00"
            );
        }
    }

    // =====================================================
    // FIXED 16 DIGIT HEX
    // =====================================================
    public static String getFixed16Hex(String input) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    md.digest(input.getBytes());

            StringBuilder sb =
                    new StringBuilder();

            for (byte b : hash) {

                sb.append(
                        String.format("%02X", b)
                );
            }

            return sb.substring(0, 16);

        } catch (Exception e) {

            Log.e(TAG,
                    "getFixed16Hex: ",
                    e);

            return "0000000000000000";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "onResume: Registered Received");
                registerReceiver(
                        balanceReceiver,
                        new IntentFilter("BALANCE_UPDATED"),
                        Context.RECEIVER_NOT_EXPORTED
                );
            }
        }
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        unregisterReceiver(balanceReceiver);
    }

    private final BroadcastReceiver balanceReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.e(TAG, "BALANCE UPDATED");
                    runOnUiThread(() -> updateUi());
                }
            };
}