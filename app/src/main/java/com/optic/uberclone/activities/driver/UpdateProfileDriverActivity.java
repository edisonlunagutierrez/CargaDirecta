package com.optic.uberclone.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.optic.uberclone.R;
import com.optic.uberclone.activities.client.UpdateProfileActivity;
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.models.Client;
import com.optic.uberclone.models.Driver;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientProvider;
import com.optic.uberclone.providers.DriverProvider;
import com.optic.uberclone.providers.ImagesProvider;
import com.optic.uberclone.utils.CompressorBitmapImage;
import com.squareup.picasso.Picasso;
import com.optic.uberclone.utils.FileUtil;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdateProfileDriverActivity extends AppCompatActivity {

    /**
     * Esta actividad permite al conductor actualizar su perfil.
     * Contiene varios campos de texto y un botón para actualizar la información.
     * También permite al conductor seleccionar una imagen de perfil desde la galería.
     */
    private ImageView mImageViewProfile; // Vista de imagen de perfil
    private Button mButtonUpdate; // Botón para actualizar el perfil
    private TextView mTextViewName; // Campo de texto para el nombre
    private TextView mTextViewBrandVehicle; // Campo de texto para la marca del vehículo
    private TextView mTextViewPlateVehicle; // Campo de texto para la placa del vehículo
    private CircleImageView mCircleImageBack; // Vista de círculo para retroceder

    // Proveedores de datos
    private DriverProvider mDriverProvider; // Proveedor de datos del conductor
    private AuthProvider mAuthProvider; // Proveedor de autenticación
    private ImagesProvider mImageProvider; // Proveedor de imágenes

    private File mImageFile; // Archivo de imagen seleccionado
    private String mImage; // Ruta de la imagen seleccionada

    private final int GALLERY_REQUEST = 1; // Código de solicitud para abrir la galería
    private ProgressDialog mProgressDialog; // Diálogo de progreso
    private String mName; // Nombre del conductor
    private String mVehicleBrand; // Marca del vehículo del conductor
    private String mVehiclePlate; // Placa del vehículo del conductor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile_driver);

        // Inicialización de vistas y proveedores
        mImageViewProfile = findViewById(R.id.imageViewProfile);
        mButtonUpdate = findViewById(R.id.btnUpdateProfile);
        mTextViewName = findViewById(R.id.textInputName);
        mTextViewBrandVehicle = findViewById(R.id.textInputVehicleBrand);
        mTextViewPlateVehicle = findViewById(R.id.textInputVehiclePlate);
        mCircleImageBack = findViewById(R.id.circleImageBack);
        mDriverProvider = new DriverProvider();
        mAuthProvider = new AuthProvider();
        mImageProvider = new ImagesProvider("driver_images");
        mProgressDialog = new ProgressDialog(this);

        // Obtener la información del conductor
        getDriverInfo();

        // Configuración de listeners para las vistas
        mImageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery(); // Abrir la galería cuando se hace clic en la imagen de perfil
            }
        });

        mButtonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateProfile(); // Actualizar el perfil cuando se hace clic en el botón
            }
        });

        mCircleImageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // Finalizar la actividad cuando se hace clic en el botón de retroceso
            }
        });
    }


    /**
     * Abre la galería de imágenes para que el usuario pueda seleccionar una imagen de perfil.
     */
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    /**
     * Se ejecuta cuando se selecciona una imagen de la galería.
     * Actualiza la vista de la imagen de perfil con la imagen seleccionada.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            try {
                // Convierte la URI de la imagen seleccionada en un archivo y lo establece como imagen de perfil
                mImageFile = com.optic.uberclone.utils.FileUtil.from(this, data.getData());
                mImageViewProfile.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath()));
            } catch(Exception e) {
                Log.d("ERROR", "Mensaje: " + e.getMessage());
            }
        }
    }


    /**
     * Obtiene la información del conductor y la muestra en la interfaz de usuario.
     * Si el conductor tiene una imagen de perfil, la carga en el ImageView correspondiente utilizando Picasso.
     */
    private void getDriverInfo() {
        mDriverProvider.getDriver(mAuthProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String vehicleBrand = dataSnapshot.child("vehicleBrand").getValue().toString();
                    String vehiclePlate = dataSnapshot.child("vehiclePlate").getValue().toString();
                    String image = "";
                    if (dataSnapshot.hasChild("image")) {
                        // Si el conductor tiene una imagen de perfil, la carga en el ImageView usando Picasso
                        image = dataSnapshot.child("image").getValue().toString();
                        Picasso.with(UpdateProfileDriverActivity.this).load(image).into(mImageViewProfile);
                    }
                    // Establece el nombre, la marca del vehículo y la placa del vehículo en los TextView correspondientes
                    mTextViewName.setText(name);
                    mTextViewBrandVehicle.setText(vehicleBrand);
                    mTextViewPlateVehicle.setText(vehiclePlate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Actualiza el perfil del conductor.
     * Obtiene el nombre, la marca del vehículo y la placa del vehículo de los campos de texto.
     * Si se proporciona un nombre y se ha seleccionado una imagen de perfil, guarda la imagen y actualiza la información del conductor en la base de datos.
     */
    private void updateProfile() {
        mName = mTextViewName.getText().toString();
        mVehicleBrand = mTextViewBrandVehicle.getText().toString();
        mVehiclePlate = mTextViewPlateVehicle.getText().toString();
        if (!mName.equals("") && mImageFile != null) {
            mProgressDialog.setMessage("Espere un momento...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            // Guarda la imagen del perfil y actualiza la información del conductor
            saveImage();
        }
        else {
            Toast.makeText(this, "Ingresa la imagen y el nombre", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Guarda la imagen seleccionada como imagen de perfil del conductor.
     * Sube la imagen al almacenamiento Firebase Storage y actualiza la información del conductor en la base de datos con la URL de la imagen.
     */
    private void saveImage() {
        mImageProvider.saveImage(UpdateProfileDriverActivity.this, mImageFile, mAuthProvider.getId()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    // Si la subida de la imagen es exitosa, obtiene la URL de la imagen desde Firebase Storage
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String image = uri.toString();
                            // Crea un objeto Driver con la URL de la imagen y la información del conductor
                            Driver driver = new Driver();
                            driver.setImage(image);
                            driver.setName(mName);
                            driver.setId(mAuthProvider.getId());
                            driver.setVehicleBrand(mVehicleBrand);
                            driver.setVehiclePlate(mVehiclePlate);
                            // Actualiza la información del conductor en la base de datos con la nueva imagen y la información
                            mDriverProvider.update(driver).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Cierra el diálogo de progreso y muestra un mensaje de éxito
                                    mProgressDialog.dismiss();
                                    Toast.makeText(UpdateProfileDriverActivity.this, "Su informacion se actualizo correctamente", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
                else {
                    // Si hay un error al subir la imagen, muestra un mensaje de error
                    Toast.makeText(UpdateProfileDriverActivity.this, "Hubo un error al subir la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}
