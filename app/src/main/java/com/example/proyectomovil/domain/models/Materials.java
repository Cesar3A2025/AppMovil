package com.example.proyectomovil.domain.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Materials {
    private int id;
    private String name;
    private String image;         // <-- aquí guardamos image_url del backend
    private String description;
    private String clasification;
    private String aptitude;
    private String typeCategory;

    public Materials() { }

    public Materials(int id, String name, String image, String description,
                     String clasification, String aptitude, String typeCategory) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.description = description;
        this.clasification = clasification;
        this.aptitude = aptitude;
        this.typeCategory = typeCategory;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getClasification() { return clasification; }
    public void setClasification(String clasification) { this.clasification = clasification; }

    public String getAptitude() { return aptitude; }
    public void setAptitude(String aptitude) { this.aptitude = aptitude; }

    public String getTypeCategory() { return typeCategory; }
    public void setTypeCategory(String typeCategory) { this.typeCategory = typeCategory; }

    public static List<Materials> parseList(String body) throws JSONException {
        List<Materials> list = new ArrayList<>();

        JSONArray arr;
        try {
            JSONObject obj = new JSONObject(body);
            if (obj.has("data")) {
                arr = obj.getJSONArray("data");
            } else {
                arr = new JSONArray(body);
            }
        } catch (JSONException e) {
            arr = new JSONArray(body);
        }

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            Materials m = new Materials();
            m.setId(o.optInt("id"));
            m.setName(o.optString("name"));
            m.setImage(o.optString("image_url", null)); // <-- CORREGIDO
            m.setDescription(o.optString("description"));
            m.setClasification(o.optString("clasification"));
            m.setAptitude(o.optString("aptitude"));
            m.setTypeCategory(o.optString("type_category"));

            list.add(m);
        }
        return list;
    }
}
