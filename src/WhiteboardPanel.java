import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class WhiteboardPanel extends JPanel {
    private final ObjectOutputStream out;
    private final List<List<DrawCommand>> actions = new ArrayList<>();
    private final Stack<List<DrawCommand>> undoStack = new Stack<>();
    private Color currentColor = Color.BLACK;
    private String currentMode = "DRAW";
    private int fontSize = 16;
    private int prevX, prevY;
    private List<DrawCommand> currentAction;

    public WhiteboardPanel(ObjectOutputStream out) {
        this.out = out;
        setBackground(Color.WHITE);

        // Mouse Listener for drawing or text
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                prevX = e.getX();
                prevY = e.getY();
                currentAction = new ArrayList<>(); // Start a new action

                if (currentMode.equals("TEXT")) {
                    String text = JOptionPane.showInputDialog("Enter text:");
                    if (text != null && !text.trim().isEmpty()) {
                        DrawCommand command = new DrawCommand(e.getX(), e.getY(), text, currentColor, fontSize);
                        sendCommand(command);
                        currentAction.add(command);
                        actions.add(currentAction); // Add the text action
                        currentAction = null; // End the action immediately
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (currentMode.equals("DRAW") && currentAction != null) {
                    actions.add(currentAction); // Add the completed drawing action
                    currentAction = null;
                }
            }
        });

        // Mouse Motion Listener for smooth drawing
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (currentMode.equals("DRAW")) {
                    int x = e.getX();
                    int y = e.getY();
                    DrawCommand command = new DrawCommand(prevX, prevY, x, y, currentColor);
                    sendCommand(command);
                    currentAction.add(command); // Group drawing commands into one action
                    prevX = x;
                    prevY = y;
                }
            }
        });
    }

    public void updateDrawing(DrawCommand command) {
        synchronized (actions) {
            if (command.isClear) {
                actions.clear();
                undoStack.clear();
            } else if (command.isUndo) {
                if (!actions.isEmpty()) {
                    undoStack.push(actions.remove(actions.size() - 1));
                }
            } else {
                if (actions.isEmpty() || command.isNewAction) {
                    actions.add(new ArrayList<>());
                }
                actions.get(actions.size() - 1).add(command);
            }
        }
        repaint();
    }

    private void sendCommand(DrawCommand command) {
        try {
            out.writeObject(command);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (actions) {
            if (command.isClear) {
                actions.clear();
                undoStack.clear();
            } else if (command.isUndo) {
                if (!actions.isEmpty()) {
                    undoStack.push(actions.remove(actions.size() - 1));
                }
            } else {
                if (currentAction != null) {
                    currentAction.add(command);
                }
            }
        }
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        synchronized (actions) {
            for (List<DrawCommand> action : actions) {
                for (DrawCommand command : action) {
                    g2d.setColor(command.color);
                    if (command.text != null) {
                        g2d.setFont(new Font("Arial", Font.PLAIN, command.fontSize));
                        g2d.drawString(command.text, command.x, command.y);
                    } else {
                        g2d.drawLine(command.x, command.y, command.endX, command.endY);
                    }
                }
            }
        }
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode;
    }

    public void setFontSize(int size) {
        this.fontSize = size;
    }

    public void clearWhiteboard() {
        sendCommand(new DrawCommand(true)); // Send clear command
    }

    public void undoLastAction() {
        sendCommand(new DrawCommand(false, true)); // Send undo command
    }
}

// Serializable command for drawing and text
class DrawCommand implements Serializable {
    int x, y, endX, endY;
    String text;
    Color color;
    int fontSize;
    boolean isClear;
    boolean isUndo;
    boolean isNewAction;

    // Constructor for line drawing commands
    public DrawCommand(int x, int y, int endX, int endY, Color color) {
        this(x, y, null, color, 0);
        this.endX = endX;
        this.endY = endY;
    }

    // Constructor for text commands
    public DrawCommand(int x, int y, String text, Color color, int fontSize) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.fontSize = fontSize;
        this.isClear = false;
        this.isUndo = false;
        this.isNewAction = false;
    }

    // Constructor for Clear or Undo commands
    public DrawCommand(boolean isClear) {
        this.isClear = isClear;
        this.isUndo = false;
    }

    public DrawCommand(boolean isClear, boolean isUndo) {
        this.isClear = isClear;
        this.isUndo = isUndo;
    }
}
