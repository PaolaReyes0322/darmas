USE darmas;

-- =========================================================
--                     CREACION DE VISTAS
-- =========================================================

-- Cambiar el delimitador temporalmente para permitir la creación de vistas con múltiples sentencias.
DELIMITER //

-- Vista 1: vw_DonacionesPendientes
-- Muestra todas las donaciones que están pendientes de ser validadas.
-- Es ideal para el panel de control del coordinador.
DROP VIEW IF EXISTS vw_DonacionesPendientes //

CREATE VIEW vw_DonacionesPendientes AS
SELECT
    d.id_donacion,
    u.nombre AS nombre_donante,
    u.apellido AS apellido_donante,
    u.email AS email_donante,
    cr.nombre_categoria,
    d.cantidad,
    d.descripcion,
    d.fecha_donacion
FROM Donacion AS d
INNER JOIN Usuarios AS u ON d.id_donante = u.id_usuario
INNER JOIN Categoria_Recurso AS cr ON d.id_categoria = cr.id_categoria
WHERE d.estado_donacion = 'pendiente'//

-- Vista 2: vw_StockActual
-- Proporciona un resumen del stock disponible, agrupado por categoría.
-- Es perfecto para el módulo de control de inventario.
DROP VIEW IF EXISTS vw_StockActual //

CREATE VIEW vw_StockActual AS
SELECT
    cr.nombre_categoria,
    SUM(s.cantidad_disponible) AS cantidad_total_disponible,
    cr.unidad_medida,
    GROUP_CONCAT(DISTINCT s.ubicacion_almacen) AS ubicaciones,
    MIN(s.fecha_vencimiento) AS proximo_vencimiento
FROM Stock AS s
INNER JOIN Donacion AS d ON s.id_donacion = d.id_donacion
INNER JOIN Categoria_Recurso AS cr ON d.id_categoria = cr.id_categoria
WHERE s.estado_producto = 'en_stock'
GROUP BY cr.nombre_categoria, cr.unidad_medida //

-- Vista 3: vw_ActividadVoluntarios
-- Muestra el número total de entregas completadas por cada voluntario.
-- Útil para reportes de desempeño y estadísticas.
DROP VIEW IF EXISTS vw_ActividadVoluntarios //

CREATE VIEW vw_ActividadVoluntarios AS
SELECT
    u.nombre,
    u.apellido,
    u.email,
    COUNT(e.id_entrega) AS total_entregas_completadas
FROM Entrega AS e
INNER JOIN Usuarios AS u ON e.id_voluntario = u.id_usuario
WHERE e.estado_entrega = 'completada'
GROUP BY u.nombre, u.apellido, u.email //

-- Vista 4: vw_ReporteImpactoAnual
-- Resumen del impacto del sistema por año, mostrando la cantidad total donada y entregada.
DROP VIEW IF EXISTS vw_ReporteImpactoAnual //

CREATE VIEW vw_ReporteImpactoAnual AS
SELECT
    YEAR(d.fecha_donacion) AS anio,
    cr.nombre_categoria,
    SUM(d.cantidad) AS cantidad_donada,
    SUM(de.cantidad_entregada) AS cantidad_entregada
FROM Donacion AS d
INNER JOIN Categoria_Recurso AS cr ON d.id_categoria = cr.id_categoria
LEFT JOIN Stock AS s ON d.id_donacion = s.id_donacion
LEFT JOIN Detalle_Entrega AS de ON s.id_stock = de.id_stock
GROUP BY anio, cr.nombre_categoria
ORDER BY anio DESC, cr.nombre_categoria //

-- Restaurar el delimitador por defecto
DELIMITER ;


-- =========================================================
--                CONSULTAS OPTIMIZADAS (EJEMPLOS)
-- =========================================================

-- Consulta 1: Buscar un usuario por email (usando el índice idx_usuarios_email).
-- Esta es la consulta más común para el inicio de sesión.
SELECT id_usuario, nombre, apellido, tipo_usuario, estado
FROM Usuarios
WHERE email = 'ejemplo@correo.com';

-- Consulta 2: Obtener todas las donaciones de un donante específico (usando idx_donacion_donante).
-- Útil para el historial de donaciones en el dashboard del donante.
SELECT id_donacion, fecha_donacion, cantidad, estado_donacion
FROM Donacion
WHERE id_donante = 100; -- Reemplaza 100 con el ID del donante

-- Consulta 3: Obtener todas las solicitudes de un beneficiario por estado.
-- Uso del índice idx_solicitud_beneficiario y idx_solicitud_estado.
SELECT id_solicitud, fecha_solicitud, prioridad
FROM Solicitud
WHERE id_beneficiario = 200 AND estado_solicitud = 'pendiente'; -- Reemplaza 200 con el ID del beneficiario

-- Consulta 4: Encontrar las entregas pendientes de un voluntario específico (usando idx_entrega_voluntario).
SELECT e.id_entrega, s.id_solicitud, s.prioridad, e.fecha_entrega
FROM Entrega AS e
INNER JOIN Solicitud AS s ON e.id_solicitud = s.id_solicitud
WHERE e.id_voluntario = 300 AND e.estado_entrega = 'en_proceso'; -- Reemplaza 300 con el ID del voluntario
