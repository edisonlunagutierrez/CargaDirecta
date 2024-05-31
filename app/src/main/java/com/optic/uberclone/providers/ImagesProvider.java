package com.optic.uberclone.providers;

import android.content.Context;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.optic.uberclone.utils.CompressorBitmapImage;

import java.io.File;

// Clase que proporciona métodos para gestionar imágenes en Firebase Storage
public class ImagesProvider {

    // Referencia al almacenamiento Firebase
    private StorageReference mStorage;

    // Constructor de la clase
    public ImagesProvider(String ref) {
        // Inicializa mStorage con la referencia especificada en Firebase Storage
        mStorage = FirebaseStorage.getInstance().getReference().child(ref);
    }

    // Método para guardar una imagen en Firebase Storage
    public UploadTask saveImage(Context context, File image, String idUser) {
        // Comprime la imagen para reducir su tamaño
        byte[] imageByte = CompressorBitmapImage.getImage(context, image.getPath(), 500, 500);
        // Crea una referencia al archivo de imagen en Firebase Storage
        final StorageReference storage = mStorage.child(idUser + ".jpg");
        // Actualiza mStorage con la referencia del archivo de imagen
        mStorage = storage;
        // Sube la imagen a Firebase Storage y retorna la tarea de carga
        UploadTask uploadTask = storage.putBytes(imageByte);
        return uploadTask;
    }

    // Método para obtener la referencia al almacenamiento Firebase
    public StorageReference getStorage() {
        return mStorage;
    }
}

