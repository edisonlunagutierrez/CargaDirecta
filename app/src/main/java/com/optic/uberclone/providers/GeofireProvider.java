package com.optic.uberclone.providers;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Clase que proporciona métodos para interactuar con la base de datos Firebase Realtime Database y GeoFire para gestionar la ubicación de conductores
public class GeofireProvider {

    // Referencia a la base de datos Firebase
    private DatabaseReference mDatabase;
    // Objeto GeoFire para interactuar con la base de datos utilizando GeoFire
    private GeoFire mGeofire;

    // Constructor de la clase
    public GeofireProvider(String reference) {
        // Inicializa mDatabase con la referencia especificada en la base de datos
        mDatabase = FirebaseDatabase.getInstance().getReference().child(reference);
        // Inicializa mGeofire con la referencia a la base de datos
        mGeofire = new GeoFire(mDatabase);
    }

    // Método para guardar la ubicación de un conductor en la base de datos
    public void saveLocation(String idDriver, LatLng latLng) {
        // Establece la ubicación del conductor en GeoFire utilizando el ID del conductor y las coordenadas LatLng
        mGeofire.setLocation(idDriver, new GeoLocation(latLng.latitude, latLng.longitude));
    }

    // Método para eliminar la ubicación de un conductor de la base de datos
    public void removeLocation(String idDriver) {
        // Elimina la ubicación del conductor de GeoFire utilizando su ID
        mGeofire.removeLocation(idDriver);
    }

    // Método para obtener una lista de conductores activos dentro de un radio específico desde una ubicación dada
    public GeoQuery getActiveDrivers(LatLng latLng, double radius) {
        // Crea una consulta GeoQuery para buscar conductores activos dentro del radio especificado desde la ubicación LatLng
        GeoQuery geoQuery = mGeofire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude), radius);
        // Elimina todos los listeners asociados con la consulta GeoQuery
        geoQuery.removeAllListeners();
        return geoQuery;
    }

    // Método para obtener la referencia a la ubicación de un conductor en la base de datos
    public DatabaseReference getDriverLocation(String idDriver) {
        return mDatabase.child(idDriver).child("l");
    }

    // Método para verificar si un conductor está trabajando actualmente
    public DatabaseReference isDriverWorking(String idDriver) {
        // Obtiene la referencia a la ubicación de "drivers_working" en la base de datos y agrega el ID del conductor como hijo
        return FirebaseDatabase.getInstance().getReference().child("drivers_working").child(idDriver);
    }
}

