package com.optic.uberclone.activities.client;

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
import com.optic.uberclone.activities.driver.CalificationClientActivity;
import com.optic.uberclone.activities.driver.MapDriverActivity;
import com.optic.uberclone.models.ClientBooking;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.HistoryBookingProvider;

import java.util.Date;

public class CalificationDriverActivity extends AppCompatActivity {

    private TextView mTextViewOrigin;
    private TextView mTextViewDestination;
    private RatingBar mRatinBar;
    private Button mButtonCalification;

    private ClientBookingProvider mClientBookingProvider;
    private AuthProvider mAuthProvider;

    private HistoryBooking mHistoryBooking;
    private HistoryBookingProvider mHistoryBookingProvider;

    private float mCalification = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calification_driver);

        // Referencias a los elementos de la interfaz de usuario
        mTextViewDestination = findViewById(R.id.textViewDestinationCalification);
        mTextViewOrigin = findViewById(R.id.textViewOriginCalification);
        mRatinBar = findViewById(R.id.ratingbarCalification);
        mButtonCalification = findViewById(R.id.btnCalification);

        // Inicialización de proveedores y objetos necesarios
        mClientBookingProvider = new ClientBookingProvider();
        mHistoryBookingProvider = new HistoryBookingProvider();
        mAuthProvider = new AuthProvider();

        // Listener para cambios en la calificación
        mRatinBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float calification, boolean b) {
                mCalification = calification;
            }
        });

        // Listener para el clic en el botón de calificación
        mButtonCalification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calificate(); // Método para procesar la calificación
            }
        });

        getClientBooking(); // Obtener detalles del viaje del cliente
    }

    // Método para obtener detalles del viaje del cliente
    private void getClientBooking() {
        mClientBookingProvider.getClientBooking(mAuthProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Si existe el viaje, se muestran los detalles en la interfaz de usuario
                    ClientBooking clientBooking = dataSnapshot.getValue(ClientBooking.class);
                    mTextViewOrigin.setText(clientBooking.getOrigin());
                    mTextViewDestination.setText(clientBooking.getDestination());

                    // Se crea un objeto HistoryBooking con los detalles del viaje
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
                // Manejo de errores de la base de datos
            }
        });
    }

    // Método para procesar la calificación
    private void calificate() {
        if (mCalification > 0) {
            // Si se proporciona una calificación válida, se actualiza el objeto HistoryBooking
            mHistoryBooking.setCalificationDriver(mCalification);
            mHistoryBooking.setTimestamp(new Date().getTime());

            // Consulta a la base de datos para ver si existe una entrada para este viaje en el historial
            mHistoryBookingProvider.getHistoryBooking(mHistoryBooking.getIdHistoryBooking()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Si existe una entrada, se actualiza la calificación del conductor
                        mHistoryBookingProvider.updateCalificactionDriver(mHistoryBooking.getIdHistoryBooking(), mCalification).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Mensaje de éxito y redirección a la actividad de mapa
                                Toast.makeText(CalificationDriverActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CalificationDriverActivity.this, MapClientActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    } else {
                        // Si no existe una entrada, se crea una nueva en el historial
                        mHistoryBookingProvider.create(mHistoryBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Mensaje de éxito y redirección a la actividad de mapa
                                Toast.makeText(CalificationDriverActivity.this, "La calificacion se guardo correctamente", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CalificationDriverActivity.this, MapClientActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Manejo de errores de la base de datos
                }
            });
        } else {
            // Si no se proporciona una calificación válida, se muestra un mensaje al usuario
            Toast.makeText(this, "Debes ingresar la calificacion", Toast.LENGTH_SHORT).show();
        }
    }

}