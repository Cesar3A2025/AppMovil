package com.example.proyectomovil.domain.models;

import org.json.JSONObject;

public class Reading {
    public int id;
    public int idUser;
    public float temperature;
    public float humidity;
    public int mq135;
    public float ds18b20Temp;
    public int soilMoisture;
    public String airQualityStatus;
    public String date;
    public String time;

    public static Reading fromJson(JSONObject o){
        Reading r = new Reading();
        r.id = o.optInt("id");
        r.idUser = o.optInt("idUser");
        r.temperature = (float)o.optDouble("temperature");
        r.humidity = (float)o.optDouble("humidity");
        r.mq135 = o.optInt("mq135");
        r.ds18b20Temp = (float)o.optDouble("ds18b20_temp");
        r.soilMoisture = o.optInt("soil_moisture");
        r.airQualityStatus = o.optString("air_quality_status");
        r.date = o.optString("date");
        r.time = o.optString("time");
        return r;
    }
}
