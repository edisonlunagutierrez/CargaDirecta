package com.optic.uberclone.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.HistoryBookingDetailClientActivity;
import com.optic.uberclone.models.HistoryBooking;
import com.optic.uberclone.providers.DriverProvider;
import com.squareup.picasso.Picasso;

public class HistoryBookingClientAdapter extends FirebaseRecyclerAdapter<HistoryBooking, HistoryBookingClientAdapter.ViewHolder> {

    private DriverProvider mDriverProvider;
    private Context mContext;

    public HistoryBookingClientAdapter(FirebaseRecyclerOptions<HistoryBooking> options, Context context) {
        super(options);
        mDriverProvider = new DriverProvider();
        mContext = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull final ViewHolder holder, int position, @NonNull HistoryBooking historyBooking) {
        // Obtiene el ID de la reserva de historial en la posición actual
        final String id = getRef(position).getKey();

        // Establece los datos de la reserva en las vistas correspondientes
        holder.textViewOrigin.setText(historyBooking.getOrigin());
        holder.textViewDestination.setText(historyBooking.getDestination());
        holder.textViewCalification.setText(String.valueOf(historyBooking.getCalificationClient()));

        // Obtiene información adicional del conductor asociado con la reserva
        mDriverProvider.getDriver(historyBooking.getIdDriver()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene el nombre del conductor y lo establece en el TextView correspondiente
                    String name = dataSnapshot.child("name").getValue().toString();
                    holder.textViewName.setText(name);
                    // Si el conductor tiene una imagen, la carga utilizando Picasso
                    if (dataSnapshot.hasChild("image")) {
                        String image = dataSnapshot.child("image").getValue().toString();
                        Picasso.with(mContext).load(image).into(holder.imageViewHistoryBooking);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Maneja la cancelación de la operación de obtención de datos del conductor
            }
        });

        // Configura un OnClickListener en el elemento de la lista para abrir los detalles de la reserva al hacer clic
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, HistoryBookingDetailClientActivity.class);
                intent.putExtra("idHistoryBooking", id);
                mContext.startActivity(intent);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el diseño del elemento de la lista y crea un nuevo ViewHolder que contiene las vistas
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_history_booking, parent, false);
        return new ViewHolder(view);
    }

    // Clase ViewHolder para mantener las referencias a las vistas dentro de cada elemento de la lista
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewName;
        private TextView textViewOrigin;
        private TextView textViewDestination;
        private TextView textViewCalification;
        private ImageView imageViewHistoryBooking;
        private View mView;

        public ViewHolder(View view) {
            super(view);
            // Inicializa las vistas dentro del ViewHolder
            mView = view;
            textViewName = view.findViewById(R.id.textViewName);
            textViewOrigin = view.findViewById(R.id.textViewOrigin);
            textViewDestination = view.findViewById(R.id.textViewDestination);
            textViewCalification = view.findViewById(R.id.textViewCalification);
            imageViewHistoryBooking = view.findViewById(R.id.imageViewHistoryBooking);
        }
    }

}
