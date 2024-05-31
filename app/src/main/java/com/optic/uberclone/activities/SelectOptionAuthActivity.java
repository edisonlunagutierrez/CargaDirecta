package com.optic.uberclone.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.RegisterActivity;
import com.optic.uberclone.activities.driver.RegisterDriverActivity;
import com.optic.uberclone.includes.MyToolbar;

public class SelectOptionAuthActivity extends AppCompatActivity {

    Button mButtonGoToLogin;
    Button mButtonGoToRegister;
    SharedPreferences mPref;


    /**
     * Inicializa los elementos de la interfaz de usuario y los listeners para los botones de ir al inicio de sesión y al registro.
     * Además, obtiene una instancia de SharedPreferences para almacenar el tipo de usuario seleccionado.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_option_auth);
        MyToolbar.show(this, "Seleccionar opción", true); // Muestra la barra de herramientas personalizada

        // Inicializa los botones y les asigna los listeners para la navegación
        mButtonGoToLogin = findViewById(R.id.btnGoToLogin);
        mButtonGoToRegister = findViewById(R.id.btnGoToRegister);
        mButtonGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLogin(); // Método para navegar a la actividad de inicio de sesión
            }
        });
        mButtonGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToRegister(); // Método para navegar a la actividad de registro
            }
        });

        // Obtiene una instancia de SharedPreferences para almacenar el tipo de usuario seleccionado
        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);
    }

    /**
     * Navega a la actividad de inicio de sesión.
     */
    public void goToLogin() {
        Intent intent = new Intent(SelectOptionAuthActivity.this, LoginActivity.class);
        startActivity(intent);
    }


    public void goToRegister() {
        // Recupera el tipo de usuario almacenado en las preferencias compartidas
        String typeUser = mPref.getString("user", "");

        // Comprueba si el tipo de usuario es "client"
        if (typeUser.equals("client")) {
            // Si el tipo de usuario es "client", crea un Intent para la actividad de registro de clientes
            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterActivity.class);
            // Inicia la actividad de registro de clientes
            startActivity(intent);
        } else {
            // Si el tipo de usuario no es "client", asume que es otro tipo de usuario
            // Crea un Intent para la actividad de registro de conductores
            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterDriverActivity.class);
            // Inicia la actividad de registro de conductores
            startActivity(intent);
        }
    }

}
