package com.optic.uberclone.activities.driver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.HistoryBookingClientActivity;
import com.optic.uberclone.adapters.HistoryBookingClientAdapter;
import com.optic.uberclone.adapters.HistoryBookingDriverAdapter;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.AuthProvider;

public class HistoryBookingDriverActivity extends AppCompatActivity {

    private RecyclerView mReciclerView;
    private HistoryBookingDriverAdapter mAdapter;
    private AuthProvider mAuthProvider;

    // Configura la actividad y el adaptador al mostrar el historial de viajes del conductor
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_driver);
        // Muestra la barra de herramientas personalizada con el título "Historial de viajes"
        MyToolbar.show(this, "Historial de viajes", true);

        // Inicializa y configura el RecyclerView para mostrar el historial de viajes
        mReciclerView = findViewById(R.id.recyclerViewHistoryBooking);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mReciclerView.setLayoutManager(linearLayoutManager);
    }

    // Inicia el proceso de escucha del adaptador al iniciar la actividad
    @Override
    protected void onStart() {
        super.onStart();
        // Inicializa el proveedor de autenticación
        mAuthProvider = new AuthProvider();
        // Construye la consulta para obtener el historial de viajes del conductor actual
        Query query = FirebaseDatabase.getInstance().getReference()
                .child("HistoryBooking")
                .orderByChild("idDriver")
                .equalTo(mAuthProvider.getId());
        // Configura las opciones del adaptador de FirebaseRecycler utilizando la consulta y la clase HistoryBooking
        FirebaseRecyclerOptions<HistoryBooking> options = new FirebaseRecyclerOptions.Builder<HistoryBooking>()
                .setQuery(query, HistoryBooking.class)
                .build();
        // Inicializa y configura el adaptador del historial de viajes del conductor
        mAdapter = new HistoryBookingDriverAdapter(options, HistoryBookingDriverActivity.this);
        // Establece el adaptador en el RecyclerView y comienza a escuchar los cambios en la base de datos
        mReciclerView.setAdapter(mAdapter);
        mAdapter.startListening();
    }


    // Detiene la escucha del adaptador al detener la actividad
    @Override
    protected void onStop() {
        super.onStop();
        // Detiene la escucha del adaptador para evitar fugas de memoria
        mAdapter.stopListening();
    }

}
