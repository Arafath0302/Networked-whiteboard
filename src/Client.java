import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Connect to the server
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Create the GUI
                JFrame frame = new JFrame("Enhanced Whiteboard");
                WhiteboardPanel panel = new WhiteboardPanel(out);

                // Toolbar for options
                JToolBar toolbar = new JToolBar();

                // Color Picker Button
                JButton colorButton = new JButton("Color");
                colorButton.addActionListener(e -> {
                    Color selectedColor = JColorChooser.showDialog(frame, "Choose Color", Color.BLACK);
                    if (selectedColor != null) {
                        panel.setCurrentColor(selectedColor);
                    }
                });

                // Draw Button
                JButton drawButton = new JButton("Draw");
                drawButton.addActionListener(e -> panel.setCurrentMode("DRAW"));

                // Text Button
                JButton textButton = new JButton("Text");
                textButton.addActionListener(e -> panel.setCurrentMode("TEXT"));

                // Font Size Selector
                JComboBox<Integer> fontSizeCombo = new JComboBox<>(new Integer[]{12, 16, 20, 24, 30});
                fontSizeCombo.addActionListener(e -> panel.setFontSize((Integer) fontSizeCombo.getSelectedItem()));

                // Clear Button
                JButton clearButton = new JButton("Clear");
                clearButton.addActionListener(e -> panel.clearWhiteboard());

                // Undo Button
                JButton undoButton = new JButton("Undo");
                undoButton.addActionListener(e -> panel.undoLastAction());

                // Add buttons to toolbar
                toolbar.add(colorButton);
                toolbar.add(drawButton);
                toolbar.add(textButton);
                toolbar.add(new JLabel("Font Size:"));
                toolbar.add(fontSizeCombo);
                toolbar.add(clearButton);
                toolbar.add(undoButton);

                // Add components to frame
                frame.add(toolbar, BorderLayout.NORTH);
                frame.add(panel, BorderLayout.CENTER);
                frame.setSize(800, 600);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

                // Thread to handle incoming commands
                new Thread(() -> {
                    try {
                        Object message;
                        while ((message = in.readObject()) != null) {
                            panel.updateDrawing((DrawCommand) message);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
