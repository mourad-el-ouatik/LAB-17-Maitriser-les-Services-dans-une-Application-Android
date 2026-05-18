package com.example.servicechronometrejava;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvTemps;
    private Button btnStart, btnStop;
    private ChronometreService chronometreService;
    private boolean isBound = false;

    // Handler qui rafraîchit l'UI toutes les 500ms
    private final Handler handler = new Handler();
    private final Runnable updateUI = new Runnable() {
        @Override
        public void run() {
            if (isBound && chronometreService != null) {
                tvTemps.setText(chronometreService.getTempsFormate());
            }
            handler.postDelayed(this, 500); // toutes les 500ms
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChronometreService.LocalBinder binder = (ChronometreService.LocalBinder) service;
            chronometreService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemps = findViewById(R.id.tvTemps);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> demarrer());
        btnStop.setOnClickListener(v -> arreter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateUI); // démarre le rafraîchissement
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateUI); // stoppe quand l'app n'est pas visible
    }

    private void demarrer() {
        Intent intent = new Intent(this, ChronometreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void arreter() {
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        Intent intent = new Intent(this, ChronometreService.class);
        intent.setAction("STOP");
        startService(intent); // envoie l'action STOP
        tvTemps.setText("00:00");
    }

    @Override
    protected void onDestroy() {
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        super.onDestroy();
    }
}