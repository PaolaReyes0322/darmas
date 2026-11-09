import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagementPanel extends JPanel {

    private final AppFrame appFrame; // Nuevo campo para la referencia a la ventana principal
    private final JTextField nombreField;
    private final JTextField apellidoField;
    private final JTextField emailField;
    private final JTextField telefonoField;
    private final JComboBox<String> tipoUsuarioComboBox;
    private final JTextField calleField;
    private final JTextField ciudadField;
    private final JTextField codigoPostalField;
    private final JTable userTable;
    private final DefaultTableModel tableModel;

    // Nuevo constructor que acepta un objeto AppFrame
    public UserManagementPanel(AppFrame appFrame) {
        this.appFrame = appFrame; // Guardamos la referencia para usarla más tarde
        setLayout(new BorderLayout());

        // Panel de formularios
        JPanel formPanel = new JPanel(new GridLayout(9, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Registrar Nuevo Usuario"));

        nombreField = new JTextField();
        apellidoField = new JTextField();
        emailField = new JTextField();
        telefonoField = new JTextField();
        tipoUsuarioComboBox = new JComboBox<>(new String[]{"donante", "beneficiario", "voluntario", "coordinador", "administrador"});
        calleField = new JTextField();
        ciudadField = new JTextField();
        codigoPostalField = new JTextField();

        formPanel.add(new JLabel("Nombre:"));
        formPanel.add(nombreField);
        formPanel.add(new JLabel("Apellido:"));
        formPanel.add(apellidoField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Teléfono:"));
        formPanel.add(telefonoField);
        formPanel.add(new JLabel("Tipo de Usuario:"));
        formPanel.add(tipoUsuarioComboBox);
        formPanel.add(new JLabel("Calle:"));
        formPanel.add(calleField);
        formPanel.add(new JLabel("Ciudad:"));
        formPanel.add(ciudadField);
        formPanel.add(new JLabel("Código Postal:"));
        formPanel.add(codigoPostalField);

        JButton registerButton = new JButton("Registrar Usuario");
        registerButton.addActionListener(e -> registerUser());
        formPanel.add(registerButton);

        // Nuevo botón para volver al login
        JButton backButton = new JButton("Volver al Inicio de Sesión");
        backButton.addActionListener(e -> appFrame.showLoginPanel());
        formPanel.add(backButton);

        add(formPanel, BorderLayout.NORTH);

        // Tabla para mostrar usuarios
        String[] columnNames = {"ID", "Nombre", "Apellido", "Email", "Tipo Usuario"};
        tableModel = new DefaultTableModel(columnNames, 0);
        userTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(userTable);

        add(scrollPane, BorderLayout.CENTER);

        loadUsers();
    }
    
    // Dejo el constructor sin argumentos por si lo necesitas en algún otro lugar
    // Si no lo necesitas, lo puedes borrar.
    public UserManagementPanel() {
        this(null);
    }

    private void registerUser() {
        String nombre = nombreField.getText();
        String apellido = apellidoField.getText();
        String email = emailField.getText();
        String telefono = telefonoField.getText();
        String tipoUsuario = (String) tipoUsuarioComboBox.getSelectedItem();
        String calle = calleField.getText();
        String ciudad = ciudadField.getText();
        String codigoPostal = codigoPostalField.getText();

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (CallableStatement cstmt = conn.prepareCall("{CALL usp_RegistrarUsuarioConDireccion(?, ?, ?, ?, ?, ?, ?, ?)}")) {
            cstmt.setString(1, nombre);
            cstmt.setString(2, apellido);
            cstmt.setString(3, email);
            cstmt.setString(4, telefono);
            cstmt.setString(5, tipoUsuario);
            cstmt.setString(6, calle);
            cstmt.setString(7, ciudad);
            cstmt.setString(8, codigoPostal);
            
            cstmt.execute();
            JOptionPane.showMessageDialog(this, "Usuario registrado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            loadUsers(); // Recargar la tabla
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar usuario: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private void loadUsers() {
        tableModel.setRowCount(0); // Limpiar la tabla
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String query = "SELECT id_usuario, nombre, apellido, email, tipo_usuario FROM Usuarios";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id_usuario"),
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("email"),
                    rs.getString("tipo_usuario")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }
}
