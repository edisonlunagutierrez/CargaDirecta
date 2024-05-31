package com.optic.uberclone.activities.driver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.CalificationDriverActivity;
import com.optic.uberclone.activities.client.MapClientActivity;
import com.optic.uberclone.models.ClientBooking;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.HistoryBookingProvider;

import java.util.Date;

public class CalificationClientActivity extends AppCompatActivity {

    private TextView mTextViewOrigin;
    private TextView mTextViewDestination;
    private RatingBar mRatinBar;
    private Button mButtonCalification;

    private ClientBookingProvider mClientBookingProvider;

    private String mExtraClientId;

    private HistoryBooking mHistoryBooking;
    private HistoryBookingProvider mHistoryBookingProvider;

    private float mCalification = 0;

    // Configura la interfaz de usuario y los elementos de la actividad para la calificación del cliente
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calification_client);

        // Inicializa y asigna vistas a variables
        mTextViewDestination = findViewById(R.id.textViewDestinationCalification);
        mTextViewOrigin = findViewById(R.id.textViewOriginCalification);
        mRatinBar = findViewById(R.id.ratingbarCalification);
        mButtonCalification = findViewById(R.id.btnCalification);

        // Inicializa los proveedores de reserva de cliente e historial de reservas
        mClientBookingProvider = new ClientBookingProvider();
        mHistoryBookingProvider = new HistoryBookingProvider();

        // Obtiene el ID del cliente extraído de los datos adicionales pasados a través de la intención
        mExtraClientId = getIntent().getStringExtra("idClient");

        // Configura el Listener para detectar cambios en la calificación seleccionada
        mRatinBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float calification, boolean b) {
                mCalification = calification;
            }
        });

        // Configura el Listener para el botón de calificación para manejar la calificación
        mButtonCalification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calificate();
            }
        });

        // Obtiene la información de la reserva del cliente
        getClientBooking();
    }


    // Obtiene la información de la reserva del cliente y la muestra en la interfaz de usuario
    private void getClientBooking() {
        // Obtiene la reserva del cliente mediante el ID del cliente extraído de los datos adicionales de la intención
        mClientBookingProvider.getClientBooking(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene los detalles de la reserva del cliente y los asigna a un objeto ClientBooking
                    ClientBooking clientBooking = dataSnapshot.getValue(ClientBooking.class);
                    // Muestra la información de la reserva en la interfaz de usuario
                    mTextViewOrigin.setText(clientBooking.getOrigin());
                    mTextViewDestination.setText(clientBooking.getDestination());
                    // Crea un objeto HistoryBooking para almacenar los detalles de la reserva
                    mHistoryBooking = new HistoryBooking(
                            clientBooking.getIdHistoryBooking(),
                            clientBooking.getIdClient(),
                            clientBooking.getIdDriver(),
                            clientBooking.getDestination(),
                            clientBooking.getOrigin(),
                            clientBooking.getTime(),
                            clientBooking.getKm(),
                            clientBooking.getStatus(),
                            clientBooking.getOriginLat(),
                            clientBooking.getOriginLng(),
                            clientBooking.getDestinationLat(),
                            clientBooking.getDestinationLng()
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Maneja el caso en que la operación sea cancelada
            }
        });
    }


    // Guarda la calificación otorgada por el cliente para el viaje y actualiza el registro de historial correspondiente
    private void calificate() {
        // Verifica si se ha asignado una calificación
        if (mCalification > 0) {
            // Establece la calificación y la marca de tiempo en el objeto HistoryBooking
            mHistoryBooking.setCalificationClient(mCalification);
            mHistoryBooking.setTimestamp(new Date().getTime());
            // Busca el registro de historial de la reserva en la base de datos
            mHistoryBookingProvider.getHistoryBooking(mHistoryBooking.getIdHistoryBooking()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Si el registro de historial ya existe, actualiza la calificación del cliente
                        mHistoryBookingProvider.updateCalificactionClient(mHistoryBooking.getIdHistoryBooking(), mCalification).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Muestra un mensaje de éxito y redirige a la actividad del mapa del conductor
                                Toast.makeText(CalificationClientActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CalificationClientActivity.this, MapDriverActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        // Si el registro de historial no existe, crea uno nuevo con la calificación del cliente
                        mHistoryBookingProvider.create(mHistoryBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Muestra un mensaje de éxito y redirige a la actividad del mapa del conductor
                                Toast.makeText(CalificationClientActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CalificationClientActivity.this, MapDriverActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Maneja el caso en que la operación sea cancelada
                }
            });
        } else {
            // Muestra un mensaje si el usuario no ha ingresado una calificación
            Toast.makeText(this, "Debes ingresar la calificacion", Toast.LENGTH_SHORT).show();
        }
    }

}
