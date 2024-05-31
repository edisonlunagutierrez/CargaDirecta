package com.optic.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.models.ClientBooking;
import com.optic.uberclone.models.FCMBody;
import com.optic.uberclone.models.FCMResponse;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.GeofireProvider;
import com.optic.uberclone.providers.GoogleApiProvider;
import com.optic.uberclone.providers.NotificationProvider;
import com.optic.uberclone.providers.TokenProvider;
import com.optic.uberclone.retrofit.IFCMApi;
import com.optic.uberclone.utils.DecodePoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestDriverActivity extends AppCompatActivity {

    private LottieAnimationView mAnimation;
    private TextView mTextViewLookingFor;
    private Button mButtonCancelRequest;
    private GeofireProvider mGeofireProvider;

    private String mExtraOrigin;
    private String mExtraDestination;
    private double mExtraOriginLat;
    private double mExtraOriginLng;
    private double mExtraDestinationLat;
    private double mExtraDestinationLng;
    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private double mRadius = 0.1;
    private boolean mDriverFound = false;
    private String  mIdDriverFound = "";
    private LatLng mDriverFoundLatLng;
    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;
    private ClientBookingProvider mClientBookingProvider;
    private AuthProvider mAuthProvider;
    private GoogleApiProvider mGoogleApiProvider;

    private ValueEventListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_driver);

        // Vistas
        mAnimation = findViewById(R.id.animation);
        mTextViewLookingFor = findViewById(R.id.textViewLookingFor);
        mButtonCancelRequest = findViewById(R.id.btnCancelRequest);

        // Se inicia la animación
        mAnimation.playAnimation();

        // Se obtienen los datos del origen y destino del intent
        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");
        mExtraOriginLat = getIntent().getDoubleExtra("origin_lat", 0);
        mExtraOriginLng = getIntent().getDoubleExtra("origin_lng", 0);
        mExtraDestinationLat = getIntent().getDoubleExtra("destination_lat", 0);
        mExtraDestinationLng = getIntent().getDoubleExtra("destination_lng", 0);
        // Se crea LatLng para el origen y destino
        mOriginLatLng = new LatLng(mExtraOriginLat, mExtraOriginLng);
        mDestinationLatLng= new LatLng(mExtraDestinationLat, mExtraDestinationLng);

        // Inicialización de proveedores y servicios
        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();
        mNotificationProvider = new NotificationProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mAuthProvider = new AuthProvider();
        mGoogleApiProvider = new GoogleApiProvider(RequestDriverActivity.this);

        // Configuración del clic en el botón de cancelar solicitud
        mButtonCancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelRequest();
            }
        });

        // Se busca al conductor más cercano
        getClosestDriver();
    }

    // Método para cancelar la solicitud de viaje
    private void cancelRequest() {
        // Se elimina la solicitud del cliente en la base de datos
        mClientBookingProvider.delete(mAuthProvider.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Se envía una notificación de cancelación
                sendNotificationCancel();
            }
        });
    }


    private void getClosestDriver() {
        // Se consulta a la base de datos de conductores activos dentro del radio establecido
        mGeofireProvider.getActiveDrivers(mOriginLatLng, mRadius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Si no se ha encontrado ningún conductor, se marca como encontrado y se crea la solicitud de viaje
                if (!mDriverFound) {
                    mDriverFound = true;
                    mIdDriverFound = key;
                    mDriverFoundLatLng = new LatLng(location.latitude, location.longitude);
                    mTextViewLookingFor.setText("CONDUCTOR ENCONTRADO\nESPERANDO RESPUESTA");
                    createClientBooking();
                    Log.d("DRIVER", "ID: " + mIdDriverFound);
                }
            }

            @Override
            public void onKeyExited(String key) {
                // Método no utilizado en esta implementación
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // Método no utilizado en esta implementación
            }

            @Override
            public void onGeoQueryReady() {
                // Método llamado cuando la búsqueda de conductores en el radio especificado ha finalizado
                if (!mDriverFound) {
                    // Se incrementa el radio para seguir buscando conductores
                    mRadius = mRadius + 0.1f;
                    // Si el radio excede un valor máximo y no se ha encontrado ningún conductor, se muestra un mensaje de error
                    if (mRadius > 5) {
                        mTextViewLookingFor.setText("NO SE ENCONTRO UN CONDUCTOR");
                        Toast.makeText(RequestDriverActivity.this, "NO SE ENCONTRO UN CONDUCTOR", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        // Si aún hay espacio para aumentar el radio, se realiza una nueva búsqueda
                        getClosestDriver();
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                // Método llamado en caso de error en la consulta geográfica
            }
        });
    }


    private void createClientBooking() {
        // Se obtiene la dirección y la duración estimada del viaje entre el origen y el conductor encontrado
        mGoogleApiProvider.getDirections(mOriginLatLng, mDriverFoundLatLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {
                    // Se procesa la respuesta JSON obtenida de la solicitud a la API de Google Directions
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polylines = route.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    // Se obtienen los textos de distancia y duración del viaje
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");
                    // Se envía una notificación al conductor con la duración y distancia estimada del viaje
                    sendNotification(durationText, distanceText);
                } catch(Exception e) {
                    Log.d("Error", "Error encontrado " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Método llamado en caso de falla en la solicitud a la API de Google Directions
            }
        });
    }


    private void sendNotificationCancel() {
        // Se obtiene el token de sesión del conductor al que se le canceló la solicitud
        mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Si el conductor tiene un token de sesión registrado en la base de datos
                    String token = dataSnapshot.child("token").getValue().toString();
                    // Se crea un mapa con los datos de la notificación
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "VIAJE CANCELADO");
                    map.put("body", "El cliente canceló la solicitud");
                    // Se crea un objeto FCMBody con el token del conductor y el mapa de datos
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                    // Se envía la notificación al conductor
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null) {
                                // Si la notificación se envía correctamente
                                if (response.body().getSuccess() == 1) {
                                    // Se muestra un mensaje de éxito y se redirige al usuario a la actividad del mapa
                                    Toast.makeText(RequestDriverActivity.this, "La solicitud se canceló correctamente", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Si la notificación no se pudo enviar, se muestra un mensaje de error
                                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificación", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Si la respuesta del servidor es nula, se muestra un mensaje de error
                                Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificación", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            // En caso de error en la solicitud, se muestra un mensaje de error
                            Log.d("Error", "Error " + t.getMessage());
                        }
                    });
                } else {
                    // Si el conductor no tiene un token de sesión registrado en la base de datos, se muestra un mensaje de error
                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificación porque el conductor no tiene un token de sesión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Método llamado si la operación de lectura de la base de datos se cancela
            }
        });
    }


    private void sendNotification(final String time, final String km) {
        // Se obtiene el token de sesión del conductor al que se le enviará la notificación
        mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Si el conductor tiene un token de sesión registrado en la base de datos
                    String token = dataSnapshot.child("token").getValue().toString();
                    // Se crea un mapa con los datos de la notificación
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "SOLICITUD DE SERVICIO A " + time + " DE TU POSICION");
                    map.put("body", "Un cliente está solicitando un servicio a una distancia de " + km + "\n" +
                            "Recoger en: " + mExtraOrigin + "\n" +
                            "Destino: " + mExtraDestination
                    );
                    map.put("idClient", mAuthProvider.getId());
                    map.put("origin", mExtraOrigin);
                    map.put("destination", mExtraDestination);
                    map.put("min", time);
                    map.put("distance", km);
                    // Se crea un objeto FCMBody con el token del conductor y el mapa de datos
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                    // Se envía la notificación al conductor
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null) {
                                // Si la notificación se envía correctamente
                                if (response.body().getSuccess() == 1) {
                                    // Se crea un objeto ClientBooking con los datos del servicio solicitado por el cliente
                                    ClientBooking clientBooking = new ClientBooking(
                                            mAuthProvider.getId(),
                                            mIdDriverFound,
                                            mExtraDestination,
                                            mExtraOrigin,
                                            time,
                                            km,
                                            "create",
                                            mExtraOriginLat,
                                            mExtraOriginLng,
                                            mExtraDestinationLat,
                                            mExtraDestinationLng
                                    );
                                    // Se crea el registro de solicitud de servicio en la base de datos
                                    mClientBookingProvider.create(clientBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Se verifica el estado del registro de solicitud de servicio
                                            checkStatusClientBooking();
                                        }
                                    });
                                } else {
                                    // Si la notificación no se pudo enviar, se muestra un mensaje de error
                                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificación", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Si la respuesta del servidor es nula, se muestra un mensaje de error
                                Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificación", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            // En caso de error en la solicitud, se muestra un mensaje de error
                            Log.d("Error", "Error " + t.getMessage());
                        }
                    });
                } else {
                    // Si el conductor no tiene un token de sesión registrado en la base de datos, se muestra un mensaje de error
                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificación porque el conductor no tiene un token de sesión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Método llamado si la operación de lectura de la base de datos se cancela
            }
        });
    }


    private void checkStatusClientBooking() {
        // Se establece un listener para verificar el estado del registro de solicitud de servicio del cliente
        mListener = mClientBookingProvider.getStatus(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Si existe el registro en la base de datos
                    String status = dataSnapshot.getValue().toString();
                    // Si el estado es "accept" (aceptado por el conductor)
                    if (status.equals("accept")) {
                        // Se inicia la actividad para el seguimiento del servicio
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientBookingActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    // Si el estado es "cancel" (cancelado por el conductor)
                    else if (status.equals("cancel")) {
                        // Se muestra un mensaje indicando que el conductor no aceptó el viaje
                        Toast.makeText(RequestDriverActivity.this, "El conductor no aceptó el viaje", Toast.LENGTH_SHORT).show();
                        // Se vuelve a la actividad principal del cliente
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Método llamado si la operación de lectura de la base de datos se cancela
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Se remueve el listener cuando la actividad es destruida para evitar fugas de memoria
        if (mListener != null) {
            mClientBookingProvider.getStatus(mAuthProvider.getId()).removeEventListener(mListener);
        }
    }

}
