package com.optic.uberclone.channel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.optic.uberclone.R;

// Clase para ayudar en la creación y gestión de notificaciones en Android
public class NotificationHelper extends ContextWrapper {

    // Identificador y nombre del canal de notificación
    private static final String CHANNEL_ID = "com.optic.uberclone";
    private static final String CHANNEL_NAME = "CargaDirecta";

    // Administrador de notificaciones
    private NotificationManager manager;

    // Constructor de la clase
    public NotificationHelper(Context base) {
        super(base);
        // Si la versión de Android es 8.0 o superior, crea el canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    // Método para crear el canal de notificación (solo para versiones de Android 8.0 y posteriores)
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        // Crea un canal de notificación con el ID, nombre y nivel de importancia especificados
        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        // Configura opciones adicionales del canal, como luces, vibración y visibilidad en la pantalla de bloqueo
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLightColor(Color.GRAY);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        // Registra el canal de notificación con el administrador de notificaciones
        getManager().createNotificationChannel(notificationChannel);
    }

    // Método para obtener el administrador de notificaciones
    public NotificationManager getManager() {
        // Si el administrador no ha sido inicializado, obtén una instancia del servicio de notificaciones
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    // Método para obtener una instancia de Notification.Builder (para versiones de Android 8.0 y posteriores)
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotification(String title, String body, PendingIntent intent, Uri soundUri) {
        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.ic_car)
                .setStyle(new Notification.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    // Método similar al anterior, pero con acciones adicionales para la notificación
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getNotificationActions(String title,
                                                       String body,
                                                       Uri soundUri,
                                                       Notification.Action acceptAction ,
                                                       Notification.Action cancelAction) {
        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_car)
                .addAction(acceptAction)
                .addAction(cancelAction)
                .setStyle(new Notification.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    // Método para obtener una instancia de NotificationCompat.Builder (para versiones de Android anteriores a 8.0)
    public NotificationCompat.Builder getNotificationOldAPI(String title, String body, PendingIntent intent, Uri soundUri) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(intent)
                .setSmallIcon(R.drawable.ic_car)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title));
    }

    // Método similar al anterior, pero con acciones adicionales para la notificación
    public NotificationCompat.Builder getNotificationOldAPIActions(
            String title,
            String body,
            Uri soundUri,
            NotificationCompat.Action acceptAction,
            NotificationCompat.Action cancelAction) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.ic_car)
                .addAction(acceptAction)
                .addAction(cancelAction)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title));
    }
}

