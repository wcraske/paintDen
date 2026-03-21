import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

class UserInterface implements ActionListener, Client.MessageListener {

    Client client;
    private boolean usernameSet = false;
    private String username = null;
    private DataBaseManager db;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private DrawingCanvas canvas;

    public UserInterface() {
        frame = new JFrame("ฅʕ՞•ﻌ•՞ʔฅ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setMinimumSize(new Dimension(700, 450));
        frame.setLocationRelativeTo(null);

        // ── Left: drawing panel ──────────────────────────────────────────────
        canvas = new DrawingCanvas();

        JSlider brushSlider = new JSlider(1, 20, 4);
        brushSlider.addChangeListener(e -> canvas.brushSize = brushSlider.getValue());

        JPanel palette = new JPanel(new GridLayout(2, 6, 3, 3));
        Color[] colors = { Color.BLACK, Color.WHITE, Color.RED, Color.ORANGE,
                           Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE,
                           Color.MAGENTA, Color.PINK, Color.GRAY, Color.DARK_GRAY };
        for (Color c : colors) {
            JButton btn = new JButton();
            btn.setBackground(c);
            btn.setPreferredSize(new Dimension(24, 24));
            btn.setBorderPainted(false);
            btn.addActionListener(e -> canvas.drawColor = c);
            palette.add(btn);
        }

        JButton eraseBtn = new JButton("Undo");
        eraseBtn.addActionListener(e -> canvas.undo());

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> canvas.clear());

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> onSendDrawing());

        JPanel tools = new JPanel(new GridLayout(1, 3, 4, 0));
        tools.add(eraseBtn);
        tools.add(clearBtn);
        tools.add(sendBtn);

        JPanel bottomLeft = new JPanel(new BorderLayout(0, 4));
        bottomLeft.add(brushSlider, BorderLayout.NORTH);
        bottomLeft.add(tools, BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setPreferredSize(new Dimension(220, 0));
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        leftPanel.add(palette, BorderLayout.NORTH);
        leftPanel.add(canvas, BorderLayout.CENTER);
        leftPanel.add(bottomLeft, BorderLayout.SOUTH);

        // ── Right: chat panel ────────────────────────────────────────────────
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        chatArea.setMargin(new Insets(8, 8, 8, 8));

        messageField = new JTextField();
        messageField.addActionListener(this);
        JButton sendText = new JButton("Send");
        sendText.addActionListener(this);

        JPanel inputBar = new JPanel(new BorderLayout(6, 0));
        inputBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        inputBar.add(messageField, BorderLayout.CENTER);
        inputBar.add(sendText, BorderLayout.EAST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        rightPanel.add(inputBar, BorderLayout.SOUTH);

        // ── Frame ────────────────────────────────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(220);
        split.setDividerSize(1);
        split.setEnabled(false);
        frame.add(split);
        frame.setVisible(true);

        db = new DataBaseManager();
        username = db.getUsername();
        if (username == null) {
            chatArea.append("Enter your username: \n");
        } else {
            usernameSet = true;
            client = new Client(this);
            client.sendMessage(username);
            frame.setTitle("ฅʕ՞•ﻌ•՞ʔฅ");
        }

        updateTitleBlink();
    }

    // ── Stub ─────────────────────────────────────────────────────────────────

    private void onSendDrawing() {
        BufferedImage image = canvas.getImage();
        // TODO: encode to PNG/Base64 and send via client
    }

    // ── Random title blink animation ─────────────────────────────────────────

    private void updateTitleBlink() {
        String blinking = "ฅʕ՞–ﻌ–՞ʔฅ";
        String normal   = "ฅʕ՞•ﻌ•՞ʔฅ";

        new Thread(() -> {
            try {
                while (true) {
                    double rnd = Math.random();
                    if (rnd < 0.01) {
                        SwingUtilities.invokeLater(() -> frame.setTitle(blinking));
                        Thread.sleep(150);
                        SwingUtilities.invokeLater(() -> frame.setTitle(normal));
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ── Text chat ────────────────────────────────────────────────────────────

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (!msg.isEmpty()) {
            client.sendMessage(msg);
            chatArea.append(username + ": " + msg + "\n");
            messageField.setText("");
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (!usernameSet) {
            username = messageField.getText().trim();
            db.setUsername(username);
            usernameSet = true;
            client = new Client(this);
            client.sendMessage(username);
        } else {
            sendMessage();
        }
    }

    @Override public void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UserInterface::new);
    }

    // ── Drawing canvas ───────────────────────────────────────────────────────

    static class DrawingCanvas extends JPanel {
        Color drawColor = Color.BLACK;
        int brushSize = 4;
        private BufferedImage image;
        private Graphics2D g2;
        private int lx, ly;
        private int cursorX = -1, cursorY = -1;
        private boolean pressing = false;
        // Undo stack: snapshot before each stroke begins
        private final ArrayDeque<BufferedImage> undoStack = new ArrayDeque<>();

        DrawingCanvas() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            MouseAdapter m = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    saveUndo();
                    pressing = true;
                    lx = e.getX(); ly = e.getY();
                    cursorX = e.getX(); cursorY = e.getY();
                    dot(e); repaint();
                }
                public void mouseDragged(MouseEvent e) {
                    stroke(e); lx = e.getX(); ly = e.getY();
                    cursorX = e.getX(); cursorY = e.getY(); repaint();
                }
                public void mouseReleased(MouseEvent e) { pressing = false; repaint(); }
                public void mouseMoved(MouseEvent e)    { cursorX = e.getX(); cursorY = e.getY(); repaint(); }
                public void mouseExited(MouseEvent e)   { cursorX = -1; cursorY = -1; repaint(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private void saveUndo() {
            ensure();
            BufferedImage snap = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            snap.createGraphics().drawImage(image, 0, 0, null);
            undoStack.push(snap);
            if (undoStack.size() > 50) undoStack.pollLast(); 
        }

        void undo() {
            if (!undoStack.isEmpty()) {
                image = undoStack.pop();
                g2 = image.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                repaint();
            }
        }

        private void dot(MouseEvent e) {
            ensure();
            g2.setColor(drawColor);
            g2.fillOval(e.getX() - brushSize / 2, e.getY() - brushSize / 2, brushSize, brushSize);
        }

        private void stroke(MouseEvent e) {
            ensure();
            g2.setColor(drawColor);
            g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(lx, ly, e.getX(), e.getY());
        }

        void clear() {
            saveUndo();
            ensure();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, image.getWidth(), image.getHeight());
            repaint();
        }

        BufferedImage getImage() { ensure(); return image; }

        private void ensure() {
            if (image == null || image.getWidth() != getWidth() || image.getHeight() != getHeight()) {
                BufferedImage n = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
                Graphics2D ng = n.createGraphics();
                ng.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ng.setColor(Color.WHITE);
                ng.fillRect(0, 0, n.getWidth(), n.getHeight());
                if (image != null) ng.drawImage(image, 0, 0, null);
                ng.dispose();
                if (g2 != null) g2.dispose();
                image = n;
                g2 = image.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) g.drawImage(image, 0, 0, null);
            // Draw brush preview circle
            if (cursorX >= 0) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = brushSize;
                // filled preview when pressing, outline otherwise
                if (pressing) {
                    g2d.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), 120));
                    g2d.fillOval(cursorX - r / 2, cursorY - r / 2, r, r);
                }
                g2d.setColor(Color.DARK_GRAY);
                g2d.setStroke(new BasicStroke(1f));
                g2d.drawOval(cursorX - r / 2, cursorY - r / 2, r, r);
                g2d.dispose();
            }
        }
    }
}