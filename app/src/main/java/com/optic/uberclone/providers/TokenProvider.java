package com.optic.uberclone.providers;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.optic.uberclone.models.Token;

// Clase que proporciona métodos para gestionar los tokens de dispositivo en la base de datos
public class TokenProvider {

    // Referencia a la base de datos Firebase
    DatabaseReference mDatabase;

    // Constructor de la clase
    public TokenProvider() {
        // Inicializa mDatabase con la referencia a la ubicación "Tokens" en la base de datos
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Tokens");
    }

    // Método para crear un nuevo token de dispositivo en la base de datos
    public void create(final String idUser) {
        // Verifica si el ID de usuario es nulo
        if (idUser == null) return;
        // Obtiene el token de dispositivo del usuario actual y lo guarda en la base de datos
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                // Crea un objeto Token con el token de dispositivo
                Token token = new Token(instanceIdResult.getToken());
                // Guarda el token de dispositivo en la base de datos con la clave igual al ID de usuario
                mDatabase.child(idUser).setValue(token);
            }
        });
    }

    // Método para obtener la referencia al token de dispositivo en la base de datos
    public DatabaseReference getToken(String idUser) {
        return mDatabase.child(idUser);
    }
}

