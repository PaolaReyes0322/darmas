USE darmas;

/*
Índice para la columna 'email' en la tabla Usuarios.
Optimiza las búsquedas de usuarios por su correo electrónico, vital para el inicio de sesión.
*/
CREATE INDEX idx_usuarios_email ON Usuarios(email);

/*
Acelera las consultas que buscan donaciones por donante, categoría o coordinador.
*/
CREATE INDEX idx_donacion_donante ON Donacion(id_donante);
CREATE INDEX idx_donacion_categoria ON Donacion(id_categoria);
CREATE INDEX idx_donacion_coordinador ON Donacion(id_coordinador_validador);

/*
Índice para las búsquedas de stock por donación y estado.
Para reportes de inventario y para el disparador que actualiza el estado.
*/
CREATE INDEX idx_stock_donacion_estado ON Stock(id_donacion, estado_producto);

/*
Acelera las consultas que buscan solicitudes por beneficiario o estado.
*/
CREATE INDEX idx_solicitud_beneficiario ON Solicitud(id_beneficiario);
CREATE INDEX idx_solicitud_estado ON Solicitud(estado_solicitud);

/*
Acelera las consultas que relacionan entregas con solicitudes y voluntarios.
*/
CREATE INDEX idx_entrega_solicitud ON Entrega(id_solicitud);
CREATE INDEX idx_entrega_voluntario ON Entrega(id_voluntario);
/*
Índice para búsquedas en la tabla de Programacion.
Acelera las consultas que buscan programaciones por usuario, tipo de actividad o estado.
*/ 
CREATE INDEX idx_programacion_usuario_actividad ON Programacion(id_usuario, tipo_actividad);

/*
Mejora la velocidad al buscar eventos de auditoría por usuario o administrador.
*/
CREATE INDEX idx_auditoria_usuario_afectado ON Auditoria(id_usuario_afectado);
CREATE INDEX idx_auditoria_admin_responsable ON Auditoria(id_administrador_responsable);

/*
Índice para el estado de las donaciones, ya que es un campo que se consultará con frecuencia.
*/
CREATE INDEX idx_donacion_estado ON Donacion(estado_donacion);

/*
Índice para la fecha de registro de los usuarios, útil para reportes y estadísticas.
*/ 
CREATE INDEX idx_usuarios_fecha_registro ON Usuarios(fecha_registro);