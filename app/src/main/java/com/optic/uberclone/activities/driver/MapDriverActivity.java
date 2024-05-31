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
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.MainActivity;
import com.optic.uberclone.activities.client.HistoryBookingClientActivity;
import com.optic.uberclone.activities.client.MapClientActivity;
import com.optic.uberclone.activities.client.UpdateProfileActivity;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.GeofireProvider;
import com.optic.uberclone.providers.TokenProvider;

public class MapDriverActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Declaración de variables miembro
    private GoogleMap mMap; // Representa el mapa de Google
    private SupportMapFragment mMapFragment; // Fragmento que contiene el mapa
    private AuthProvider mAuthProvider; // Proveedor de autenticación
    private GeofireProvider mGeofireProvider; // Proveedor de Geofire para la gestión de ubicaciones
    private TokenProvider mTokenProvider; // Proveedor de tokens

    private LocationRequest mLocationRequest; // Solicitud de ubicación
    private FusedLocationProviderClient mFusedLocation; // Cliente de proveedor de ubicación fusionada

    private final static int LOCATION_REQUEST_CODE = 1; // Código de solicitud de ubicación
    private final static int SETTINGS_REQUEST_CODE = 2; // Código de solicitud de configuración

    private Marker mMarker; // Marcador en el mapa para representar la posición del usuario

    private Button mButtonConnect; // Botón para conectar/desconectar el servicio de ubicación
    private boolean mIsConnect = false; // Estado de conexión al servicio de ubicación

    private LatLng mCurrentLatLng; // Última ubicación conocida del usuario

    private ValueEventListener mListener; // Escuchador de eventos de base de datos en tiempo real

    // Método de devolución de llamada para recibir actualizaciones de ubicación
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    // Almacenar la ubicación actual del usuario
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Eliminar el marcador anterior y agregar uno nuevo en la ubicación actual
                    if (mMarker != null) {
                        mMarker.remove();
                    }
                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude()))
                            .title("Tu posición")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car))
                    );

                    // Centrar el mapa en la nueva ubicación del usuario
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(16f)
                                    .build()
                    ));

                    // Actualizar la ubicación en la base de datos
                    updateLocation();
                    Log.d("ENTRO", "ACTUALIZANDO POSICIÓN");
                }
            }
        }
    };


    // Método llamado cuando se crea la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver); // Establece el diseño de la actividad
        MyToolbar.show(this, "Conductor", false); // Muestra la barra de herramientas personalizada sin botón de retroceso

        // Inicialización de proveedores y clientes
        mAuthProvider = new AuthProvider(); // Proveedor de autenticación
        mGeofireProvider = new GeofireProvider("active_drivers"); // Proveedor Geofire para conductores activos
        mTokenProvider = new TokenProvider(); // Proveedor de tokens de notificación

        mFusedLocation = LocationServices.getFusedLocationProviderClient(this); // Cliente para obtener la ubicación fusionada

        // Obtener referencia al fragmento del mapa y llamar al método asíncrono para obtener el mapa
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        // Configuración del botón de conexión/desconexión y definición del evento onClick
        mButtonConnect = findViewById(R.id.btnConnect);
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsConnect) {
                    disconnect(); // Desconectar el servicio de ubicación
                } else {
                    startLocation(); // Iniciar el servicio de ubicación
                }
            }
        });

        generateToken(); // Generar el token de notificación
        isDriverWorking(); // Verificar si el conductor está trabajando actualmente
    }

    // Método llamado cuando la actividad se destruye
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Si hay un escuchador de eventos activo, se elimina para evitar fugas de memoria
        if (mAuthProvider.existSession()) {
            String userId = mAuthProvider.getId();
            if (userId != null && mListener != null) {
                mGeofireProvider.isDriverWorking(userId).removeEventListener(mListener);
            }
        }
    }



    // Método para verificar si el conductor está trabajando
    private void isDriverWorking() {
        // Establece un oyente de valor en la ubicación del conductor en la base de datos
        String userId = mAuthProvider.getId();
        if (userId != null) {
            mListener = mGeofireProvider.isDriverWorking(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Verifica si existe algún dato en la ubicación del conductor
                    if (dataSnapshot.exists()) {
                        disconnect(); // Desconecta al conductor si está trabajando
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Maneja la cancelación del evento
                }
            });
        }
    }

    // Método para actualizar la ubicación del conductor en la base de datos
    private void updateLocation() {
        // Verifica si existe una sesión de autenticación y si se ha obtenido la ubicación actual
        if (mAuthProvider.existSession() && mCurrentLatLng != null) {
            String userId = mAuthProvider.getId();
            if (userId != null) {
                mGeofireProvider.saveLocation(userId, mCurrentLatLng); // Guarda la ubicación del conductor
            }
        }
    }

    // Método llamado cuando el mapa está listo para ser usado
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // Establece el tipo de mapa
        mMap.getUiSettings().setZoomControlsEnabled(true); // Habilita los controles de zoom

        // Verifica si se tienen permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Sale del método si no se tienen los permisos necesarios
        }
        mMap.setMyLocationEnabled(false); // Deshabilita la capa de ubicación del usuario en el mapa

        // Configuración de la solicitud de ubicación
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // Intervalo de actualización de ubicación en milisegundos
        mLocationRequest.setFastestInterval(1000); // Intervalo más rápido en milisegundos
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Prioridad de la solicitud de ubicación
        mLocationRequest.setSmallestDisplacement(5); // Distancia mínima de desplazamiento en metros antes de que se actualice la ubicación
    }


    // Método llamado cuando se solicitan permisos al usuario
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Verifica si la solicitud de permisos es para la ubicación
        if (requestCode == LOCATION_REQUEST_CODE) {
            // Verifica si se otorgaron permisos de ubicación
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Verifica si los permisos de ubicación están activados
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Verifica si el GPS está activado
                    if (gpsActived()) {
                        // Solicita actualizaciones de ubicación
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    } else {
                        showAlertDialogNOGPS(); // Muestra un diálogo si el GPS está desactivado
                    }
                } else {
                    checkLocationPermissions(); // Verifica los permisos de ubicación
                }
            } else {
                checkLocationPermissions(); // Verifica los permisos de ubicación
            }
        }
    }

    // Método llamado cuando se recibe un resultado de una actividad
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Verifica si la solicitud fue para activar la ubicación y si el GPS está activado
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActived()) {
            // Verifica si se tienen permisos de ubicación
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return; // Sale del método si no se tienen los permisos necesarios
            }
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper()); // Solicita actualizaciones de ubicación
        } else {
            showAlertDialogNOGPS(); // Muestra un diálogo si el GPS está desactivado
        }
    }


    // Método para mostrar un diálogo de alerta cuando el GPS está desactivado
    private void showAlertDialogNOGPS() {
        // Construye el diálogo de alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicacion para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Abre la configuración de ubicación del dispositivo
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SETTINGS_REQUEST_CODE);
                    }
                }).create().show(); // Muestra el diálogo
    }

    // Método para verificar si el GPS está activado
    private boolean gpsActived() {
        boolean isActive = false;
        // Obtiene el servicio de ubicación
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Verifica si el proveedor de GPS está activado
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isActive = true; // Establece que el GPS está activado
        }
        return isActive; // Devuelve el estado del GPS
    }

    // Método para desconectar el dispositivo
    private void disconnect() {
        // Verifica si se tienen las actualizaciones de ubicación
        if (mFusedLocation != null) {
            mButtonConnect.setText("Conectarse"); // Establece el texto del botón como "Conectarse"
            mIsConnect = false; // Establece la conexión como false
            mFusedLocation.removeLocationUpdates(mLocationCallback); // Remueve las actualizaciones de ubicación
            // Verifica si hay una sesión de autenticación
            String userId = mAuthProvider.getId();
            if (userId != null) {
                mGeofireProvider.removeLocation(userId); // Remueve la ubicación del proveedor de Geofire
            }
        } else {
            Toast.makeText(this, "No te puedes desconectar", Toast.LENGTH_SHORT).show(); // Muestra un mensaje si no se puede desconectar
        }
    }


    // Método para iniciar la obtención de la ubicación del dispositivo
    private void startLocation() {
        // Verifica si la versión del SDK es mayor o igual a Marshmallow (23)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verifica si se otorgó el permiso de ubicación
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Verifica si el GPS está activado
                if (gpsActived()) {
                    mButtonConnect.setText("Desconectarse"); // Cambia el texto del botón a "Desconectarse"
                    mIsConnect = true; // Establece la conexión como true
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper()); // Solicita las actualizaciones de ubicación
                } else {
                    showAlertDialogNOGPS(); // Muestra un diálogo de alerta si el GPS está desactivado
                }
            } else {
                checkLocationPermissions(); // Verifica los permisos de ubicación
            }
        } else {
            // Verifica si el GPS está activado
            if (gpsActived()) {
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper()); // Solicita las actualizaciones de ubicación
            } else {
                showAlertDialogNOGPS(); // Muestra un diálogo de alerta si el GPS está desactivado
            }
        }
    }

    // Método para verificar los permisos de ubicación
    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Verifica si se debe mostrar un diálogo explicativo sobre la solicitud de permisos
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Muestra un diálogo explicativo sobre la solicitud de permisos
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicación requiere permisos de ubicación para funcionar correctamente.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Solicita los permisos de ubicación
                                ActivityCompat.requestPermissions(MapDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                // Solicita directamente los permisos de ubicación
                ActivityCompat.requestPermissions(MapDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    // Método para crear el menú de opciones en la actividad
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu, menu); // Infla el menú de opciones del conductor
        return super.onCreateOptionsMenu(menu);
    }

    // Método para manejar las acciones al seleccionar elementos del menú de opciones
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout(); // Cierra sesión del conductor
        }
        if (item.getItemId() == R.id.action_update) {
            // Inicia la actividad para actualizar el perfil del conductor
            Intent intent = new Intent(MapDriverActivity.this, UpdateProfileDriverActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.action_history) {
            // Inicia la actividad para ver el historial de viajes del conductor
            Intent intent = new Intent(MapDriverActivity.this, HistoryBookingDriverActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // Método para cerrar sesión del conductor
    void logout() {
        disconnect(); // Desconecta al conductor
        mAuthProvider.logout(); // Cierra sesión del proveedor de autenticación
        Intent intent = new Intent(MapDriverActivity.this, MainActivity.class); // Crea un intento para ir a la actividad principal
        startActivity(intent); // Inicia la actividad principal
        finish(); // Finaliza la actividad actual
    }

    // Método para generar un token de sesión para el conductor
    void generateToken() {
        String userId = mAuthProvider.getId();
        if (userId != null) {
            mTokenProvider.create(userId); // Genera un token de sesión para el conductor
        }
    }

}
