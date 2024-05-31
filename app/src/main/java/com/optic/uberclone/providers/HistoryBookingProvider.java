package com.optic.uberclone.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.optic.uberclone.models.ClientBooking;
import com.optic.uberclone.models.HistoryBooking;

import java.util.HashMap;
import java.util.Map;

// Clase que proporciona métodos para gestionar el historial de reservas en la base de datos
public class HistoryBookingProvider {

    // Referencia a la base de datos Firebase
    private DatabaseReference mDatabase;

    // Constructor de la clase
    public HistoryBookingProvider() {
        // Inicializa mDatabase con la referencia a la ubicación "HistoryBooking" en la base de datos
        mDatabase = FirebaseDatabase.getInstance().getReference().child("HistoryBooking");
    }

    // Método para crear un nuevo registro de historial de reserva en la base de datos
    public Task<Void> create(HistoryBooking historyBooking) {
        // Guarda el historial de reserva en la base de datos con la clave igual al ID del historial de reserva
        return mDatabase.child(historyBooking.getIdHistoryBooking()).setValue(historyBooking);
    }

    // Método para actualizar la calificación del cliente en un historial de reserva
    public Task<Void> updateCalificactionClient(String idHistoryBooking, float calificacionClient) {
        // Crea un mapa con la nueva calificación del cliente
        Map<String, Object> map = new HashMap<>();
        map.put("calificationClient", calificacionClient);
        // Actualiza la calificación del cliente en el historial de reserva en la base de datos
        return mDatabase.child(idHistoryBooking).updateChildren(map);
    }

    // Método para actualizar la calificación del conductor en un historial de reserva
    public Task<Void> updateCalificactionDriver(String idHistoryBooking, float calificacionDriver) {
        // Crea un mapa con la nueva calificación del conductor
        Map<String, Object> map = new HashMap<>();
        map.put("calificationDriver", calificacionDriver);
        // Actualiza la calificación del conductor en el historial de reserva en la base de datos
        return mDatabase.child(idHistoryBooking).updateChildren(map);
    }

    // Método para obtener la referencia a un historial de reserva en la base de datos
    public DatabaseReference getHistoryBooking(String idHistoryBooking) {
        return mDatabase.child(idHistoryBooking);
    }
}

