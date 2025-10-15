package com.example.proyectomovil.domain.models;

import org.json.JSONObject;

public class Reading {
    private int id;
    private int idUser;
    private float temperature;
    private float humidity;
    private int mq135;
    private float ds18b20Temp;
    private int soilMoisture;
    private String airQualityStatus;
    private String date;
    private String time;

    public Reading() {}

    // ---------- Getters y Setters ----------
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }

    public float getHumidity() { return humidity; }
    public void setHumidity(float humidity) { this.humidity = humidity; }

    public int getMq135() { return mq135; }
    public void setMq135(int mq135) { this.mq135 = mq135; }

    public float getDs18b20Temp() { return ds18b20Temp; }
    public void setDs18b20Temp(float ds18b20Temp) { this.ds18b20Temp = ds18b20Temp; }

    public int getSoilMoisture() { return soilMoisture; }
    public void setSoilMoisture(int soilMoisture) { this.soilMoisture = soilMoisture; }

    public String getAirQualityStatus() { return airQualityStatus; }
    public void setAirQualityStatus(String airQualityStatus) { this.airQualityStatus = airQualityStatus; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    // ---------- Parseo desde JSON ----------
    public static Reading fromJson(JSONObject o){
        Reading r = new Reading();
        r.setId(o.optInt("id"));
        r.setIdUser(o.optInt("idUser"));
        r.setTemperature((float)o.optDouble("temperature"));
        r.setHumidity((float)o.optDouble("humidity"));
        r.setMq135(o.optInt("mq135"));
        r.setDs18b20Temp((float)o.optDouble("ds18b20_temp"));
        r.setSoilMoisture(o.optInt("soil_moisture"));
        r.setAirQualityStatus(o.optString("air_quality_status"));
        r.setDate(o.optString("date"));
        r.setTime(o.optString("time"));
        return r;
    }
}
