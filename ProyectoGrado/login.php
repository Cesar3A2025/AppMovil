<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
header('Content-Type: application/json');
require_once 'connection.php';

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if (empty($email) || empty($password)) {
    echo json_encode([
        'success' => false,
        'message' => 'Faltan parámetros'
    ]);
    exit;
}

$stmt = $mysqli->prepare("SELECT id, name, email, password FROM users WHERE email = ? LIMIT 1");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Usuario no encontrado'
    ]);
    exit;
}

$user = $result->fetch_assoc();

if (password_verify($password, $user['password'])) {
    echo json_encode([
        'success' => true,
        'id' => $user['id'],
        'name' => $user['name'],
        'email' => $user['email']
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Contraseña incorrecta'
    ]);
}

$stmt->close();
$mysqli->close();
?>
