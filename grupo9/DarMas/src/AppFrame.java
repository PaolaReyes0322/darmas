import javax.swing.*;
import java.awt.*;

public class AppFrame extends JFrame {
    
    // Nombres para las "tarjetas" del CardLayout
    private static final String LOGIN_PANEL = "LoginPanel";
    private static final String USER_MANAGEMENT_PANEL = "UserManagementPanel";
    private static final String MAIN_CONTENT_PANEL = "MainContentPanel";
    
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    public AppFrame() {
        setTitle("Sistema Darmas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new LoginPanel(this), LOGIN_PANEL);
        mainPanel.add(new UserManagementPanel(this), USER_MANAGEMENT_PANEL);
    
        add(mainPanel);
        
        showLoginPanel();
    }
    
    public void showLoginPanel() {
        cardLayout.show(mainPanel, LOGIN_PANEL);
    }
    
    public void showUserManagementPanel() { // Método renombrado
        cardLayout.show(mainPanel, USER_MANAGEMENT_PANEL);
    }
    
    public void showMainContentPanel(int userId, String userType) {
        mainPanel.add(new MainContentPanel(userId, userType, this), MAIN_CONTENT_PANEL);
        cardLayout.show(mainPanel, MAIN_CONTENT_PANEL);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AppFrame().setVisible(true);
        });
    }
}
