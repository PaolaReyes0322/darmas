DELIMITER //

CREATE TRIGGER tr_auditoria_eliminar_usuario
BEFORE DELETE ON Usuarios
FOR EACH ROW
BEGIN
    INSERT INTO Auditoria (tipo_evento, descripcion_evento, id_usuario_afectado)
    VALUES (
        'Eliminacion de Usuario',
        CONCAT('Se ha eliminado el usuario con ID: ', OLD.id_usuario, ' y rol: ', OLD.tipo_usuario),
        OLD.id_usuario
    );
END //

DELIMITER ;