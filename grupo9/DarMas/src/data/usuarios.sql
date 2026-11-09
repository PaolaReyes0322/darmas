-------------------------------------------------
--        CREACION DE TABLAS PRINCIPALES      
-------------------------------------------------
CREATE DATABASE darmas;
USE darmas;
CREATE TABLE Direcciones (
	id_direccion INT PRIMARY KEY AUTO_INCREMENT,
    calle VARCHAR(255) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(20) NOT NULL
);

CREATE TABLE Usuarios (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    id_direccion INT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('activo', 'inactivo', 'pendiente') NOT NULL DEFAULT 'pendiente',
    tipo_usuario ENUM('donante', 'beneficiario', 'voluntario', 'coordinador', 'administrador') NOT NULL,
	CONSTRAINT fk_usuario_direccion FOREIGN KEY (id_direccion) REFERENCES Direcciones(id_direccion)
);

-------------------------------------------------
--  TABLAS HIJAS PARA LA HERENCIA DE USUARIOS 
-------------------------------------------------
CREATE TABLE Donantes (
    id_donante INT PRIMARY KEY,
    CONSTRAINT fk_donante_usuario FOREIGN KEY (id_donante) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Beneficiarios (
    id_beneficiario INT PRIMARY KEY,
    CONSTRAINT fk_beneficiario_usuario FOREIGN KEY (id_beneficiario) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Voluntarios (
    id_voluntario INT PRIMARY KEY,
    CONSTRAINT fk_voluntario_usuario FOREIGN KEY (id_voluntario) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Coordinadores (
    id_coordinador INT PRIMARY KEY,
    CONSTRAINT fk_coordinador_usuario FOREIGN KEY (id_coordinador) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Administradores (
    id_administrador INT PRIMARY KEY,
    cargo VARCHAR(100),
    nivel_acceso ENUM('full', 'parcial') NOT NULL,
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_administrador) REFERENCES Usuarios(id_usuario)
);

------------------------------------------------
--       TABLAS DE LA LOGICA DE NEGOCIO      
------------------------------------------------

CREATE TABLE Categoria_Recurso (
    id_categoria INT PRIMARY KEY AUTO_INCREMENT,
    nombre_categoria VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    requiere_refrigeracion BOOLEAN DEFAULT FALSE,
    fecha_vencimiento_promedio INT, 
    unidad_medida VARCHAR(50)
);

CREATE TABLE Donacion (
    id_donacion INT PRIMARY KEY AUTO_INCREMENT,
    fecha_donacion DATE NOT NULL,
    cantidad DECIMAL(10, 2) NOT NULL CHECK (cantidad > 0),
    descripcion TEXT,
    estado_donacion ENUM('pendiente', 'validado', 'rechazado','agotado') NOT NULL DEFAULT 'pendiente',
    fecha_validacion DATE,
    observaciones TEXT,
    id_donante INT NOT NULL,
    id_categoria INT NOT NULL,
    id_coordinador_validador INT,
    CONSTRAINT fk_donacion_donante FOREIGN KEY (id_donante) REFERENCES Usuarios(id_usuario),
    CONSTRAINT fk_donacion_categoria FOREIGN KEY (id_categoria) REFERENCES Categoria_Recurso(id_categoria),
    CONSTRAINT fk_donacion_coordinador FOREIGN KEY (id_coordinador_validador) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Stock ( 
    id_stock INT PRIMARY KEY AUTO_INCREMENT,
    cantidad_disponible DECIMAL(10, 2) NOT NULL,
    fecha_ingreso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_vencimiento DATE,
    ubicacion_almacen VARCHAR(255),
    estado_producto ENUM('en_stock', 'vencido', 'entregado') NOT NULL DEFAULT 'en_stock',
    id_donacion INT NOT NULL,
    CONSTRAINT fk_stock_donacion FOREIGN KEY (id_donacion) REFERENCES Donacion(id_donacion)
);

CREATE TABLE Solicitud (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    fecha_solicitud DATE NOT NULL,
    estado_solicitud ENUM('pendiente', 'aprobada', 'entregada','cancelada') NOT NULL DEFAULT 'pendiente',
    prioridad ENUM('alta', 'media', 'baja') NOT NULL,
    justificacion TEXT,
    fecha_aprobacion DATE,
    id_beneficiario INT NOT NULL,
    CONSTRAINT fk_solicitud_beneficiario FOREIGN KEY (id_beneficiario) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Detalle_Solicitud (
    id_detalle_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    cantidad_solicitada DECIMAL(10, 2) NOT NULL, CHECK (cantidad_solicitada > 0),
    id_solicitud INT NOT NULL,
    id_categoria INT NOT NULL,
    CONSTRAINT fk_detalle_solicitud FOREIGN KEY (id_solicitud) REFERENCES Solicitud(id_solicitud),
    CONSTRAINT fk_detalle_categoria FOREIGN KEY (id_categoria) REFERENCES Categoria_Recurso(id_categoria)
);

CREATE TABLE Entrega (
    id_entrega INT PRIMARY KEY AUTO_INCREMENT,
    fecha_entrega DATE NOT NULL,
    estado_entrega ENUM('en_proceso', 'completada', 'cancelada') NOT NULL DEFAULT 'en_proceso',
    observaciones TEXT,
    id_solicitud INT NOT NULL,
    id_voluntario INT NOT NULL,
    CONSTRAINT fk_entrega_solicitud FOREIGN KEY (id_solicitud) REFERENCES Solicitud(id_solicitud),
    CONSTRAINT fk_entrega_voluntario FOREIGN KEY (id_voluntario) REFERENCES Usuarios(id_usuario)
);

CREATE TABLE Detalle_Entrega (
    id_detalle_entrega INT PRIMARY KEY AUTO_INCREMENT,
    cantidad_entregada DECIMAL(10, 2) NOT NULL,
    id_entrega INT NOT NULL,
    id_stock INT NOT NULL,
    CONSTRAINT fk_detalle_entrega FOREIGN KEY (id_entrega) REFERENCES Entrega(id_entrega),
    CONSTRAINT fk_detalle_stock FOREIGN KEY (id_stock) REFERENCES Stock(id_stock)
);

CREATE TABLE Programacion (
    id_programacion INT PRIMARY KEY AUTO_INCREMENT,
    fecha_programada DATE NOT NULL,
    hora_inicio TIME,
    hora_fin TIME,
    tipo_actividad ENUM('recoleccion', 'entrega') NOT NULL,
    estado ENUM('pendiente', 'completada', 'cancelada') NOT NULL DEFAULT 'pendiente',
	id_direccion INT NOT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT fk_programacion_usuario FOREIGN KEY (id_usuario) REFERENCES Usuarios(id_usuario),
    CONSTRAINT fk_programacion_direccion FOREIGN KEY (id_direccion) REFERENCES Direcciones(id_direccion)
);

CREATE TABLE Respaldo (
    id_respaldo INT PRIMARY KEY AUTO_INCREMENT,
    fecha_respaldo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo_respaldo ENUM('automatico', 'manual') NOT NULL,
    ubicacion_archivo VARCHAR(255),
    estado_respaldo ENUM('exitoso', 'fallido') NOT NULL,
    id_administrador INT NOT NULL,
    CONSTRAINT fk_respaldo_administrador FOREIGN KEY (id_administrador) REFERENCES Administradores(id_administrador)
);


CREATE TABLE Auditoria (
    id_evento INT PRIMARY KEY AUTO_INCREMENT,
    tipo_evento VARCHAR(50),
    descripcion_evento TEXT,
    fecha_evento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_usuario_afectado INT,
    id_administrador_responsable INT,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario_afectado) REFERENCES Usuarios(id_usuario),
    CONSTRAINT fk_auditoria_admin FOREIGN KEY (id_administrador_responsable) REFERENCES Administradores(id_administrador)
);
