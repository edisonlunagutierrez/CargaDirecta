package com.optic.uberclone.utils;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;



/**
 * Clase utilitaria para comprimir imágenes y convertirlas a bytes.
 */
public class CompressorBitmapImage {

    /**
     * Método que comprime una imagen y la convierte en un array de bytes (byte[]).
     *
     * @param ctx    Contexto de la aplicación.
     * @param path   Ruta de la imagen a comprimir.
     * @param width  Ancho máximo deseado para la imagen comprimida.
     * @param height Altura máxima deseada para la imagen comprimida.
     * @return Array de bytes (byte[]) que representa la imagen comprimida.
     */
    public static byte[] getImage(Context ctx, String path, int width, int height) {
        // Obtiene el archivo de la ruta proporcionada
        final File file_thumb_path = new File(path);
        Bitmap thumb_bitmap = null;

        try {
            // Comprime la imagen a un tamaño específico y la convierte a Bitmap
            thumb_bitmap = new Compressor(ctx)
                    .setMaxWidth(width)
                    .setMaxHeight(height)
                    .setQuality(75)
                    .compressToBitmap(file_thumb_path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convierte el Bitmap comprimido a un array de bytes (byte[])
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] thumb_byte = baos.toByteArray();
        return thumb_byte;
    }
}

