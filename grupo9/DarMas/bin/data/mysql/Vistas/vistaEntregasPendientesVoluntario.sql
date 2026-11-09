CREATE VIEW VistaEntregasPendientesVoluntario AS
SELECT
    e.id_entrega,
    e.fecha_entrega,
    e.estado_entrega,
    u.nombre AS voluntario_nombre,
    s.justificacion AS motivo_solicitud
FROM Entrega e
JOIN Usuarios u ON e.id_voluntario = u.id_usuario
JOIN Solicitud s ON e.id_solicitud = s.id_solicitud
WHERE e.estado_entrega = 'en_proceso';