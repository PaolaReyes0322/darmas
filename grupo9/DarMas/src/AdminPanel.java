import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Este es un ejemplo de cómo agregar el botón de respaldo al panel del administrador.
 * Asume que ya tienes una clase AdminPanel o un panel similar.
 */
public class AdminPanel extends JPanel {

    private JButton backupButton;

    public AdminPanel() {
        // Configuración básica del panel
        setLayout(new FlowLayout());
        setBorder(BorderFactory.createTitledBorder("Panel de Administración"));

        // Inicializar el botón de respaldo
        backupButton = new JButton("Crear Respaldo de DB");
        add(backupButton);

        // Agregar el listener al botón para ejecutar la acción
        backupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Llamar al método que invoca el procedimiento almacenado
                // Se asume que la clase DatabaseManager existe y tiene el método crearRespaldo()
                crearRespaldoDeBaseDeDatos();
            }
        });
    }

    /**
     * Método que llama al procedimiento almacenado 'usp_CrearRespaldo'
     * para crear un respaldo de la base de datos.
     */
    private void crearRespaldoDeBaseDeDatos() {
        Connection conn = null;
        CallableStatement cstmt = null;
        try {
            // Obtener la conexión a la base de datos
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "No se pudo establecer conexión con la base de datos.", "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Llamar al procedimiento almacenado
            String sql = "{call usp_CrearRespaldo()}";
            cstmt = conn.prepareCall(sql);
            cstmt.execute();

            JOptionPane.showMessageDialog(this, "El respaldo de la base de datos se ha creado correctamente.", "Respaldo Exitoso", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al crear el respaldo de la base de datos: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            try {
                if (cstmt != null) cstmt.close();
                if (conn != null) DatabaseConnection.closeConnection(conn);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }}