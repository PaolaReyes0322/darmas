import javax.swing.*;
import java.awt.*;

public class MainContentPanel extends JPanel {

    private final JTabbedPane tabbedPane;

    public MainContentPanel(int userId, String userType, AppFrame appFrame) {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();

        // Se decide qué pestañas mostrar según el tipo de usuario
        switch (userType) {
            case "administrador":
                // Pestañas para el Administrador
                tabbedPane.addTab("Reportes del Sistema", new ReportsPanel());
                tabbedPane.addTab("Auditoría", new AuditPanel());
                break;
            case "coordinador":
                // Pestañas para el Coordinador
                
                tabbedPane.addTab("Control de Stock", new StockControlPanel());
                tabbedPane.addTab("Validación de Solicitudes", new RequestValidationPanel(appFrame));
                break;
            case "voluntario":
                // Pestañas para el Voluntario
                tabbedPane.addTab("Registro de Entrega", new DeliveryPanel(userId));
                tabbedPane.addTab("Reportes del Sistema", new ReportsPanel());
                break;
            case "donante":
                // Pestañas para el Donante
                tabbedPane.addTab("Registro de Donación", new DonationRegistrationPanel());
                break;
            case "beneficiario":
                // Pestañas para el Beneficiario
                // Como mencionaste que el beneficiario solo llena la descripción,
                // usaríamos el mismo panel de entrega, pero con campos de solo lectura.
                // Esta es una implementación conceptual.
                tabbedPane.addTab("Crear Solicitud", new RequestPanel(userId, appFrame));
                break;
            default:
                JOptionPane.showMessageDialog(this, "Tipo de usuario no válido.", "Error", JOptionPane.ERROR_MESSAGE);
                break;
        }

        // Botón de cerrar sesión
        JButton logoutButton = new JButton("Cerrar Sesión");
        logoutButton.addActionListener(e -> appFrame.showLoginPanel());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("  ID de Usuario: " + userId + " | Rol: " + userType);
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }
}
