package com.optic.uberclone.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.optic.uberclone.models.ClientBooking;

import java.util.HashMap;
import java.util.Map;

// Clase que proporciona métodos para gestionar las reservas de clientes en la base de datos
public class ClientBookingProvider {

    // Referencia a la base de datos Firebase
    private DatabaseReference mDatabase;

    // Constructor de la clase
    public ClientBookingProvider() {
        // Inicializa mDatabase con la referencia a la ubicación "ClientBooking" en la base de datos
        mDatabase = FirebaseDatabase.getInstance().getReference().child("ClientBooking");
    }

    // Método para crear una nueva reserva de cliente en la base de datos
    public Task<Void> create(ClientBooking clientBooking) {
        // Guarda la reserva de cliente en la base de datos con la clave igual al ID del cliente
        return mDatabase.child(clientBooking.getIdClient()).setValue(clientBooking);
    }

    // Método para actualizar el estado de una reserva de cliente
    public Task<Void> updateStatus(String idClientBooking, String status) {
        // Crea un mapa para actualizar el estado de la reserva de cliente
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        // Actualiza el estado de la reserva de cliente en la base de datos
        return mDatabase.child(idClientBooking).updateChildren(map);
    }

    // Método para actualizar el ID de la reserva de historial en una reserva de cliente
    public Task<Void> updateIdHistoryBooking(String idClientBooking) {
        // Genera una nueva clave única para la reserva de historial
        String idPush = mDatabase.push().getKey();
        // Crea un mapa para actualizar el ID de la reserva de historial en la reserva de cliente
        Map<String, Object> map = new HashMap<>();
        map.put("idHistoryBooking", idPush);
        // Actualiza el ID de la reserva de historial en la base de datos
        return mDatabase.child(idClientBooking).updateChildren(map);
    }

    // Método para obtener la referencia al estado de una reserva de cliente en la base de datos
    public DatabaseReference getStatus(String idClientBooking) {
        return mDatabase.child(idClientBooking).child("status");
    }

    // Método para obtener la referencia a una reserva de cliente en la base de datos
    public DatabaseReference getClientBooking(String idClientBooking) {
        return mDatabase.child(idClientBooking);
    }

    // Método para eliminar una reserva de cliente de la base de datos
    public Task<Void> delete(String idClientBooking) {
        // Elimina la reserva de cliente de la base de datos
        return mDatabase.child(idClientBooking).removeValue();
    }
}

