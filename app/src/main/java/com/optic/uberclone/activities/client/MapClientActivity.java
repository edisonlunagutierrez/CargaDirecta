package com.optic.uberclone.activities.client;

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
import android.location.Address;
import android.location.Geocoder;
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

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DatabaseError;
import com.google.maps.android.SphericalUtil;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.MainActivity;
import com.optic.uberclone.activities.driver.MapDriverActivity;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.GeofireProvider;
import com.optic.uberclone.providers.TokenProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapClientActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Importaciones necesarias
    private GoogleMap mMap;  // Objeto GoogleMap para interactuar con el mapa
    private SupportMapFragment mMapFragment;  // Fragmento de mapa de soporte
    private AuthProvider mAuthProvider;  // Proveedor de autenticación

    private LocationRequest mLocationRequest;  // Solicitud de ubicación
    private FusedLocationProviderClient mFusedLocation;  // Cliente para obtener la ubicación del usuario

    private GeofireProvider mGeofireProvider;  // Proveedor de Geofire para interactuar con la base de datos geoespacial
    private TokenProvider mTokenProvider;  // Proveedor de tokens para la autenticación

    private final static int LOCATION_REQUEST_CODE = 1;  // Código de solicitud de permiso de ubicación
    private final static int SETTINGS_REQUEST_CODE = 2;  // Código de solicitud de activación de GPS

    private Marker mMarker;  // Marcador en el mapa

    private LatLng mCurrentLatLng;  // Ubicación actual del usuario

    private List<Marker> mDriversMarkers = new ArrayList<>();  // Lista de marcadores de conductores activos

    private boolean mIsFirstTime = true;  // Bandera para la primera vez que se ejecuta la aplicación

    private PlacesClient mPlaces;  // Cliente para interactuar con la API de Places
    private AutocompleteSupportFragment mAutocomplete;  // Fragmento de autocompletado para el lugar de origen
    private AutocompleteSupportFragment mAutocompleteDestination;  // Fragmento de autocompletado para el destino

    private String mOrigin;  // Origen de la solicitud de viaje
    private LatLng mOriginLatLng;  // Ubicación del origen

    private String mDestination;  // Destino de la solicitud de viaje
    private LatLng mDestinationLatLng;  // Ubicación del destino

    private GoogleMap.OnCameraIdleListener mCameraListener;  // Listener para la cámara del mapa

    private Button mButtonRequestDriver;  // Botón para solicitar un conductor


    // Callback para manejar la actualización de la ubicación del usuario
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            // Iterar sobre todas las ubicaciones obtenidas en el resultado
            for (Location location : locationResult.getLocations()) {
                // Verificar que el contexto de la aplicación no sea nulo para evitar errores
                if (getApplicationContext() != null) {
                    // Actualizar la ubicación actual del usuario
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Mover la cámara del mapa para mostrar la nueva ubicación del usuario
                    // con un nivel de zoom específico
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(15f)
                                    .build()
                    ));

                    // Si es la primera vez que se obtiene la ubicación del usuario,
                    // realizar acciones adicionales como obtener conductores activos y limitar la búsqueda
                    if (mIsFirstTime) {
                        mIsFirstTime = false;
                        getActiveDrivers();  // Obtener conductores activos cercanos
                        limitSearch();  // Limitar la búsqueda de lugares autocompletados
                    }
                }
            }
        }
    };


    // Método onCreate: Configuración inicial de la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client);  // Establecer el diseño de la actividad

        // Mostrar la barra de herramientas personalizada
        MyToolbar.show(this, "Cliente", false);

        // Inicializar proveedores y clientes necesarios
        mAuthProvider = new AuthProvider();  // Proveedor de autenticación
        mGeofireProvider = new GeofireProvider("active_drivers");  // Proveedor de Geofire
        mTokenProvider = new TokenProvider();  // Proveedor de tokens
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);  // Cliente para obtener la ubicación del usuario

        // Obtener el fragmento de mapa y asignar el método onMapReady para inicializar el mapa
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        // Configurar el botón de solicitud de conductor para que responda al clic del usuario
        mButtonRequestDriver = findViewById(R.id.btnRequestDriver);
        mButtonRequestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestDriver();  // Método para solicitar un conductor
            }
        });

        // Inicializar Places API y configurar fragmentos de autocompletado para el origen y destino
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }
        mPlaces = Places.createClient(this);
        instanceAutocompleteOrigin();  // Método para configurar fragmento de autocompletado para el origen
        instanceAutocompleteDestination();  // Método para configurar fragmento de autocompletado para el destino

        // Configurar el listener de movimiento de la cámara del mapa
        onCameraMove();

        // Generar un token de autenticación
        generateToken();
    }


    // Método para solicitar un conductor
    private void requestDriver() {
        // Verificar si se han seleccionado tanto el origen como el destino
        if (mOriginLatLng != null && mDestinationLatLng != null) {
            // Si se han seleccionado ambos, crear un Intent para iniciar la actividad DetailRequestActivity
            Intent intent = new Intent(MapClientActivity.this, DetailRequestActivity.class);
            // Pasar datos de ubicación y nombre de origen y destino a través de extras en el Intent
            intent.putExtra("origin_lat", mOriginLatLng.latitude);
            intent.putExtra("origin_lng", mOriginLatLng.longitude);
            intent.putExtra("destination_lat", mDestinationLatLng.latitude);
            intent.putExtra("destination_lng", mDestinationLatLng.longitude);
            intent.putExtra("origin", mOrigin);
            intent.putExtra("destination", mDestination);
            // Iniciar la actividad DetailRequestActivity
            startActivity(intent);
        } else {
            // Si no se han seleccionado ambos, mostrar un mensaje de advertencia al usuario
            Toast.makeText(this, "Debe seleccionar el lugar de recogida y el destino", Toast.LENGTH_SHORT).show();
        }
    }


    // Método para limitar la búsqueda de lugares autocompletados basados en la ubicación del usuario
    private void limitSearch() {
        // Calcular coordenadas para el límite norte y sur basadas en la ubicación actual del usuario
        LatLng northSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000, 0);  // 5000 metros hacia el norte
        LatLng southSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000, 180);  // 5000 metros hacia el sur

        // Establecer límite de ubicación para el fragmento de autocompletado del origen
        mAutocomplete.setLocationBias(RectangularBounds.newInstance(southSide, northSide));
        // Establecer límite de ubicación para el fragmento de autocompletado del destino
        mAutocompleteDestination.setLocationBias(RectangularBounds.newInstance(southSide, northSide));
    }


    // Método para manejar el movimiento de la cámara del mapa
    private void onCameraMove() {
        // Definir un listener para detectar cuando la cámara del mapa se detiene
        mCameraListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try {
                    // Crear un objeto Geocoder para obtener información de ubicación a partir de coordenadas geográficas
                    Geocoder geocoder = new Geocoder(MapClientActivity.this);

                    // Obtener la ubicación del centro de la cámara del mapa
                    mOriginLatLng = mMap.getCameraPosition().target;

                    // Obtener información de dirección a partir de las coordenadas geográficas
                    List<Address> addressList = geocoder.getFromLocation(mOriginLatLng.latitude, mOriginLatLng.longitude, 1);

                    // Extraer información de la dirección, como ciudad y país
                    String city = addressList.get(0).getLocality();
                    String country = addressList.get(0).getCountryName();
                    String address = addressList.get(0).getAddressLine(0);

                    // Construir la dirección completa utilizando la dirección, ciudad y país
                    mOrigin = address + " " + city;

                    // Establecer el texto del fragmento de autocompletado de origen con la dirección completa
                    mAutocomplete.setText(address + " " + city);
                } catch (Exception e) {
                    // Manejar cualquier excepción que pueda ocurrir al obtener la información de ubicación
                    Log.d("Error: ", "Mensaje error: " + e.getMessage());
                }
            }
        };
    }


    // Método para configurar el fragmento de autocompletado para el lugar de origen
    private void instanceAutocompleteOrigin() {
        // Obtener el fragmento de autocompletado del diseño de la actividad
        mAutocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteOrigin);

        // Especificar los campos de lugar que se deben devolver con las sugerencias de autocompletado
        mAutocomplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        // Establecer un mensaje de sugerencia para el fragmento de autocompletado
        mAutocomplete.setHint("Lugar de recogida");

        // Establecer un listener para detectar cuando se selecciona un lugar de la lista de sugerencias
        mAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // Obtener el nombre del lugar seleccionado y sus coordenadas geográficas
                mOrigin = place.getName();
                mOriginLatLng = place.getLatLng();

                // Registrar información del lugar seleccionado en el registro de depuración
                Log.d("PLACE", "Name: " + mOrigin);
                Log.d("PLACE", "Lat: " + mOriginLatLng.latitude);
                Log.d("PLACE", "Lng: " + mOriginLatLng.longitude);
            }

            @Override
            public void onError(@NonNull Status status) {
                // Manejar cualquier error que pueda ocurrir durante la selección del lugar
            }
        });
    }


    // Método para configurar el fragmento de autocompletado para el destino
    private void instanceAutocompleteDestination() {
        // Obtener el fragmento de autocompletado del destino del diseño de la actividad
        mAutocompleteDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteDestination);

        // Especificar los campos de lugar que se deben devolver con las sugerencias de autocompletado
        mAutocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        // Establecer un mensaje de sugerencia para el fragmento de autocompletado de destino
        mAutocompleteDestination.setHint("Destino");

        // Establecer un listener para detectar cuando se selecciona un lugar de la lista de sugerencias
        mAutocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // Obtener el nombre del lugar seleccionado y sus coordenadas geográficas
                mDestination = place.getName();
                mDestinationLatLng = place.getLatLng();

                // Registrar información del lugar seleccionado en el registro de depuración
                Log.d("PLACE", "Name: " + mDestination);
                Log.d("PLACE", "Lat: " + mDestinationLatLng.latitude);
                Log.d("PLACE", "Lng: " + mDestinationLatLng.longitude);
            }

            @Override
            public void onError(@NonNull Status status) {
                // Manejar cualquier error que pueda ocurrir durante la selección del lugar
            }
        });
    }


    // Método para obtener conductores activos y mostrar sus ubicaciones en el mapa
    private void getActiveDrivers() {
        // Obtener conductores activos dentro de un radio específico alrededor de la ubicación del usuario
        mGeofireProvider.getActiveDrivers(mCurrentLatLng, 10).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Cuando un conductor entra en el área de búsqueda:
                // Verificar si el marcador del conductor ya está en el mapa
                for (Marker marker : mDriversMarkers) {
                    if (marker.getTag() != null) {
                        if (marker.getTag().equals(key)) {
                            return; // Si el marcador ya está en el mapa, no hacer nada
                        }
                    }
                }

                // Si el marcador del conductor no está en el mapa, crear y agregar un nuevo marcador en su ubicación
                LatLng driverLatLng = new LatLng(location.latitude, location.longitude);
                Marker marker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Conductor disponible").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)));
                marker.setTag(key);
                mDriversMarkers.add(marker); // Agregar el marcador a la lista de marcadores de conductores
            }

            @Override
            public void onKeyExited(String key) {
                // Cuando un conductor sale del área de búsqueda:
                // Eliminar el marcador correspondiente del mapa y de la lista de marcadores de conductores
                for (Marker marker : mDriversMarkers) {
                    if (marker.getTag() != null) {
                        if (marker.getTag().equals(key)) {
                            marker.remove();
                            mDriversMarkers.remove(marker);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // Cuando la ubicación de un conductor cambia:
                // Actualizar la posición del marcador correspondiente en el mapa
                for (Marker marker : mDriversMarkers) {
                    if (marker.getTag() != null) {
                        if (marker.getTag().equals(key)) {
                            marker.setPosition(new LatLng(location.latitude, location.longitude));
                        }
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {
                // Método invocado cuando la consulta geográfica está lista (no se utiliza en este contexto)
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                // Manejar cualquier error que pueda ocurrir durante la consulta geográfica (no se utiliza en este contexto)
            }
        });
    }


    // Método invocado cuando el mapa está listo para ser utilizado
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Asignar el mapa proporcionado por el objeto googleMap a la variable mMap
        mMap = googleMap;

        // Establecer el tipo de mapa a Normal
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Habilitar los controles de zoom en el mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Establecer un listener para detectar cuando la cámara del mapa se ha detenido después de un movimiento
        mMap.setOnCameraIdleListener(mCameraListener);

        // Configurar la solicitud de ubicación del usuario
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // Intervalo de actualización de ubicación en milisegundos
        mLocationRequest.setFastestInterval(1000); // Intervalo más rápido para actualizar la ubicación en milisegundos
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Prioridad alta para obtener una ubicación precisa
        mLocationRequest.setSmallestDisplacement(5); // Mínimo desplazamiento en metros para que se considere una nueva ubicación

        // Iniciar la obtención de la ubicación del usuario
        startLocation();
    }


    // Método invocado cuando se solicita permiso al usuario para acceder a recursos protegidos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Verificar si la solicitud de permiso corresponde a la solicitud de ubicación
        if (requestCode == LOCATION_REQUEST_CODE) {
            // Verificar si se otorgaron permisos
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Verificar si se otorgó permiso de ubicación fina
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Verificar si el GPS está activado
                    if (gpsActived()) {
                        // Solicitar actualizaciones de ubicación y habilitar la capa de ubicación en el mapa
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    } else {
                        // Mostrar un diálogo de alerta indicando que el GPS está desactivado
                        showAlertDialogNOGPS();
                    }
                } else {
                    // Si no se otorgó el permiso de ubicación fina, volver a verificar los permisos
                    checkLocationPermissions();
                }
            } else {
                // Si los permisos no fueron otorgados, volver a verificar los permisos
                checkLocationPermissions();
            }
        }
    }


    // Método invocado cuando se recibe un resultado de otra actividad
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Verificar si el resultado corresponde al código de solicitud de ajustes y si el GPS está activado
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActived()) {
            // Verificar si se tiene el permiso necesario para acceder a la ubicación
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return; // Si no se tiene el permiso necesario, salir del método
            }
            // Solicitar actualizaciones de ubicación y habilitar la capa de ubicación en el mapa
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
        // Si el resultado corresponde al código de solicitud de ajustes y el GPS no está activado
        else if (requestCode == SETTINGS_REQUEST_CODE && !gpsActived()){
            // Mostrar un diálogo de alerta indicando que el GPS está desactivado
            showAlertDialogNOGPS();
        }
    }


    // Método para mostrar un diálogo de alerta cuando el GPS está desactivado
    private void showAlertDialogNOGPS() {
        // Crear un constructor de diálogo de alerta con el contexto de esta actividad
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Establecer el mensaje del diálogo
        builder.setMessage("Por favor activa tu ubicacion para continuar")

                // Establecer el botón positivo del diálogo para abrir la configuración de ubicación del dispositivo
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Abrir la actividad de configuración de ubicación del dispositivo para que el usuario pueda activar el GPS
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SETTINGS_REQUEST_CODE);
                    }
                })
                .create() // Crear el diálogo
                .show(); // Mostrar el diálogo
    }


    // Método para verificar si el GPS está activado en el dispositivo
    private boolean gpsActived() {
        // Inicializar la variable para indicar si el GPS está activado
        boolean isActive = false;
        // Obtener el servicio de ubicación del sistema
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Verificar si el proveedor de ubicación GPS está habilitado en el dispositivo
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Si el proveedor de ubicación GPS está habilitado, asignar true a la variable isActive
            isActive = true;
        }
        // Devolver el estado de activación del GPS
        return isActive;
    }


    // Método para iniciar la obtención de la ubicación del dispositivo
    private void startLocation() {
        // Verificar la versión de Android para manejar los permisos de ubicación de forma adecuada
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verificar si se tienen los permisos de ubicación ACCESS_FINE_LOCATION otorgados
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Verificar si el GPS está activado
                if (gpsActived()) {
                    // Si los permisos de ubicación están otorgados y el GPS está activado, solicitar actualizaciones de ubicación y habilitar la capa de ubicación en el mapa
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
                else {
                    // Si el GPS no está activado, mostrar un diálogo de alerta para solicitar su activación
                    showAlertDialogNOGPS();
                }
            }
            else {
                // Si los permisos de ubicación no están otorgados, solicitarlos al usuario
                checkLocationPermissions();
            }
        } else {
            // Para versiones anteriores a Android Marshmallow, iniciar la obtención de ubicación sin verificar permisos
            if (gpsActived()) {
                // Verificar si el GPS está activado y solicitar actualizaciones de ubicación
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                // Habilitar la capa de ubicación en el mapa
                mMap.setMyLocationEnabled(true);
            }
            else {
                // Si el GPS no está activado, mostrar un diálogo de alerta para solicitar su activación
                showAlertDialogNOGPS();
            }
        }
    }


    // Método para verificar los permisos de ubicación y solicitarlos si no están otorgados
    private void checkLocationPermissions() {
        // Verificar si los permisos de ubicación ACCESS_FINE_LOCATION no están otorgados
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Verificar si se debe mostrar un mensaje de explicación al usuario sobre la necesidad de los permisos de ubicación
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Si se debe mostrar un mensaje de explicación, mostrar un diálogo de alerta con un mensaje y un botón para solicitar los permisos
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicación requiere de los permisos de ubicación para poder utilizarse")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Al hacer clic en el botón OK, solicitar los permisos de ubicación
                                ActivityCompat.requestPermissions(MapClientActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            }
            else {
                // Si no se debe mostrar un mensaje de explicación, solicitar directamente los permisos de ubicación
                ActivityCompat.requestPermissions(MapClientActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }


    // Método que infla el menú de opciones en la barra de acciones
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú de opciones desde el recurso de menú "client_menu"
        getMenuInflater().inflate(R.menu.client_menu, menu);

        // Devolver el resultado del método onCreateOptionsMenu de la superclase
        return super.onCreateOptionsMenu(menu);
    }


    // Método que maneja los eventos de selección de elementos del menú de opciones
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Verifica el ID del elemento del menú seleccionado
        if (item.getItemId() == R.id.action_logout) {
            // Si se selecciona la opción de cierre de sesión, llama al método logout()
            logout();
        }
        if (item.getItemId() == R.id.action_update) {
            // Si se selecciona la opción de actualizar perfil, crea un intent para abrir la actividad UpdateProfileActivity
            Intent intent = new Intent(MapClientActivity.this, UpdateProfileActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.action_history) {
            // Si se selecciona la opción de historial, crea un intent para abrir la actividad HistoryBookingClientActivity
            Intent intent = new Intent(MapClientActivity.this, HistoryBookingClientActivity.class);
            startActivity(intent);
        }
        // Devuelve el resultado del método onOptionsItemSelected de la superclase
        return super.onOptionsItemSelected(item);
    }

    // Método para cerrar sesión del usuario
    void logout() {
        // Llama al método logout() del proveedor de autenticación
        mAuthProvider.logout();
        // Crea un intent para abrir la actividad MainActivity
        Intent intent = new Intent(MapClientActivity.this, MainActivity.class);
        startActivity(intent);
        // Finaliza la actividad actual
        finish();
    }

    // Método para generar un token de autenticación
    void generateToken() {
        // Llama al método create() del proveedor de tokens, pasando el ID del proveedor de autenticación actual
        mTokenProvider.create(mAuthProvider.getId());
    }

}
