package com.tftp.view;

import com.tftp.io.client.TFTPClient;
import com.tftp.model.client.ClientModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class GUI extends JFrame {
    private TFTPClient client;
    private ClientModel model;

    private JTextField ipField;
    private JTextField portField;
    private JTextField directoryField;
    private JTextField downloadField;
    private JTextField uploadField;

    private JTextArea statusArea;
    private JTextArea logArea;

    private JProgressBar progressBar;
    private JLabel progressLabel;

    private JButton browseButton;
    private JButton downloadButton;
    private JButton uploadButton;
    private JButton stopButton;
    private JButton quitButton;

    public GUI() {
        initializeModel();
        initializeUI();
        setupEventHandlers();
    }

    private void initializeModel() {
        this.model = new ClientModel();
        this.model.setListener(new ClientModel.ClientModelListener() {
            @Override
            public void onStatusUpdated(String status) {
                SwingUtilities.invokeLater(() -> {
                    statusArea.setText(status);
                    statusArea.setCaretPosition(statusArea.getDocument().getLength());
                });
            }

            @Override
            public void onLogUpdated(String logMessage) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(logMessage + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }

            @Override
            public void onProgressUpdated(long current, long total) {
                SwingUtilities.invokeLater(() -> {
                    int progress = (int) ((double) current / total * 100);
                    progressBar.setValue(progress);
                    progressLabel.setText(current + "/" + total + " bytes");
                });
            }
        });
    }

    private void initializeUI() {
        setTitle("TFTP Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel connectionPanel = createConnectionPanel();
        topPanel.add(connectionPanel);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel filePanel = createFilePanel();
        topPanel.add(filePanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = createStatusLogPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        setSize(600, 700);
        setLocationRelativeTo(null);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Connection Settings",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.add(new JLabel("IP:"));
        ipField = new JTextField("127.0.0.1");
        panel.add(ipField);

        panel.add(new JLabel("Port:"));
        portField = new JTextField("69");
        panel.add(portField);

        panel.add(new JLabel("Directory:"));
        JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
        directoryField = new JTextField("D:/Прога/TFTP/Client");
        dirPanel.add(directoryField, BorderLayout.CENTER);
        browseButton = new JButton("Browse");
        dirPanel.add(browseButton, BorderLayout.EAST);
        panel.add(dirPanel);

        return panel;
    }

    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File Operations",
                TitledBorder.LEFT, TitledBorder.TOP));

        panel.add(new JLabel("Download:"));
        JPanel downloadPanel = new JPanel(new BorderLayout(5, 0));
        downloadField = new JTextField();
        downloadPanel.add(downloadField, BorderLayout.CENTER);
        downloadButton = new JButton("Get");
        downloadPanel.add(downloadButton, BorderLayout.EAST);
        panel.add(downloadPanel);

        panel.add(new JLabel("Upload:"));
        JPanel uploadPanel = new JPanel(new BorderLayout(5, 0));
        uploadField = new JTextField();
        uploadPanel.add(uploadField, BorderLayout.CENTER);
        uploadButton = new JButton("Put");
        uploadPanel.add(uploadButton, BorderLayout.EAST);
        panel.add(uploadPanel);

        return panel;
    }

    private JPanel createStatusLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Status",
                TitledBorder.LEFT, TitledBorder.TOP));

        statusArea = new JTextArea(4, 20);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setText("Ready to connect");
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusPanel.add(statusScroll, BorderLayout.CENTER);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Log",
                TitledBorder.LEFT, TitledBorder.TOP));

        logArea = new JTextArea(8, 20);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Transfer Progress",
                TitledBorder.LEFT, TitledBorder.TOP));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        progressLabel = new JLabel("0/0 bytes", SwingConstants.CENTER);

        JPanel progressContent = new JPanel(new BorderLayout(5, 0));
        progressContent.add(progressBar, BorderLayout.CENTER);
        progressContent.add(progressLabel, BorderLayout.EAST);

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        progressPanel.add(progressContent, BorderLayout.CENTER);
        progressPanel.add(stopButton, BorderLayout.EAST);

        quitButton = new JButton("Quit");
        quitButton.setPreferredSize(new Dimension(80, 30));

        JPanel quitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        quitPanel.add(quitButton);

        panel.add(progressPanel, BorderLayout.CENTER);
        panel.add(quitPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseDirectory());
        downloadButton.addActionListener(e -> downloadFile());
        uploadButton.addActionListener(e -> uploadFile());
        stopButton.addActionListener(e -> stopTransfer());
        quitButton.addActionListener(e -> quitApplication());
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Local Directory");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    private void downloadFile() {
        String filename = downloadField.getText().trim();
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a filename to download",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        initializeClient();
        if (client == null) return;

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                downloadButton.setEnabled(false);
                uploadButton.setEnabled(false);
                stopButton.setEnabled(true);
            });

            boolean success = client.downloadFile(filename, model);

            SwingUtilities.invokeLater(() -> {
                downloadButton.setEnabled(true);
                uploadButton.setEnabled(true);
                stopButton.setEnabled(false);

                if (success)
                    progressBar.setValue(100);
            });
        }).start();
    }

    private void uploadFile() {
        String filename = uploadField.getText().trim();
        if (filename.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a filename to upload",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        initializeClient();
        if (client == null) return;

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                downloadButton.setEnabled(false);
                uploadButton.setEnabled(false);
                stopButton.setEnabled(true);
            });

            boolean success = client.uploadFile(filename, model);

            SwingUtilities.invokeLater(() -> {
                downloadButton.setEnabled(true);
                uploadButton.setEnabled(true);
                stopButton.setEnabled(false);

                if (success)
                    progressBar.setValue(100);
            });
        }).start();
    }

    private void stopTransfer() {
        if (client != null)
            client.stopTransfer();
        SwingUtilities.invokeLater(() -> {
            stopButton.setEnabled(false);
        });
    }

    private void quitApplication() {
        if (client != null && client.isTransferInProgress()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "A transfer is in progress. Are you sure you want to quit?",
                    "Confirm Quit", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION)
                return;
            client.stopTransfer();
        }
        System.exit(0);
    }

    private void initializeClient() {
        try {
            String ip = ipField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String directory = directoryField.getText().trim();

            this.client = new TFTPClient(ip, port, directory);
            model.updateStatus("Connected to " + ip + ":" + port);
            model.updateLog("Client initialized with directory: " + directory);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number",
                    "Error", JOptionPane.ERROR_MESSAGE);
            client = null;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize client: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            client = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();
            gui.setVisible(true);
        });
    }
}