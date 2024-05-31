package com.optic.uberclone.activities.driver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.GeofireProvider;

public class NotificationBookingActivity extends AppCompatActivity {

    /**
     * Declaración de variables de la interfaz de usuario y proveedores de datos relacionados con la solicitud de reserva del cliente.
     * Se incluyen elementos como texto, botones y reproductor de medios.
     * El contador se utiliza para establecer un límite de tiempo para que el conductor acepte la reserva.
     * Se implementa un temporizador que disminuye el contador cada segundo hasta que se alcance el límite o se cancele la reserva.
     */
    private TextView mTextViewDestination; // TextView para mostrar el destino de la reserva
    private TextView mTextViewOrigin; // TextView para mostrar el origen de la reserva
    private TextView mTextViewMin; // TextView para mostrar la duración estimada de la reserva
    private TextView mTextViewDistance; // TextView para mostrar la distancia estimada de la reserva
    private TextView mTextViewCounter; // TextView para mostrar el contador de tiempo restante para aceptar la reserva
    private Button mbuttonAccept; // Botón para aceptar la reserva
    private Button mbuttonCancel; // Botón para cancelar la reserva

    private ClientBookingProvider mClientBookingProvider; // Proveedor de datos relacionados con la reserva del cliente
    private GeofireProvider mGeofireProvider; // Proveedor de datos relacionados con la ubicación geográfica
    private AuthProvider mAuthProvider; // Proveedor de autenticación

    private String mExtraIdClient; // ID del cliente para la reserva
    private String mExtraOrigin; // Origen de la reserva
    private String mExtraDestination; // Destino de la reserva
    private String mExtraMin; // Duración estimada de la reserva en minutos
    private String mExtraDistance; // Distancia estimada de la reserva

    private MediaPlayer mMediaPlayer; // Reproductor de medios para efectos de sonido

    private int mCounter = 45; // Contador para establecer el tiempo límite para aceptar la reserva
    private Handler mHandler; // Manejador para programar la ejecución del temporizador

