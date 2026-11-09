DELIMITER //

CREATE TRIGGER tr_actualizar_stock_donacion
AFTER UPDATE ON Donacion
FOR EACH ROW
BEGIN
    -- Se activa si el estado de la donación cambia de 'pendiente' a 'validado'
    IF OLD.estado_donacion = 'pendiente' AND NEW.estado_donacion = 'validado' THEN
        INSERT INTO Stock (cantidad_disponible, fecha_vencimiento, id_donacion)
        VALUES (
            NEW.cantidad,
            DATE_ADD(CURDATE(), INTERVAL (SELECT fecha_vencimiento_promedio FROM Categoria_Recurso WHERE id_categoria = NEW.id_categoria) DAY),
            NEW.id_donacion
        );
    END IF;
END //

DELIMITER ;