USE darmas;

-- =========================================================
--                 PROCEDIMIENTOS ALMACENADOS
-- =========================================================

-- Cambiar el delimitador temporalmente
DELIMITER //

-- Procedimiento para registrar un nuevo usuario y su dirección de forma segura.
-- Utiliza transacciones para garantizar que si falla una parte, todo se revierta.
-- Este procedimiento ha sido modificado para coincidir con la estructura de tu tabla 'Direcciones'.
DROP PROCEDURE IF EXISTS usp_RegistrarUsuarioConDireccion //

CREATE PROCEDURE usp_RegistrarUsuarioConDireccion(
    IN p_nombre VARCHAR(100),
    IN p_apellido VARCHAR(100),
    IN p_email VARCHAR(255),
    IN p_telefono VARCHAR(20),
    IN p_tipo_usuario ENUM('donante', 'beneficiario', 'voluntario', 'coordinador', 'administrador'),
    IN p_calle VARCHAR(255),
    IN p_ciudad VARCHAR(100),
    IN p_codigo_postal VARCHAR(20)
)
BEGIN
    DECLARE id_direccion_generado INT;
    DECLARE id_usuario_generado INT;

    START TRANSACTION;

    -- Establecer nivel de aislamiento para garantizar que se lean únicamente datos confirmados
    -- y evitar lecturas sucias durante la inserción de usuario y dirección. Este nivel es
    -- suficiente ya que solo realizamos inserciones y no dependemos de datos leídos.
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    BEGIN
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error en el procedimiento usp_RegistrarUsuarioConDireccion';
        END;

        -- 1. Insertamos la dirección y obtenemos su ID
        INSERT INTO Direcciones (calle, ciudad, codigo_postal)
        VALUES (p_calle, p_ciudad, p_codigo_postal);

        SET id_direccion_generado = LAST_INSERT_ID();

        -- 2. Insertamos el usuario, referenciando el ID de la dirección
        INSERT INTO Usuarios (nombre, apellido, email, telefono, id_direccion, tipo_usuario)
        VALUES (p_nombre, p_apellido, p_email, p_telefono, id_direccion_generado, p_tipo_usuario);

        SET id_usuario_generado = LAST_INSERT_ID();

        -- 3. Insertamos el registro en la tabla hija correspondiente
        IF p_tipo_usuario = 'donante' THEN
            INSERT INTO Donantes (id_donante) VALUES (id_usuario_generado);
        ELSEIF p_tipo_usuario = 'beneficiario' THEN
            INSERT INTO Beneficiarios (id_beneficiario) VALUES (id_usuario_generado);
        ELSEIF p_tipo_usuario = 'voluntario' THEN
            INSERT INTO Voluntarios (id_voluntario) VALUES (id_usuario_generado);
        ELSEIF p_tipo_usuario = 'coordinador' THEN
            INSERT INTO Coordinadores (id_coordinador) VALUES (id_usuario_generado);
        ELSEIF p_tipo_usuario = 'administrador' THEN
            INSERT INTO Administradores (id_administrador, cargo, nivel_acceso)
            VALUES (id_usuario_generado, 'Cargo Predeterminado', 'parcial');
        END IF;

        COMMIT;

        SELECT id_usuario_generado AS id_usuario;
    END;
END //

-- Procedimiento para que un Donante registre una nueva donación.
DROP PROCEDURE IF EXISTS usp_RegistrarDonacionRecibida //

