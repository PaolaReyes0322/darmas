import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportsPanel extends JPanel {

    private final JTable voluntariosTable;
    private final DefaultTableModel voluntariosModel;
    private final JTable impactoTable;
    private final DefaultTableModel impactoModel;
    private JButton backupButton; // Botón para el respaldo de la base de datos

    public ReportsPanel() {
        setLayout(new BorderLayout()); 
        setBorder(BorderFactory.createTitledBorder("Reportes del Sistema"));

        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        
        // Tabla de Actividad de Voluntarios
        String[] voluntariosColumns = {"Nombre", "Apellido", "Email", "Total Entregas"};
        voluntariosModel = new DefaultTableModel(voluntariosColumns, 0);
        voluntariosTable = new JTable(voluntariosModel);
        JScrollPane voluntariosScrollPane = new JScrollPane(voluntariosTable);
        voluntariosScrollPane.setBorder(BorderFactory.createTitledBorder("Actividad de Voluntarios"));
        tablesPanel.add(voluntariosScrollPane);

        // Tabla de Reporte de Impacto Anual
        String[] impactoColumns = {"Año", "Categoría", "Cantidad Donada", "Cantidad Entregada"};
        impactoModel = new DefaultTableModel(impactoColumns, 0);
        impactoTable = new JTable(impactoModel);
        JScrollPane impactoScrollPane = new JScrollPane(impactoTable);
        impactoScrollPane.setBorder(BorderFactory.createTitledBorder("Reporte de Impacto Anual"));
        tablesPanel.add(impactoScrollPane);
        
        add(tablesPanel, BorderLayout.CENTER);

        // Panel para los botones, ubicado en la parte inferior
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Usamos FlowLayout para centrar los botones

        // Botón de Actualizar
        JButton refreshButton = new JButton("Actualizar");
        refreshButton.addActionListener(e -> {
            loadReports();
            JOptionPane.showMessageDialog(this, "Datos actualizados correctamente.", "Actualización", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(refreshButton);

        // Botón de Respaldo de la base de datos
        backupButton = new JButton("Crear Respaldo de DB");
        backupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                crearRespaldoDeBaseDeDatos();
            }
        });
        buttonPanel.add(backupButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Cargar los datos la primera vez que se crea el panel
        loadReports();
    }

    private void loadReports() {
        loadVolunteersReport();
        loadImpactReport();
    }

    private void loadVolunteersReport() {
        voluntariosModel.setRowCount(0);
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String query = "SELECT nombre, apellido, email, total_entregas_completadas FROM vw_ActividadVoluntarios";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                voluntariosModel.addRow(new Object[]{
                    rs.getString("nombre"),
                    rs.getString("apellido"),
                    rs.getString("email"),
                    rs.getInt("total_entregas_completadas")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar reporte de voluntarios: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private void loadImpactReport() {
        impactoModel.setRowCount(0);
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;

        String query = "SELECT anio, nombre_categoria, cantidad_donada, cantidad_entregada FROM vw_ReporteImpactoAnual";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                impactoModel.addRow(new Object[]{
                    rs.getInt("anio"),
                    rs.getString("nombre_categoria"),
                    rs.getDouble("cantidad_donada"),
                    rs.getDouble("cantidad_entregada")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar reporte de impacto: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
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
    }
}
