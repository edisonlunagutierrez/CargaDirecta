package com.optic.uberclone.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.DetailRequestActivity;
import com.optic.uberclone.activities.client.RequestDriverActivity;
import com.optic.uberclone.models.ClientBooking;
import com.optic.uberclone.models.FCMBody;
import com.optic.uberclone.models.FCMResponse;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientBookingProvider;
import com.optic.uberclone.providers.ClientProvider;
import com.optic.uberclone.providers.GeofireProvider;
import com.optic.uberclone.providers.GoogleApiProvider;
import com.optic.uberclone.providers.NotificationProvider;
import com.optic.uberclone.providers.TokenProvider;
import com.optic.uberclone.utils.DecodePoints;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapDriverBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Declaraciones de variables miembro de la actividad MapDriverActivity
    private GoogleMap mMap; // Instancia de GoogleMap para interactuar con el mapa
    private SupportMapFragment mMapFragment; // Fragmento de mapa de soporte
    private AuthProvider mAuthProvider; // Proveedor de autenticación
    private GeofireProvider mGeofireProvider; // Proveedor de Geofire para interactuar con la base de datos de Firebase
    private ClientProvider mClientProvider; // Proveedor de cliente para interactuar con la información del cliente
    private ClientBookingProvider mClientBookingProvider; // Proveedor de reserva de cliente para interactuar con la información de la reserva del cliente
    private TokenProvider mTokenProvider; // Proveedor de token para generar y gestionar tokens de autenticación
    private NotificationProvider mNotificationProvider; // Proveedor de notificaciones para enviar notificaciones al conductor

    private LocationRequest mLocationRequest; // Solicitud de ubicación para configurar el servicio de ubicación
    private FusedLocationProviderClient mFusedLocation; // Cliente de ubicación fusionada para acceder a los servicios de ubicación de Google Play

    private final static int LOCATION_REQUEST_CODE = 1; // Código de solicitud de permiso de ubicación
    private final static int SETTINGS_REQUEST_CODE = 2; // Código de solicitud para abrir la configuración de ubicación

    private Marker mMarker; // Marcador en el mapa que representa la posición del conductor
    private LatLng mCurrentLatLng; // Última posición conocida del conductor

    private TextView mTextViewClientBooking; // TextView para mostrar el nombre del cliente
    private TextView mTextViewEmailClientBooking; // TextView para mostrar el correo electrónico del cliente
    private TextView mTextViewOriginClientBooking; // TextView para mostrar el origen de la reserva del cliente
    private TextView mTextViewDestinationClientBooking; // TextView para mostrar el destino de la reserva del cliente
    private ImageView mImageViewBooking; // ImageView para mostrar la imagen de la reserva del cliente

    private String mExtraClientId; // ID adicional del cliente para la reserva

    private LatLng mOriginLatLng; // Latitud y longitud de origen de la reserva del cliente
    private LatLng mDestinationLatLng; // Latitud y longitud de destino de la reserva del cliente

    private GoogleApiProvider mGoogleApiProvider; // Proveedor de API de Google para obtener direcciones y rutas
    private List<LatLng> mPolylineList; // Lista de puntos de polilínea para trazar la ruta en el mapa
    private PolylineOptions mPolylineOptions; // Opciones de polilínea para personalizar la apariencia de la ruta

    private boolean mIsFirstTime = true; // Bandera para indicar si es la primera vez que se ejecuta la actividad
    private boolean mIsCloseToClient = false; // Bandera para indicar si el conductor está cerca del cliente

    private Button mButtonStartBooking; // Botón para iniciar la reserva del cliente
    private Button mButtonFinishBooking; // Botón para finalizar la reserva del cliente

    // Callback de ubicación para recibir actualizaciones de ubicación del cliente
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    // Actualiza la posición actual del conductor
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Elimina el marcador anterior y añade uno nuevo en la posición actual del conductor
                    if (mMarker != null) {
                        mMarker.remove();
                    }
                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude()))
                            .title("Tu posición")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)));

                    // Mueve la cámara a la posición actual del conductor
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(16f)
                                    .build()));

                    // Actualiza la ubicación del conductor en la base de datos
                    updateLocation();

                    // Verifica si es la primera vez que se ejecuta la actividad y obtiene la reserva del cliente
                    if (mIsFirstTime) {
                        mIsFirstTime = false;
                        getClientBooking();
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver_booking);

        // Inicialización de proveedores y clientes necesarios
        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("drivers_working");
        mTokenProvider = new TokenProvider();
        mClientProvider = new ClientProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mNotificationProvider = new NotificationProvider();

        // Obtención de la instancia del cliente de ubicación fusionada
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);

        // Obtención del fragmento de mapa y configuración del mapa de manera asincrónica
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        // Vinculación de vistas del diseño con variables en Java
        mTextViewClientBooking = findViewById(R.id.textViewClientBooking);
        mTextViewEmailClientBooking = findViewById(R.id.textViewEmailClientBooking);
        mTextViewOriginClientBooking = findViewById(R.id.textViewOriginClientBooking);
        mTextViewDestinationClientBooking = findViewById(R.id.textViewDestinationClientBooking);
        mButtonStartBooking = findViewById(R.id.btnStartBooking);
        mButtonFinishBooking = findViewById(R.id.btnFinishBooking);
        mImageViewBooking = findViewById(R.id.imageViewClientBooking);

        // Obtención del ID del cliente de la reserva
        mExtraClientId = getIntent().getStringExtra("idClient");

        // Inicialización del proveedor de la API de Google
        mGoogleApiProvider = new GoogleApiProvider(MapDriverBookingActivity.this);

        // Obtención de información del cliente
        getClient();

        // Configuración de listeners para los botones de inicio y finalización de la reserva
        mButtonStartBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Verificación de si el conductor está lo suficientemente cerca del cliente para iniciar la reserva
                if (mIsCloseToClient) {
                    startBooking(); // Método para iniciar la reserva
                } else {
                    // Notificación si el conductor no está lo suficientemente cerca del cliente
                    Toast.makeText(MapDriverBookingActivity.this, "Debes estar más cerca a la posición de recogida", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mButtonFinishBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishBooking(); // Método para finalizar la reserva
            }
        });
    }


    private void finishBooking() {
        // Actualización del estado de la reserva como "finalizada" en la base de datos
        mClientBookingProvider.updateStatus(mExtraClientId, "finish");
        // Actualización del ID de la reserva en el historial de reservas
        mClientBookingProvider.updateIdHistoryBooking(mExtraClientId);
        // Envío de una notificación al cliente indicando que el viaje ha finalizado
        sendNotification("Viaje finalizado");
        // Detención de las actualizaciones de ubicación del conductor
        if (mFusedLocation != null) {
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
        // Eliminación de la ubicación del conductor de la base de datos geoespacial
        mGeofireProvider.removeLocation(mAuthProvider.getId());
        // Redirección a la actividad de calificación del cliente para calificar el viaje
        Intent intent = new Intent(MapDriverBookingActivity.this, CalificationClientActivity.class);
        intent.putExtra("idClient", mExtraClientId);
        startActivity(intent);
        finish(); // Finalización de la actividad actual
    }

    private void startBooking() {
        // Actualización del estado de la reserva como "iniciada" en la base de datos
        mClientBookingProvider.updateStatus(mExtraClientId, "start");
        // Ocultar el botón de inicio de la reserva
        mButtonStartBooking.setVisibility(View.GONE);
        // Mostrar el botón de finalización de la reserva
        mButtonFinishBooking.setVisibility(View.VISIBLE);
        // Borrar cualquier marcador previo en el mapa y agregar un marcador en la ubicación de destino
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_pin_blue)));
        // Dibujar la ruta en el mapa hacia el destino
        drawRoute(mDestinationLatLng);
        // Envío de una notificación al cliente indicando que el viaje ha comenzado
        sendNotification("Viaje iniciado");
    }


    /**
     * Calcula la distancia entre dos ubicaciones geográficas dadas.
     *
     * @param clientLatLng La ubicación del cliente.
     * @param driverLatLng La ubicación del conductor.
     * @return La distancia en metros entre las dos ubicaciones.
     */
    private double getDistanceBetween(LatLng clientLatLng, LatLng driverLatLng) {
        double distance = 0;
        // Crear objetos de ubicación para el cliente y el conductor
        Location clientLocation = new Location("");
        Location driverLocation = new Location("");
        // Establecer las coordenadas de latitud y longitud para cada ubicación
        clientLocation.setLatitude(clientLatLng.latitude);
        clientLocation.setLongitude(clientLatLng.longitude);
        driverLocation.setLatitude(driverLatLng.latitude);
        driverLocation.setLongitude(driverLatLng.longitude);
        // Calcular la distancia entre las dos ubicaciones usando el método distanceTo() de la clase Location
        distance = clientLocation.distanceTo(driverLocation);
        return distance; // Devolver la distancia entre las ubicaciones en metros
    }


    /**
     * Obtiene la información de la reserva del cliente correspondiente y actualiza la interfaz de usuario con los detalles de la reserva.
     */
    private void getClientBooking() {
        mClientBookingProvider.getClientBooking(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener los datos de destino y origen de la reserva del cliente
                    String destination = dataSnapshot.child("destination").getValue().toString();
                    String origin = dataSnapshot.child("origin").getValue().toString();
                    double destinatioLat = Double.parseDouble(dataSnapshot.child("destinationLat").getValue().toString());
                    double destinatioLng = Double.parseDouble(dataSnapshot.child("destinationLng").getValue().toString());

                    double originLat = Double.parseDouble(dataSnapshot.child("originLat").getValue().toString());
                    double originLng = Double.parseDouble(dataSnapshot.child("originLng").getValue().toString());
                    // Crear objetos LatLng para las coordenadas de origen y destino
                    mOriginLatLng = new LatLng(originLat, originLng);
                    mDestinationLatLng = new LatLng(destinatioLat, destinatioLng);
                    // Actualizar la interfaz de usuario con los detalles de origen y destino de la reserva
                    mTextViewOriginClientBooking.setText("Recoger en: " + origin);
                    mTextViewDestinationClientBooking.setText("Destino: " + destination);
                    // Añadir marcador en la ubicación de recogida y trazar la ruta desde la ubicación actual del conductor hasta el punto de recogida
                    mMap.addMarker(new MarkerOptions().position(mOriginLatLng).title("Recoger aquí").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_pin_red)));
                    drawRoute(mOriginLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar la cancelación de la operación si es necesario
            }
        });
    }


    /**
     * Dibuja la ruta en el mapa desde la ubicación actual del conductor hasta el punto de destino proporcionado.
     * @param latLng Coordenadas del punto de destino al que se trazará la ruta.
     */
    private void drawRoute(LatLng latLng) {
        // Solicitar la dirección a la API de Google Maps para obtener la ruta desde la ubicación actual hasta el destino
        mGoogleApiProvider.getDirections(mCurrentLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {
                    // Procesar la respuesta JSON de la solicitud de dirección
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polylines = route.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    // Decodificar los puntos de la polilínea para obtener la lista de coordenadas de la ruta
                    mPolylineList = DecodePoints.decodePoly(points);
                    // Configurar las opciones de la polilínea
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(Color.GREEN);
                    mPolylineOptions.width(13f);
                    mPolylineOptions.startCap(new SquareCap());
                    mPolylineOptions.jointType(JointType.ROUND);
                    mPolylineOptions.addAll(mPolylineList);
                    // Dibujar la polilínea en el mapa
                    mMap.addPolyline(mPolylineOptions);

                    // Obtener la distancia y la duración del recorrido
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");

                    // (Opcional) Puedes usar distanceText y durationText según tus necesidades

                } catch (Exception e) {
                    Log.d("Error", "Error encontrado " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Manejar el fallo en la solicitud de dirección si es necesario
            }
        });
    }


    /**
     * Obtiene los detalles del cliente asociado al viaje en curso y los muestra en la interfaz de usuario.
     */
    private void getClient() {
        // Obtener los detalles del cliente desde la base de datos
        mClientProvider.getClient(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener el correo electrónico y el nombre del cliente
                    String email = dataSnapshot.child("email").getValue().toString();
                    String name = dataSnapshot.child("name").getValue().toString();
                    String image = "";
                    // Verificar si el cliente tiene una imagen de perfil
                    if (dataSnapshot.hasChild("image")) {
                        // Obtener la URL de la imagen de perfil y cargarla en el ImageView utilizando Picasso
                        image = dataSnapshot.child("image").getValue().toString();
                        Picasso.with(MapDriverBookingActivity.this).load(image).into(mImageViewBooking);
                    }
                    // Establecer el nombre y el correo electrónico del cliente en los TextView correspondientes
                    mTextViewClientBooking.setText(name);
                    mTextViewEmailClientBooking.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar cualquier error en la lectura de datos si es necesario
            }
        });
    }


    /**
     * Actualiza la ubicación del conductor en la base de datos en tiempo real y verifica si el conductor está cerca del punto de recogida del cliente.
     * Si el conductor está cerca del punto de recogida, se habilita el botón para iniciar el viaje.
     */
    private void updateLocation() {
        // Verificar si hay una sesión de usuario activa y si se ha obtenido la ubicación actual del conductor
        if (mAuthProvider.existSession() && mCurrentLatLng != null) {
            // Guardar la ubicación actual del conductor en la base de datos
            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);
            // Verificar si el conductor ya está cerca del punto de recogida del cliente
            if (!mIsCloseToClient) {
                // Verificar la distancia entre la ubicación actual del conductor y el punto de recogida del cliente
                if (mOriginLatLng != null && mCurrentLatLng != null) {
                    double distance = getDistanceBetween(mOriginLatLng, mCurrentLatLng); // Distancia en metros
                    if (distance <= 200) { // Si la distancia es menor o igual a 200 metros
                        // Habilitar el inicio del viaje y mostrar un mensaje al usuario
                        mIsCloseToClient = true;
                        Toast.makeText(this, "Estás cerca de la posición de recogida", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }


    /**
     * Método invocado cuando el mapa está listo para ser utilizado.
     * Configura el mapa, establece el tipo de mapa y activa los controles de zoom.
     * Si se tienen los permisos necesarios, activa la ubicación del usuario en el mapa y configura la solicitud de ubicación.
     * Llama al método para iniciar la obtención de la ubicación del conductor.
     *
     * @param googleMap Instancia de GoogleMap que representa el mapa.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Establecer la instancia de GoogleMap y configurar el tipo de mapa y controles de zoom
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Verificar los permisos de ubicación del dispositivo
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Si no se tienen los permisos necesarios, se detiene el proceso
        }

        // Activar la visualización de la ubicación del usuario en el mapa
        mMap.setMyLocationEnabled(false);

        // Configurar la solicitud de ubicación con parámetros personalizados
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);

        // Iniciar la obtención de la ubicación del conductor
        startLocation();
    }


    /**
     * Método invocado cuando se recibe una respuesta a una solicitud de permisos.
     * Se verifica si la solicitud corresponde al código de permisos de ubicación.
     * Si se otorga el permiso, se verifica si el GPS está activado y se solicita la ubicación.
     * En caso de no otorgarse el permiso, se vuelve a solicitar.
     *
     * @param requestCode  Código de la solicitud de permisos.
     * @param permissions  Arreglo de permisos solicitados.
     * @param grantResults Resultados de la solicitud de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Verificar si la solicitud corresponde a la solicitud de permisos de ubicación
        if (requestCode == LOCATION_REQUEST_CODE) {
            // Verificar si se otorgaron permisos
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Verificar si el permiso de ubicación está activado
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Verificar si el GPS está activado
                    if (gpsActived()) {
                        // Solicitar actualizaciones de ubicación
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    } else {
                        // Mostrar diálogo de alerta para activar el GPS
                        showAlertDialogNOGPS();
                    }
                } else {
                    // Si no se otorgó el permiso, volver a solicitarlo
                    checkLocationPermissions();
                }
            } else {
                // Si no se otorgó el permiso, volver a solicitarlo
                checkLocationPermissions();
            }
        }
    }


    /**
     * Método invocado cuando se recibe un resultado de una actividad iniciada con startActivityForResult.
     * Se verifica si el resultado corresponde al código de solicitud de configuraciones de ubicación y si el GPS está activado.
     * Si el GPS está activado, se solicitan actualizaciones de ubicación.
     * Si el GPS no está activado, se muestra un diálogo de alerta para activarlo.
     *
     * @param requestCode Código de la solicitud.
     * @param resultCode  Código de resultado devuelto por la actividad.
     * @param data        Datos adicionales opcionales.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Verificar si el resultado corresponde al código de solicitud de configuraciones de ubicación y si el GPS está activado
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActived()) {
            // Verificar si se otorgaron permisos de ubicación
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // Solicitar actualizaciones de ubicación
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } else {
            // Mostrar diálogo de alerta para activar el GPS
            showAlertDialogNOGPS();
        }
    }

    /**
     * Método para mostrar un diálogo de alerta solicitando al usuario activar la ubicación.
     * Se muestra un mensaje y se ofrece la opción de ir a la configuración para activar la ubicación.
     */
    private void showAlertDialogNOGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicacion para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Abrir la configuración de ubicación del dispositivo
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SETTINGS_REQUEST_CODE);
                    }
                }).create().show();
    }


    /**
     * Método para verificar si el GPS está activado en el dispositivo.
     *
     * @return true si el GPS está activado, false en caso contrario.
     */
    private boolean gpsActived() {
        boolean isActive = false;
        // Obtener el servicio de ubicación del sistema
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Verificar si el proveedor de GPS está activado
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isActive = true;
        }
        return isActive;
    }

    /**
     * Método para desconectar al conductor.
     * Se detienen las actualizaciones de ubicación y se remueve la ubicación del conductor de la base de datos en tiempo real.
     * Si no se pueden detener las actualizaciones de ubicación, se muestra un mensaje de error.
     */
    private void disconnect() {
        // Verificar si las actualizaciones de ubicación están activas
        if (mFusedLocation != null) {
            // Detener las actualizaciones de ubicación
            mFusedLocation.removeLocationUpdates(mLocationCallback);
            // Verificar si existe una sesión de autenticación activa
            if (mAuthProvider.existSession()) {
                // Remover la ubicación del conductor de la base de datos en tiempo real
                mGeofireProvider.removeLocation(mAuthProvider.getId());
            }
        } else {
            // Mostrar mensaje de error si no se puede desconectar
            Toast.makeText(this, "No te puedes desconectar", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Método para iniciar la obtención de la ubicación del conductor.
     * Si el SDK de Android es igual o superior a la versión M, se verifica si se tienen los permisos de ubicación.
     * Si los permisos están otorgados y el GPS está activado, se solicitan las actualizaciones de ubicación.
     * Si los permisos no están otorgados, se solicita al usuario que los otorgue.
     * Si el SDK de Android es inferior a la versión M, se verifica si el GPS está activado y se solicitan las actualizaciones de ubicación si está activado.
     * Si el GPS no está activado, se muestra un diálogo para que el usuario lo active.
     */
    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verificar si se tienen los permisos de ubicación
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Verificar si el GPS está activado
                if (gpsActived()) {
                    // Solicitar las actualizaciones de ubicación
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                } else {
                    // Mostrar un diálogo para activar el GPS
                    showAlertDialogNOGPS();
                }
            } else {
                // Solicitar permisos de ubicación al usuario
                checkLocationPermissions();
            }
        } else {
            // Verificar si el GPS está activado
            if (gpsActived()) {
                // Solicitar las actualizaciones de ubicación
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            } else {
                // Mostrar un diálogo para activar el GPS
                showAlertDialogNOGPS();
            }
        }
    }

    /**
     * Método para verificar los permisos de ubicación.
     * Si los permisos no están otorgados, se solicita al usuario que los otorgue.
     * Si el usuario ha denegado previamente los permisos, se muestra un diálogo explicando por qué son necesarios.
     */
    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Mostrar un diálogo explicando por qué se necesitan los permisos de ubicación
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicación requiere de los permisos de ubicación para poder utilizarse")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Solicitar los permisos de ubicación al usuario
                                ActivityCompat.requestPermissions(MapDriverBookingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                // Solicitar los permisos de ubicación al usuario
                ActivityCompat.requestPermissions(MapDriverBookingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }


    /**
     * Método para enviar una notificación al cliente sobre el estado del viaje.
     * Se obtiene el token del cliente para enviar la notificación.
     * Si el cliente tiene un token, se construye el cuerpo de la notificación y se envía.
     * Si no se puede enviar la notificación debido a algún error, se muestra un mensaje de error.
     */
    private void sendNotification(final String status) {
        // Obtener el token del cliente
        mTokenProvider.getToken(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener el token del cliente
                    String token = dataSnapshot.child("token").getValue().toString();
                    // Construir el mapa de datos para la notificación
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "ESTADO DE TU VIAJE");
                    map.put("body", "Tu estado del viaje es: " + status);
                    // Construir el cuerpo de la notificación
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                    // Enviar la notificación
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null) {
                                if (response.body().getSuccess() != 1) {
                                    // Mostrar un mensaje si no se pudo enviar la notificación
                                    Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificación", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Mostrar un mensaje si no se pudo enviar la notificación
                                Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificación", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            // Mostrar un mensaje si ocurre un error al enviar la notificación
                            Log.d("Error", "Error " + t.getMessage());
                        }
                    });
                } else {
                    // Mostrar un mensaje si el conductor no tiene un token de sesión
                    Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificación porque el conductor no tiene un token de sesión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar la cancelación de la operación
            }
        });
    }



}
