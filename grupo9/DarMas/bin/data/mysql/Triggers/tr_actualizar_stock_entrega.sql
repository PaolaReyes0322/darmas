DELIMITER //

CREATE TRIGGER tr_actualizar_stock_entrega
AFTER INSERT ON Detalle_Entrega
FOR EACH ROW
BEGIN
    UPDATE Stock
    SET cantidad_disponible = cantidad_disponible - NEW.cantidad_entregada
    WHERE id_stock = NEW.id_stock;
END //

DELIMITER ;