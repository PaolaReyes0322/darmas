import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginPanel extends JPanel {

    private final AppFrame appFrame;
    private final JTextField emailField;

    public LoginPanel(AppFrame appFrame) {
        this.appFrame = appFrame;
        setLayout(new GridBagLayout());
        setBackground(new Color(240, 248, 255)); // Fondo azul claro

        // Configuración de fuentes
        Font titleFont = new Font("SansSerif", Font.BOLD, 28);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 14);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 14);

        // Título
        JLabel titleLabel = new JLabel("Bienvenido a Darmas");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(60, 60, 60));

        // Etiqueta del email
        JLabel emailLabel = new JLabel("Correo Electrónico:");
        emailLabel.setFont(labelFont);
        emailField = new JTextField(20);

        // Botón de Iniciar Sesión
        JButton loginButton = new JButton("Iniciar Sesión");
        loginButton.setFont(buttonFont);
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.addActionListener(e -> attemptLogin());

        // Botón de Registrarse
        JButton registerButton = new JButton("Registrarse");
        registerButton.setFont(buttonFont);
        registerButton.setBackground(new Color(173, 216, 230));
        registerButton.setForeground(new Color(60, 60, 60));
        registerButton.setFocusPainted(false);
        registerButton.addActionListener(e -> appFrame.showUserManagementPanel());
        
        // Layout con GridBagLayout para un mejor control
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(emailLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(emailField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(loginButton, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(registerButton, gbc);
    }

    private void attemptLogin() {
        String email = emailField.getText();
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un correo electrónico.", "Error de Inicio de Sesión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String query = "SELECT id_usuario, tipo_usuario, nombre FROM Usuarios WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int userId = rs.getInt("id_usuario");
                    String userType = rs.getString("tipo_usuario");
                    String userName = rs.getString("nombre");
                    
                    JOptionPane.showMessageDialog(this, "¡Bienvenido, " + userName + "!", "Inicio de Sesión Exitoso", JOptionPane.INFORMATION_MESSAGE);
                    appFrame.showMainContentPanel(userId, userType);
                } else {
                    JOptionPane.showMessageDialog(this, "El correo electrónico no está registrado. Por favor, regístrese.", "Usuario No Encontrado", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al intentar iniciar sesión: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }
}
