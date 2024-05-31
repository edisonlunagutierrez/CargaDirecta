package com.optic.uberclone.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.optic.uberclone.activities.driver.MapDriverBookingActivity;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.GeofireProvider;

// Clase que actúa como receptor de la acción de aceptación de una reserva por parte de un conductor
public class AcceptReceiver extends BroadcastReceiver {

    // Proveedor de reservas de cliente
    private ClientBookingProvider mClientBookingProvider;
    // Proveedor de ubicaciones de conductores en GeoFire
    private GeofireProvider mGeofireProvider;
    // Proveedor de autenticación
    private AuthProvider mAuthProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Inicializa los proveedores necesarios
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");

        // Elimina la ubicación del conductor de GeoFire
        mGeofireProvider.removeLocation(mAuthProvider.getId());

        // Obtiene el ID del cliente de los extras del intent
        String idClient = intent.getExtras().getString("idClient");

        // Inicializa el proveedor de reservas de cliente
        mClientBookingProvider = new ClientBookingProvider();
        // Actualiza el estado de la reserva del cliente a "accept"
        mClientBookingProvider.updateStatus(idClient, "accept");

        // Cancela la notificación de la reserva recibida
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        // Abre la actividad de mapa para la reserva aceptada
        Intent intent1 = new Intent(context, MapDriverBookingActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent1.setAction(Intent.ACTION_RUN);
        intent1.putExtra("idClient", idClient);
        context.startActivity(intent1);
    }
}

