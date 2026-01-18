package com.hylauncher.launcher;

import com.hylauncher.version.GameVersion;
import com.hylauncher.version.VersionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class VersionSelectorDialog extends JDialog
{
    private GameVersion selectedVersion;
    private final VersionManager versionManager;
    private JList<GameVersion> versionList;
    private DefaultListModel<GameVersion> listModel;
    private JLabel statusLabel;
    private JButton installButton;
    private JButton deleteButton;
    private JButton refreshButton;

    public VersionSelectorDialog(Frame parent, VersionManager versionManager)
    {
        super(parent, "Select game version", true);
        this.versionManager = versionManager;

        setSize(700, 500);
        setLocationRelativeTo(parent);
        setResizable(false);

        initComponents();
        loadVersions();
    }

    private void initComponents()
    {
        getContentPane().setBackground(new Color(18, 18, 18));
        setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(9, 9, 9));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Available versions:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        refreshButton = createStyledButton("Refresh");
        refreshButton.addActionListener(e -> refreshVersions());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(refreshButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        versionList = new JList<>(listModel);
        versionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        versionList.setBackground(new Color(26, 26, 32));
        versionList.setForeground(Color.WHITE);
        versionList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        versionList.setBorder(new EmptyBorder(10, 10, 10, 10));

        versionList.setCellRenderer(new VersionListCellRenderer());

        versionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        versionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectVersion();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(versionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 40)));
        scrollPane.getViewport().setBackground(new Color(26, 26, 32));

        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(18, 18, 18));
        bottomPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        statusLabel = new JLabel("Loading game versions...");
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        deleteButton = createStyledButton("Delete");
        deleteButton.setBackground(new Color(239, 68, 68, 180));
        deleteButton.addActionListener(e -> deleteVersion());
        deleteButton.setEnabled(false);

        installButton = createStyledButton("Select");
        installButton.setBackground(new Color(255, 168, 69, 180));
        installButton.addActionListener(e -> selectVersion());
        installButton.setEnabled(false);

        JButton cancelButton = createStyledButton("Cancel");
        cancelButton.setBackground(new Color(60, 60, 60));
        cancelButton.addActionListener(e -> {
            selectedVersion = null;
            dispose();
        });

        buttonPanel.add(deleteButton);
        buttonPanel.add(installButton);
        buttonPanel.add(cancelButton);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text)
    {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(60, 60, 60));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 36));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(button.getBackground().brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(button.getBackground().darker());
            }
        });

        return button;
    }

    private void loadVersions()
    {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<GameVersion> versions;

            @Override
            protected Void doInBackground() {
                versions = versionManager.loadCachedVersions();

                if (versions.isEmpty())
                {
                    try {
                        versions = versionManager.scanAvailableVersions((percent, message) ->
                                SwingUtilities.invokeLater(() ->
                                        statusLabel.setText(message + " (" + percent + "%)")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                displayVersions(versions);
            }
        };

        worker.execute();
    }

    private void refreshVersions()
    {
        refreshButton.setEnabled(false);
        statusLabel.setText("Updating versions list...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<GameVersion> versions;

            @Override
            protected Void doInBackground() {
                try {
                    versions = versionManager.scanAvailableVersions((percent, message) ->
                            SwingUtilities.invokeLater(() ->
                                    statusLabel.setText(message + " (" + percent + "%)")));
                } catch (Exception e) {
                    e.printStackTrace();
                    versions = versionManager.loadCachedVersions();
                }
                return null;
            }

            @Override
            protected void done() {
                displayVersions(versions);
                refreshButton.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void displayVersions(List<GameVersion> versions)
    {
        listModel.clear();

        for (GameVersion version : versions)
        {
            boolean actuallyInstalled = versionManager.isVersionInstalled(version.getPatchNumber());

            GameVersion displayVersion = new GameVersion(
                    version.getName(),
                    version.getFileName(),
                    version.getDownloadUrl(),
                    version.getPatchNumber(),
                    version.getSize(),
                    actuallyInstalled
            );

            listModel.addElement(displayVersion);
        }

        statusLabel.setText("count: " + versions.size());

        if (!versions.isEmpty()) {
            versionList.setSelectedIndex(0);
        }
    }

    private void updateButtonStates()
    {
        GameVersion selected = versionList.getSelectedValue();

        if (selected != null)
        {
            boolean installed = versionManager.isVersionInstalled(selected.getPatchNumber());
            installButton.setEnabled(true);
            installButton.setText("Select");
            deleteButton.setEnabled(installed);
        }
        else
        {
            installButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
    }

    private void selectVersion()
    {
        selectedVersion = versionList.getSelectedValue();
        if (selectedVersion != null) {
            dispose();
        }
    }

    private void deleteVersion()
    {
        GameVersion version = versionList.getSelectedValue();
        if (version == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete version" + version.getName() + "?\nThis action cannot be undone.",
                "Confirm to delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION)
        {
            try {
                versionManager.deleteVersion(version.getPatchNumber());
                statusLabel.setText("Version deleted: " + version.getName());

                int selectedIndex = versionList.getSelectedIndex();
                GameVersion updated = new GameVersion(
                        version.getName(),
                        version.getFileName(),
                        version.getDownloadUrl(),
                        version.getPatchNumber(),
                        version.getSize(),
                        false
                );
                listModel.set(selectedIndex, updated);
                updateButtonStates();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error while delete: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public GameVersion getSelectedVersion() {
        return selectedVersion;
    }

    private static class VersionListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus)
        {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof GameVersion version)
            {
                String text = String.format(
                        "<html><b>%s</b><br/>" +
                                "<span style='font-size:11px; color:#aaaaaa;'>Size: %s | Patch #%d%s</span></html>",
                        version.getName(),
                        version.getFormattedSize(),
                        version.getPatchNumber(),
                        version.isInstalled() ? " | ✓ Installed" : ""
                );

                label.setText(text);
                label.setBorder(new EmptyBorder(8, 12, 8, 12));

                if (version.isInstalled())
                {
                    label.setForeground(new Color(144, 238, 144));
                    if (!isSelected) {
                        label.setBackground(new Color(40, 50, 40));
                    }
                }
                else
                {
                    label.setForeground(Color.WHITE);
                    if (!isSelected) {
                        label.setBackground(new Color(26, 26, 32));
                    }
                }

                if (isSelected)
                {
                    label.setBackground(new Color(255, 168, 69, 80));
                    label.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 168, 69), 1),
                            new EmptyBorder(7, 11, 7, 11)
                    ));
                }
            }

            return label;
        }
    }
}