    // Runnable para ejecutar la lógica del temporizador
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mCounter = mCounter - 1;
            mTextViewCounter.setText(String.valueOf(mCounter)); // Actualizar el TextView con el nuevo valor del contador
            if (mCounter > 0) {
                initTimer(); // Iniciar el temporizador nuevamente si el contador es mayor que cero
            } else {
                cancelBooking(); // Cancelar la reserva si el contador alcanza cero
            }
        }
    };

    private ValueEventListener mListener; // Escuchador de eventos para escuchar cambios en la reserva del cliente

    /**
     * Método para iniciar el temporizador.
     * Se programa una ejecución diferida de la lógica del temporizador cada segundo.
     */
    private void initTimer() {
        mHandler = new Handler();
        mHandler.postDelayed(runnable, 1000);
    }


    /**
     * Método que se llama cuando se crea la actividad de notificación de reserva.
     * Se inicializan los elementos de la interfaz de usuario y se recuperan los datos extra de la intención.
     * Se establecen los valores de origen, destino, duración estimada y distancia estimada en los TextView correspondientes.
     * Se inicializa el reproductor de medios con el tono de llamada y se configura para que se reproduzca en bucle.
     * Se instancia el proveedor de datos relacionados con la reserva del cliente.
     * Se configura la ventana para mantener la pantalla encendida y mostrar la actividad sobre la pantalla de bloqueo.
     * Se inicia el temporizador para contar el tiempo restante para aceptar la reserva.
     * Se verifica si el cliente canceló la reserva.
     * Se establece un listener de clics para el botón de aceptar la reserva.
     * Se establece un listener de clics para el botón de cancelar la reserva.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_booking);

        // Inicialización de elementos de la interfaz de usuario
        mTextViewDestination = findViewById(R.id.textViewDestination);
        mTextViewOrigin = findViewById(R.id.textViewOrigin);
        mTextViewMin = findViewById(R.id.textViewMin);
        mTextViewDistance = findViewById(R.id.textViewDistance);
        mTextViewCounter = findViewById(R.id.textViewCounter);
        mbuttonAccept = findViewById(R.id.btnAcceptBooking);
        mbuttonCancel = findViewById(R.id.btnCancelBooking);

        // Recuperación de datos extra de la intención
        mExtraIdClient = getIntent().getStringExtra("idClient");
        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");
        mExtraMin = getIntent().getStringExtra("min");
        mExtraDistance = getIntent().getStringExtra("distance");

        // Establecimiento de los valores de origen, destino, duración estimada y distancia estimada en los TextView correspondientes
        mTextViewDestination.setText(mExtraDestination);
        mTextViewOrigin.setText(mExtraOrigin);
        mTextViewMin.setText(mExtraMin);
        mTextViewDistance.setText(mExtraDistance);

        // Inicialización del reproductor de medios con el tono de llamada y configuración para reproducción en bucle
        mMediaPlayer = MediaPlayer.create(this, R.raw.ringtone);
        mMediaPlayer.setLooping(true);

        // Instanciación del proveedor de datos relacionados con la reserva del cliente
        mClientBookingProvider = new ClientBookingProvider();

        // Configuración de la ventana para mantener la pantalla encendida y mostrar la actividad sobre la pantalla de bloqueo
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        // Inicio del temporizador para contar el tiempo restante para aceptar la reserva
        initTimer();

        // Verificación si el cliente canceló la reserva
        checkIfClientCancelBooking();

        // Establecimiento de un listener de clics para el botón de aceptar la reserva
        mbuttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptBooking();
            }
        });

        // Establecimiento de un listener de clics para el botón de cancelar la reserva
        mbuttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelBooking();
            }
        });
    }


    /**
     * Verifica si el cliente ha cancelado la reserva.
     * Se añade un listener a la reserva del cliente para detectar cambios en los datos.
     * Cuando se produce un cambio, se comprueba si la reserva del cliente ya no existe en la base de datos.
     * Si la reserva ha sido cancelada, se muestra un mensaje de cancelación, se detiene el temporizador y se redirige a la actividad del mapa del conductor.
     */
    private void checkIfClientCancelBooking() {
        mListener = mClientBookingProvider.getClientBooking(mExtraIdClient).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // Si la reserva del cliente ya no existe, se muestra un mensaje de cancelación
                    Toast.makeText(NotificationBookingActivity.this, "El cliente canceló el viaje", Toast.LENGTH_LONG).show();
                    // Se detiene el temporizador si está en marcha
                    if (mHandler != null) mHandler.removeCallbacks(runnable);
                    // Se redirige a la actividad del mapa del conductor
                    Intent intent = new Intent(NotificationBookingActivity.this, MapDriverActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Método llamado cuando se cancela la operación de lectura de datos
            }
        });
    }


    /**
     * Cancela la reserva actual.
     * Detiene el temporizador si está en marcha.
     * Actualiza el estado de la reserva del cliente a "cancelada" en la base de datos.
     * Cancela la notificación asociada a la reserva.
     * Redirige a la actividad del mapa del conductor.
     */
    private void cancelBooking() {
        // Detiene el temporizador si está en marcha
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        // Actualiza el estado de la reserva del cliente a "cancelada" en la base de datos
        mClientBookingProvider.updateStatus(mExtraIdClient, "cancel");
        // Cancela la notificación asociada a la reserva
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
        // Redirige a la actividad del mapa del conductor
        Intent intent = new Intent(NotificationBookingActivity.this, MapDriverActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Acepta la reserva actual.
     * Detiene el temporizador si está en marcha.
     * Elimina la ubicación del conductor de la lista de conductores activos.
     * Actualiza el estado de la reserva del cliente a "aceptada" en la base de datos.
     * Cancela la notificación asociada a la reserva.
     * Inicia la actividad de reservas del conductor.
     */
    private void acceptBooking() {
        // Detiene el temporizador si está en marcha
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        // Elimina la ubicación del conductor de la lista de conductores activos
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mGeofireProvider.removeLocation(mAuthProvider.getId());
        // Actualiza el estado de la reserva del cliente a "aceptada" en la base de datos
        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(mExtraIdClient, "accept");
        // Cancela la notificación asociada a la reserva
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
        // Inicia la actividad de reservas del conductor
        Intent intent1 = new Intent(NotificationBookingActivity.this, MapDriverBookingActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent1.setAction(Intent.ACTION_RUN);
        intent1.putExtra("idClient", mExtraIdClient);
        startActivity(intent1);
    }


    /**
     * Pausa la reproducción del sonido de notificación cuando la actividad está en pausa.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Pausa la reproducción del sonido de notificación si está reproduciéndose
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Libera los recursos de MediaPlayer cuando la actividad está detenida.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Libera los recursos de MediaPlayer si está reproduciéndose
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.release();
        }
    }

    /**
     * Reanuda la reproducción del sonido de notificación cuando la actividad se reanuda.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reanuda la reproducción del sonido de notificación si no está reproduciéndose
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    /**
     * Limpia los recursos y los callbacks cuando la actividad está siendo destruida.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detiene el temporizador si está en marcha
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        // Pausa la reproducción del sonido de notificación si está reproduciéndose
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        // Remueve el listener de la base de datos si está asignado
        if (mListener != null) {
            mClientBookingProvider.getClientBooking(mExtraIdClient).removeEventListener(mListener);
        }
    }

}
