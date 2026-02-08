package com.girlkun.tool.screens.server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class ServerRunnerScr extends JInternalFrame {

    // Global State
    private static Process serverProcess;
    private static boolean isRunning = false;
    private static StringBuilder logBuffer = new StringBuilder();
    private static ServerRunnerScr currentInstance;

    private JButton btnStart;
    private JTextArea txtOutput;
    private JProgressBar progressBar;

    public ServerRunnerScr() {
        super("Server Runner", true, true, true, true);
        currentInstance = this;
        initComponents();
        setSize(800, 600);
        restoreState();

        // Remove reference when closed
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                if (currentInstance == ServerRunnerScr.this) {
                    currentInstance = null;
                }
            }
        });
    }

    private void restoreState() {
        txtOutput.setText(logBuffer.toString());
        txtOutput.setCaretPosition(txtOutput.getDocument().getLength());

        if (isRunning) {
            btnStart.setEnabled(false);
            progressBar.setIndeterminate(true);
            progressBar.setString("Running...");
        } else {
            btnStart.setEnabled(true);
            progressBar.setIndeterminate(false);
            progressBar.setString("Stopped");
            progressBar.setValue(0);
        }
    }

    private void initComponents() {
        // Control Panel
        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnStart = new JButton("Run Server");
        btnStart.setBackground(new Color(0, 153, 51));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 14));

        btnStart.addActionListener(evt -> startServer());

        pnlControls.add(btnStart);

        // Output Text Area
        txtOutput = new JTextArea();
        txtOutput.setEditable(false);
        txtOutput.setBackground(Color.BLACK);
        txtOutput.setForeground(Color.GREEN);
        txtOutput.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(txtOutput);

        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Stopped");

        // Layout
        setLayout(new BorderLayout());
        add(pnlControls, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);
    }

    private void startServer() {
        if (isRunning)
            return;

        GlobalLog("Starting server...");
        isRunning = true;
        updateUIState(); // Update this instance immediately

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "java", "-Xms4G", "-Xmx6G", "-Xss512k", "-XX:+UseZGC", // Use appropriate memory settings
                        "-jar", "Server/Michelin_Boy.jar");
                pb.directory(new File("data"));
                pb.redirectErrorStream(true);

                Process proc = pb.start(); // Local variable first
                serverProcess = proc; // Assign to static field

                // Read output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String finalLine = line;
                        GlobalLog(finalLine);
                    }
                }

                int exitCode = proc.waitFor();
                serverProcess = null; // Clear static reference on exit
                SwingUtilities.invokeLater(() -> {
                    GlobalLog("Server process exited with code " + exitCode);
                    GlobalReset();
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    GlobalLog("Error starting server: " + e.getMessage());
                    GlobalReset();
                });
            }
        }).start();
    }

    // Updates UI for this instance based on static state
    private void updateUIState() {
        if (isRunning) {
            btnStart.setEnabled(false);
            progressBar.setIndeterminate(true);
            progressBar.setString("Running...");
        } else {
            btnStart.setEnabled(true);
            progressBar.setIndeterminate(false);
            progressBar.setString("Stopped");
            progressBar.setValue(0);
        }
    }

    private static void GlobalReset() {
        isRunning = false;
        if (currentInstance != null) {
            currentInstance.updateUIState();
        }
    }

    private static void GlobalLog(String text) {
        logBuffer.append(text).append("\n");
        if (currentInstance != null) {
            SwingUtilities.invokeLater(() -> {
                if (currentInstance != null) {
                    currentInstance.txtOutput.append(text + "\n");
                    currentInstance.txtOutput.setCaretPosition(currentInstance.txtOutput.getDocument().getLength());
                }
            });
        }
    }
}
