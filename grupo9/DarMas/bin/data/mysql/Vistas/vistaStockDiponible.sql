CREATE VIEW VistaStockDisponible AS
SELECT
    s.id_stock,
    cr.nombre_categoria,
    s.cantidad_disponible,
    s.fecha_vencimiento,
    s.ubicacion_almacen,
    d.descripcion AS donacion_original
FROM Stock s
JOIN Donacion d ON s.id_donacion = d.id_donacion
JOIN Categoria_Recurso cr ON d.id_categoria = cr.id_categoria
WHERE s.cantidad_disponible > 0 AND s.estado_producto = 'en_stock';