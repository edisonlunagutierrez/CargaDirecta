package com.optic.uberclone.retrofit;

import com.optic.uberclone.models.FCMBody;
import com.optic.uberclone.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

// Interfaz para definir las solicitudes a la API de Firebase Cloud Messaging (FCM)
public interface IFCMApi {

    // Anotación para especificar los encabezados de la solicitud
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAg6ttj9E:APA91bH-0lpj8Eu7wAh5wtOWLOn8QxADQQfXQHRkfuM-bK2b48lfJ3c3iwPBxOb30x3z7ziqzJIkTQVmciG8Gxhdj_zkekgvPKBadXvY_a1D2rHQ7vqY9aYJ4l3B4uph1mq4AfBMDXEs"
    })
    // Método POST para enviar una notificación utilizando FCM
    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);

}