CREATE PROCEDURE usp_RegistrarDonacionRecibida(
    IN p_id_donante INT,
    IN p_id_categoria INT,
    IN p_cantidad DECIMAL(10, 2),
    IN p_descripcion TEXT
)
BEGIN
    START TRANSACTION;

    -- Establecemos READ COMMITTED para que las verificaciones de existencia de donante y
    -- categoría no lean datos no confirmados. Al estar insertando un registro, este nivel
    -- garantiza la integridad sin restringir excesivamente la concurrencia.
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

    BEGIN
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error en el procedimiento usp_RegistrarDonacionRecibida';
        END;

        -- Verificar si el donante y la categoría existen
        IF NOT EXISTS (SELECT 1 FROM Donantes WHERE id_donante = p_id_donante) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El ID de donante no existe.';
        END IF;
        
        IF NOT EXISTS (SELECT 1 FROM Categoria_Recurso WHERE id_categoria = p_id_categoria) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El ID de categoría no existe.';
        END IF;

        -- Insertar la donación con estado inicial 'pendiente'
        INSERT INTO Donacion (fecha_donacion, cantidad, descripcion, id_donante, id_categoria)
        VALUES (CURDATE(), p_cantidad, p_descripcion, p_id_donante, p_id_categoria);

        COMMIT;

        SELECT LAST_INSERT_ID() AS id_donacion;
    END;
END //

-- Procedimiento para que un Coordinador valide una donación y la mueva al Stock.
DROP PROCEDURE IF EXISTS usp_ValidarDonacionRecibidaYAgregarStock //

CREATE PROCEDURE usp_ValidarDonacionRecibidaYAgregarStock(
    IN p_id_donacion INT,
    IN p_id_coordinador INT,
    IN p_ubicacion_almacen VARCHAR(255),
    IN p_fecha_vencimiento DATE
)
BEGIN
    START TRANSACTION;

        -- Establecer REPEATABLE READ para evitar lecturas no repetibles al validar la donación.
        -- Este nivel mantiene bloqueadas las filas leídas hasta el final de la transacción,
        -- garantizando consistencia entre la lectura de la donación y su posterior actualización.
        SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;

    BEGIN
        DECLARE cantidad_donada DECIMAL(10, 2);
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            ROLLBACK;
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error en el procedimiento usp_ValidarDonacionRecibidaYAgregarStock';
        END;

        -- Verificar si la donación existe y está pendiente.
        -- Usamos FOR UPDATE para bloquear la fila y evitar que otras transacciones modifiquen la donación
        -- mientras recuperamos la cantidad. Esto previene la pérdida de actualizaciones y lecturas inconsistentes.
        SELECT cantidad INTO cantidad_donada
        FROM Donacion
        WHERE id_donacion = p_id_donacion AND estado_donacion = 'pendiente'
        FOR UPDATE;

        IF cantidad_donada IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Donación no encontrada o no está pendiente.';
        END IF;

        -- Actualizar el estado de la donación a 'validado'
        UPDATE Donacion
        SET
            estado_donacion = 'validado',
            fecha_validacion = CURDATE(),
            id_coordinador_validador = p_id_coordinador
        WHERE id_donacion = p_id_donacion;

        -- Insertar la donación en la tabla de Stock
        INSERT INTO Stock (cantidad_disponible, fecha_vencimiento, ubicacion_almacen, id_donacion)
        VALUES (cantidad_donada, p_fecha_vencimiento, p_ubicacion_almacen, p_id_donacion);

        COMMIT;
        SELECT 'Donación validada y agregada al stock con éxito.' AS resultado;
    END;
END //

-- NUEVO Procedimiento para que el BENEFICIARIO cree una solicitud.
DROP PROCEDURE IF EXISTS usp_CrearSolicitud //
CREATE PROCEDURE usp_CrearSolicitud(
    IN p_id_beneficiario INT,
    IN p_justificacion TEXT,
    IN p_prioridad ENUM('baja', 'media', 'alta'),
    IN p_id_categoria INT,
    IN p_cantidad_solicitada DECIMAL(10, 2)
)
BEGIN
    DECLARE v_solicitud_id INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error al crear la solicitud.';
    END;
    
    START TRANSACTION;
    
    -- Insertar en la tabla Solicitud con estado 'pendiente'
    INSERT INTO Solicitud (fecha_solicitud, estado_solicitud, prioridad, justificacion, id_beneficiario)
    VALUES (CURDATE(), 'pendiente', p_prioridad, p_justificacion, p_id_beneficiario);
    
    SET v_solicitud_id = LAST_INSERT_ID();
    
    -- Insertar en la tabla Detalle_Solicitud
    INSERT INTO Detalle_Solicitud (cantidad_solicitada, id_solicitud, id_categoria)
    VALUES (p_cantidad_solicitada, v_solicitud_id, p_id_categoria);
    
    COMMIT;
    
    SELECT v_solicitud_id AS id_solicitud;
