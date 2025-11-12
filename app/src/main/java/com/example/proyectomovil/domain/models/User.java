package com.example.proyectomovil.domain.models;

import org.json.JSONObject;

public class User {
    public int id;
    public String name;
    public String firstLastName;
    public String secondLastName;
    public String username;
    public String email;
    public String role;
    public String state;

    public static User fromJson(JSONObject o){
        if (o == null) return null;
        User u = new User();
        u.id = o.optInt("id");
        u.name = o.optString("name");
        u.firstLastName = o.optString("firstLastName");
        u.secondLastName = o.optString("secondLastName");
        u.username = o.optString("username");
        u.email = o.optString("email");
        u.role = o.optString("role");
        u.state = o.optString("state");
        return u;
    }
}
