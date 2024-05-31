package com.optic.uberclone.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.optic.uberclone.providers.ClientBookingProvider;

// Clase que actúa como receptor de la acción de cancelación de una reserva por parte del cliente
public class CancelReceiver extends BroadcastReceiver {

    // Proveedor de reservas de cliente
    private ClientBookingProvider mClientBookingProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Obtiene el ID del cliente de los extras del intent
        String idClient = intent.getExtras().getString("idClient");

        // Inicializa el proveedor de reservas de cliente
        mClientBookingProvider = new ClientBookingProvider();
        // Actualiza el estado de la reserva del cliente a "cancel"
        mClientBookingProvider.updateStatus(idClient, "cancel");

        // Cancela la notificación de la reserva recibida
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
    }
}

