DELIMITER //

CREATE PROCEDURE sp_aprobar_solicitud (
    IN p_id_solicitud INT,
    IN p_id_coordinador INT,
    OUT p_resultado VARCHAR(100)
)
BEGIN
    DECLARE v_id_beneficiario INT;
    DECLARE v_estado_solicitud ENUM('pendiente', 'aprobada', 'entregada');
    
    -- Control de error y transacción
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_resultado = 'Error: No se pudo aprobar la solicitud.';
    END;
    
    START TRANSACTION;

    -- Obtener el estado actual de la solicitud
    SELECT estado_solicitud INTO v_estado_solicitud FROM Solicitud WHERE id_solicitud = p_id_solicitud;

    IF v_estado_solicitud = 'pendiente' THEN
        -- Actualizar el estado y la fecha de aprobación de la solicitud
        UPDATE Solicitud
        SET estado_solicitud = 'aprobada',
            fecha_aprobacion = CURDATE()
        WHERE id_solicitud = p_id_solicitud;

        -- Se podría insertar un registro de programación para la entrega aquí
        -- INSERT INTO Programacion (...)

        SET p_resultado = 'Solicitud aprobada con éxito.';
    ELSE
        SET p_resultado = 'La solicitud ya ha sido procesada.';
    END IF;

    COMMIT;
END //

DELIMITER ;