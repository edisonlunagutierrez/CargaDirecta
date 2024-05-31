package com.optic.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.models.ClientBooking;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.DriverProvider;
import com.optic.uberclone.providers.HistoryBookingProvider;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryBookingDetailClientActivity extends AppCompatActivity {
    private TextView mTextViewName; // TextView para mostrar el nombre del conductor
    private TextView mTextViewOrigin; // TextView para mostrar el origen del viaje
    private TextView mTextViewDestination; // TextView para mostrar el destino del viaje
    private TextView mTextViewYourCalification; // TextView para mostrar la calificación del cliente
    private RatingBar mRatingBarCalification; // RatingBar para mostrar la calificación del cliente
    private CircleImageView mCircleImage; // ImageView para mostrar la imagen del conductor
    private CircleImageView mCircleImageBack; // ImageView para regresar a la actividad anterior

    private String mExtraId; // Variable para almacenar el ID del historial de viajes
    private HistoryBookingProvider mHistoryBookingProvider; // Proveedor de historial de viajes
    private DriverProvider mDriverProvider; // Proveedor de conductores

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_detail_client);

        // Obtención de referencias a los elementos de la interfaz de usuario
        mTextViewName = findViewById(R.id.textViewNameBookingDetail);
        mTextViewOrigin = findViewById(R.id.textViewOriginHistoryBookingDetail);
        mTextViewDestination = findViewById(R.id.textViewDestinationHistoryBookingDetail);
        mTextViewYourCalification = findViewById(R.id.textViewCalificationHistoryBookingDetail);
        mRatingBarCalification = findViewById(R.id.ratingBarHistoryBookingDetail);
        mCircleImage = findViewById(R.id.circleImageHistoryBookingDetail);
        mCircleImageBack = findViewById(R.id.circleImageBack);

        // Inicialización de proveedores y obtención del ID de historial de viajes extra
        mDriverProvider = new DriverProvider();
        mExtraId = getIntent().getStringExtra("idHistoryBooking");
        mHistoryBookingProvider = new HistoryBookingProvider();
        getHistoryBooking(); // Método para obtener los detalles del historial de viajes

        // Configuración del OnClickListener para el botón de regresar
        mCircleImageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Finalizar la actividad actual al hacer clic en el botón de regresar
            }
        });
    }

    // Método para obtener los detalles del historial de viajes
    private void getHistoryBooking() {
        mHistoryBookingProvider.getHistoryBooking(mExtraId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    HistoryBooking historyBooking = dataSnapshot.getValue(HistoryBooking.class);
                    // Mostrar el origen, destino y calificación del conductor en TextViews
                    mTextViewOrigin.setText(historyBooking.getOrigin());
                    mTextViewDestination.setText(historyBooking.getDestination());
                    mTextViewYourCalification.setText("Tu calificacion: " + historyBooking.getCalificationDriver());

                    // Si existe una calificación del cliente, establecerla en el RatingBar
                    if (dataSnapshot.hasChild("calificationClient")) {
                        mRatingBarCalification.setRating((float) historyBooking.getCalificationClient());
                    }

                    // Obtener el conductor asociado al historial de viajes
                    mDriverProvider.getDriver(historyBooking.getIdDriver()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String name = dataSnapshot.child("name").getValue().toString();
                                mTextViewName.setText(name.toUpperCase()); // Mostrar el nombre del conductor
                                // Si el conductor tiene una imagen, cargarla en el ImageView utilizando Picasso
                                if (dataSnapshot.hasChild("image")) {
                                    String image = dataSnapshot.child("image").getValue().toString();
                                    Picasso.with(HistoryBookingDetailClientActivity.this).load(image).into(mCircleImage);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Manejar errores de la base de datos
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar errores de la base de datos
            }
        });
    }

}
