CREATE VIEW VistaDonacionesPendientes AS
SELECT
    d.id_donacion,
    u.nombre AS donante_nombre,
    u.apellido AS donante_apellido,
    d.fecha_donacion,
    d.descripcion,
    cr.nombre_categoria
FROM Donacion d
JOIN Usuarios u ON d.id_donante = u.id_usuario
JOIN Categoria_Recurso cr ON d.id_categoria = cr.id_categoria
WHERE d.estado_donacion = 'pendiente';