END //


-- NUEVO Procedimiento para que el COORDINADOR valide una solicitud.
DROP PROCEDURE IF EXISTS usp_ValidarSolicitud //
CREATE PROCEDURE usp_ValidarSolicitud(
    IN p_id_solicitud INT,
    IN p_decision ENUM('aprobada', 'rechazada')
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error al validar la solicitud.';
    END;

    START TRANSACTION;

    -- Nivel de aislamiento READ COMMITTED para que la actualización de la solicitud no vea datos
    -- no confirmados de otras transacciones. Es suficiente porque se realiza una única actualización
    -- sobre la solicitud y no se leen otros datos de forma concurrente.
    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    
    -- Actualizar el estado de la solicitud
    UPDATE Solicitud
    SET estado_solicitud = p_decision,
        fecha_aprobacion = IF(p_decision = 'aprobada', CURDATE(), NULL)
    WHERE id_solicitud = p_id_solicitud AND estado_solicitud = 'pendiente';
    
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: La solicitud no existe o no se encuentra en estado pendiente.';
    END IF;
    
    COMMIT;
END //

-- NUEVO Procedimiento para que el VOLUNTARIO registre la entrega.
DROP PROCEDURE IF EXISTS usp_RegistrarDonacionEntregada //
CREATE PROCEDURE usp_RegistrarDonacionEntregada(
    IN p_id_solicitud INT,
    IN p_id_voluntario INT,
    IN p_id_stock INT,
    IN p_cantidad_a_entregar DECIMAL(10, 2)
)
BEGIN
    DECLARE v_entrega_id INT;
    DECLARE v_stock_disponible DECIMAL(10, 2);
    DECLARE v_solicitud_estado ENUM('pendiente', 'aprobada', 'entregada','cancelada');
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error en el procedimiento usp_RegistrarDonacionEntregada.';
    END;

    START TRANSACTION;

    -- Configurar aislamiento SERIALIZABLE para evitar fenómenos de phantom y lecturas no
    -- repetibles durante la asignación del stock a la entrega. Este nivel bloquea rangos de claves
    -- para asegurar que no se inserten o modifiquen filas que alteren el conjunto de datos leído.
    SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
    
    -- 1. Validar que la solicitud exista y esté en estado 'aprobada'.
    -- Aplicamos FOR UPDATE para bloquear la fila de la solicitud y evitar que su estado cambie
    -- mientras se lleva a cabo esta transacción.
    SELECT estado_solicitud INTO v_solicitud_estado
    FROM Solicitud
    WHERE id_solicitud = p_id_solicitud
    FOR UPDATE;

    IF v_solicitud_estado IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El ID de la solicitud no existe.';
    ELSEIF v_solicitud_estado <> 'aprobada' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: La solicitud no está aprobada y no puede ser entregada.';
    END IF;

    -- 2. Validar que el voluntario y el stock existan
    IF NOT EXISTS (SELECT 1 FROM Voluntarios WHERE id_voluntario = p_id_voluntario) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El ID del voluntario no existe.';
    END IF;
    
    -- Obtenemos la cantidad disponible y bloqueamos la fila de stock con FOR UPDATE para
    -- garantizar que nadie más modifique la disponibilidad mientras se procesa la entrega.
    SELECT cantidad_disponible INTO v_stock_disponible
    FROM Stock
    WHERE id_stock = p_id_stock
    FOR UPDATE;
    
    IF v_stock_disponible IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El ID del stock no existe.';
    ELSEIF p_cantidad_a_entregar > v_stock_disponible THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: No hay suficiente stock disponible.';
    END IF;
    
    -- 3. Registrar la entrega y actualizar la solicitud
    INSERT INTO Entrega (fecha_entrega, estado_entrega, id_solicitud, id_voluntario)
    VALUES (CURDATE(), 'en_proceso', p_id_solicitud, p_id_voluntario);
    
    SET v_entrega_id = LAST_INSERT_ID();
    
    UPDATE Solicitud
    SET estado_solicitud = 'entregada'
    WHERE id_solicitud = p_id_solicitud;
    
    -- 4. Registrar el detalle de la entrega y actualizar el stock
    INSERT INTO Detalle_Entrega (cantidad_entregada, id_entrega, id_stock)
    VALUES (p_cantidad_a_entregar, v_entrega_id, p_id_stock);

    UPDATE Stock
    SET cantidad_disponible = cantidad_disponible - p_cantidad_a_entregar
    WHERE id_stock = p_id_stock;

    COMMIT;
    SELECT 'Entrega registrada con éxito.' AS resultado;
END //

-- Disparador que cambia el estado de una Donacion si la cantidad en Stock llega a 0.
DROP TRIGGER IF EXISTS trg_Stock_Bajo_Donacion_Estado //

CREATE TRIGGER trg_Stock_Bajo_Donacion_Estado
AFTER UPDATE ON Stock
FOR EACH ROW
BEGIN
    IF NEW.cantidad_disponible <= 0 AND OLD.cantidad_disponible > 0 THEN
        UPDATE Donacion
        SET estado_donacion = 'agotado'
        WHERE id_donacion = NEW.id_donacion;
    END IF;
END //

-- Cambiar el delimitador temporalmente


-- Procedimiento para simular la creación de un respaldo de la base de datos.
-- En un entorno real, este procedimiento podría invocar un script de shell
-- o realizar una copia de tablas a un esquema de respaldo.
-- Para este ejemplo, simplemente devuelve un mensaje de éxito.
DROP PROCEDURE IF EXISTS usp_CrearRespaldo //

CREATE PROCEDURE usp_CrearRespaldo()
BEGIN
    -- Aquí iría la lógica para crear el respaldo.
    -- Por ejemplo: un comando para exportar la base de datos a un archivo.
    -- En MySQL/MariaDB, esto típicamente se hace fuera de un SP con mysqldump.
    -- Este SP sirve como un punto de invocación desde la aplicación.
    
    -- Simulamos el proceso y devolvemos un mensaje de éxito.
    SELECT 'Respaldo de la base de datos creado correctamente.' AS mensaje;
END //




-- Disparador que verifica que la categoria de un recurso exista antes de insertar una Donacion.
DROP TRIGGER IF EXISTS trg_Donacion_Categoria_Existente //

CREATE TRIGGER trg_Donacion_Categoria_Existente
BEFORE INSERT ON Donacion
FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Categoria_Recurso WHERE id_categoria = NEW.id_categoria) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: No se puede registrar la donación. La categoría de recurso no existe.';
    END IF;
END //

-- Disparador para registrar un evento de auditoría cada vez que se crea una nueva Solicitud.
DROP TRIGGER IF EXISTS trg_Solicitud_Auditoria_Insert //

CREATE TRIGGER trg_Solicitud_Auditoria_Insert
AFTER INSERT ON Solicitud
FOR EACH ROW
BEGIN
    INSERT INTO Auditoria (tipo_evento, descripcion_evento, id_usuario_afectado)
    VALUES (
        'CREATE_SOLICITUD',
        CONCAT('Nueva solicitud creada con ID ', NEW.id_solicitud, ' por el beneficiario ', NEW.id_beneficiario, '.'),
        NEW.id_beneficiario
    );
END //

-- Restablecer el delimitador por defecto
DELIMITER ;
