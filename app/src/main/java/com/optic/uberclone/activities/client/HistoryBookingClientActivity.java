package com.optic.uberclone.activities.client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.optic.uberclone.R;
import com.optic.uberclone.adapters.HistoryBookingClientAdapter;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.AuthProvider;

public class HistoryBookingClientActivity extends AppCompatActivity {

    private RecyclerView mReciclerView; // RecyclerView para mostrar el historial de viajes
    private HistoryBookingClientAdapter mAdapter; // Adaptador para el RecyclerView
    private AuthProvider mAuthProvider; // Proveedor de autenticación

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_client);
        MyToolbar.show(this, "Historial de viajes", true); // Configuración de la barra de herramientas

        // Obtención de la referencia al RecyclerView y configuración del LayoutManager
        mReciclerView = findViewById(R.id.recyclerViewHistoryBooking);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mReciclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuthProvider = new AuthProvider(); // Inicialización del proveedor de autenticación
        // Creación de una consulta para obtener los viajes del cliente actual desde la base de datos
        Query query = FirebaseDatabase.getInstance().getReference()
                .child("HistoryBooking")
                .orderByChild("idClient")
                .equalTo(mAuthProvider.getId());
        // Configuración de las opciones para el adaptador utilizando FirebaseRecyclerOptions
        FirebaseRecyclerOptions<HistoryBooking> options = new FirebaseRecyclerOptions.Builder<HistoryBooking>()
                .setQuery(query, HistoryBooking.class)
                .build();
        // Creación del adaptador utilizando las opciones y la actividad actual
        mAdapter = new HistoryBookingClientAdapter(options, HistoryBookingClientActivity.this);

        // Establecimiento del adaptador en el RecyclerView y comenzar a escuchar los cambios en la base de datos
        mReciclerView.setAdapter(mAdapter);
        mAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detener la escucha de cambios en la base de datos cuando la actividad ya no está visible
        mAdapter.stopListening();
    }

}
