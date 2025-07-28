<?php
header('Content-Type: application/json');
require_once 'connection.php';

if (!isset($_GET['idUser'])) {
    echo json_encode(['success' => false, 'message' => 'Falta parámetro idUser']);
    exit;
}

$idUser = intval($_GET['idUser']);

$sql = "SELECT temperature, humidity, ds18b20_temp, soil_moisture, mq135, 
               ammonia, co2, co, benzene, alcohol, smoke
        FROM readings 
        WHERE idUser = ? 
        ORDER BY id DESC 
        LIMIT 1";

$stmt = $mysqli->prepare($sql);

if (!$stmt) {
    echo json_encode(['success' => false, 'message' => 'Error en la consulta']);
    exit;
}

$stmt->bind_param("i", $idUser);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $reading = $result->fetch_assoc();

    // Convertir a float para evitar errores de parseo en Android
    foreach ($reading as $key => $value) {
        $reading[$key] = floatval($value);
    }

    echo json_encode(['success' => true, 'data' => $reading]);
} else {
    echo json_encode(['success' => false, 'message' => 'No se encontraron datos']);
}

$stmt->close();
$mysqli->close();
?>
