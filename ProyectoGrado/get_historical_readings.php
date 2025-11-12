<?php
require_once 'connection.php';
header('Content-Type: application/json');

$idUser = isset($_GET['idUser']) ? intval($_GET['idUser']) : 0;

if ($idUser === 0) {
    echo json_encode(['success' => false, 'message' => 'ID de usuario requerido']);
    exit;
}

// Consulta de los últimos 5 registros
$sql = "SELECT temperature, humidity, ds18b20_temp, soil_moisture, mq135, 
               CONCAT(date, ' ', time) AS datetime
        FROM readings
        WHERE idUser = ?
        ORDER BY id DESC
        LIMIT 5";

$stmt = $mysqli->prepare($sql);
$stmt->bind_param("i", $idUser);
$stmt->execute();
$result = $stmt->get_result();

$data = [];
while ($row = $result->fetch_assoc()) {
    $data[] = [
        'temperature' => floatval($row['temperature']),
        'humidity' => floatval($row['humidity']),
        'ds18b20_temp' => floatval($row['ds18b20_temp']),
        'soil_moisture' => floatval($row['soil_moisture']),
        'mq135' => floatval($row['mq135']),
        'datetime' => $row['datetime']
    ];
}

echo json_encode([
    'success' => true,
    'data' => array_reverse($data) //mostrar cronológicamente
]);

$stmt->close();
$mysqli->close();
?>
