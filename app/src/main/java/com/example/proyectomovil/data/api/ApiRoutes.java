package com.example.proyectomovil.data.api;

public final class ApiRoutes {
    private ApiRoutes(){}

    // https://compos.alwaysdata.net/
    // 192.168.0.8
    // http://192.168.0.55/composta_esp33/public/
    public static final String BASE = "https://compos.alwaysdata.net/api";
    public static final String BASE_IMAGES = "https://compos.alwaysdata.net";

    // Users (según UserController.php)
    public static final String LOGIN           = BASE + "/login";
    public static final String USER_GET_BY_ID  = BASE + "/get_user_by_id";
    public static final String USER_UPDATE     = BASE + "/update_user";

    // Readings (según UserController.php)
    public static final String READINGS_LATEST = BASE + "/get_last_reading";
    public static final String READINGS_HIST   = BASE + "/get_historical_readings";

    // Materials (según MaterialController.php)
    public static final String MATERIALS       = BASE + "/materials";

    public static final String EXPORT_READINGS_XLSX = BASE + "/export_readings_xlsx";
    public static final String EXPORT_SALES_XLSX = BASE + "/export_sales_xlsx";

    public static final String DOWNLOAD_SALES_PDF = BASE + "/export_sales_pdf";
    public static final String DOWNLOAD_READINGS_PDF = BASE + "/export_readings_pdf";


    // Alerts (según AlertController.php)
    public static final String ALERTS = BASE + "/get_alerts";
}