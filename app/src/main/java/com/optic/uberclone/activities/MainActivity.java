package com.optic.uberclone.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.MapClientActivity;
import com.optic.uberclone.activities.driver.MapDriverActivity;

public class MainActivity extends AppCompatActivity {

    /**
     * Configura los botones y SharedPreferences para permitir que el usuario seleccione si es cliente o conductor.
     */
    Button mButtonIAmClient; // Botón para indicar que el usuario es un cliente
    Button mButtonIAmDriver; // Botón para indicar que el usuario es un conductor
    SharedPreferences mPref; // Almacena el tipo de usuario seleccionado (cliente o conductor)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Establece el diseño de la actividad

// Obtiene una instancia de SharedPreferences para almacenar el tipo de usuario seleccionado
        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPref.edit(); // Editor para modificar SharedPreferences

// Asocia los elementos de la interfaz con los objetos de la actividad
        mButtonIAmClient = findViewById(R.id.btnIAmClient); // Botón "Soy Cliente"
        mButtonIAmDriver = findViewById(R.id.btnIAmDriver); // Botón "Soy Conductor"

// Configura un OnClickListener para el botón "Soy Cliente"
        mButtonIAmClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("user", "client"); // Almacena "client" en SharedPreferences
                editor.apply(); // Aplica los cambios en SharedPreferences
                goToSelectAuth(); // Inicia la actividad de selección de autenticación
            }
        });

// Configura un OnClickListener para el botón "Soy Conductor"
        mButtonIAmDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("user", "driver"); // Almacena "driver" en SharedPreferences
                editor.apply(); // Aplica los cambios en SharedPreferences
                goToSelectAuth(); // Inicia la actividad de selección de autenticación
            }
        });
    }


    /**
     * Verifica si hay un usuario autenticado al iniciar la actividad.
     * Si hay un usuario autenticado, redirige al usuario a la actividad correspondiente (cliente o conductor).
     * Si no hay usuario autenticado, no realiza ninguna acción.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) { // Verifica si hay un usuario autenticado
            String user = mPref.getString("user", ""); // Obtiene el tipo de usuario almacenado en SharedPreferences
            if (user.equals("client")) { // Si el usuario es un cliente
                // Redirige al usuario a la actividad del cliente y limpia la pila de actividades anteriores
                Intent intent = new Intent(MainActivity.this, MapClientActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else { // Si el usuario es un conductor
                // Redirige al usuario a la actividad del conductor y limpia la pila de actividades anteriores
                Intent intent = new Intent(MainActivity.this, MapDriverActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    }

    /**
     * Inicia la actividad para que el usuario seleccione la opción de autenticación (cliente o conductor).
     */
    private void goToSelectAuth() {
        Intent intent = new Intent(MainActivity.this, SelectOptionAuthActivity.class);
        startActivity(intent);
    }

}
