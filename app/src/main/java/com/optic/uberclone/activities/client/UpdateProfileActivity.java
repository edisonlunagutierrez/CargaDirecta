package com.optic.uberclone.activities.client;

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
import com.optic.uberclone.includes.MyToolbar;
import com.optic.uberclone.models.Client;
import com.optic.uberclone.providers.AuthProvider;
import com.optic.uberclone.providers.ClientProvider;
import com.optic.uberclone.utils.FileUtil;
import com.optic.uberclone.providers.ImagesProvider;
import com.optic.uberclone.utils.CompressorBitmapImage;
import com.squareup.picasso.Picasso;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdateProfileActivity extends AppCompatActivity {

    private ImageView mImageViewProfile;
    private Button mButtonUpdate;
    private TextView mTextViewName;
    private CircleImageView mCircleImageBack;


    private ClientProvider mClientProvider;
    private AuthProvider mAuthProvider;
    private ImagesProvider mImageProvider;


    private File mImageFile;
    private String mImage;

    private final int GALLERY_REQUEST = 1;
    private ProgressDialog mProgressDialog;
    private String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);
        // Inicialización de la vista y componentes de la actividad para actualizar el perfil del cliente

        // Obtención de referencias a los elementos de la interfaz de usuario
        mImageViewProfile = findViewById(R.id.imageViewProfile);
        mButtonUpdate = findViewById(R.id.btnUpdateProfile);
        mTextViewName = findViewById(R.id.textInputName);
        mCircleImageBack = findViewById(R.id.circleImageBack);
        mProgressDialog = new ProgressDialog(this); // Inicializa el ProgressDialog

        // Inicialización de proveedores de datos
        mClientProvider = new ClientProvider();
        mAuthProvider = new AuthProvider();

        // Obtener el ID del usuario autenticado
        String userId = mAuthProvider.getId();

        // Pasar el ID del usuario al constructor de ImagesProvider
        mImageProvider = new ImagesProvider(userId);

        // Obtener y mostrar la información del cliente
        getClientInfo();

        // Configuración del OnClickListener para el ImageView de perfil (para abrir la galería)
        mImageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery(); // Método para abrir la galería de imágenes
            }
        });

        // Configuración del OnClickListener para el botón de actualización de perfil
        mButtonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateProfile(); // Método para actualizar el perfil del cliente
            }
        });
    }

    // Método para obtener y mostrar la información del cliente
    private void getClientInfo() {
        mClientProvider.getClient(mAuthProvider.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtiene el nombre del cliente desde la base de datos y lo muestra en el TextView correspondiente
                    String name = dataSnapshot.child("name").getValue().toString();
                    mTextViewName.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Método llamado si la operación de lectura de la base de datos se cancela
            }
        });
    }

    // Método para abrir la galería de imágenes
    private void openGallery() {
        // Se crea un intent implícito para abrir la galería de imágenes del dispositivo
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Método llamado cuando se completa una actividad iniciada para obtener una imagen de la galería
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            // Verifica si la solicitud fue para obtener una imagen de la galería y si la operación fue exitosa
            try {
                // Obtiene la ruta del archivo de imagen seleccionado desde la galería y lo asigna al mImageFile
                mImageFile = FileUtil.from(this, data.getData());
                // Muestra la imagen seleccionada en el ImageView de perfil
                mImageViewProfile.setImageBitmap(BitmapFactory.decodeFile(mImageFile.getAbsolutePath()));
            } catch(Exception e) {
                // Manejo de excepciones en caso de error al obtener la imagen
                Log.d("ERROR", "Mensaje: " + e.getMessage());
            }
        }
    }

    // Método para actualizar el perfil del cliente
    private void updateProfile() {
        // Obtiene el nombre ingresado por el usuario
        mName = mTextViewName.getText().toString();
        // Verifica si se ha seleccionado una imagen y se ha ingresado un nombre
        if (!mName.equals("") && mImageFile != null) {
            // Muestra un ProgressDialog mientras se actualiza el perfil
            mProgressDialog.setMessage("Espere un momento...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();

            // Guarda la imagen seleccionada en el almacenamiento y actualiza la información del perfil
            saveImage();
        } else {
            // Muestra un mensaje de advertencia si el usuario no ha seleccionado una imagen o no ha ingresado un nombre
            Toast.makeText(this, "Ingresa la imagen y el nombre", Toast.LENGTH_SHORT).show();
        }
    }


    // Método para guardar la imagen seleccionada en el almacenamiento y actualizar la información del perfil del cliente
    private void saveImage() {
        // Guarda la imagen en el almacenamiento asociado al cliente con la ayuda de la clase ImageProvider
        mImageProvider.saveImage(UpdateProfileActivity.this, mImageFile, mAuthProvider.getId()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                // Verifica si la operación de guardado de la imagen fue exitosa
                if (task.isSuccessful()) {
                    // Obtiene la URL de descarga de la imagen almacenada en el almacenamiento
                    mImageProvider.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Obtiene la URL de la imagen y la convierte a String
                            String image = uri.toString();
                            // Crea un objeto de tipo Cliente con la nueva imagen y el nombre ingresado por el usuario
                            Client client = new Client();
                            client.setImage(image);
                            client.setName(mName);
                            client.setId(mAuthProvider.getId());
                            // Actualiza la información del perfil del cliente en la base de datos
                            mClientProvider.update(client).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // Cierra el ProgressDialog una vez que se ha completado la actualización
                                    mProgressDialog.dismiss();
                                    // Muestra un mensaje indicando que la información del perfil se ha actualizado correctamente
                                    Toast.makeText(UpdateProfileActivity.this, "Su informacion se actualizo correctamente", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else {
                    // Muestra un mensaje de error si no se pudo completar la operación de guardado de la imagen
                    Toast.makeText(UpdateProfileActivity.this, "Hubo un error al subir la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}