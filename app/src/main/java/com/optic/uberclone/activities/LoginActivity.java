package com.optic.uberclone.activities;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.MapClientActivity;
import com.optic.uberclone.activities.client.RegisterActivity;
import com.optic.uberclone.activities.driver.MapDriverActivity;
import com.optic.uberclone.includes.MyToolbar;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;


public class LoginActivity extends AppCompatActivity {

    /**
     * Elementos de la interfaz de usuario y variables relacionadas con la autenticación y la base de datos.
     * Incluye campos de entrada de texto para correo electrónico y contraseña, un botón de inicio de sesión y una imagen circular para retroceder.
     * También se inicializan las instancias de FirebaseAuth y DatabaseReference, se crea un diálogo de espera y se inicializa SharedPreferences.
     */
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputPassword;
    Button mButtonLogin;
    private CircleImageView mCircleImageBack;

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    AlertDialog mDialog;

    SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //MyToolbar.show(this, "Login de usuario", true);

        // Inicialización de los elementos de la interfaz de usuario
        mTextInputEmail    = findViewById(R.id.textInputEmail);
        mTextInputPassword = findViewById(R.id.textInputPassword);
        mButtonLogin       = findViewById(R.id.btnLogin);
        mCircleImageBack = findViewById(R.id.circleImageBack);

        // Inicialización de las instancias de FirebaseAuth y DatabaseReference
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Creación de un diálogo de espera
        mDialog = new SpotsDialog.Builder().setContext(LoginActivity.this).setMessage("Espere un momento").build();
        // Inicialización de SharedPreferences para almacenar el tipo de usuario

        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);

        // Configuración del botón de inicio de sesión para responder al clic del usuario
        mButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        // Configuración de la imagen circular para que al hacer clic retroceda en la actividad
        mCircleImageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }


    /**
     * Método para realizar el inicio de sesión del usuario.
     * Obtiene el correo electrónico y la contraseña ingresados por el usuario desde los campos de entrada de texto.
     * Valida que los campos no estén vacíos y que la contraseña tenga al menos 6 caracteres.
     * Muestra un diálogo de espera mientras se procesa el inicio de sesión.
     * Utiliza el método signInWithEmailAndPassword de FirebaseAuth para realizar el inicio de sesión con el correo electrónico y la contraseña proporcionados.
     * Si el inicio de sesión es exitoso, redirige al usuario a la actividad correspondiente según el tipo de usuario (cliente o conductor).
     * En caso de error, muestra un mensaje de error al usuario.
     */
    private void login() {
        String email = mTextInputEmail.getText().toString();
        String password = mTextInputPassword.getText().toString();

        if (!email.isEmpty() && !password.isEmpty()) {
            if (password.length() >= 6) {
                // Muestra un diálogo de espera mientras se procesa el inicio de sesión
                mDialog.show();
                // Utiliza FirebaseAuth para realizar el inicio de sesión con el correo electrónico y la contraseña proporcionados
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Verifica si el inicio de sesión fue exitoso
                        if (task.isSuccessful()) {
                            // Obtiene el tipo de usuario almacenado en SharedPreferences
                            String user = mPref.getString("user", "");
                            // Redirige al usuario a la actividad correspondiente según el tipo de usuario
                            if (user.equals("client")) {
                                Intent intent = new Intent(LoginActivity.this, MapClientActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(LoginActivity.this, MapDriverActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        } else {
                            // Muestra un mensaje de error si el inicio de sesión falla
                            Toast.makeText(LoginActivity.this, "La contraseña o el correo electrónico son incorrectos", Toast.LENGTH_SHORT).show();
                        }
                        // Oculta el diálogo de espera después de completar el inicio de sesión
                        mDialog.dismiss();
                    }
                });
            } else {
                // Muestra un mensaje si la contraseña tiene menos de 6 caracteres
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Muestra un mensaje si los campos de correo electrónico y contraseña están vacíos
            Toast.makeText(this, "El correo electrónico y la contraseña son obligatorios", Toast.LENGTH_SHORT).show();
        }
    }

}
