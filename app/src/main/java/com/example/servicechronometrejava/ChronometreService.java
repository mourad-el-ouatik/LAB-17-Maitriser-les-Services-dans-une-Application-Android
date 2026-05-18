package com.example.servicechronometrejava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChronometreService extends Service {

    private final IBinder binder = new LocalBinder();

    private int secondes = 0;
    private boolean isRunning = false;
    private ScheduledExecutorService executor;
    private static final int NOTIFICATION_ID = 1001;
    private NotificationManager notificationManager;

    public class LocalBinder extends Binder {
        public ChronometreService getService() {
            return ChronometreService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        creerNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;

        if ("STOP".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!isRunning) {
            isRunning = true;
            secondes = 0; // remet à zéro au démarrage
            startForeground(NOTIFICATION_ID, creerNotification());
            demarrerChronometre();
        }
        return START_STICKY;
    }

    private void demarrerChronometre() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                secondes++;
                updateNotification();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void creerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "chrono_channel",
                    "Chronomètre Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification creerNotification() {
        return new NotificationCompat.Builder(this, "chrono_channel")
                .setContentTitle("Chronomètre en cours")
                .setContentText("Temps : " + formatTemps(secondes))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, creerNotification());
    }

    private String formatTemps(int sec) {
        int minutes = sec / 60;
        int secondesRest = sec % 60;
        return String.format("%02d:%02d", minutes, secondesRest);
    }

    // Getter pour que l'Activity puisse lire le temps affiché
    public String getTempsFormate() {
        return formatTemps(secondes);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        secondes = 0;
        if (executor != null) {
            executor.shutdown();
        }
        stopForeground(true);
        super.onDestroy();
    }
}