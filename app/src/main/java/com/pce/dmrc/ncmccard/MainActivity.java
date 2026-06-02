package com.pce.dmrc.ncmccard;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NCMC_CARD";

    private Button btnStartCardService;
    private Button btnViewHistory;
    private Button btnOfflineTopUp;

    private TextView tvServicePan, tvServiceBalance, tvCardBalance, tvCardNumber, tvStationName, tvDateTime, tvFareAmount, tvRemainingBalance, tvTrxStatus, tvKms, tvSpent, tvTrips;

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
                                            Locale.getDefault(),
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

    private boolean isDefaultPaymentApp() {

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);

        if (adapter == null) {
            return false;
        }

        CardEmulation cardEmulation = CardEmulation.getInstance(adapter);

        ComponentName service = new ComponentName(
                this,
                MyApduHostService.class); // Your HCE service class

        return cardEmulation.isDefaultServiceForCategory(
                service,
                CardEmulation.CATEGORY_PAYMENT);
    }

    private void selectDefaultApp() {
        Intent intent = new Intent(
                CardEmulation.ACTION_CHANGE_DEFAULT);

        intent.putExtra(
                CardEmulation.EXTRA_SERVICE_COMPONENT,
                new ComponentName(this, MyApduHostService.class));

        intent.putExtra(
                CardEmulation.EXTRA_CATEGORY,
                CardEmulation.CATEGORY_PAYMENT);

        startActivity(intent);
    }

    private boolean isNfcEnabled() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            return false;
        }

        return nfcAdapter.isEnabled();
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

        tvCardBalance = findViewById(R.id.tvCardBalance);

        tvCardNumber = findViewById(R.id.tvCardNumber);

        tvStationName = findViewById(R.id.tvStationName);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvFareAmount = findViewById(R.id.tvFareAmount);
        tvRemainingBalance = findViewById(R.id.tvRemainingBalance);
        tvTrxStatus = findViewById(R.id.tvTrxStatus);

        tvKms = findViewById(R.id.tvKms);
        tvSpent = findViewById(R.id.tvSpent);
        tvTrips = findViewById(R.id.tvTrips);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryBottomSheet();
            }
        });

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

            tvCardNumber.setText(pan);

            String balance =
                    sharedPreferences.getString(
                            Constants.SERVICE_BALANCE,
                            "000000000000"
                    );

            long value =
                    Long.parseLong(balance);

            String formatted =
                    String.format(
                            Locale.getDefault(),
                            "%.2f",
                            value / 100.0
                    );

            tvServiceBalance.setText(
                    "**** Card Balance ****\n₹" +
                            formatted
            );

            tvCardBalance.setText("₹" + formatted);

            String serviceData = sharedPreferences.getString(Constants.SERVICE_DATA, Constants.TEMP_SERVICE_DATA);

            String rollingTrips = sharedPreferences.getString(Constants.ROLLING_TRIPS, "0");
            String rollingSpent = sharedPreferences.getString(Constants.ROLLING_SPENT, "0");
            String rollingDistance = sharedPreferences.getString(Constants.ROLLING_DISTANCE, "0");

            tvTrips.setText(rollingTrips);
            tvSpent.setText("₹" + String.format(Locale.getDefault(), "%.2f", Long.parseLong(rollingSpent) / 10.0));
            tvKms.setText(rollingDistance + " kms");

            String fareAmount = String.format(Locale.getDefault(), "%.2f", Long.parseLong(serviceData.substring(26, 30), 16) / 10.0);

            Log.d(TAG, "updateUi: Fare Amount: " + fareAmount);

            tvFareAmount.setText("₹" + fareAmount);

            double validCardBalance = Double.parseDouble(formatted) + Double.parseDouble(fareAmount);

            String finalCardBalance = String.format(Locale.getDefault(), "%.2f", validCardBalance);

            tvRemainingBalance.setText("₹" + finalCardBalance);

            String stationId = String.valueOf(Integer.parseInt(serviceData.substring(30, 34), 16));

            Log.d(TAG, "updateUi: Station Id: " + stationId);

            if (stationId.equals("25")) {
                tvStationName.setText("New Delhi");
                tvTrxStatus.setText("Exit");
                tvTrxStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
                tvTrxStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.lightRed));
            } else {
                tvStationName.setText("Shivaji Stadium");
                tvTrxStatus.setText("Entry");
                tvTrxStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
                tvTrxStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.lightGreen));
            }

            String trxStatus = serviceData.substring(40, 41);

            Log.e(TAG, "updateUi: Trx Status: " + trxStatus);

            Log.d(TAG, "updateUi: " + trxStatus);

            String dateTrxHex = serviceData.substring(20, 26);

            String dateTimeStr = getTransactionDateTime(dateTrxHex);

            tvDateTime.setText(dateTimeStr);

        }
    }

    private String getTransactionDateTime(String data) {
        String dateTimeStr = String.valueOf(Integer.parseInt(data, 16));

        Log.d(TAG, "updateUi: Date Time Str: " + dateTimeStr);

        try {
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyMMdd");
            String effDateStr = sharedPreferences.getString(Constants.SERVICE_DATE, "000000");
            Date effDate = sdf1.parse(effDateStr);
            long effTimeStamp = Objects.requireNonNull(effDate).getTime() / 1000L;

            String finalDateTrxStr = String.valueOf(effTimeStamp + Long.parseLong(dateTimeStr));

            long finalTmp = Long.parseLong(finalDateTrxStr) * 1000L; // example

            SimpleDateFormat sdf2 = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

            return sdf2.format(new Date(finalTmp));
        } catch (Exception e) {
            Log.e(TAG, "updateUi: ", e);
        }
        return "";
    }

    private void showHistoryBottomSheet() {

        BottomSheetDialog dialog =
                new BottomSheetDialog(this);

        View view = getLayoutInflater().inflate(
                R.layout.bottom_sheet_layout,
                null
        );

        RecyclerView rvHistory =
                view.findViewById(R.id.rvHistory);

        rvHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        List<HistoryItem> historyList = new ArrayList<>();

        String serviceData = sharedPreferences.getString(Constants.SERVICE_DATA, Constants.TEMP_SERVICE_DATA);

        // 17
        int RECORD_LENGTH = 34;
        Log.d(TAG, "showHistoryBottomSheet: " + serviceData.substring(42, serviceData.length() - 1));
        String historyServiceData = serviceData.substring(42, serviceData.length() - 1);

//        FF178301020A04E7A90015000009500100

        for (int i = 0;
             i + RECORD_LENGTH <= historyServiceData.length();
             i += RECORD_LENGTH) {

            String record =
                    historyServiceData.substring(
                            i,
                            i + RECORD_LENGTH
                    );

            // txn date
            String dateTimeHex = record.substring(12, 18);
            String dateTimeStr = getTransactionDateTime(dateTimeHex);

            Log.d(TAG, "showHistoryBottomSheet: dateTimeStr: " + dateTimeStr);

            // txn status
            String txnStatusStr = record.substring(31, 32);

            Log.d(TAG, "showHistoryBottomSheet: txnStatusStr: " + txnStatusStr);

//            String stationId = record.substring(18, 22);
//
//            Log.d(TAG, "showHistoryBottomSheet: StationId: " + stationId);

            Log.d("HISTORY", record);

            String txnAmtFare = String.format(Locale.getDefault(), "%.2f", Long.parseLong(record.substring(22, 26), 16) / 10.0);

            String txnAmtBal = String.format(Locale.getDefault(), "%.2f", (Long.parseLong(record.substring(26, 30)) * 10.0) / 10.0);

            Log.d(TAG, "showHistoryBottomSheet: txnAmtFare: " + record.substring(22, 26));
//            Log.d(TAG, "showHistoryBottomSheet: txnAmtFare: " + record.substring(22, 26));

            if (txnStatusStr.equals("1")) {
                historyList.add(new HistoryItem("Entry", "", "₹0.00", "₹" + txnAmtBal, dateTimeStr));
            } else {
                historyList.add(new HistoryItem("Exit", "", "₹" + txnAmtFare, "₹" + txnAmtBal, dateTimeStr));
            }
        }

//        List<HistoryItem> historyList = new ArrayList<>();

//        historyList.add(
//                new HistoryItem(
//                        "Entry",
//                        "New Delhi",
//                        "₹0.00",
//                        "₹250.00",
//                        "02 Jun 2026, 10:15 AM"
//                )
//        );
//
//        historyList.add(
//                new HistoryItem(
//                        "Exit",
//                        "Shivaji Stadium",
//                        "₹20.00",
//                        "₹230.00",
//                        "02 Jun 2026, 10:42 AM"
//                )
//        );
//
//        historyList.add(
//                new HistoryItem(
//                        "Entry",
//                        "Rajiv Chowk",
//                        "₹0.00",
//                        "₹230.00",
//                        "02 Jun 2026, 11:05 AM"
//                )
//        );
//
//        historyList.add(
//                new HistoryItem(
//                        "Exit",
//                        "Kashmere Gate",
//                        "₹15.00",
//                        "₹215.00",
//                        "02 Jun 2026, 11:22 AM"
//                )
//        );

        HistoryAdapter adapter =
                new HistoryAdapter(
                        this,
                        historyList
                );

        rvHistory.setAdapter(adapter);

        dialog.setContentView(view);
        dialog.show();
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