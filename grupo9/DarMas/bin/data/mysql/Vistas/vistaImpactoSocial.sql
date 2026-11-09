CREATE VIEW VistaImpactoSocial AS
SELECT
    (SELECT COUNT(*) FROM Donacion WHERE estado_donacion = 'validado') AS total_donaciones_validas,
    (SELECT COUNT(DISTINCT id_beneficiario) FROM Solicitud WHERE estado_solicitud = 'aprobada') AS total_beneficiarios_atendidos,
    (SELECT SUM(cantidad_entregada) FROM Detalle_Entrega) AS total_items_entregados;