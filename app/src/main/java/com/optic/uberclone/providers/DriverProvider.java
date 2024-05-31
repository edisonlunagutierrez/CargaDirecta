package com.optic.uberclone.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.optic.uberclone.models.Client;
import com.optic.uberclone.models.Driver;

import java.util.HashMap;
import java.util.Map;

// Clase que proporciona métodos para gestionar los conductores en la base de datos
public class DriverProvider {

    // Referencia a la base de datos Firebase
    DatabaseReference mDatabase;

    // Constructor de la clase
    public DriverProvider() {
        // Inicializa mDatabase con la referencia a la ubicación "Users/Drivers" en la base de datos
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers");
    }

    // Método para crear un nuevo conductor en la base de datos
    public Task<Void> create(Driver driver) {
        // Guarda los datos del conductor en la base de datos con la clave igual al ID del conductor
        return mDatabase.child(driver.getId()).setValue(driver);
    }

    // Método para obtener la referencia a un conductor en la base de datos
    public DatabaseReference getDriver(String idDriver) {
        return mDatabase.child(idDriver);
    }

    // Método para actualizar los datos de un conductor en la base de datos
    public Task<Void> update(Driver driver) {
        // Crea un mapa con los datos actualizados del conductor
        Map<String, Object> map = new HashMap<>();
        map.put("name", driver.getName());
        map.put("image", driver.getImage());
        map.put("vehicleBrand", driver.getVehicleBrand());
        map.put("vehiclePlate", driver.getVehiclePlate());
        // Actualiza los datos del conductor en la base de datos
        return mDatabase.child(driver.getId()).updateChildren(map);
    }
}

