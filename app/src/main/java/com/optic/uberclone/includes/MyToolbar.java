package com.optic.uberclone.includes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.optic.uberclone.R;

public class MyToolbar {

    // Método para mostrar una barra de herramientas con título y, opcionalmente, un botón de navegación hacia atrás
    public static void show(AppCompatActivity activity, String title, boolean upButton) {
        // Obtiene una referencia a la barra de herramientas desde el layout de la actividad
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        // Establece la barra de herramientas como la barra de soporte de la actividad
        activity.setSupportActionBar(toolbar);
        // Establece el título de la barra de herramientas con el título proporcionado
        activity.getSupportActionBar().setTitle(title);
        // Configura si se debe mostrar el botón de navegación hacia atrás en la barra de herramientas
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(upButton);
    }


}
