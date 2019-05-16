package edu.pucmm.sjjwt.utilidades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.ResponseTransformer;

/**
 * Created by vacax on 26/06/17.
 */
public class JsonUtilidades {

    private static Gson gson = new GsonBuilder().serializeNulls().create();

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static ResponseTransformer json() {
        return JsonUtilidades::toJson;
    }
}
