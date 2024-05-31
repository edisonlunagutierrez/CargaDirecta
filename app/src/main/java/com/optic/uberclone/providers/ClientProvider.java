package com.optic.uberclone.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.optic.uberclone.models.Client;

import java.util.HashMap;
import java.util.Map;

// Clase que proporciona métodos para gestionar los clientes en la base de datos
public class ClientProvider {

    // Referencia a la base de datos Firebase
    DatabaseReference mDatabase;

    // Constructor de la clase
    public ClientProvider() {
        // Inicializa mDatabase con la referencia a la ubicación "Users/Clients" en la base de datos
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Clients");
    }

    // Método para crear un nuevo cliente en la base de datos
    public Task<Void> create(Client client) {
        // Crea un mapa con los datos del cliente
        Map<String, Object> map = new HashMap<>();
        map.put("name", client.getName());
        map.put("email", client.getEmail());
        // Guarda los datos del cliente en la base de datos con la clave igual al ID del cliente
        return mDatabase.child(client.getId()).setValue(map);
    }

    // Método para actualizar los datos de un cliente en la base de datos
    public Task<Void> update(Client client) {
        // Crea un mapa con los datos actualizados del cliente
        Map<String, Object> map = new HashMap<>();
        map.put("name", client.getName());
        map.put("image", client.getImage());
        // Actualiza los datos del cliente en la base de datos
        return mDatabase.child(client.getId()).updateChildren(map);
    }

    // Método para obtener la referencia a un cliente en la base de datos
    public DatabaseReference getClient(String idClient) {
        return mDatabase.child(idClient);
    }
}

