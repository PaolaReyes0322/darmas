import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DonationRegistrationPanel extends JPanel {

    private final JTextField donorIdField;
    private final JTextField quantityField;
    private final JTextArea descriptionArea;
    private final JComboBox<CategoryItem> categoryComboBox; // Ahora usamos un JComboBox

    public DonationRegistrationPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior para el formulario
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Registrar Nueva Donación"));

        donorIdField = new JTextField(10);
        quantityField = new JTextField(10);
        descriptionArea = new JTextArea(5, 20);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);

        // Inicializar JComboBox
        categoryComboBox = new JComboBox<>();
        loadCategories(); // Cargar datos desde la base de datos

        formPanel.add(new JLabel("ID del Donante:"));
        formPanel.add(donorIdField);
        formPanel.add(new JLabel("Categoría:"));
        formPanel.add(categoryComboBox);
        formPanel.add(new JLabel("Cantidad:"));
        formPanel.add(quantityField);
        formPanel.add(new JLabel("Descripción:"));
        formPanel.add(descriptionScrollPane);

        // Panel inferior para el botón
        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Registrar Donación");
        registerButton.addActionListener(e -> registerDonation());
        buttonPanel.add(registerButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Cargar las categorías desde la base de datos
    private void loadCategories() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            String sql = "SELECT id_categoria, nombre_categoria FROM Categoria_Recurso";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

                categoryComboBox.removeAllItems();
                while (rs.next()) {
                    int id = rs.getInt("id_categoria");
                    String name = rs.getString("nombre_categoria");
                    categoryComboBox.addItem(new CategoryItem(id, name));
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + ex.getMessage(),
                        "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn);
            }
        }
    }

    private void registerDonation() {
        try {
            int donorId = Integer.parseInt(donorIdField.getText());
            double quantity = Double.parseDouble(quantityField.getText());
            String description = descriptionArea.getText();

            CategoryItem selectedCategory = (CategoryItem) categoryComboBox.getSelectedItem();
            if (selectedCategory == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una categoría.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int categoryId = selectedCategory.getId();

            // Llamar al procedimiento almacenado
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                String query = "{CALL usp_RegistrarDonacionRecibida(?, ?, ?, ?)}";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, donorId);
                    pstmt.setInt(2, categoryId);
                    pstmt.setDouble(3, quantity);
                    pstmt.setString(4, description);

                    pstmt.execute();

                    JOptionPane.showMessageDialog(this, "Donación registrada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

                    donorIdField.setText("");
                    quantityField.setText("");
                    descriptionArea.setText("");
                    categoryComboBox.setSelectedIndex(0);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error al registrar donación: " + ex.getMessage(),
                            "Error SQL", JOptionPane.ERROR_MESSAGE);
                } finally {
                    DatabaseConnection.closeConnection(conn);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese valores válidos para ID y Cantidad.",
                    "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Clase interna para almacenar ID y nombre en el JComboBox
    private static class CategoryItem {
        private final int id;
        private final String name;

        public CategoryItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name; // Lo que se muestra en el JComboBox
        }
    }
}
