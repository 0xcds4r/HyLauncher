package com.linghy.mods;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ModManagerDialog extends JDialog
{
    private final ModManager modManager;

    private JTabbedPane tabbedPane;
    private JList<ModManager.InstalledMod> installedList;
    private DefaultListModel<ModManager.InstalledMod> installedModel;

    private JButton deleteButton;
    private JButton checkUpdatesButton;

    private JLabel statusLabel;

    public ModManagerDialog(Frame parent)
    {
        super(parent, "Mod manager", true);
        this.modManager = new ModManager();

        setSize(900, 600);
        setLocationRelativeTo(parent);
        setUndecorated(true);

        initComponents();
        loadInstalledMods();
    }

    private void initComponents()
    {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(26, 26, 26));
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 100), 2));

        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(26, 26, 26));
        tabbedPane.setForeground(Color.WHITE);

        JPanel installedPanel = createInstalledPanel();

        tabbedPane.addTab("Installed mods", installedPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        addWindowDragListener();
    }

    private JPanel createHeaderPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(9, 9, 9));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Mod Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 168, 69));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);

        checkUpdatesButton = createHeaderButton("↻");
        checkUpdatesButton.setToolTipText("Check updates");
        checkUpdatesButton.addActionListener(e -> checkForUpdates());

        JButton openFolderButton = createHeaderButton("M");
        openFolderButton.setToolTipText("Open mods folder");
        openFolderButton.addActionListener(e -> openModsFolder());

        JButton closeButton = createHeaderButton("×");
        closeButton.setFont(new Font("Arial", Font.PLAIN, 20));
        closeButton.addActionListener(e -> dispose());

        controls.add(checkUpdatesButton);
        controls.add(openFolderButton);
        controls.add(closeButton);

        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(controls, BorderLayout.EAST);

        return panel;
    }

    private JButton createHeaderButton(String text)
    {
        JButton btn = new JButton(text);
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(40, 30));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(new Color(255, 168, 69));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(Color.LIGHT_GRAY);
            }
        });

        return btn;
    }

    private JPanel createInstalledPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextField installedSearch = new JTextField();
        installedSearch.setBackground(new Color(26, 26, 32));
        installedSearch.setForeground(Color.WHITE);
        installedSearch.setCaretColor(new Color(255, 168, 69));
        installedSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 168, 69, 60), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        panel.add(installedSearch, BorderLayout.NORTH);

        installedModel = new DefaultListModel<>();
        installedList = new JList<>(installedModel);
        installedList.setBackground(new Color(26, 26, 32));
        installedList.setForeground(Color.WHITE);
        installedList.setCellRenderer(new InstalledModRenderer());
        installedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        installedList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        JScrollPane scrollPane = new JScrollPane(installedList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 60), 1));
        scrollPane.getViewport().setBackground(new Color(26, 26, 32));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel()
    {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        statusLabel = new JLabel("Done");
        statusLabel.setForeground(new Color(160, 160, 170));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        deleteButton = createStyledButton("Delete");
        deleteButton.setBackground(new Color(239, 68, 68, 160));
        deleteButton.addActionListener(e -> deleteSelectedMods());
        deleteButton.setEnabled(false);

        JButton closeButton = createStyledButton("Close");
        closeButton.setBackground(new Color(60, 60, 70));
        closeButton.addActionListener(e -> dispose());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.add(statusLabel);

        panel.add(leftPanel, BorderLayout.WEST);

        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private JButton createStyledButton(String text)
    {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));

        button.addMouseListener(new MouseAdapter()
        {
            Color originalColor = button.getBackground();

            @Override
            public void mouseEntered(MouseEvent e)
            {
                if (button.isEnabled()) {
                    button.setBackground(originalColor.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalColor);
            }
        });

        return button;
    }

    private void loadInstalledMods()
    {
        SwingWorker<List<ModManager.InstalledMod>, Void> worker = new SwingWorker<List<ModManager.InstalledMod>, Void>()
        {
            @Override
            protected List<ModManager.InstalledMod> doInBackground() {
                return modManager.getInstalledMods();
            }

            @Override
            protected void done()
            {
                try {
                    List<ModManager.InstalledMod> mods = get();
                    installedModel.clear();

                    for (ModManager.InstalledMod mod : mods) {
                        installedModel.addElement(mod);
                    }

                    statusLabel.setText("Total installed mods: " + mods.size());
                } catch (Exception e) {
                    statusLabel.setText("Failed to load modlist");
                }
            }
        };
        worker.execute();
    }

    private void deleteSelectedMods()
    {
        int[] selectedIndicesArray = installedList.getSelectedIndices();
        if (selectedIndicesArray.length == 0) return;

        int result = JOptionPane.showConfirmDialog(this,
                "Delete selected mods? (" + selectedIndicesArray.length + ")?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION)
        {
            for (int index : selectedIndicesArray)
            {
                ModManager.InstalledMod mod = installedModel.getElementAt(index);
                try {
                    modManager.uninstallMod(mod.id);
                } catch (Exception e) {
                    System.err.println("Failed to delete mod: " + e.getMessage());
                }
            }

            loadInstalledMods();
        }
    }

    private void checkForUpdates()
    {

    }

    private void openModsFolder()
    {
        try {
            modManager.openModsFolder();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to open dir: " + e.getMessage(),
                    "Err",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateButtonStates() {
        deleteButton.setEnabled(!installedList.isSelectionEmpty());
    }

    private void addWindowDragListener()
    {
        Point dragOffset = new Point();

        addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                dragOffset.x = e.getX();
                dragOffset.y = e.getY();
            }
        });

        addMouseMotionListener(new MouseAdapter()
        {
            public void mouseDragged(MouseEvent e)
            {
                Point location = getLocation();
                setLocation(location.x + e.getX() - dragOffset.x,
                        location.y + e.getY() - dragOffset.y);
            }
        });
    }

    private static class InstalledModRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ModManager.InstalledMod) {
                ModManager.InstalledMod mod = (ModManager.InstalledMod) value;
                String html = String.format(
                        "<html><div style='padding:8px;'>" +
                                "<div style='font-size:14px; font-weight:bold; margin-bottom:4px;'>%s</div>" +
                                "<div style='font-size:11px; color:#999;'>%s • %s</div>" +
                                "</div></html>",
                        mod.name, mod.author, mod.version
                );
                label.setText(html);
            }

            label.setBorder(new EmptyBorder(10, 15, 10, 15));
            label.setBackground(isSelected
                    ? new Color(255, 168, 69, 100)
                    : new Color(26, 26, 32));
            label.setForeground(Color.WHITE);

            return label;
        }
    }
}