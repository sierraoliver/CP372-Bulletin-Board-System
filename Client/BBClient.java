import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * BBClient - GUI Client for the CP372 Bulletin Board System.
 *
 * Connects to a BBoard server via TCP sockets and provides a graphical
 * interface for all bulletin board operations: POST, GET, PIN, UNPIN,
 * SHAKE, CLEAR, and DISCONNECT.
 *
 * Primary Responsibility: Ayla Topuz
 */
public class BBClient extends JFrame {

    // ======================== Connection State ========================
    private Socket socket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private boolean connected = false;

    // ======================== Server Config (received on handshake) ====
    private int boardWidth;
    private int boardHeight;
    private int noteWidth;
    private int noteHeight;
    private String[] validColors;

    // ======================== GUI Components ==========================

    // --- Connection Panel ---
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;

    // --- POST Panel ---
    private JTextField postXField;
    private JTextField postYField;
    private JComboBox<String> postColorBox;
    private JTextField postMessageField;
    private JButton postButton;

    // --- GET Panel ---
    private JCheckBox getColorCheck;
    private JComboBox<String> getColorBox;
    private JCheckBox getContainsCheck;
    private JTextField getContainsXField;
    private JTextField getContainsYField;
    private JCheckBox getRefersToCheck;
    private JTextField getRefersToField;
    private JButton getButton;
    private JButton getPinsButton;
    private JButton getAllButton;

    // --- PIN / UNPIN Panel ---
    private JTextField pinXField;
    private JTextField pinYField;
    private JButton pinButton;
    private JButton unpinButton;

    // --- Action Buttons ---
    private JButton shakeButton;
    private JButton clearButton;

    // --- Output Area ---
    private JTextPane outputPane;
    private StyledDocument outputDoc;

