package com.example.proyectomovil.data.api;

public final class ApiRoutes {
    private ApiRoutes(){}

    // Cambia la IP/base si es necesario
    public static final String BASE = "https://compos.alwaysdata.net/api";

    // Users (según UserController.php)
    public static final String LOGIN           = BASE + "/login";
    public static final String USER_GET_BY_ID  = BASE + "/get_user_by_id";
    public static final String USER_UPDATE     = BASE + "/update_user";

    // Readings (según UserController.php)
    public static final String READINGS_LATEST = BASE + "/get_last_reading";
    public static final String READINGS_HIST   = BASE + "/get_historical_readings";

    // Materials (según MaterialController.php)
    public static final String MATERIALS       = BASE + "/materials";
    public static String materialById(int id){ return BASE + "/materials/" + id; }
}