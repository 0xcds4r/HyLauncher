package com.hylauncher.launcher;

import com.hylauncher.env.Environment;
import com.hylauncher.java.JREDownloader;
import com.hylauncher.model.ProgressUpdate;
import com.hylauncher.pwr.GameInstaller;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hylauncher.version.GameVersion;
import com.hylauncher.version.VersionManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LauncherPanel extends JPanel {
    private final JFrame parent;
    private String username = "";
    private boolean isDownloading = false;

    private JLabel usernameLabel;
    private JTextField usernameField;
    private JButton playButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JLabel progressPercentLabel;
    private JLabel speedLabel;
    private BufferedImage backgroundImage;
    private JButton folderButton;

    private double currentProgress = 0;
    private String currentMessage = "Ready to play";

    private static final Path USERNAME_FILE =
            Environment.getDefaultAppDir().resolve("username.txt");

    private VersionManager versionManager;
    private GameVersion selectedVersion;

    public LauncherPanel(JFrame parent) {
        this.parent = parent;
        this.versionManager = new VersionManager();
        setLayout(new BorderLayout());
        setOpaque(false);

        loadBackgroundImage();
        initComponents();
        loadSavedUsername();
    }

    private void loadBackgroundImage() {
        try {
            URI uri = new URI("https://hytale.com/static/images/backgrounds/content-upper-new-1920.jpg");
            backgroundImage = ImageIO.read(uri.toURL());
        } catch (Exception e) {
            System.err.println("Failed to load background: " + e.getMessage());
        }
    }

    private void loadSavedUsername() {
        try {
            if (Files.exists(USERNAME_FILE)) {
                String savedName = Files.readString(USERNAME_FILE, StandardCharsets.UTF_8).trim();
                if (!savedName.isEmpty() && savedName.length() <= 16) {
                    usernameField.setText(savedName);
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load username: " + e.getMessage());
        }

        usernameField.setText("");
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);

            g2d.setColor(new Color(0, 0, 0, 102));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(0, 0, 0, 0),
                    0, getHeight(), new Color(9, 9, 9, 255)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void initComponents()
    {
        add(createTitleBar(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(20, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JPanel leftPanel = createUsernameSection();
        leftPanel.setPreferredSize(new Dimension(350, 180));
        centerPanel.add(leftPanel, BorderLayout.WEST);

        JPanel newsPanel = createNewsSection();
        centerPanel.add(new JScrollPane(newsPanel), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        add(createBottomControls(), BorderLayout.SOUTH);
    }

    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(0, 40));

        JButton minimizeBtn = createTitleBarButton("-");
        minimizeBtn.addActionListener(e -> parent.setState(JFrame.ICONIFIED));

        JButton closeBtn = createTitleBarButton("×");
        closeBtn.addActionListener(e -> System.exit(0));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(new Color(239, 68, 68)); }
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(Color.LIGHT_GRAY); }
        });

        titleBar.add(minimizeBtn);
        titleBar.add(closeBtn);
        return titleBar;
    }

    private JButton createTitleBarButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(Color.LIGHT_GRAY);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.PLAIN, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel createUsernameSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 16, 0));

        JLabel label = new JLabel("Enter username");
        label.setForeground(new Color(160, 160, 170));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        usernameField = new RoundTextField("");
        usernameField.setFont(new Font("Segoe UI", Font.BOLD, 17));
        usernameField.setForeground(Color.WHITE);
        usernameField.setBackground(new Color(26, 26, 32));
        usernameField.setCaretColor(new Color(255, 168, 69));
        usernameField.setSelectionColor(new Color(255, 168, 69, 140));
        usernameField.setSelectedTextColor(Color.BLACK);
        usernameField.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        usernameField.setPreferredSize(new Dimension(280, 32));
        usernameField.setMaximumSize(new Dimension(340, 32));

        usernameField.addActionListener(e -> {
            saveUsername();
            usernameField.transferFocus();
        });

        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                usernameField.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 160), 1));
            }

            @Override
            public void focusLost(FocusEvent e) {
                usernameField.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                saveUsername();
                usernameField.setCaretPosition(usernameField.getText().length());
            }
        });

        panel.add(label);
        panel.add(usernameField);

        panel.add(Box.createVerticalStrut(8));

        JLabel infoLine1 = new JLabel("forked from ArchDevs/HyLauncher (Go)");
        infoLine1.setForeground(new Color(120, 120, 130));
        infoLine1.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        infoLine1.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(infoLine1);

        JLabel infoLine2 = new JLabel("rewritten by 0xcds4r/HyLauncher (Java)");
        infoLine2.setForeground(new Color(120, 120, 130));
        infoLine2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        infoLine2.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(infoLine2);

        return panel;
    }

    private void saveUsername()
    {
        String name = usernameField.getText().trim();

        if (name.isEmpty()) {
            name = "";
            usernameField.setText(name);
        }
        if (name.length() > 16) {
            name = name.substring(0, 16);
            usernameField.setText(name);
        }

        try {
            Files.createDirectories(USERNAME_FILE.getParent());
            Files.writeString(USERNAME_FILE, name, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save username: " + e.getMessage());
        }
    }

    private void startEditingUsername() {
        usernameField.setText(username);
        usernameLabel.setVisible(false);
        usernameField.setVisible(true);
        usernameField.requestFocus();
        usernameField.selectAll();
    }

    private void finishEditingUsername() {
        if (!usernameField.isVisible()) return;
        username = usernameField.getText().trim();
        if (username.isEmpty()) username = "";
        if (username.length() > 16) username = username.substring(0, 16);
        usernameLabel.setText(username);
        usernameField.setVisible(false);
        usernameLabel.setVisible(true);
    }

    private List<NewsItem> fetchHytaleNews() {
        List<NewsItem> news = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://hytale.com/news")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(5000)
                    .get();

            Elements wrappers = doc.select(".postWrapper");

            for (Element wrapper : wrappers) {
                if (news.size() >= 8) break;

                Element postLink = wrapper.selectFirst("a.post");
                if (postLink == null) continue;

                String title = getSafeText(wrapper, "h4.post__details__heading", "No Title");

                Element bodyEl = wrapper.selectFirst("span.post__details__body");
                String rawDesc = "";

                if (bodyEl != null) {
                    rawDesc = bodyEl.html()
                            .replaceAll("(?i)<br\\s*/?>", "\n")
                            .replaceAll("<[^>]+>", "")
                            .trim();
                }

                String desc = rawDesc.length() > 90 ? rawDesc.substring(0, 90) + "..." : rawDesc;

                String meta = getSafeText(wrapper, "span.post__details__meta", "");

                String date = "Unknown date";

                Pattern p = Pattern.compile(
                        "(January|February|March|April|May|June|July|August|September|October|November|December)\\s+" +
                                "\\d{1,2}(st|nd|rd|th)\\s+\\d{4}"
                );

                Matcher m = p.matcher(meta);
                if (m.find()) {
                    date = m.group();
                }

                String url = postLink.absUrl("href");

                String imgUrl = "";
                Element img = wrapper.selectFirst("span.post__image img");
                if (img != null) {
                    imgUrl = img.absUrl("src");
                }

                news.add(new NewsItem(title, date, desc, url, imgUrl));
            }

        } catch (Exception e) {
            System.err.println("Hytale News Loading Error: " + e.getMessage());
        }

        if (news.isEmpty()) {
            news.add(new NewsItem(
                    "News unavailable",
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    "Failed to load news. You may not have an internet connection.",
                    "https://hytale.com/news",
                    ""
            ));
        }

        return news;
    }

    private String getSafeText(Element parent, String selector, String fallback) {
        Element el = parent.selectFirst(selector);
        return (el != null) ? el.text() : fallback;
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private JPanel createNewsCard(NewsItem item) {
        JPanel newsCard = new JPanel(new BorderLayout(16, 0));
        newsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 168, 69, 60), 1, false),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        newsCard.setBackground(new Color(9, 9, 9, 140));
        newsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        int textWidth = 420;

        String htmlText =
                "<html>" +
                        "<table width='" + textWidth + "' cellpadding='0' cellspacing='0'>" +
                        "<tr><td>" +
                        "<b style='color:#ffffff; font-size:15px;'>" + escapeHtml(item.title) + "</b><br>" +
                        "<small style='color:#bbbbbb; font-size:12px;'>" + item.date + "</small><br><br>" +
                        "<span style='color:#dddddd; font-size:13px;'>" +
                        escapeHtml(item.description) +
                        "</span>" +
                        "</td></tr>" +
                        "</table>" +
                        "</html>";

        JLabel newsLabel = new JLabel(htmlText);
        newsLabel.setVerticalAlignment(SwingConstants.TOP);

        newsCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newsCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!item.url.isEmpty()) {
                    try {
                        Desktop.getDesktop().browse(new URI(item.url));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        JPanel imgPanel = new JPanel();
        imgPanel.setBackground(new Color(9, 9, 9, 0));

        if (!item.imageUrl.isEmpty())
        {
            SwingWorker<ImageIcon, Void> imgWorker = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    BufferedImage original = ImageIO.read(new URL(item.imageUrl));

                    int targetWidth = 160;
                    int targetHeight = 90;

                    double imgRatio = (double) original.getWidth() / original.getHeight();
                    double panelRatio = (double) targetWidth / targetHeight;

                    int newWidth, newHeight;
                    if (imgRatio > panelRatio) {
                        newWidth = targetWidth;
                        newHeight = (int) (targetWidth / imgRatio);
                    } else {
                        newHeight = targetHeight;
                        newWidth = (int) (targetHeight * imgRatio);
                    }

                    Image scaled = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        JLabel imgLabel = new JLabel(icon);
                        imgLabel.setPreferredSize(new Dimension(160, 90));
                        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        imgLabel.setVerticalAlignment(SwingConstants.CENTER);

                        imgPanel.removeAll();
                        imgPanel.setLayout(new BorderLayout());
                        imgPanel.add(imgLabel, BorderLayout.CENTER);
                        imgPanel.revalidate();
                        imgPanel.repaint();
                    } catch (Exception e) {
                        JLabel placeholder = new JLabel("Hytale News", SwingConstants.CENTER);
                        placeholder.setForeground(new Color(255, 168, 69, 100));
                        imgPanel.add(placeholder);
                    }
                }
            };
            imgWorker.execute();

            JLabel placeholder = new JLabel("...", SwingConstants.CENTER);
            placeholder.setForeground(new Color(255, 168, 69, 60));
            imgPanel.add(placeholder);
        } else {
            JLabel placeholder = new JLabel("Hytale News", SwingConstants.CENTER);
            placeholder.setForeground(new Color(255, 168, 69, 100));
            imgPanel.add(placeholder);
        }

        newsCard.add(newsLabel, BorderLayout.CENTER);
        newsCard.add(imgPanel, BorderLayout.EAST);

        return newsCard;
    }

    private JPanel createNewsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 168, 69, 0), 1, false),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel loadingLabel = new JLabel("Loading news...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.LIGHT_GRAY);
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(loadingLabel);

        SwingWorker<List<NewsItem>, Void> newsWorker = new SwingWorker<>() {
            @Override
            protected List<NewsItem> doInBackground() {
                return fetchHytaleNews();
            }

            @Override
            protected void done() {
                try {
                    List<NewsItem> newsItems = get();
                    panel.removeAll();

                    for (NewsItem item : newsItems) {
                        JPanel newsCard = createNewsCard(item);
                        panel.add(newsCard);
                        panel.add(Box.createVerticalStrut(16));
                    }

                    panel.revalidate();
                    panel.repaint();
                } catch (Exception e) {
                    System.err.println("Error loading news: " + e.getMessage());
                    panel.removeAll();

                    JLabel errorLabel = new JLabel("Error loading news", SwingConstants.CENTER);
                    errorLabel.setForeground(Color.GRAY);
                    panel.add(errorLabel);

                    panel.revalidate();
                    panel.repaint();
                }
            }
        };

        newsWorker.execute();
        return panel;
    }

    private void openGameFolder() {
        try {
            Path gameFolder;

            if (selectedVersion != null)
            {
                gameFolder = versionManager.getVersionDirectory(selectedVersion.getPatchNumber());
            }
            else
            {
                gameFolder = Environment.getDefaultAppDir()
                        .resolve("release").resolve("package").resolve("game");

                Path latestFolder = gameFolder.resolve("latest");
                if (Files.exists(latestFolder)) {
                    gameFolder = latestFolder;
                }
            }

            Files.createDirectories(gameFolder);

            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().open(gameFolder.toFile());
            }
            else
            {
                String os = Environment.getOS();
                ProcessBuilder pb;

                switch (os)
                {
                    case "windows" -> pb = new ProcessBuilder("explorer", gameFolder.toString());
                    case "darwin" -> pb = new ProcessBuilder("open", gameFolder.toString());
                    case "linux" -> pb = new ProcessBuilder("xdg-open", gameFolder.toString());
                    default -> {
                        JOptionPane.showMessageDialog(this,
                                "Game folder: " + gameFolder.toString(),
                                "Game path",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }

                pb.start();
            }

            System.out.println("Opened game folder: " + gameFolder);

        } catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to open folder: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createBottomControls()
    {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JButton versionButton = new JButton("Select version");
        versionButton.setFont(new Font("Arial", Font.BOLD, 14));
        versionButton.setForeground(Color.WHITE);
        versionButton.setBackground(new Color(60, 60, 70));
        versionButton.setPreferredSize(new Dimension(160, 90));
        versionButton.setFocusPainted(false);
        versionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        versionButton.addActionListener(e -> openVersionSelector());

        folderButton = new JButton("Folder");
        folderButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        folderButton.setForeground(Color.WHITE);
        folderButton.setBackground(new Color(60, 60, 70));
        folderButton.setPreferredSize(new Dimension(90, 90));
        folderButton.setFocusPainted(false);
        folderButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        folderButton.setToolTipText("Open game folder");
        folderButton.addActionListener(e -> openGameFolder());

        folderButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                folderButton.setBackground(new Color(80, 80, 90));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                folderButton.setBackground(new Color(60, 60, 70));
            }
        });

        playButton = new JButton("PLAY");
        playButton.setFont(new Font("Arial", Font.BOLD, 36));
        playButton.setForeground(Color.WHITE);
        playButton.setBackground(new Color(255, 168, 69, 180));
        playButton.setPreferredSize(new Dimension(300, 90));
        playButton.setFocusPainted(false);
        playButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> handlePlay());

        leftPanel.add(versionButton);
        leftPanel.add(folderButton);
        leftPanel.add(playButton);

        bottom.add(leftPanel, BorderLayout.WEST);

        JPanel progressPanel = createProgressSection();
        bottom.add(progressPanel, BorderLayout.EAST);

        if (selectedVersion == null)
        {
            List<GameVersion> versions = versionManager.loadCachedVersions();

            if (!versions.isEmpty())
            {
                selectedVersion = versions.get(0);
                updateFolderButtonText();
            }
        }

        return bottom;
    }

    private void updateFolderButtonText() {
        if (folderButton == null) return;

        if (selectedVersion != null) {
            folderButton.setText(String.format(
                    "Patch %d",
                    selectedVersion.getPatchNumber()
            ));
            folderButton.setToolTipText("Open game folder - " + selectedVersion.getName());
        } else {
            folderButton.setText("Folder");
            folderButton.setToolTipText("Open game folder");
        }
    }

    private void openVersionSelector()
    {
        VersionSelectorDialog dialog = new VersionSelectorDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                versionManager
        );

        dialog.setVisible(true);

        GameVersion selected = dialog.getSelectedVersion();
        if (selected != null) {
            selectedVersion = selected;
            playButton.setText("PLAY");
            updateFolderButtonText();
            System.out.println("Selected version: " + selected.getName());
        }
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(Color.GRAY);
        btn.setBackground(new Color(9, 9, 9, 140));
        btn.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 26), 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(255, 168, 69, 13));
                btn.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 77), 1, true));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(9, 9, 9, 140));
                btn.setBorder(BorderFactory.createLineBorder(new Color(255, 168, 69, 26), 1, true));
            }
        });
        return btn;
    }

    private JPanel createProgressSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        progressPercentLabel = new JLabel("0%");
        progressPercentLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 48));
        progressPercentLabel.setForeground(Color.WHITE);
        progressPercentLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        statusLabel = new JLabel("READY TO PLAY");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        speedLabel = new JLabel("Ready");
        speedLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        speedLabel.setForeground(Color.GRAY);
        speedLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setForeground(new Color(255, 168, 69));
        progressBar.setBackground(new Color(30, 30, 30));
        progressBar.setPreferredSize(new Dimension(300, 10));
        progressBar.setAlignmentX(Component.RIGHT_ALIGNMENT);

        panel.add(progressPercentLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(speedLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(progressBar);

        return panel;
    }

    private void handlePlay() {
        if (isDownloading) return;

        String playerName = usernameField.getText().trim();

        if (playerName.isEmpty()) {
            playerName = "";
            usernameField.setText(playerName);
            saveUsername();
        }

        if (playerName.length() > 16) {
            playerName = playerName.substring(0, 16);
            usernameField.setText(playerName);
            saveUsername();
            JOptionPane.showMessageDialog(this, "username must be < 16 symbols",
                    "WARNING!", JOptionPane.WARNING_MESSAGE);
        }

        if (selectedVersion == null) {
            List<GameVersion> versions = versionManager.loadCachedVersions();
            if (versions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Press 'Select version' for select game version",
                        "Version not selected",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            selectedVersion = versions.get(0);
        }

        isDownloading = true;
        playButton.setText("DOWNLOADING...");
        playButton.setEnabled(false);

        String finalPlayerName = playerName;
        GameVersion versionToInstall = selectedVersion;

        SwingWorker<Path, ProgressUpdate> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                try {
                    publish(new ProgressUpdate("jre", 0, "Checking JRE...", "", "", 0, 0));
                    JREDownloader.downloadJRE(this::publish);

                    publish(new ProgressUpdate("game", 0, "Installing " +
                            versionToInstall.getName() + "...", "", "", 0, 0));

                    Path gameDir = GameInstaller.installGameVersion(versionToInstall, this::publish);

                    versionManager.markVersionInstalled(versionToInstall);

                    publish(new ProgressUpdate("launch", 100, "Launching game...", "", "", 0, 0));
                    return gameDir;

                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void process(java.util.List<ProgressUpdate> chunks) {
                for (ProgressUpdate update : chunks) {
                    updateProgress(update);
                }
            }

            @Override
            protected void done() {
                try {
                    Path gameDir = get();

                    launchGameFromDirectory(gameDir, finalPlayerName);

                    playButton.setText("PLAY");
                    playButton.setEnabled(true);
                    isDownloading = false;

                    progressBar.setValue(0);
                    progressPercentLabel.setText("0%");
                    statusLabel.setText("READY TO PLAY");
                    speedLabel.setText("Ready");

                } catch (Exception e) {
                    e.printStackTrace();
                    playButton.setText("ERROR - RETRY");
                    playButton.setEnabled(true);
                    isDownloading = false;

                    JOptionPane.showMessageDialog(LauncherPanel.this,
                            "Error while loading/installing:\n" + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void updateProgress(ProgressUpdate update) {
        currentProgress = update.getProgress();
        currentMessage = update.getMessage();

        progressBar.setValue((int) currentProgress);
        progressPercentLabel.setText(String.format("%.0f%%", currentProgress));
        statusLabel.setText(currentMessage.toUpperCase());

        if (!update.getSpeed().isEmpty()) {
            speedLabel.setText(update.getSpeed());
        } else if (currentProgress >= 100) {
            speedLabel.setText("Complete");
        }
    }

    private String findClientExecutable(Path clientDir) throws IOException {
        if (!Files.exists(clientDir)) {
            throw new IOException("Client directory not found: " + clientDir);
        }

        String[] candidates = {"HytaleClient", "Hytale", "HytaleClient.bin.x86_64"};

        for (String name : candidates) {
            Path candidate = clientDir.resolve(name);
            if (Files.exists(candidate) && Files.isExecutable(candidate)) {
                return name;
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(clientDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && Files.isExecutable(entry)) {
                    return entry.getFileName().toString();
                }
            }
        }

        throw new IOException("No executable found in Client directory");
    }

    private void launchGameFromDirectory(Path gameDir, String playerName) throws Exception {
        if (playerName == null || playerName.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Enter username!",
                        "Error while running",
                        JOptionPane.WARNING_MESSAGE
                );
            });
            return;
        }

        Path clientDir = gameDir.resolve("Client");
        String clientName = findClientExecutable(clientDir);
        Path clientPath = clientDir.resolve(clientName);

        if (!Files.exists(clientPath) || !Files.isExecutable(clientPath)) {
            throw new Exception("Game client executable not found or not executable: " + clientPath);
        }

        Path userDataDir = Environment.getDefaultAppDir().resolve("UserData");
        Files.createDirectories(userDataDir);

        if (!Environment.getOS().equals("windows")) {
            clientPath.toFile().setExecutable(true, false);
        }

        ProcessBuilder pb = new ProcessBuilder(
                clientPath.toAbsolutePath().toString(),
                "--app-dir", gameDir.toAbsolutePath().toString(),
                "--user-dir", userDataDir.toAbsolutePath().toString(),
                "--java-exec", JREDownloader.getJavaExec(),
                "--auth-mode", "offline",
                "--uuid", UUIDGen.generateUUID(playerName),
                "--name", playerName,
                "--identity-token", UUIDGen.generateIdentityToken(playerName),
                "--session-token", UUIDGen.generateSessionToken()
        );

        pb.directory(gameDir.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        System.out.println("Launching from: " + gameDir);
        System.out.println("Command: " + pb.command());

        Process process = pb.start();
    }

    private void launchGame(String playerName) throws Exception
    {
        if (playerName == null || playerName.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Enter username!",
                        "Error while running",
                        JOptionPane.WARNING_MESSAGE
                );
            });
            return;
        }

        Path gameDir = Environment.getDefaultAppDir()
                .resolve("release").resolve("package")
                .resolve("game/latest");

        Path clientDir = gameDir.resolve("Client");
        String clientName = findClientExecutable(clientDir);

        Path clientPath = clientDir.resolve(clientName);

        if (!Files.exists(clientPath) || !Files.isExecutable(clientPath)) {
            throw new Exception("Game client executable not found or not executable: " + clientPath);
        }

        Path userDataDir = Environment.getDefaultAppDir().resolve("UserData");
        Files.createDirectories(userDataDir);

        if (!Environment.getOS().equals("windows")) {
            clientPath.toFile().setExecutable(true, false);
        }

        ProcessBuilder pb = new ProcessBuilder(
                clientPath.toAbsolutePath().toString(),
                "--app-dir", gameDir.toAbsolutePath().toString(),
                "--user-dir", userDataDir.toAbsolutePath().toString(),
                "--java-exec", JREDownloader.getJavaExec(),
                "--auth-mode", "offline",
                "--uuid", "1da855d2-6219-4d02-ad93-c4b160b073c3",
                "--name", playerName
        );

        pb.directory(gameDir.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        System.out.println("Launching: " + pb.command());

        Process process = pb.start();

//        new Timer(5000, e -> System.exit(0)).start();
    }
}