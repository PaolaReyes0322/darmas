
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuditPanel extends JPanel {

    private final JTable auditTable;
    private final DefaultTableModel tableModel;

    public AuditPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Registro de Auditoría"));

        // Se corrigieron los nombres de las columnas para que coincidan con tu tabla
        String[] columnNames = {"ID Evento", "Tipo de Evento", "Descripción", "ID Usuario Afectado", "Fecha"};
        tableModel = new DefaultTableModel(columnNames, 0);
        auditTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(auditTable);
        add(scrollPane, BorderLayout.CENTER);
        
        loadAuditLogs();
    }

    private void loadAuditLogs() {
        tableModel.setRowCount(0);
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        // Se corrigió el query SQL para usar los nombres de columnas de tu tabla
        String query = "SELECT id_evento, tipo_evento, descripcion_evento, id_usuario_afectado, fecha_evento FROM Auditoria ORDER BY fecha_evento DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id_evento"), // Se cambió de id_auditoria a id_evento
                    rs.getString("tipo_evento"),
                    rs.getString("descripcion_evento"),
                    rs.getInt("id_usuario_afectado"),
                    rs.getTimestamp("fecha_evento")
                });
            }
        } catch (SQLException ex) {
            // Este mensaje de error también se corrigió para que sea más claro
            JOptionPane.showMessageDialog(this, "Error al cargar el registro de auditoría: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }
}
