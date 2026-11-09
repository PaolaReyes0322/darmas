import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Panel para que el rol de "voluntario" pueda registrar entregas de solicitudes aprobadas.
 * Muestra una tabla con todas las solicitudes aprobadas y permite al voluntario seleccionar
 * la solicitud y el stock para registrar una entrega.
 */
public class DeliveryPanel extends JPanel {

    private final int volunteerId;
    private final DefaultTableModel requestsTableModel;
    private final JTable requestsTable;
    private final DefaultTableModel stockTableModel;
    private final JTable stockTable;

    public DeliveryPanel(int volunteerId) {
        this.volunteerId = volunteerId;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Panel de Solicitudes Aprobadas
        JPanel requestsPanel = new JPanel(new BorderLayout());
        requestsPanel.setBorder(BorderFactory.createTitledBorder("Solicitudes Aprobadas"));
        String[] requestsColumnNames = {"ID Solicitud", "ID Beneficiario", "Justificación", "Categoría", "Cantidad Solicitada", "Prioridad", "Fecha"};
        requestsTableModel = new DefaultTableModel(requestsColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestsTable = new JTable(requestsTableModel);
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && requestsTable.getSelectedRow() != -1) {
                loadAvailableStockForRequest();
            }
        });
        requestsPanel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0.5;
        add(requestsPanel, gbc);

        // Panel de Stock Disponible
        JPanel stockPanel = new JPanel(new BorderLayout());
        stockPanel.setBorder(BorderFactory.createTitledBorder("Stock Disponible"));
        String[] stockColumnNames = {"ID Stock", "Cantidad Disponible", "Fecha Vencimiento", "Ubicación"};
        stockTableModel = new DefaultTableModel(stockColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        stockTable = new JTable(stockTableModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockPanel.add(new JScrollPane(stockTable), BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(stockPanel, gbc);

        // Panel de Controles
        JPanel controlPanel = new JPanel();
        JTextField deliveryAmountField = new JTextField(10);
        JButton registerDeliveryButton = new JButton("Registrar Entrega");
        
        registerDeliveryButton.addActionListener(e -> registerDelivery(deliveryAmountField.getText()));

        controlPanel.add(new JLabel("Cantidad a Entregar:"));
        controlPanel.add(deliveryAmountField);
        controlPanel.add(registerDeliveryButton);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(controlPanel, gbc);

        // Cargar las solicitudes aprobadas al iniciar
        loadApprovedRequests();
    }
    
    /**
     * Carga todas las solicitudes con estado 'aprobada' desde la base de datos y las muestra en la tabla.
     */
    private void loadApprovedRequests() {
        requestsTableModel.setRowCount(0); // Limpiar la tabla
        stockTableModel.setRowCount(0); // Limpiar la tabla de stock
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String query = "SELECT s.id_solicitud, s.id_beneficiario, s.justificacion, s.prioridad, ds.cantidad_solicitada, cr.nombre_categoria, s.fecha_solicitud " +
                       "FROM Solicitud s " +
                       "JOIN Detalle_Solicitud ds ON s.id_solicitud = ds.id_solicitud " +
                       "JOIN Categoria_Recurso cr ON ds.id_categoria = cr.id_categoria " +
                       "WHERE s.estado_solicitud = 'aprobada'";

        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                requestsTableModel.addRow(new Object[]{
                    rs.getInt("id_solicitud"),
                    rs.getInt("id_beneficiario"),
                    rs.getString("justificacion"),
                    rs.getString("nombre_categoria"),
                    rs.getDouble("cantidad_solicitada"),
                    rs.getString("prioridad"),
                    rs.getDate("fecha_solicitud")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar solicitudes aprobadas: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    /**
     * Carga el stock disponible para la categoría de la solicitud seleccionada.
     */
    private void loadAvailableStockForRequest() {
        int selectedRow = requestsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        String categoryName = (String) requestsTableModel.getValueAt(selectedRow, 3);
        int categoryId = getCategoryId(categoryName);
        
        if (categoryId == -1) {
            JOptionPane.showMessageDialog(this, "Categoría no encontrada para el stock.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        stockTableModel.setRowCount(0); // Limpiar la tabla de stock
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String query = "SELECT s.id_stock, s.cantidad_disponible, s.fecha_vencimiento, s.ubicacion_almacen " +
                       "FROM Stock s " +
                       "JOIN Donacion d ON s.id_donacion = d.id_donacion " +
                       "WHERE d.id_categoria = ? AND s.cantidad_disponible > 0 AND s.estado_producto = 'en_stock'";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stockTableModel.addRow(new Object[]{
                        rs.getInt("id_stock"),
                        rs.getDouble("cantidad_disponible"),
                        rs.getDate("fecha_vencimiento"),
                        rs.getString("ubicacion_almacen")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar stock disponible: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private void registerDelivery(String amountStr) {
        int selectedRequestRow = requestsTable.getSelectedRow();
        int selectedStockRow = stockTable.getSelectedRow();
        
        if (selectedRequestRow == -1 || selectedStockRow == -1 || amountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione una solicitud y un stock y escriba la cantidad a entregar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int requestId = (int) requestsTableModel.getValueAt(selectedRequestRow, 0);
            int stockId = (int) stockTableModel.getValueAt(selectedStockRow, 0);
            double amount = Double.parseDouble(amountStr);
            double availableStock = (double) stockTableModel.getValueAt(selectedStockRow, 1);
            
            if (amount <= 0 || amount > availableStock) {
                JOptionPane.showMessageDialog(this, "La cantidad a entregar no es válida.", "Error de Cantidad", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return;

            try (CallableStatement cstmt = conn.prepareCall("{CALL usp_RegistrarDonacionEntregada(?, ?, ?, ?)}")) {
                cstmt.setInt(1, requestId);
                cstmt.setInt(2, this.volunteerId);
                cstmt.setInt(3, stockId);
                cstmt.setDouble(4, amount);
                
                cstmt.execute();
                
                JOptionPane.showMessageDialog(this, "Entrega registrada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                // Recargar las tablas después de registrar la entrega
                loadApprovedRequests();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al registrar entrega: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int getCategoryId(String categoryName) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return -1;
        
        String query = "SELECT id_categoria FROM Categoria_Recurso WHERE nombre_categoria = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, categoryName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_categoria");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
        return -1;
    }
}