    // ======================== Color Constants =========================
    private static final Color BG_COLOR = new Color(245, 245, 248);
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color ACCENT = new Color(59, 130, 246);
    private static final Color ACCENT_HOVER = new Color(37, 99, 235);
    private static final Color ERROR_COLOR = new Color(220, 38, 38);
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74);
    private static final Color DISCONNECT_COLOR = new Color(107, 114, 128);
    private static final Color SENT_COLOR = new Color(79, 70, 229);
    private static final Color TIMESTAMP_COLOR = new Color(156, 163, 175);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);
    private static final Color DANGER_HOVER = new Color(220, 38, 38);
    private static final Color WARNING_COLOR = new Color(234, 179, 8);
    private static final Color WARNING_HOVER = new Color(202, 138, 4);

    // ======================== Constructor =============================
    public BBClient() {
        super("Bulletin Board Client");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handleWindowClose();
            }
        });
        setSize(920, 720);
        setMinimumSize(new Dimension(780, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);

        initStyles();
        buildUI();
        setCommandsEnabled(false);
    }

    // ======================== UI Construction =========================

    private void initStyles() {
        // Pre-create styled document for output pane
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        // Left side: controls
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(BG_COLOR);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 6));
        controlPanel.setPreferredSize(new Dimension(380, 0));

        controlPanel.add(buildConnectionPanel());
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buildPostPanel());
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buildGetPanel());
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buildPinPanel());
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buildActionsPanel());

        // Wrap in scroll pane in case window is small
        JScrollPane controlScroll = new JScrollPane(controlPanel);
        controlScroll.setBorder(null);
        controlScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Right side: output
        JPanel outputPanel = buildOutputPanel();

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlScroll, outputPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.0);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);
    }

    // --- Connection Panel ---
    private JPanel buildConnectionPanel() {
        JPanel panel = createSectionPanel("Connection");

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // IP
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        fieldsPanel.add(createLabel("IP:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        ipField = createTextField("localhost", 12);
        fieldsPanel.add(ipField, gbc);

        // Port
        gbc.gridx = 2; gbc.weightx = 0;
        fieldsPanel.add(createLabel("Port:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        portField = createTextField("4554", 6);
        fieldsPanel.add(portField, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.setOpaque(false);
        connectButton = createStyledButton("Connect", ACCENT, ACCENT_HOVER, Color.WHITE);
        disconnectButton = createStyledButton("Disconnect", DISCONNECT_COLOR, new Color(75, 85, 99), Color.WHITE);
        disconnectButton.setEnabled(false);
        btnPanel.add(connectButton);
        btnPanel.add(disconnectButton);

        // Status
        statusLabel = new JLabel("● Disconnected");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(ERROR_COLOR);
        btnPanel.add(Box.createHorizontalStrut(8));
        btnPanel.add(statusLabel);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 4, 2, 4);
        fieldsPanel.add(btnPanel, gbc);

        panel.add(fieldsPanel);

        // Action Listeners
        connectButton.addActionListener(e -> handleConnect());
        disconnectButton.addActionListener(e -> handleDisconnect());

        // Enter key in port field triggers connect
        portField.addActionListener(e -> handleConnect());

        return panel;
    }

    // --- POST Panel ---
    private JPanel buildPostPanel() {
        JPanel panel = createSectionPanel("Post Note");

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // X, Y coordinates
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        fieldsPanel.add(createLabel("X:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        postXField = createTextField("", 5);
        fieldsPanel.add(postXField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        fieldsPanel.add(createLabel("Y:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        postYField = createTextField("", 5);
        fieldsPanel.add(postYField, gbc);

        // Color
        gbc.gridx = 4; gbc.weightx = 0;
        fieldsPanel.add(createLabel("Color:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.4;
        postColorBox = new JComboBox<>(new String[]{"(connect first)"});
        postColorBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fieldsPanel.add(postColorBox, gbc);

        // Message
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        fieldsPanel.add(createLabel("Message:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 5; gbc.weightx = 1.0;
        postMessageField = createTextField("", 20);
        fieldsPanel.add(postMessageField, gbc);

        // Post button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 6;
        gbc.insets = new Insets(6, 4, 2, 4);
        postButton = createStyledButton("Post Note", ACCENT, ACCENT_HOVER, Color.WHITE);
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(postButton);
        fieldsPanel.add(btnWrap, gbc);

        panel.add(fieldsPanel);

        postButton.addActionListener(e -> handlePost());
        postMessageField.addActionListener(e -> handlePost());

        return panel;
    }

    // --- GET Panel ---
    private JPanel buildGetPanel() {
        JPanel panel = createSectionPanel("Query Notes");

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // color filter
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        getColorCheck = new JCheckBox("color=");
        getColorCheck.setOpaque(false);
        getColorCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fieldsPanel.add(getColorCheck, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        getColorBox = new JComboBox<>(new String[]{"(connect first)"});
        getColorBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        getColorBox.setEnabled(false);
        fieldsPanel.add(getColorBox, gbc);

        // contains filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        getContainsCheck = new JCheckBox("contains=");
        getContainsCheck.setOpaque(false);
        getContainsCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fieldsPanel.add(getContainsCheck, gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        getContainsXField = createTextField("", 5);
        getContainsXField.setEnabled(false);
        fieldsPanel.add(getContainsXField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.5;
        getContainsYField = createTextField("", 5);
        getContainsYField.setEnabled(false);
        fieldsPanel.add(getContainsYField, gbc);

        // refersTo filter
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        getRefersToCheck = new JCheckBox("refersTo=");
        getRefersToCheck.setOpaque(false);
        getRefersToCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fieldsPanel.add(getRefersToCheck, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        getRefersToField = createTextField("", 15);
        getRefersToField.setEnabled(false);
        fieldsPanel.add(getRefersToField, gbc);

        // Toggle enable/disable of filter fields based on checkboxes
        getColorCheck.addActionListener(e -> getColorBox.setEnabled(getColorCheck.isSelected()));
        getContainsCheck.addActionListener(e -> {
            getContainsXField.setEnabled(getContainsCheck.isSelected());
            getContainsYField.setEnabled(getContainsCheck.isSelected());
        });
        getRefersToCheck.addActionListener(e -> getRefersToField.setEnabled(getRefersToCheck.isSelected()));

        // Buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        gbc.insets = new Insets(6, 4, 2, 4);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.setOpaque(false);
        getButton = createStyledButton("Search", ACCENT, ACCENT_HOVER, Color.WHITE);
        getPinsButton = createStyledButton("Get Pins", ACCENT, ACCENT_HOVER, Color.WHITE);
        getAllButton = createStyledButton("Get All", ACCENT, ACCENT_HOVER, Color.WHITE);
        btnPanel.add(getButton);
        btnPanel.add(getPinsButton);
        btnPanel.add(getAllButton);
        fieldsPanel.add(btnPanel, gbc);

        panel.add(fieldsPanel);

        getButton.addActionListener(e -> handleGet());
        getPinsButton.addActionListener(e -> handleGetPins());
        getAllButton.addActionListener(e -> handleGetAll());

        return panel;
    }

    // --- PIN / UNPIN Panel ---
    private JPanel buildPinPanel() {
        JPanel panel = createSectionPanel("Pin / Unpin");

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        fieldsPanel.add(createLabel("X:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        pinXField = createTextField("", 5);
        fieldsPanel.add(pinXField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        fieldsPanel.add(createLabel("Y:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        pinYField = createTextField("", 5);
        fieldsPanel.add(pinYField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        gbc.insets = new Insets(6, 4, 2, 4);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.setOpaque(false);
        pinButton = createStyledButton("Pin", ACCENT, ACCENT_HOVER, Color.WHITE);
        unpinButton = createStyledButton("Unpin", ACCENT, ACCENT_HOVER, Color.WHITE);
        btnPanel.add(pinButton);
        btnPanel.add(unpinButton);
        fieldsPanel.add(btnPanel, gbc);

        panel.add(fieldsPanel);

        pinButton.addActionListener(e -> handlePin());
        unpinButton.addActionListener(e -> handleUnpin());

        return panel;
    }

    // --- SHAKE / CLEAR Panel ---
    private JPanel buildActionsPanel() {
        JPanel panel = createSectionPanel("Board Actions");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnPanel.setOpaque(false);
        shakeButton = createStyledButton("Shake", WARNING_COLOR, WARNING_HOVER, Color.WHITE);
        clearButton = createStyledButton("Clear Board", DANGER_COLOR, DANGER_HOVER, Color.WHITE);
        btnPanel.add(shakeButton);
        btnPanel.add(clearButton);

        panel.add(btnPanel);

        shakeButton.addActionListener(e -> handleShake());
        clearButton.addActionListener(e -> handleClear());

        return panel;
    }

    // --- Output Panel ---
    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 12));

        JLabel title = new JLabel("Server Output");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 0));

        outputPane = new JTextPane();
        outputPane.setEditable(false);
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputPane.setBackground(new Color(30, 30, 30));
        outputPane.setForeground(new Color(212, 212, 216));
        outputPane.setCaretColor(Color.WHITE);
        outputDoc = outputPane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(outputPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        // Clear output button
        JButton clearOutputBtn = new JButton("Clear Log");
        clearOutputBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        clearOutputBtn.addActionListener(e -> {
            try { outputDoc.remove(0, outputDoc.getLength()); } catch (Exception ex) {}
        });

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(clearOutputBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ======================== UI Helpers ==============================

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 225)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        panel.add(lbl);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return lbl;
    }

    private JTextField createTextField(String defaultText, int columns) {
        JTextField field = new JTextField(defaultText, columns);
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setMargin(new Insets(4, 6, 4, 6));
        return field;
    }

    private JButton createStyledButton(String text, Color bg, Color hoverBg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(6, 14, 6, 14));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverBg);
            }
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bg);
            }
        });

        return btn;
    }

    private void setCommandsEnabled(boolean enabled) {
        postButton.setEnabled(enabled);
        getButton.setEnabled(enabled);
        getPinsButton.setEnabled(enabled);
        getAllButton.setEnabled(enabled);
        pinButton.setEnabled(enabled);
        unpinButton.setEnabled(enabled);
        shakeButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    // ======================== Output Logging ==========================

    private void appendOutput(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);
                StyleConstants.setFontFamily(attrs, "Monospaced");
                StyleConstants.setFontSize(attrs, 12);

                String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                SimpleAttributeSet tsAttrs = new SimpleAttributeSet();
                StyleConstants.setForeground(tsAttrs, TIMESTAMP_COLOR);
                StyleConstants.setFontFamily(tsAttrs, "Monospaced");
                StyleConstants.setFontSize(tsAttrs, 11);

                outputDoc.insertString(outputDoc.getLength(), "[" + timestamp + "] ", tsAttrs);
                outputDoc.insertString(outputDoc.getLength(), text + "\n", attrs);

                // Auto-scroll to bottom
                outputPane.setCaretPosition(outputDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void logSent(String command) {
        appendOutput("→ " + command, SENT_COLOR);
    }

    private void logReceived(String response) {
        Color color;
        if (response.startsWith("OK")) {
            color = SUCCESS_COLOR;
        } else if (response.startsWith("ERROR")) {
            color = ERROR_COLOR;
        } else if (response.startsWith("NOTE") || response.startsWith("PIN")) {
            color = new Color(147, 197, 253); // light blue for data lines
        } else if (response.equals("NO_RESULTS")) {
            color = new Color(253, 224, 71); // yellow for no results
        } else {
            color = new Color(212, 212, 216); // default light gray
        }
        appendOutput("← " + response, color);
    }

    private void logInfo(String message) {
        appendOutput("ℹ " + message, new Color(156, 163, 175));
    }

    private void logError(String message) {
        appendOutput("✗ " + message, ERROR_COLOR);
    }

    private void logSuccess(String message) {
        appendOutput("✓ " + message, SUCCESS_COLOR);
    }

    // ======================== Connection Handling =====================

    private void handleConnect() {
        if (connected) {
            logError("Already connected.");
            return;
        }

        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        // Validate IP
        if (ip.isEmpty()) {
            logError("Please enter a server IP address.");
            return;
        }

        // Validate port
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                logError("Port must be between 1 and 65535.");
                return;
            }
        } catch (NumberFormatException ex) {
            logError("Invalid port number.");
            return;
        }

        // Attempt connection in background thread to avoid freezing GUI
        connectButton.setEnabled(false);
        statusLabel.setText("● Connecting...");
        statusLabel.setForeground(WARNING_COLOR);

        new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 5000); // 5 second timeout
                serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                serverOut = new PrintWriter(socket.getOutputStream(), true);

                // Read handshake from server
                // Server sends 4 lines:
                //   BOARD <width> <height>
                //   NOTE_SIZE <width> <height>
                //   COLORS <c1> <c2> ...
                //   READY
                String boardLine = serverIn.readLine();
                String noteLine = serverIn.readLine();
                String colorLine = serverIn.readLine();
                String readyLine = serverIn.readLine();

                if (boardLine == null || noteLine == null || colorLine == null || readyLine == null) {
                    throw new IOException("Server closed connection during handshake.");
                }

                // Parse "BOARD 1000 700"
                String[] boardParts = boardLine.trim().split("\\s+");
                boardWidth = Integer.parseInt(boardParts[1]);
                boardHeight = Integer.parseInt(boardParts[2]);

                // Parse "NOTE_SIZE 50 150"
                String[] noteParts = noteLine.trim().split("\\s+");
                noteWidth = Integer.parseInt(noteParts[1]);
                noteHeight = Integer.parseInt(noteParts[2]);

                // Parse "COLORS red yellow green blue"
                String[] colorParts = colorLine.trim().split("\\s+");
                validColors = new String[colorParts.length - 1];
                System.arraycopy(colorParts, 1, validColors, 0, colorParts.length - 1);

                connected = true;

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("● Connected");
                    statusLabel.setForeground(SUCCESS_COLOR);
                    connectButton.setEnabled(false);
                    disconnectButton.setEnabled(true);
                    ipField.setEnabled(false);
                    portField.setEnabled(false);
                    setCommandsEnabled(true);

                    // Populate color dropdowns
                    postColorBox.removeAllItems();
                    getColorBox.removeAllItems();
                    for (String c : validColors) {
                        postColorBox.addItem(c);
                        getColorBox.addItem(c);
                    }

                    logSuccess("Connected to " + ip + ":" + port);
                    logInfo("Board: " + boardWidth + "x" + boardHeight
                          + " | Note: " + noteWidth + "x" + noteHeight
                          + " | Colors: " + String.join(", ", validColors));
                });

            } catch (SocketTimeoutException ex) {
                SwingUtilities.invokeLater(() -> {
                    logError("Connection timed out. Is the server running?");
                    resetConnectionUI();
                });
            } catch (ConnectException ex) {
                SwingUtilities.invokeLater(() -> {
                    logError("Cannot connect to server. Connection refused.");
                    resetConnectionUI();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    logError("Connection failed: " + ex.getMessage());
                    resetConnectionUI();
                });
            }
        }).start();
    }

    private void handleDisconnect() {
        if (!connected) return;
        sendCommand("DISCONNECT");
        cleanup();
        logInfo("Disconnected from server.");
    }

    private void handleWindowClose() {
        if (connected) {
            try {
                serverOut.println("DISCONNECT");
            } catch (Exception e) { /* ignore */ }
            cleanup();
        }
        dispose();
        System.exit(0);
    }

    private void cleanup() {
        connected = false;
        try { if (serverIn != null) serverIn.close(); } catch (Exception e) {}
        try { if (serverOut != null) serverOut.close(); } catch (Exception e) {}
        try { if (socket != null) socket.close(); } catch (Exception e) {}
        serverIn = null;
        serverOut = null;
        socket = null;

        SwingUtilities.invokeLater(() -> {
            resetConnectionUI();
        });
    }

    private void resetConnectionUI() {
        statusLabel.setText("● Disconnected");
        statusLabel.setForeground(ERROR_COLOR);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        ipField.setEnabled(true);
        portField.setEnabled(true);
        setCommandsEnabled(false);
    }

    // ======================== Command Handlers ========================

    private void handlePost() {
        if (!connected) return;

        String xStr = postXField.getText().trim();
        String yStr = postYField.getText().trim();
        String color = (String) postColorBox.getSelectedItem();
        String message = postMessageField.getText().trim();

        // --- Client-side validation ---
        if (xStr.isEmpty() || yStr.isEmpty()) {
            logError("POST: X and Y coordinates are required.");
            return;
        }

        int x, y;
        try {
            x = Integer.parseInt(xStr);
            y = Integer.parseInt(yStr);
        } catch (NumberFormatException ex) {
            logError("POST: Coordinates must be integers.");
            return;
        }

        if (x < 0 || y < 0) {
            logError("POST: Coordinates must be non-negative.");
            return;
        }

        // Check bounds using known board and note dimensions
        if (x + noteWidth > boardWidth || y + noteHeight > boardHeight) {
            logError("POST: Note would exceed board boundaries ("
                + boardWidth + "x" + boardHeight + ").");
            return;
        }

        if (color == null || color.equals("(connect first)")) {
            logError("POST: Please select a valid color.");
            return;
        }

        if (message.isEmpty()) {
            logError("POST: Message cannot be empty.");
            return;
        }

        if (message.contains("\n")) {
            logError("POST: Message cannot contain newline characters.");
            return;
        }

        String command = "POST " + x + " " + y + " " + color + " " + message;
        String response = sendCommand(command);
        if (response != null) {
            postMessageField.setText("");
        }
    }

    private void handleGet() {
        if (!connected) return;

        StringBuilder cmd = new StringBuilder("GET");

        if (getColorCheck.isSelected()) {
            String color = (String) getColorBox.getSelectedItem();
            if (color == null || color.equals("(connect first)")) {
                logError("GET: Select a valid color.");
                return;
            }
            cmd.append(" color=").append(color);
        }

        if (getContainsCheck.isSelected()) {
            String cx = getContainsXField.getText().trim();
            String cy = getContainsYField.getText().trim();
            if (cx.isEmpty() || cy.isEmpty()) {
                logError("GET: Contains filter requires both X and Y.");
                return;
            }
            try {
                int xi = Integer.parseInt(cx);
                int yi = Integer.parseInt(cy);
                if (xi < 0 || yi < 0) {
                    logError("GET: Coordinates must be non-negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                logError("GET: Contains coordinates must be integers.");
                return;
            }
            cmd.append(" contains=").append(cx).append(" ").append(cy);
        }

        if (getRefersToCheck.isSelected()) {
            String sub = getRefersToField.getText().trim();
            if (sub.isEmpty()) {
                logError("GET: RefersTo filter requires a substring.");
                return;
            }
            if (sub.contains(" ")) {
                logError("GET: RefersTo substring cannot contain spaces.");
                return;
            }
            cmd.append(" refersTo=").append(sub);
        }

        sendCommandMultiLine(cmd.toString());
    }

    private void handleGetPins() {
        if (!connected) return;
        sendCommandMultiLine("GET PINS");
    }

    private void handleGetAll() {
        if (!connected) return;
        sendCommandMultiLine("GET");
    }

    private void handlePin() {
        if (!connected) return;

        String xStr = pinXField.getText().trim();
        String yStr = pinYField.getText().trim();

        if (xStr.isEmpty() || yStr.isEmpty()) {
            logError("PIN: X and Y coordinates are required.");
            return;
        }

        int x, y;
        try {
            x = Integer.parseInt(xStr);
            y = Integer.parseInt(yStr);
        } catch (NumberFormatException ex) {
            logError("PIN: Coordinates must be integers.");
            return;
        }

        if (x < 0 || y < 0) {
            logError("PIN: Coordinates must be non-negative.");
            return;
        }

        if (x >= boardWidth || y >= boardHeight) {
            logError("PIN: Coordinates out of board bounds.");
            return;
        }

        sendCommand("PIN " + x + " " + y);
    }

    private void handleUnpin() {
        if (!connected) return;

        String xStr = pinXField.getText().trim();
        String yStr = pinYField.getText().trim();

        if (xStr.isEmpty() || yStr.isEmpty()) {
            logError("UNPIN: X and Y coordinates are required.");
            return;
        }

        int x, y;
        try {
            x = Integer.parseInt(xStr);
            y = Integer.parseInt(yStr);
        } catch (NumberFormatException ex) {
            logError("UNPIN: Coordinates must be integers.");
            return;
        }

        if (x < 0 || y < 0) {
            logError("UNPIN: Coordinates must be non-negative.");
            return;
        }

        sendCommand("UNPIN " + x + " " + y);
    }

    private void handleShake() {
        if (!connected) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "SHAKE will remove all unpinned notes.\nAre you sure?",
            "Confirm Shake", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            sendCommand("SHAKE");
        }
    }

    private void handleClear() {
        if (!connected) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "CLEAR will remove ALL notes and pins.\nThis cannot be undone. Continue?",
            "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            sendCommand("CLEAR");
        }
    }

    // ======================== Network Communication ===================

    /**
     * Sends a command and reads a single-line response.
     * Returns the response string, or null on failure.
     */
    private String sendCommand(String command) {
        if (!connected || serverOut == null) {
            logError("Not connected to server.");
            return null;
        }

        try {
            logSent(command);
            serverOut.println(command);

            String response = serverIn.readLine();
            if (response == null) {
                logError("Server closed the connection.");
                cleanup();
                return null;
            }

            logReceived(response);
            return response;

        } catch (IOException ex) {
            logError("Communication error: " + ex.getMessage());
            cleanup();
            return null;
        }
    }

    /**
     * Sends a command and reads a multi-line response.
     * Sierra's server returns GET/GET PINS results as:
     *   - "NO_RESULTS" (single line) if nothing found
     *   - Multiple data lines (NOTE... or PIN...) terminated by an empty line
     * Handles both formats.
     */
    private void sendCommandMultiLine(String command) {
        if (!connected || serverOut == null) {
            logError("Not connected to server.");
            return;
        }

        try {
            logSent(command);
            serverOut.println(command);

            // Read lines until we get an empty line, an error, or NO_RESULTS
            while (true) {
                String line = serverIn.readLine();
                if (line == null) {
                    logError("Server closed the connection.");
                    cleanup();
                    return;
                }
                // Empty line signals end of multi-line response
                if (line.isEmpty()) {
                    break;
                }
                logReceived(line);
                // Single-line responses that end the exchange
                if (line.startsWith("ERROR") || line.startsWith("NO_RESULTS")) {
                    break;
                }
            }

        } catch (IOException ex) {
            logError("Communication error: " + ex.getMessage());
            cleanup();
        }
    }

    // ======================== Main ===================================

    public static void main(String[] args) {
        // Set system look and feel for a nicer appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default
        }

        SwingUtilities.invokeLater(() -> {
            BBClient client = new BBClient();
            client.setVisible(true);
        });
    }
}
