package com.optic.uberclone.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.driver.MapDriverBookingActivity;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.DriverProvider;
import com.optic.uberclone.providers.GeofireProvider;
import com.optic.uberclone.providers.GoogleApiProvider;
import com.optic.uberclone.providers.TokenProvider;
import com.optic.uberclone.utils.DecodePoints;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapClientBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvider mAuthProvider;

    private GeofireProvider mGeofireProvider;
    private TokenProvider mTokenProvider;
    private ClientBookingProvider mClientBookingProvider;
    private DriverProvider mDriverProvider;

    private Marker mMarkerDriver;

    private boolean mIsFirstTime = true;

    private String mOrigin;
    private LatLng mOriginLatLng;

    private String mDestination;
    private LatLng mDestinationLatLng;
    private LatLng mDriverLatLng;


    private TextView mTextViewClientBooking;
    private TextView mTextViewEmailClientBooking;
    private TextView mTextViewOriginClientBooking;
    private TextView mTextViewDestinationClientBooking;
    private TextView mTextViewStatusBooking;
    private ImageView mImageViewBooking;


    private GoogleApiProvider mGoogleApiProvider;
    private List<LatLng> mPolylineList;
    private PolylineOptions mPolylineOptions;

    private ValueEventListener mListener;
    private String mIdDriver;
    private ValueEventListener mListenerStatus;

    // Método llamado cuando se crea la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client_booking);

        // Inicialización de proveedores y variables
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("drivers_working");
        mTokenProvider = new TokenProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mGoogleApiProvider = new GoogleApiProvider(MapClientBookingActivity.this);
        mDriverProvider = new DriverProvider();

        // Obtención de la referencia al fragmento del mapa
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        // Inicialización de Places si no se ha inicializado ya
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }

        // Obtención de referencias a elementos de la interfaz de usuario
        mTextViewClientBooking = findViewById(R.id.textViewDriverBooking);
        mTextViewEmailClientBooking = findViewById(R.id.textViewEmailDriverBooking);
        mTextViewStatusBooking = findViewById(R.id.textViewStatusBooking);
        mTextViewOriginClientBooking = findViewById(R.id.textViewOriginDriverBooking);
        mTextViewDestinationClientBooking = findViewById(R.id.textViewDestinationDriverBooking);
        mImageViewBooking = findViewById(R.id.imageViewClientBooking);

        // Obtener estado y detalles de la reserva del cliente
        getStatus();
        getClientBooking();
    }

    // Método para obtener el estado de la reserva actual
    private void getStatus() {
        // Se agrega un ValueEventListener a la referencia del estado de la reserva del cliente
        mListenerStatus = mClientBookingProvider.getStatus(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String status = dataSnapshot.getValue().toString();
                    // Actualiza la interfaz de usuario según el estado de la reserva
                    if (status.equals("accept")) {
                        mTextViewStatusBooking.setText("Estado: Aceptado");
                    }
                    if (status.equals("start")) {
                        mTextViewStatusBooking.setText("Estado: Viaje Iniciado");
                        startBooking();
                    } else if (status.equals("finish")) {
                        mTextViewStatusBooking.setText("Estado: Viaje Finalizado");
                        finishBooking();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejo de errores en la lectura de datos
            }
        });
    }

    // Método llamado cuando se finaliza la actividad
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Se remueven los listeners de Firebase para evitar fugas de memoria
        if (mListener != null) {
            mGeofireProvider.getDriverLocation(mIdDriver).removeEventListener(mListener);
        }
        if (mListenerStatus != null) {
            mClientBookingProvider.getStatus(mAuthProvider.getId()).removeEventListener(mListenerStatus);
        }
    }

    // Método para finalizar una reserva
    private void finishBooking() {
        // Se crea un intent para abrir la actividad CalificationDriverActivity
        Intent intent = new Intent(MapClientBookingActivity.this, CalificationDriverActivity.class);
        startActivity(intent);
        // Se finaliza la actividad actual
        finish();
    }

    // Método para iniciar una reserva
    private void startBooking() {
        // Limpia el mapa de cualquier marcador previo
        mMap.clear();
        // Agrega un marcador en el destino con un icono específico
        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_pin_blue)));
        // Dibuja la ruta hasta el destino en el mapa
        drawRoute(mDestinationLatLng);
    }


    // Método para obtener los detalles de la reserva del cliente
    private void getClientBooking() {
        // Se agrega un ValueEventListener a la referencia de la reserva del cliente
        mClientBookingProvider.getClientBooking(mAuthProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene los datos de la reserva del cliente desde el snapshot de datos
                    String destination = dataSnapshot.child("destination").getValue().toString();
                    String origin = dataSnapshot.child("origin").getValue().toString();
                    String idDriver = dataSnapshot.child("idDriver").getValue().toString();
                    mIdDriver = idDriver;
                    double destinatioLat = Double.parseDouble(dataSnapshot.child("destinationLat").getValue().toString());
                    double destinatioLng = Double.parseDouble(dataSnapshot.child("destinationLng").getValue().toString());
                    double originLat = Double.parseDouble(dataSnapshot.child("originLat").getValue().toString());
                    double originLng = Double.parseDouble(dataSnapshot.child("originLng").getValue().toString());
                    mOriginLatLng = new LatLng(originLat, originLng);
                    mDestinationLatLng = new LatLng(destinatioLat, destinatioLng);

                    // Actualiza la interfaz de usuario con los detalles de la reserva
                    mTextViewOriginClientBooking.setText("recoger en: " + origin);
                    mTextViewDestinationClientBooking.setText("destino: " + destination);
                    mMap.addMarker(new MarkerOptions().position(mOriginLatLng).title("Recoger aqui").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_pin_red)));

                    // Obtiene los detalles del conductor asociado a la reserva
                    getDriver(idDriver);
                    // Obtiene la ubicación del conductor asociado a la reserva
                    getDriverLocation(idDriver);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejo de errores en la lectura de datos
            }
        });
    }

    // Método para obtener los detalles del conductor asociado a la reserva
    private void getDriver(String idDriver) {
        // Se agrega un ValueEventListener a la referencia del conductor
        mDriverProvider.getDriver(idDriver).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene los datos del conductor desde el snapshot de datos
                    String name = dataSnapshot.child("name").getValue().toString();
                    String email = dataSnapshot.child("email").getValue().toString();
                    String image = "";
                    // Si el conductor tiene una imagen asociada, se carga utilizando Picasso
                    if (dataSnapshot.hasChild("image")) {
                        image = dataSnapshot.child("image").getValue().toString();
                        Picasso.with(MapClientBookingActivity.this).load(image).into(mImageViewBooking);
                    }
                    // Actualiza la interfaz de usuario con los detalles del conductor
                    mTextViewClientBooking.setText(name);
                    mTextViewEmailClientBooking.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejo de errores en la lectura de datos
            }
        });
    }


    // Método para obtener la ubicación del conductor asociado a la reserva
    private void getDriverLocation(String idDriver) {
        // Se agrega un ValueEventListener a la referencia de la ubicación del conductor
        mListener = mGeofireProvider.getDriverLocation(idDriver).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene la latitud y longitud del conductor desde el snapshot de datos
                    double lat = Double.parseDouble(dataSnapshot.child("0").getValue().toString());
                    double lng = Double.parseDouble(dataSnapshot.child("1").getValue().toString());
                    mDriverLatLng = new LatLng(lat, lng);
                    // Si ya hay un marcador del conductor, se elimina
                    if (mMarkerDriver != null) {
                        mMarkerDriver.remove();
                    }
                    // Se agrega un nuevo marcador para la ubicación del conductor en el mapa
                    mMarkerDriver = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title("Tu conductor")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)));
                    // Si es la primera vez que se obtiene la ubicación del conductor, se anima la cámara para centrarse en él y se traza la ruta desde el origen hasta el conductor
                    if (mIsFirstTime) {
                        mIsFirstTime = false;
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder()
                                        .target(mDriverLatLng)
                                        .zoom(15f)
                                        .build()
                        ));
                        drawRoute(mOriginLatLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejo de errores en la lectura de datos
            }
        });
    }

    // Método para dibujar la ruta en el mapa desde la ubicación del conductor hasta el destino del cliente
    private void drawRoute(LatLng latLng) {
        // Se obtiene la dirección utilizando la API de Google Directions
        mGoogleApiProvider.getDirections(mDriverLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {
                    // Se procesa la respuesta JSON para obtener la ruta y los detalles del viaje
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polylines = route.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    // Se decodifican los puntos de la ruta
                    mPolylineList = DecodePoints.decodePoly(points);
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(Color.BLUE); // Cambiando el color a AZUL
                    mPolylineOptions.width(13f);
                    mPolylineOptions.startCap(new SquareCap());
                    mPolylineOptions.jointType(JointType.ROUND);
                    mPolylineOptions.addAll(mPolylineList);
                    // Se añade la polilínea al mapa para mostrar la ruta
                    mMap.addPolyline(mPolylineOptions);

                    // Se obtienen los detalles de distancia y duración del viaje
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");

                    // Estos detalles pueden ser utilizados para mostrar información adicional al usuario, como la distancia y el tiempo estimado del viaje
                } catch (Exception e) {
                    // Manejo de errores en la obtención de la ruta
                    Log.d("Error", "Error encontrado " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Manejo de errores en la solicitud de la ruta
            }
        });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Se asigna el objeto GoogleMap obtenido desde el fragmento al atributo mMap
        mMap = googleMap;
        // Se establece el tipo de mapa como normal
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // Se habilitan los controles de zoom en la interfaz del mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Se verifica si se tienen los permisos necesarios para acceder a la ubicación del usuario
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si no se tienen los permisos, se retorna sin realizar más acciones
            return;
        }
        // Se habilita la capa de ubicación del usuario en el mapa
        mMap.setMyLocationEnabled(true);
    }

}
