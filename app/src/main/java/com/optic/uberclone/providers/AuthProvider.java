package com.optic.uberclone.providers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

// Clase que proporciona métodos para manejar la autenticación de Firebase
public class AuthProvider {

    // Instancia de FirebaseAuth para manejar la autenticación
    private FirebaseAuth mAuth;

    // Constructor de la clase
    public AuthProvider() {
        // Inicializa mAuth con la instancia de FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }

    // Método para registrar un usuario con correo electrónico y contraseña
    public Task<AuthResult> register(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    // Método para iniciar sesión con correo electrónico y contraseña
    public Task<AuthResult> login(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    // Método para cerrar sesión
    public void logout() {
        mAuth.signOut();
    }

    // Método para obtener el ID del usuario actualmente autenticado
    public String getId() {
        // Obtiene el ID del usuario actual
        return mAuth.getCurrentUser().getUid();
    }

    // Método para verificar si hay una sesión de usuario activa
    public boolean existSession() {
        // Variable para almacenar si hay una sesión activa
        boolean exist = false;
        // Verifica si hay un usuario autenticado actualmente
        if (mAuth.getCurrentUser() != null) {
            exist = true; // Si hay un usuario autenticado, establece exist a true
        }
        return exist; // Retorna el estado de exist
    }
}

