<?php
$hostname = "localhost";
$user     = "root";
$password = "";
$bbdd     = "compostado";

$mysqli = new mysqli($hostname, $user, $password, $bbdd);

/*
if ($mysqli->connect_error) {
    echo "<!DOCTYPE html>
    <html lang=\"es\">
    <head>
      <meta charset=\"UTF-8\">
      <title>Conexión DB</title>
    </head>
    <body>
      <p style=\"color: red; font-weight: bold;\">Conexión fallida: " . $mysqli->connect_error . "</p>
    </body>
    </html>";
    exit;
} else {
    echo "<!DOCTYPE html>
    <html lang=\"es\">
    <head>
      <meta charset=\"UTF-8\">
      <title>Conexión DB</title>
    </head>
    <body>
      <p style=\"color: green; font-weight: bold;\">¡Conexión exitosa a la base de datos '<strong>{$bbdd}</strong>'!</p>
    </body>
    </html>";
}
*/
