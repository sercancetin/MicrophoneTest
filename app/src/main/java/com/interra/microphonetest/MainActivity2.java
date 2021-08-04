package com.interra.microphonetest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.interra.microphonetest.MainActivity.REQUEST_AUDIO_PERMISSION_CODE;

public class MainActivity2 extends AppCompatActivity {
    static final private double EMA_FILTER = 0.6;
    private static double mEMA = 0.0;
    final Handler mHandler = new Handler();
    TextView mStatusView, mStatusAvgView, mStatus;
    MediaRecorder mRecorder;

    Thread runner;
    private List<Double> valuesAvg = new ArrayList<>();
    private long timestamp = System.currentTimeMillis() / 1000L;
    private long lastTimestamp = System.currentTimeMillis() / 1000L;
    private String schedule = "NA";

    Date c = Calendar.getInstance().getTime();
    SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
    SimpleDateFormat time = new SimpleDateFormat("HH:mm:");

    String formattedDate = df.format(c);

    Speedometer speedometer;
    final Runnable updater = new Runnable() {

        public void run() {
            updateTv();
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main2);
        mStatusView = (TextView) findViewById(R.id.dbText);
        mStatusAvgView = (TextView) findViewById(R.id.dbAvgText);
        mStatus = (TextView) findViewById(R.id.dbStatus);
        speedometer=(Speedometer)findViewById(R.id.speed);

    }

    public void onResume() {
        super.onResume();
        if(CheckPermissions()){
            startRecorder();
        }else{
            RequestPermissions();
        }

    }

    public void onPause() {
        super.onPause();
        //stopRecorder();
    }

    public void startRecorder() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (java.io.IOException ioe) {
                Log.e("[Authorize]", "IOException: " +
                        Log.getStackTraceString(ioe));

            } catch (SecurityException e) {
                Log.e("[Authorize]", "SecurityException: " +
                        Log.getStackTraceString(e));
            }
            try {
                mRecorder.start();
            } catch (SecurityException e) {
                Log.e("[Authorize]", "SecurityException: " +
                        Log.getStackTraceString(e));
            }

            //mEMA = 0.0;

            if (runner == null) {
                runner = new Thread() {
                    public void run() {
                        while (runner != null) {
                            try {
                                Thread.sleep(1000);
                                //Log.i("Noise", "Tock");
                            } catch (InterruptedException e) {
                            }
                            ;
                            mHandler.post(updater);
                        }
                    }
                };
                runner.start();
                Log.d("Noise", "start runner()");
            }
        }

    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void updateTv() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay == 19){
            schedule = "Night";
        }
        else if(hourOfDay == 8){
            schedule = "Day";

        }else{
            schedule = "NA";
        }
        mStatus.setText(schedule);

        //mStatusView.setText(Double.toString((getAmplitudeEMA())) + " dB");
        double amplitude = mRecorder.getMaxAmplitude();
        if(amplitude > 0 && amplitude < 1000000) {
            double dbl = convertdDb(amplitude);
            World.setDbCount((float)dbl);
            mStatusView.setText((int)dbl+ " db");
            valuesAvg.add(dbl);
            lastTimestamp = System.currentTimeMillis() / 1000L;
            speedometer.refresh();
            if(lastTimestamp - timestamp > 5 ){
                double sum = 0;
                int count = 0;

                for(Double value : valuesAvg) {
                    count++;
                    sum+= value;
                }
                valuesAvg = new ArrayList<>();
                timestamp = lastTimestamp;
                float average = (float) sum/count;

                mStatusAvgView.setText("AVG: "+String.format("%.2f", average)+ "db");

            }
        }
    }

    public double soundDb(double ampl) {
        return 20 * (float) Math.log10(getAmplitudeEMA() / ampl);
    }
    public double convertdDb(double amplitude) {
        // Cellphones can catch up to 90 db + -
        // getMaxAmplitude returns a value between 0-32767 (in most phones). that means that if the maximum db is 90, the pressure
        // at the microphone is 0.6325 Pascal.
        // it does a comparison with the previous value of getMaxAmplitude.
        // we need to divide maxAmplitude with (32767/0.6325)
        //51805.5336 or if 100db so 46676.6381
        double EMA_FILTER = 0.6;
        SharedPreferences sp = this.getSharedPreferences("device-base", MODE_PRIVATE);
        double amp = (double) sp.getFloat("amplitude", 0);
        double mEMAValue = EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA;
        //Log.d("db", Double.toString(amp));
        //Assuming that the minimum reference pressure is 0.000085 Pascal (on most phones) is equal to 0 db
        // samsung S9 0.000028251
        return 20 * (float) Math.log10((mEMAValue/51805.5336)/ 0.000028251);
    }


    public double getAmplitude() {
        if (mRecorder != null)
            return (mRecorder.getMaxAmplitude());
        else
            return 0;

    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }
    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity2.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
}