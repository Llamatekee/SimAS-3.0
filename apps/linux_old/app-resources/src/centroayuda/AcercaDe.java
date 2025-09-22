package centroayuda;

import java.awt.*;
import javax.swing.*;

public class AcercaDe extends JDialog {

    public AcercaDe(JFrame parent) {
        super(parent, "Acerca de SimAS", true);
        initComponents();
        setLocationRelativeTo(parent); // Centrar respecto a la ventana principal
    }

    private void initComponents() {
        // Crear el panel principal con BoxLayout para una disposición vertical
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new BoxLayout(panelPrincipal, BoxLayout.Y_AXIS));
        panelPrincipal.setBackground(Color.WHITE);
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Crear los labels y el botón de cierre
        JLabel labelUniCordoba = crearLabel("Universidad de Córdoba", 24, true);
        JLabel labelEscuela = crearLabel("Escuela Politécnica Superior de Córdoba", 24, true);
        JLabel labelCarrera = crearLabel("Ingeniería Informática", 24, true);

        JLabel logo = new JLabel(new ImageIcon(getClass().getResource("/es/uco/simas/resources/logo2Antes.png")));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);  // Centrar el logo

            JLabel labelDesarrolladoPor = crearLabel("Desarrollado por: Antonio Llamas García", 18, false);
        JLabel labelDirigidoPor = crearLabel("Dirigido por: Dr. Nicolás Luis Fernández García", 18, false);
        JLabel labelVersion = crearLabel("Versión: 3.0", 18, false);
        JLabel labelFecha = crearLabel("Fecha: Septiembre 2024", 18, false);

        JButton botonCerrar = new JButton("Cerrar");
        botonCerrar.setFont(new Font("SansSerif", Font.PLAIN, 16));
        botonCerrar.setAlignmentX(Component.CENTER_ALIGNMENT);
        botonCerrar.addActionListener(e -> dispose());  // Cerrar la ventana al hacer clic

        // Añadir todos los componentes al panel principal
        panelPrincipal.add(labelUniCordoba);
        panelPrincipal.add(Box.createVerticalStrut(10)); // Espacio entre elementos
        panelPrincipal.add(labelEscuela);
        panelPrincipal.add(Box.createVerticalStrut(10));
        panelPrincipal.add(labelCarrera);
        panelPrincipal.add(Box.createVerticalStrut(20));
        panelPrincipal.add(logo);
        panelPrincipal.add(Box.createVerticalStrut(20));
        panelPrincipal.add(labelDesarrolladoPor);
        panelPrincipal.add(Box.createVerticalStrut(10));
        panelPrincipal.add(labelDirigidoPor);
        panelPrincipal.add(Box.createVerticalStrut(10));
        panelPrincipal.add(labelVersion);
        panelPrincipal.add(Box.createVerticalStrut(10));
        panelPrincipal.add(labelFecha);
        panelPrincipal.add(Box.createVerticalStrut(20));
        panelPrincipal.add(botonCerrar);

        // Configurar la ventana
        setContentPane(panelPrincipal);
        setSize(800, 600);  // Tamaño mayor para acomodar todos los elementos
        setResizable(false);
    }

    private JLabel crearLabel(String texto, int fontSize, boolean bold) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, fontSize));
        label.setForeground(new Color(33, 77, 72));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);  // Centrar el texto
        return label;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AcercaDe(null).setVisible(true);
        });
    }
}
