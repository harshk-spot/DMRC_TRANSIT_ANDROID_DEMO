package com.pce.dmrc.ncmccard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.util.Locale;

public class MyApduHostService extends HostApduService {

    private static final String TAG = "HCE";

    private final byte[] FILE_NOT_FOUND = new byte[]{(byte) 0x6A, (byte) 0x82};

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {

        String apdu = bytesToHex(commandApdu).replace(" ", "").toUpperCase();

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SERVICE_PREF, Context.MODE_PRIVATE);

        if (sharedPreferences.getInt(Constants.SERVICE_STATUS, 0) == 0) {
            return FILE_NOT_FOUND;
        }

        Log.e(TAG, "APDU RECEIVED: " + apdu);

        // =========================================================
        // SELECT PPSE
        // 2PAY.SYS.DDF01
        // =========================================================
        if (apdu.contains("325041592E5359532E4444463031")) {
            Log.e(TAG, "PPSE SELECTED");
            return hexStringToByteArray("840E325041592E5359532E44444630314F07F00000052410109000");
        }

        // =========================================================
        // SELECT Spot AID
        // F0000005241010
        // =========================================================
        else if (apdu.contains("F0000005241010")) {
            Log.e(TAG, "RUPAY AID SELECTED");
            return hexStringToByteArray("500A53706F742044656269749F120C4861727368204B616D626C659000");
        }

        // =========================================================
        // GET READ RECORD
        // 89A7E70FCFF4
        // =========================================================
        else if (apdu.contains("89A7E70FCF0A")) {
            Log.e(TAG, "READ RECORD VALUE SENT");
            String pan = sharedPreferences.getString(Constants.SERVICE_PAN, "0000000000000000");
            int panLen = pan.length() / 2;
            String panLenHex = String.format("%02X", panLen);

            String bal = sharedPreferences.getString(Constants.SERVICE_BALANCE, "000000000000");
            int balLen = bal.length() / 2;
            String balLenHex = String.format("%02X", balLen);

            String serviceData = sharedPreferences.getString(Constants.SERVICE_DATA, Constants.TEMP_SERVICE_DATA);
            int serviceDataLen = serviceData.length() / 2;
            String serviceDataLenHex = String.format("%02X", serviceDataLen);

            String serviceDate = sharedPreferences.getString(Constants.SERVICE_DATE, "000000");
            int serviceDateLen = serviceDate.length() / 2;
            String serviceDateLenHex = String.format("%02X", serviceDateLen);

            String serviceExp = sharedPreferences.getString(Constants.SERVICE_EXPIRY, "000000");
            int serviceExpLen = serviceExp.length() / 2;
            String serviceExpLenHex = String.format("%02X", serviceExpLen);

            String serviceCvv = sharedPreferences.getString(Constants.SERVICE_CVV, "0000");
            int serviceCvvLen = serviceCvv.length() / 2;
            String serviceCvvLenHex = String.format("%02X", serviceCvvLen);

            return hexStringToByteArray("5A" + panLenHex + pan + "5B" + balLenHex + bal + "5C" + serviceDataLenHex + serviceData + "5D" + serviceDateLenHex + serviceDate + "9000");
        } else if (apdu.contains("89A7E70FCF0B")) {
            try {

                if (!apdu.contains("6B60")) {
                    return FILE_NOT_FOUND;
                }

                playSuccessSound();
                // ==========================================
                // GET AMOUNT AFTER 6B
                // Example:
                // 89A7E70FCF0B6B0000000100
                //                  ^^^^^^^^
                // ==========================================
                String[] splitServiceData = apdu.split("6B60");

                String serviceData = splitServiceData[1];

                sharedPreferences.edit().putString(Constants.SERVICE_DATA, serviceData).apply();

                String fareAmount = serviceData.substring(26, 30);

                Log.e(TAG, "processCommandApdu: Fare Amount: " + fareAmount);

                long longFare = Long.parseLong(fareAmount, 16);

                if (longFare == 0) {
                    return FILE_NOT_FOUND;
                }

                    // ==========================================
                    // READ CURRENT BALANCE
                    // ==========================================

                    String serviceBalance = sharedPreferences.getString(Constants.SERVICE_BALANCE, "000000000000");

                    long currentBalance = Long.parseLong(serviceBalance);

                    // ==========================================
                    // DEDUCT BALANCE
                    // ==========================================

                    long finalBalance = currentBalance - (longFare * 10L);

                    if (finalBalance < 0) {
                        finalBalance = 0;
                    }

                    String updatedBalance = String.format(Locale.getDefault(), "%012d", finalBalance);

                    // ==========================================
                    // SAVE UPDATED BALANCE
                    // ==========================================

                    sharedPreferences.edit().putString(Constants.SERVICE_BALANCE, updatedBalance).apply();

                    Log.e(TAG, "UPDATED BALANCE: " + updatedBalance);

                Intent intent = new Intent("BALANCE_UPDATED");
                intent.setPackage(getPackageName());
                sendBroadcast(intent);

                return hexStringToByteArray("9000");

            } catch (Exception e) {

                Log.e(TAG, "DEDUCT ERROR", e);

                return FILE_NOT_FOUND;
            }
        }

        Log.e(TAG, "UNKNOWN APDU");

        return FILE_NOT_FOUND;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.e(TAG, "DEACTIVATED: " + reason);
    }

    // =========================================================
    // HEX STRING -> BYTE ARRAY
    // =========================================================
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // =========================================================
    // BYTE ARRAY -> HEX STRING
    // =========================================================
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // =====================================
// NORMAL VIBRATION
// =====================================
    public void vibrate(Context context) {

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator.vibrate(VibrationEffect.createOneShot(200, // milliseconds
                    VibrationEffect.DEFAULT_AMPLITUDE));

        } else {

            vibrator.vibrate(200);
        }
    }

    // =====================================
// SUCCESS SOUND
// =====================================
    public void playSuccessSound() {

        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200);
    }
}