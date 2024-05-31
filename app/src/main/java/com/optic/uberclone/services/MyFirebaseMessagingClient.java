package com.optic.uberclone.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.driver.NotificationBookingActivity;
import com.optic.uberclone.channel.NotificationHelper;
import com.optic.uberclone.receivers.AcceptReceiver;
import com.optic.uberclone.receivers.CancelReceiver;

import java.util.Map;

// Clase que extiende FirebaseMessagingService para manejar las notificaciones recibidas desde Firebase Cloud Messaging (FCM)
public class MyFirebaseMessagingClient extends FirebaseMessagingService {

    // Código de notificación
    private static final int NOTIFICATION_CODE = 100;

    // Método llamado cuando se genera un nuevo token de registro de FCM para este dispositivo
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        // Se podría implementar lógica adicional aquí para manejar el nuevo token
    }

    // Método llamado cuando se recibe un mensaje de notificación desde FCM
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Obtiene la notificación y los datos asociados al mensaje
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Map<String, String> data = remoteMessage.getData();
        String title = data.get("title");
        String body = data.get("body");

        // Verifica si se recibió un título válido
        if (title != null) {
            // Verifica la versión de Android para determinar cómo mostrar la notificación
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (title.contains("SOLICITUD DE SERVICIO")) {
                    // Muestra la notificación y abre la actividad de reserva para solicitudes de servicio
                    String idClient = data.get("idClient");
                    String origin = data.get("origin");
                    String destination = data.get("destination");
                    String min = data.get("min");
                    String distance = data.get("distance");
                    showNotificationApiOreoActions(title, body, idClient);
                    showNotificationActivity(idClient, origin, destination, min, distance);
                } else if (title.contains("VIAJE CANCELADO")) {
                    // Cancela la notificación de reserva cuando se cancela un viaje
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(2);
                    showNotificationApiOreo(title, body);
                } else {
                    // Muestra la notificación con un estilo predeterminado para otros tipos de mensajes
                    showNotificationApiOreo(title, body);
                }
            } else {
                if (title.contains("SOLICITUD DE SERVICIO")) {
                    // Muestra la notificación con acciones para solicitudes de servicio
                    String idClient = data.get("idClient");
                    String origin = data.get("origin");
                    String destination = data.get("destination");
                    String min = data.get("min");
                    String distance = data.get("distance");
                    showNotificationActions(title, body, idClient);
                    showNotificationActivity(idClient, origin, destination, min, distance);
                } else if (title.contains("VIAJE CANCELADO")) {
                    // Cancela la notificación de reserva cuando se cancela un viaje
                    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(2);
                    showNotification(title, body);
                } else {
                    // Muestra la notificación con un estilo predeterminado para otros tipos de mensajes
                    showNotification(title, body);
                }
            }
        }
    }

    // Método para abrir la actividad de reserva cuando se recibe una notificación
    private void showNotificationActivity(String idClient, String origin, String destination, String min, String distance) {
        // Adquiere el servicio de administración de energía
        PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        if (!isScreenOn) {
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "AppName:MyLock"
            );
            wakeLock.acquire(10000);
        }
        // Prepara e inicia la actividad de reserva con los datos proporcionados
        Intent intent = new Intent(getBaseContext(), NotificationBookingActivity.class);
        intent.putExtra("idClient", idClient);
        intent.putExtra("origin", origin);
        intent.putExtra("destination", destination);
        intent.putExtra("min", min);
        intent.putExtra("distance", distance);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // Método para mostrar una notificación con un estilo predeterminado
    private void showNotification(String title, String body) {
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(), PendingIntent.FLAG_ONE_SHOT);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotificationOldAPI(title, body, intent, sound);
        notificationHelper.getManager().notify(1, builder.build());
    }

    // Método para mostrar una notificación con acciones personalizadas
    private void showNotificationActions(String title, String body, String idClient) {
        // Intento de aceptar la solicitud de servicio
        Intent acceptIntent = new Intent(this, AcceptReceiver.class);
        acceptIntent.putExtra("idClient", idClient);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intento de cancelar la solicitud de servicio
        Intent cancelIntent = new Intent(this, CancelReceiver.class);
        cancelIntent.putExtra("idClient", idClient);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Crea las acciones personalizadas para la notificación
        NotificationCompat.Action acceptAction = new NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                "Aceptar",
                acceptPendingIntent
        ).build();
        NotificationCompat.Action cancelAction = new NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                "Cancelar",
                cancelPendingIntent
        ).build();

        // Configura la notificación con acciones personalizadas
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        NotificationCompat.Builder builder = notificationHelper.getNotificationOldAPIActions(title, body, sound, acceptAction, cancelAction);
        notificationHelper.getManager().notify(2, builder.build());
    }

    // Método para mostrar una notificación con un estilo predeterminado para Android Oreo (API 26+) y superiores
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationApiOreo(String title, String body) {
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(), PendingIntent.FLAG_ONE_SHOT);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getNotification(title, body, intent, sound);
        notificationHelper.getManager().notify(1, builder.build());
    }

    // Método para mostrar una notificación con acciones personalizadas para Android Oreo (API 26+) y superiores
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationApiOreoActions(String title, String body, String idClient) {
        // Intento de aceptar la solicitud de servicio
        Intent acceptIntent = new Intent(this, AcceptReceiver.class);
        acceptIntent.putExtra("idClient", idClient);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intento de cancelar la solicitud de servicio
        Intent cancelIntent = new Intent(this, CancelReceiver.class);
        cancelIntent.putExtra("idClient", idClient);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_CODE, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Crea las acciones personalizadas para la notificación
        Notification.Action acceptAction= new Notification.Action.Builder(
                R.mipmap.ic_launcher,
                "Aceptar",
                acceptPendingIntent
        ).build();
        Notification.Action cancelAction= new Notification.Action.Builder(
                R.mipmap.ic_launcher,
                "Cancelar",
                cancelPendingIntent
        ).build();

        // Configura la notificación con acciones personalizadas
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getNotificationActions(title, body, sound, acceptAction, cancelAction);
        notificationHelper.getManager().notify(2, builder.build());
    }
}

