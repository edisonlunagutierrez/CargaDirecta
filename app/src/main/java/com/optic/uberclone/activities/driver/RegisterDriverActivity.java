package com.optic.uberclone.activities.driver;

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
import com.optic.uberclone.activities.client.RegisterActivity;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.models.Client;
import com.optic.uberclone.models.Driver;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientProvider;
import com.optic.uberclone.providers.DriverProvider;

import dmax.dialog.SpotsDialog;

public class RegisterDriverActivity extends AppCompatActivity {

    /**
     * Clase para manejar el registro de conductores en la aplicación.
     */
    AuthProvider mAuthProvider; // Proveedor de autenticación para manejar la autenticación de usuarios.
    DriverProvider mDriverProvider; // Proveedor de controladores para manejar la información del conductor.

    // VIEWS
    Button mButtonRegister; // Botón para registrar al conductor.
    TextInputEditText mTextInputEmail; // Campo de texto para el correo electrónico del conductor.
    TextInputEditText mTextInputName; // Campo de texto para el nombre del conductor.
    TextInputEditText mTextInputVehicleBrand; // Campo de texto para la marca del vehículo del conductor.
    TextInputEditText mTextInputVehiclePlate; // Campo de texto para la placa del vehículo del conductor.
    TextInputEditText mTextInputPassword; // Campo de texto para la contraseña del conductor.

    AlertDialog mDialog; // Diálogo de progreso que se muestra mientras se realiza el registro.

    /**
     * Método llamado cuando se crea la actividad.
     * Aquí se inicializan las vistas y se configuran los listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_driver);
        MyToolbar.show(this, "Registro de conductor", true); // Muestra una barra de herramientas personalizada.

        // Inicialización de los proveedores de autenticación y de conductores.
        mAuthProvider = new AuthProvider();
        mDriverProvider = new DriverProvider();

        // Creación del diálogo de progreso.
        mDialog = new SpotsDialog.Builder().setContext(RegisterDriverActivity.this).setMessage("Espere un momento").build();

        // Inicialización de las vistas.
        mButtonRegister = findViewById(R.id.btnRegister);
        mTextInputEmail = findViewById(R.id.textInputEmail);
        mTextInputName = findViewById(R.id.textInputName);
        mTextInputVehicleBrand = findViewById(R.id.textInputVehicleBrand);
        mTextInputVehiclePlate = findViewById(R.id.textInputVehiclePlate);
        mTextInputPassword = findViewById(R.id.textInputPassword);

        // Configuración del listener del botón de registro.
        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickRegister(); // Método para manejar el clic en el botón de registro.
            }
        });
    }


    /**
     * Método para manejar el clic en el botón de registro.
     * Extrae los datos de los campos de entrada y los valida.
     * Si los datos son válidos, llama al método de registro.
     */
    void clickRegister() {
        // Extracción de los datos de los campos de texto.
        final String name = mTextInputName.getText().toString();
        final String email = mTextInputEmail.getText().toString();
        final String vehicleBrand = mTextInputVehicleBrand.getText().toString();
        final String vehiclePlate = mTextInputVehiclePlate.getText().toString();
        final String password = mTextInputPassword.getText().toString();

        // Validación de los campos de entrada.
        if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty() && !vehicleBrand.isEmpty() && !vehiclePlate.isEmpty()) {
            if (password.length() >= 6) { // La contraseña debe tener al menos 6 caracteres.
                mDialog.show(); // Muestra el diálogo de progreso.
                register(name, email, password, vehicleBrand, vehiclePlate); // Llama al método de registro.
            }
            else {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "Ingrese todos los campos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método para registrar un nuevo conductor en el sistema.
     * Utiliza el proveedor de autenticación para registrar el correo electrónico y la contraseña del conductor en Firebase Authentication.
     * Si el registro es exitoso, se crea un nuevo objeto Driver con los datos del conductor y se guarda en la base de datos.
     * @param name Nombre del conductor.
     * @param email Correo electrónico del conductor.
     * @param password Contraseña del conductor.
     * @param vehicleBrand Marca del vehículo del conductor.
     * @param vehiclePlate Placa del vehículo del conductor.
     */
    void register(final String name, final String email, String password, final String vehicleBrand, final String vehiclePlate) {
        mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                mDialog.hide(); // Oculta el diálogo de progreso.
                if (task.isSuccessful()) { // Si el registro es exitoso.
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Obtiene el ID del usuario registrado.
                    Driver driver = new Driver(id, name, email, vehicleBrand, vehiclePlate); // Crea un nuevo objeto Driver.
                    create(driver); // Guarda el objeto Driver en la base de datos.
                }
                else {
                    Toast.makeText(RegisterDriverActivity.this, "No se pudo registrar el usuario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * Método para crear un nuevo registro de conductor en la base de datos.
     * Utiliza el proveedor de conductor para realizar la creación en la base de datos.
     * @param driver Objeto Driver que contiene los datos del conductor a registrar.
     */
    void create(Driver driver) {
        mDriverProvider.create(driver).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) { // Si la creación es exitosa.
                    // Muestra un mensaje de éxito y redirige al conductor a la actividad del mapa.
                    //Toast.makeText(RegisterDriverActivity.this, "El registro se realizo exitosamente", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterDriverActivity.this, MapDriverActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                else {
                    // Si la creación falla, muestra un mensaje de error.
                    Toast.makeText(RegisterDriverActivity.this, "No se pudo crear el cliente", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}
