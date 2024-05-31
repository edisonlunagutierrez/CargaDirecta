package com.optic.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.driver.MapDriverActivity;
import com.optic.uberclone.activities.driver.RegisterDriverActivity;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.models.Client;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientProvider;

import dmax.dialog.SpotsDialog;

public class RegisterActivity extends AppCompatActivity {
    // Se declara la instancia del proveedor de autenticación y del proveedor de cliente
    AuthProvider mAuthProvider;
    ClientProvider mClientProvider;

    // VISTAS
    Button mButtonRegister;
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputName;
    TextInputEditText mTextInputPassword;

    AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Se muestra la barra de herramientas personalizada
        MyToolbar.show(this, "Registro de usuario", true);

        // Inicialización de los proveedores
        mAuthProvider = new AuthProvider();
        mClientProvider = new ClientProvider();

        // Creación del cuadro de diálogo de espera
        mDialog = new SpotsDialog.Builder().setContext(RegisterActivity.this).setMessage("Espere un momento").build();

        // Referencias a las vistas en el diseño
        mButtonRegister = findViewById(R.id.btnRegister);
        mTextInputEmail = findViewById(R.id.textInputEmail);
        mTextInputName = findViewById(R.id.textInputName);
        mTextInputPassword = findViewById(R.id.textInputPassword);

        // Configuración del clic en el botón de registro
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickRegister();
            }
        });
    }

    // Método para manejar el clic en el botón de registro
    void clickRegister() {
        // Se obtienen los valores de los campos de entrada
        final String name = mTextInputName.getText().toString();
        final String email = mTextInputEmail.getText().toString();
        final String password = mTextInputPassword.getText().toString();

        if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
            // Verificación de la longitud de la contraseña
            if (password.length() >= 6) {
                // Si la contraseña tiene al menos 6 caracteres, se muestra el cuadro de diálogo de espera y se inicia el registro
                mDialog.show();
                register(name, email, password);
            }
            else {
                // Si la contraseña no tiene al menos 6 caracteres, se muestra un mensaje de advertencia
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            // Si algún campo está vacío, se muestra un mensaje de advertencia
            Toast.makeText(this, "Ingrese todos los campos", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para realizar el registro del usuario
    void register(final String name, final String email, String password) {
        // Se llama al método de registro del proveedor de autenticación
        mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // Se oculta el cuadro de diálogo de espera
                mDialog.hide();
                if (task.isSuccessful()) {
                    // Si el registro es exitoso, se obtiene el ID del usuario y se crea un objeto cliente
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Client client = new Client(id, name, email);
                    // Se crea el cliente en la base de datos
                    create(client);
                }
                else {
                    // Si el registro falla, se muestra un mensaje de error
                    Toast.makeText(RegisterActivity.this, "No se pudo registrar el usuario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Método para crear el cliente en la base de datos
    void create(Client client) {
        // Se llama al método de creación del cliente en el proveedor de cliente
        mClientProvider.create(client).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Si la creación es exitosa, se inicia la actividad del mapa del cliente y se limpia la pila de actividades
                    Intent intent = new Intent(RegisterActivity.this, MapClientActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                else {
                    // Si la creación falla, se muestra un mensaje de error
                    Toast.makeText(RegisterActivity.this, "No se pudo crear el cliente", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
