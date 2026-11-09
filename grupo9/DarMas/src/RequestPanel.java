import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * Panel para que los usuarios de tipo "beneficiario" puedan crear nuevas solicitudes.
 * Este panel se encarga de recopilar los datos y llamar al procedimiento almacenado
 * 'usp_CrearSolicitud' en la base de datos.
 */
public class RequestPanel extends JPanel {

    private final int beneficiaryId;
    private final AppFrame appFrame;

    private final JTextArea justificacionArea;
    private final JComboBox<String> prioridadComboBox;
    private final JComboBox<String> categoriaComboBox;
    private final JTextField cantidadField;

    public RequestPanel(int beneficiaryId, AppFrame appFrame) {
        this.beneficiaryId = beneficiaryId;
        this.appFrame = appFrame;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        JLabel titleLabel = new JLabel("Crear Nueva Solicitud", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        // Justificación
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("Justificación:"), gbc);
        gbc.gridx = 1;
        justificacionArea = new JTextArea(5, 20);
        justificacionArea.setLineWrap(true);
        justificacionArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(justificacionArea);
        add(scrollPane, gbc);

        // Prioridad
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Prioridad:"), gbc);
        gbc.gridx = 1;
        prioridadComboBox = new JComboBox<>(new String[]{"baja", "media", "alta"});
        add(prioridadComboBox, gbc);

        // Categoría
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Categoría del Recurso:"), gbc);
        gbc.gridx = 1;
        categoriaComboBox = new JComboBox<>();
        loadCategories(); // Cargar las categorías al iniciar
        add(categoriaComboBox, gbc);

        // Cantidad Solicitada
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Cantidad Solicitada:"), gbc);
        gbc.gridx = 1;
        cantidadField = new JTextField();
        add(cantidadField, gbc);

        // Botón de Crear Solicitud
        JButton createRequestButton = new JButton("Crear Solicitud");
        createRequestButton.addActionListener(e -> createRequest());
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        add(createRequestButton, gbc);
    }
    
    // Método para cargar las categorías de recursos desde la base de datos
    private void loadCategories() {
        categoriaComboBox.removeAllItems(); // Limpiar items existentes
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "SELECT nombre_categoria FROM Categoria_Recurso";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                categoriaComboBox.addItem(rs.getString("nombre_categoria"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }
    
    private void createRequest() {
        String justificacion = justificacionArea.getText();
        String prioridad = (String) prioridadComboBox.getSelectedItem();
        String categoriaNombre = (String) categoriaComboBox.getSelectedItem();
        String cantidadStr = cantidadField.getText();

        if (justificacion.isEmpty() || categoriaNombre == null || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            double cantidad = Double.parseDouble(cantidadStr);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un número positivo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int categoriaId = getCategoryId(categoriaNombre);
            if (categoriaId == -1) {
                JOptionPane.showMessageDialog(this, "Categoría no encontrada.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return;

            // Llamar al procedimiento almacenado para crear la solicitud
            try (CallableStatement cstmt = conn.prepareCall("{CALL usp_CrearSolicitud(?, ?, ?, ?, ?)}")) {
                cstmt.setInt(1, this.beneficiaryId);
                cstmt.setString(2, justificacion);
                cstmt.setString(3, prioridad);
                cstmt.setInt(4, categoriaId);
                cstmt.setDouble(5, cantidad);
                
                cstmt.execute();
                
                JOptionPane.showMessageDialog(this, "Solicitud creada con éxito. Pendiente de validación.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                // Limpiar campos después del éxito
                justificacionArea.setText("");
                cantidadField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error SQL al crear la solicitud: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
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