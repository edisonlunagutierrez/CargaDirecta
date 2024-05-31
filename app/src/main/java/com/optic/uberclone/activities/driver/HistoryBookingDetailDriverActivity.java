package com.optic.uberclone.activities.driver;

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
import com.optic.uberclone.activities.client.HistoryBookingDetailClientActivity;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.ClientProvider;
import com.optic.uberclone.providers.DriverProvider;
import com.optic.uberclone.providers.HistoryBookingProvider;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryBookingDetailDriverActivity extends AppCompatActivity {

    private TextView mTextViewName;
    private TextView mTextViewOrigin;
    private TextView mTextViewDestination;
    private TextView mTextViewYourCalification;
    private RatingBar mRatingBarCalification;
    private CircleImageView mCircleImage;
    private CircleImageView mCircleImageBack;

    private String mExtraId;
    private HistoryBookingProvider mHistoryBookingProvider;
    private ClientProvider mClientProvider;

    // Configura la interfaz de usuario y obtiene los detalles de la reserva de historial
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_detail_driver);

        // Inicialización de vistas
        mTextViewName = findViewById(R.id.textViewNameBookingDetail);
        mTextViewOrigin = findViewById(R.id.textViewOriginHistoryBookingDetail);
        mTextViewDestination = findViewById(R.id.textViewDestinationHistoryBookingDetail);
        mTextViewYourCalification = findViewById(R.id.textViewCalificationHistoryBookingDetail);
        mRatingBarCalification = findViewById(R.id.ratingBarHistoryBookingDetail);
        mCircleImage = findViewById(R.id.circleImageHistoryBookingDetail);
        mCircleImageBack = findViewById(R.id.circleImageBack);

        // Proveedores de datos
        mClientProvider = new ClientProvider();
        mHistoryBookingProvider = new HistoryBookingProvider();

        // Obtiene el ID de la reserva de historial extraída de la intención
        mExtraId = getIntent().getStringExtra("idHistoryBooking");

        // Configura la interfaz de usuario y obtiene los detalles de la reserva de historial
        getHistoryBooking();

        // Configura el botón de retroceso para finalizar la actividad actual
        mCircleImageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    // Obtiene y muestra los detalles de la reserva de historial
    private void getHistoryBooking() {
        // Obtiene la reserva de historial del proveedor de historial de reservas utilizando el ID extraído
        mHistoryBookingProvider.getHistoryBooking(mExtraId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene la reserva de historial de la base de datos
                    HistoryBooking historyBooking = dataSnapshot.getValue(HistoryBooking.class);
                    // Muestra los detalles de la reserva de historial en las vistas correspondientes
                    mTextViewOrigin.setText(historyBooking.getOrigin());
                    mTextViewDestination.setText(historyBooking.getDestination());
                    mTextViewYourCalification.setText("Tu calificacion: " + historyBooking.getCalificationDriver());

                    // Verifica si la reserva de historial tiene una calificación del cliente y la muestra en la barra de calificación
                    if (dataSnapshot.hasChild("calificationClient")) {
                        mRatingBarCalification.setRating((float) historyBooking.getCalificationClient());
                    }

                    // Obtiene y muestra el nombre y la imagen del cliente asociado a la reserva de historial
                    mClientProvider.getClient(historyBooking.getIdClient()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Obtiene y muestra el nombre del cliente en mayúsculas
                                String name = dataSnapshot.child("name").getValue().toString();
                                mTextViewName.setText(name.toUpperCase());
                                // Verifica si el cliente tiene una imagen y la muestra utilizando Picasso
                                if (dataSnapshot.hasChild("image")) {
                                    String image = dataSnapshot.child("image").getValue().toString();
                                    Picasso.with(HistoryBookingDetailDriverActivity.this).load(image).into(mCircleImage);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
