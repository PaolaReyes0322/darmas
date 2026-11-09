DELIMITER //

CREATE TRIGGER tr_limite_solicitudes
BEFORE INSERT ON Solicitud
FOR EACH ROW
BEGIN
    DECLARE solicitudes_activas INT;
    SELECT COUNT(*) INTO solicitudes_activas FROM Solicitud
    WHERE id_beneficiario = NEW.id_beneficiario AND estado_solicitud IN ('pendiente', 'aprobada');
    
    IF solicitudes_activas >= 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un beneficiario no puede tener más de 3 solicitudes activas.';
    END IF;
END //

DELIMITER ;