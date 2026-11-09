
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StockControlPanel extends JPanel {

    private final JTable donationTable;
    private final DefaultTableModel tableModel;
    private final JTextField coordinadorIdField;
    private final JTextField ubicacionField;
    private final JTextField fechaVencimientoField;

    public StockControlPanel() {
        setLayout(new BorderLayout());
        
        // Panel de control
        JPanel controlPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Validar Donación"));
        coordinadorIdField = new JTextField();
        ubicacionField = new JTextField();
        fechaVencimientoField = new JTextField("YYYY-MM-DD");

        controlPanel.add(new JLabel("ID del Coordinador:"));
        controlPanel.add(coordinadorIdField);
        controlPanel.add(new JLabel("Ubicación de Almacén:"));
        controlPanel.add(ubicacionField);
        controlPanel.add(new JLabel("Fecha de Vencimiento:"));
        controlPanel.add(fechaVencimientoField);
        
        JButton validateButton = new JButton("Validar y Mover a Stock");
        validateButton.addActionListener(e -> validateDonation());
        controlPanel.add(validateButton);

        add(controlPanel, BorderLayout.NORTH);

        // Tabla de donaciones pendientes
        String[] columnNames = {"ID Donación", "ID Donante", "ID Categoría", "Cantidad", "Descripción", "Fecha Donación"};
        tableModel = new DefaultTableModel(columnNames, 0);
        donationTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(donationTable);

        add(scrollPane, BorderLayout.CENTER);

        loadPendingDonations();
    }

    private void validateDonation() {
        int selectedRow = donationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una donación de la tabla para validar.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int donacionId = (int) tableModel.getValueAt(selectedRow, 0);
        int coordinadorId;
        String ubicacion = ubicacionField.getText();
        String fechaVencimiento = fechaVencimientoField.getText();
        
        try {
            coordinadorId = Integer.parseInt(coordinadorIdField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor, introduce un ID de coordinador válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (CallableStatement cstmt = conn.prepareCall("{CALL usp_ValidarDonacionRecibidaYAgregarStock(?, ?, ?, ?)}")) {
            cstmt.setInt(1, donacionId);
            cstmt.setInt(2, coordinadorId);
            cstmt.setString(3, ubicacion);
            cstmt.setString(4, fechaVencimiento);
            
            cstmt.execute();
            JOptionPane.showMessageDialog(this, "Donación validada y movida a stock con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            loadPendingDonations(); // Recargar la tabla
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al validar donación: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private void loadPendingDonations() {
        tableModel.setRowCount(0); // Limpiar la tabla
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String query = "SELECT id_donacion, id_donante, id_categoria, cantidad, descripcion, fecha_donacion FROM Donacion WHERE estado_donacion = 'pendiente'";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id_donacion"),
                    rs.getInt("id_donante"),
                    rs.getInt("id_categoria"),
                    rs.getDouble("cantidad"),
                    rs.getString("descripcion"),
                    rs.getDate("fecha_donacion")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar donaciones pendientes: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }
}