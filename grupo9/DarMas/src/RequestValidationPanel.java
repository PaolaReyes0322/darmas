import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Panel para que el rol de "coordinador" pueda validar o rechazar solicitudes de beneficiarios.
 * Muestra una tabla con todas las solicitudes pendientes y permite al coordinador
 * cambiar su estado a 'aprobada' o 'rechazada'.
 */
public class RequestValidationPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable requestTable;
    private final AppFrame appFrame;

    public RequestValidationPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new BorderLayout());

        // Configuración de la tabla
        String[] columnNames = {"ID Solicitud", "ID Beneficiario", "Justificación", "Prioridad", "Categoría", "Cantidad", "Fecha"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Las celdas de la tabla no son editables
            }
        };
        requestTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(requestTable);
        add(scrollPane, BorderLayout.CENTER);

        // Panel de botones para la validación
        JPanel buttonPanel = new JPanel();
        JButton approveButton = new JButton("Aprobar Solicitud");
        JButton rejectButton = new JButton("Rechazar Solicitud");
        JButton refreshButton = new JButton("Actualizar");

        approveButton.addActionListener(e -> validateRequest("aprobada"));
        rejectButton.addActionListener(e -> validateRequest("rechazada"));
        refreshButton.addActionListener(e -> loadPendingRequests());

        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Cargar las solicitudes pendientes al iniciar
        loadPendingRequests();
    }

    /**
     * Carga todas las solicitudes con estado 'pendiente' desde la base de datos y las muestra en la tabla.
     */
    private void loadPendingRequests() {
        tableModel.setRowCount(0); // Limpiar la tabla

        // Obtener una conexión a la base de datos utilizando la configuración predeterminada.
        // En esta pantalla sólo se realiza una lectura simple de las solicitudes pendientes, por lo que no es necesario
        // especificar un nivel de aislamiento especial. Los niveles de aislamiento y bloqueos se manejan en los
        // procedimientos almacenados en SQL.
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        // Consulta para obtener las solicitudes pendientes con la información de la categoría
        String query = "SELECT s.id_solicitud, s.id_beneficiario, s.justificacion, s.prioridad, cr.nombre_categoria, ds.cantidad_solicitada, s.fecha_solicitud " +
                    "FROM Solicitud s " +
                    "JOIN Detalle_Solicitud ds ON s.id_solicitud = ds.id_solicitud " +
                    "JOIN Categoria_Recurso cr ON ds.id_categoria = cr.id_categoria " +
                    "WHERE s.estado_solicitud = 'pendiente'";

        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id_solicitud"),
                    rs.getInt("id_beneficiario"),
                    rs.getString("justificacion"),
                    rs.getString("prioridad"),
                    rs.getString("nombre_categoria"),
                    rs.getDouble("cantidad_solicitada"),
                    rs.getDate("fecha_solicitud")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar solicitudes: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
     * Valida la solicitud seleccionada en la tabla usando el procedimiento almacenado.
     *
     * @param decision 'aprobada' o 'rechazada'
     */
    private void validateRequest(String decision) {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una solicitud de la tabla.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) tableModel.getValueAt(selectedRow, 0);

        // Obtener la conexión sin especificar niveles de aislamiento desde Java. Los controles de concurrencia
        // y bloqueos se aplican dentro del procedimiento almacenado usp_ValidarSolicitud.
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        try (CallableStatement cstmt = conn.prepareCall("{CALL usp_ValidarSolicitud(?, ?)}")) {
            cstmt.setInt(1, requestId);
            cstmt.setString(2, decision);
            cstmt.execute();

            JOptionPane.showMessageDialog(this, "Solicitud " + decision + " con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            loadPendingRequests(); // Recargar la tabla para ver los cambios
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error SQL al validar solicitud: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }
}
