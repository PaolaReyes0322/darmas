DELIMITER //

CREATE PROCEDURE sp_crear_donacion (
    IN p_id_donante INT,
    IN p_cantidad DECIMAL(10, 2),
    IN p_descripcion TEXT,
    IN p_id_categoria INT,
    OUT p_resultado VARCHAR(100)
)
BEGIN
    DECLARE exit handler FOR SQLEXCEPTION
    BEGIN
        -- Control de error 
        ROLLBACK;
        SET p_resultado = 'Error: No se pudo registrar la donación.';
    END;

    -- Inicio del control transaccional 
    START TRANSACTION;

    INSERT INTO Donacion (
        fecha_donacion,
        cantidad,
        descripcion,
        estado_donacion,
        id_donante,
        id_categoria
    ) VALUES (
        CURDATE(),
        p_cantidad,
        p_descripcion,
        'pendiente',
        p_id_donante,
        p_id_categoria
    );

    SET p_resultado = 'Donación registrada exitosamente, pendiente de validación.';
    
    COMMIT; -- Confirmar la transacción
END //

DELIMITER ;