package com.neo.locallm.desktop;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public final class NEOLocalLMDesktop extends JFrame {
    private static final String APP_NAME = "Neo Local LLM";
    private static final int LLAMA_PORT = 18482;
    private static final int SIDE_CONTROL_WIDTH = 220;
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("Segoe UI Semibold", Font.BOLD, 13);
    private static final Font TITLE_FONT = new Font("Segoe UI Semibold", Font.BOLD, 26);
    private static final Font CHAT_FONT = new Font("Consolas", Font.PLAIN, 14);
    private static final Color CHERRY = new Color(118, 102, 138);
    private static final Color CHERRY_DARK = new Color(74, 63, 88);
    private static final Color LEAF = new Color(176, 168, 192);
    private static final Color LOGO_CHERRY = new Color(195, 22, 61);
    private static final Color LOGO_CHERRY_DARK = new Color(122, 16, 42);
    private static final Color LOGO_LEAF = new Color(22, 139, 91);
    private static final Color DARK_BG = new Color(18, 18, 18);
    private static final Color DARK_PANEL = new Color(34, 27, 40);
    private static final Color DARK_FIELD = new Color(31, 31, 31);
    private static final Color DARK_TEXT = new Color(238, 238, 238);
    private static final Color DARK_MUTED = new Color(170, 164, 176);
    private static final Color DARK_BUTTON = new Color(52, 45, 60);
    private static final Color DARK_BORDER = new Color(72, 63, 82);
    private static final Color CONTROL_BG = new Color(218, 218, 224);
    private static final Color CONTROL_FG = new Color(22, 22, 24);
    private static final Color CONTROL_BORDER = new Color(176, 176, 186);
    private static final Color LIGHT_BG = DARK_BG;
    private static final Color LIGHT_PANEL = DARK_PANEL;
    private static final Color LIGHT_FIELD = DARK_FIELD;
    private static final Color LIGHT_TEXT = DARK_TEXT;
    private static final Color LIGHT_MUTED = DARK_MUTED;
    private static final Color LIGHT_BUTTON = DARK_BUTTON;
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    private final Preferences prefs = Preferences.userNodeForPackage(NEOLocalLMDesktop.class);
    private final Path appData = Paths.get(System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("user.home")), APP_NAME);
    private Path modelsDir = Paths.get(prefs.get("models_dir", defaultDownloadsFolder().toString()));
    private final Path runtimeDir = appData.resolve("runtime");
    private final Path runtimeMarker = runtimeDir.resolve("llama-server.path");
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JComboBox<ModelItem> modelCombo = new JComboBox<>();
    private final JTextField huggingFaceTokenField = new JPasswordField();
    private final JTextField openRouterApiKeyField = new JPasswordField();
    private final JTextField modelFolderField = new JTextField();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar downloadProgress = new JProgressBar(0, 100);
    private final JCheckBox darkMode = new JCheckBox("Dark mode");
    private final Map<String, String> responseCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 64;
        }
    };
    private Process llamaServer;
    private ModelItem loadedModel;

    private record DownloadResponse(InputStream body, long contentLength) {}

    private record ModelItem(String name, String filename, String url, String onlineId, boolean online, String provider) {
        @Override public String toString() { return name; }
    }

    private static final ModelItem[] LOCAL_MODELS = new ModelItem[] {
        new ModelItem("LFM2 8B A1B", "LFM2-8B-A1B-Q4_K_M.gguf", "https://huggingface.co/LiquidAI/LFM2-8B-A1B-GGUF/resolve/main/LFM2-8B-A1B-Q4_K_M.gguf", null, false, ""),
        new ModelItem("Qwen 3 1.7B", "Qwen3-1.7B-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/Qwen3-1.7B-GGUF/resolve/main/Qwen3-1.7B-Q4_K_M.gguf", null, false, ""),
        new ModelItem("DeepSeek R1 Distill", "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf", null, false, ""),
        new ModelItem("Gemma 3 1B", "gemma-3-1b-it-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf", null, false, ""),
        new ModelItem("LFM2.5 1.2B Thinking", "LFM2.5-1.2B-Thinking-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/LFM2.5-1.2B-Thinking-GGUF/resolve/main/LFM2.5-1.2B-Thinking-Q4_K_M.gguf", null, false, ""),
        new ModelItem("LFM2.5 1.2B Thinking F16", "LFM2.5-1.2B-Thinking-F16.gguf", "https://huggingface.co/LiquidAI/LFM2.5-1.2B-Thinking-GGUF/resolve/main/LFM2.5-1.2B-Thinking-F16.gguf", null, false, ""),
        new ModelItem("Ministral 3 8B Instruct", "Ministral-3-8B-Instruct-2512-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/Ministral-3-8B-Instruct-2512-GGUF/resolve/main/Ministral-3-8B-Instruct-2512-Q4_K_M.gguf", null, false, ""),
        new ModelItem("Ministral 3 8B Reasoning", "Ministral-3-8B-Reasoning-2512-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/Ministral-3-8B-Reasoning-2512-GGUF/resolve/main/Ministral-3-8B-Reasoning-2512-Q4_K_M.gguf", null, false, ""),
        new ModelItem("Gemma 3n 4B", "gemma-3n-E4B-it-Q4_K_M.gguf", "https://huggingface.co/lmstudio-community/gemma-3n-E4B-it-text-GGUF/resolve/main/gemma-3n-E4B-it-Q4_K_M.gguf", null, false, ""),
        new ModelItem("NVIDIA Nemotron 3 Nano 4B", "NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf", "https://huggingface.co/nvidia/NVIDIA-Nemotron-3-Nano-4B-GGUF/resolve/main/NVIDIA-Nemotron3-Nano-4B-Q4_K_M.gguf", null, false, ""),
        new ModelItem("Gemma2 9B", "gemma-2-9b-it-Q4_K_M.gguf", "https://huggingface.co/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf", null, false, ""),
        new ModelItem("TinyLlama 1.1B Q5", "tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf", "https://huggingface.co/pbatra/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf", null, false, ""),
        new ModelItem("Llama 3.2 3B Instruct Uncensored", "Llama-3.2-3B-Instruct-uncensored-Q8_0.gguf", "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-uncensored-GGUF/resolve/main/Llama-3.2-3B-Instruct-uncensored-Q8_0.gguf", null, false, ""),
        new ModelItem("Qwen2.5.1-Coder 7B Instruct", "Qwen2.5.1-Coder-7B-Instruct-Q6_K_L.gguf", "https://huggingface.co/bartowski/Qwen2.5.1-Coder-7B-Instruct-GGUF/resolve/main/Qwen2.5.1-Coder-7B-Instruct-Q6_K_L.gguf", null, false, ""),
        new ModelItem("OLMo 2 1124 7B Instruct", "OLMo-2-1124-7B-Instruct-Q6_K.gguf", "https://huggingface.co/bartowski/OLMo-2-1124-7B-Instruct-GGUF/resolve/main/OLMo-2-1124-7B-Instruct-Q6_K.gguf", null, false, ""),
        new ModelItem("Gemma 4 31B IT (Hugging Face)", "online-hf-gemma-4-31b-it", null, "google/gemma-4-31B-it", true, "huggingface"),
        new ModelItem("DeepSeek V4 Flash (Hugging Face)", "online-hf-deepseek-v4-flash", null, "deepseek-ai/DeepSeek-V4-Flash", true, "huggingface"),
        new ModelItem("Gemma 4 26B A4B IT (OpenRouter)", "online-or-gemma-4-26b-a4b-it-free", null, "google/gemma-4-26b-a4b-it:free", true, "openrouter"),
        new ModelItem("Laguna M.1 (OpenRouter)", "online-or-laguna-m-1-free", null, "poolside/laguna-m.1:free", true, "openrouter"),
        new ModelItem("Nemotron 3 Super 120B A12B (OpenRouter)", "online-or-nemotron-3-super-120b-a12b-free", null, "nvidia/nemotron-3-super-120b-a12b:free", true, "openrouter"),
        new ModelItem("Step 3.5 Flash (OpenRouter)", "online-or-step-3-5-flash-free", null, "stepfun/step-3.5-flash:free", true, "openrouter")
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            new NEOLocalLMDesktop().setVisible(true);
        });
    }

    private NEOLocalLMDesktop() {
        super(APP_NAME);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 700));
        setSize(new Dimension(1100, 760));
        setIconImage(createCherryIconImage(256));
        setLocationRelativeTo(null);
        try {
            Files.createDirectories(modelsDir);
            Files.createDirectories(runtimeDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        buildUi();
        loadPrefs();
        applyTheme();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.add(new CherryMark(42), BorderLayout.WEST);
        JLabel title = new JLabel("Neo Local LLM");
        title.setFont(TITLE_FONT);
        header.add(title, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        chatArea.setEditable(false);
        chatArea.setColumns(24);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(CHAT_FONT);
        chatArea.setMargin(new Insets(14, 14, 14, 14));
        ((DefaultCaret) chatArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setMinimumSize(new Dimension(220, 240));
        chatScroll.setPreferredSize(new Dimension(360, 520));

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setPreferredSize(new Dimension(300, 0));
        right.setMinimumSize(new Dimension(280, 0));

        for (ModelItem model : LOCAL_MODELS) modelCombo.addItem(model);
        modelCombo.setFont(UI_FONT_BOLD);
        modelCombo.setMaximumRowCount(12);
        modelCombo.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, 40));
        modelCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        modelCombo.setPrototypeDisplayValue(new ModelItem(
            "Llama 3.2 3B Instruct",
            "",
            null,
            null,
            false,
            ""
        ));
        modelCombo.setRenderer(new ModelRenderer());

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.add(section("Models", modelPanel()));
        options.add(Box.createVerticalStrut(10));
        options.add(section("Online", onlinePanel()));
        options.add(Box.createVerticalStrut(10));
        options.add(section("Tools", toolsPanel()));
        darkMode.addActionListener(e -> { prefs.putBoolean("dark", darkMode.isSelected()); applyTheme(); });

        JPanel pinnedOptions = new JPanel();
        pinnedOptions.setLayout(new BoxLayout(pinnedOptions, BoxLayout.Y_AXIS));
        pinnedOptions.add(section("Download Folder", folderPanel()));
        pinnedOptions.add(Box.createVerticalStrut(10));
        pinnedOptions.add(compactSection("Appearance", appearancePanel()));
        right.add(pinnedOptions, BorderLayout.NORTH);

        JScrollPane optionsScroll = new JScrollPane(options);
        optionsScroll.setBorder(BorderFactory.createEmptyBorder());
        optionsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        optionsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        optionsScroll.getVerticalScrollBar().setUnitIncrement(18);
        right.add(optionsScroll, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(statusLabel);
        downloadProgress.setStringPainted(true);
        downloadProgress.setVisible(false);
        downloadProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(downloadProgress);
        right.add(statusPanel, BorderLayout.SOUTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, right);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setResizeWeight(0.56);
        mainSplit.setDividerLocation(620);
        mainSplit.setContinuousLayout(true);
        root.add(mainSplit, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        inputField.setFont(UI_FONT);
        inputField.setMargin(new Insets(10, 12, 10, 12));
        bottom.add(inputField, BorderLayout.CENTER);
        JButton send = new JButton("Send");
        send.putClientProperty("accent", true);
        send.addActionListener(this::sendPrompt);
        bottom.add(send, BorderLayout.EAST);
        inputField.addActionListener(this::sendPrompt);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private JPanel section(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        TitledBorder titleBorder = new TitledBorder(
            BorderFactory.createLineBorder(new Color(DARK_BORDER.getRed(), DARK_BORDER.getGreen(), DARK_BORDER.getBlue(), 150), 1, true),
            title,
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            UI_FONT_BOLD
        );
        titleBorder.setTitleColor(DARK_TEXT);
        panel.setBorder(new CompoundBorder(
            titleBorder,
                new EmptyBorder(8, 6, 8, 6)
        ));
        panel.add(content, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        return panel;
    }

    private JPanel compactSection(String title, JComponent content) {
        JPanel panel = section(title, content);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        return panel;
    }

    private JPanel modelPanel() {
        JPanel panel = verticalPanel();
        panel.add(label("Select model"));
        panel.add(leftWrap(modelCombo, 40));
        panel.add(buttonRow(
            button("Download", this::downloadSelected),
            button("Load", this::loadSelected)
        ));
        panel.add(button("Select GGUF file", this::selectCustomModel));
        return panel;
    }

    private JPanel folderPanel() {
        JPanel panel = verticalPanel();
        modelFolderField.setEditable(false);
        modelFolderField.setColumns(12);
        modelFolderField.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, 38));
        modelFolderField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        modelFolderField.setToolTipText(modelsDir.toString());
        panel.add(leftWrap(modelFolderField, 38));
        panel.add(buttonRow(
            button("Choose folder", this::chooseModelFolder),
            button("Open folder", e -> openPath(modelsDir))
        ));
        return panel;
    }

    private JPanel onlinePanel() {
        JPanel panel = verticalPanel();
        panel.add(label("Hugging Face token"));
        huggingFaceTokenField.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, 38));
        huggingFaceTokenField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        panel.add(leftWrap(huggingFaceTokenField, 38));
        panel.add(label("OpenRouter API key"));
        openRouterApiKeyField.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, 38));
        openRouterApiKeyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        panel.add(leftWrap(openRouterApiKeyField, 38));
        panel.add(button("Save online keys", e -> saveApiKeys()));
        return panel;
    }

    private JPanel toolsPanel() {
        JPanel panel = verticalPanel();
        panel.add(button("Install llama.cpp runtime", this::installRuntime));
        panel.add(buttonRow(
            button("Unload model", e -> unloadLocal()),
            button("Clear chat", e -> chatArea.setText(""))
        ));
        panel.add(button("Copy last response", e -> copyLastAssistant()));
        return panel;
    }

    private JPanel appearancePanel() {
        JPanel panel = verticalPanel();
        panel.add(darkMode);
        return panel;
    }

    private JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel buttonRow(JButton left, JButton right) {
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 8));
        grid.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, 84));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        grid.add(left);
        grid.add(right);
        return leftWrap(grid, 84);
    }

    private JPanel leftWrap(JComponent child, int height) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, height));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        wrapper.add(child, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UI_FONT_BOLD);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(10, 0, 4, 0));
        return label;
    }

    private JButton button(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT_BOLD);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new Insets(6, 8, 6, 8));
        button.setPreferredSize(new Dimension(SIDE_CONTROL_WIDTH, 38));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.addActionListener(listener);
        return button;
    }

    private void loadPrefs() {
        huggingFaceTokenField.setText(prefs.get("huggingface_token", ""));
        openRouterApiKeyField.setText(prefs.get("openrouter_api_key", ""));
        darkMode.setSelected(true);
        prefs.putBoolean("dark", true);
        modelFolderField.setText(compactPath(modelsDir));
        modelFolderField.setToolTipText(modelsDir.toString());
    }

    private void saveApiKeys() {
        prefs.put("huggingface_token", huggingFaceTokenField.getText().trim());
        prefs.put("openrouter_api_key", openRouterApiKeyField.getText().trim());
        setStatus("Online API keys saved");
    }

    private void downloadSelected(ActionEvent event) {
        ModelItem model = selectedModel();
        if (model.online) {
            setStatus("Online models do not need downloading");
            return;
        }
        Path target = modelPath(model);
        if (Files.exists(target)) {
            setStatus(model.name + " is already downloaded");
            return;
        }
        runAsync("Downloading " + model.name, () -> {
            Files.createDirectories(modelsDir);
            Path partial = target.resolveSibling(target.getFileName() + ".part");
            DownloadResponse download = openDownloadStream(URI.create(model.url));
            try (InputStream in = download.body();
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(
                     partial,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE
                 ))) {
                copyWithProgress(in, out, download.contentLength());
                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                appendSystem("Downloaded " + model.name + " to " + target);
            } finally {
                Files.deleteIfExists(partial);
                setDownloadProgress(-1, 0, 0);
            }
        });
    }

    private DownloadResponse openDownloadStream(URI uri) throws IOException, InterruptedException {
        URI current = uri;
        for (int redirect = 0; redirect < 10; redirect++) {
            HttpRequest request = HttpRequest.newBuilder(current)
                .timeout(Duration.ofHours(3))
                .header("User-Agent", APP_NAME)
                .GET()
                .build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status / 100 == 2) {
                long length = response.headers()
                    .firstValueAsLong("content-length")
                    .orElse(-1L);
                return new DownloadResponse(response.body(), length);
            }
            try (InputStream ignored = response.body()) {
                if (status / 100 == 3) {
                    String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new IOException("Download redirect missing Location header: HTTP " + status));
                    current = current.resolve(location);
                    continue;
                }
            }
            throw new IOException("Download failed: HTTP " + status + " from " + current);
        }
        throw new IOException("Download failed: too many redirects from " + uri);
    }

    private void copyWithProgress(InputStream in, OutputStream out, long totalBytes) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        long copied = 0L;
        long lastUiUpdate = 0L;
        setDownloadProgress(0, copied, totalBytes);
        while (true) {
            int read = in.read(buffer);
            if (read < 0) break;
            out.write(buffer, 0, read);
            copied += read;
            long now = System.nanoTime();
            if (now - lastUiUpdate > 150_000_000L) {
                setDownloadProgress(totalBytes > 0 ? (int) ((copied * 100) / totalBytes) : 0, copied, totalBytes);
                lastUiUpdate = now;
            }
        }
        out.flush();
        setDownloadProgress(100, copied, totalBytes);
    }

    private void loadSelected(ActionEvent event) {
        ModelItem model = selectedModel();
        if (model.online) {
            loadedModel = model;
            unloadLocal();
            setStatus("Loaded online model: " + model.name);
            return;
        }
        Path target = modelPath(model);
        if (!Files.exists(target)) {
            downloadSelected(event);
            return;
        }
        loadLocal(target, model);
    }

    private void selectCustomModel(ActionEvent event) {
        JFileChooser chooser = new JFileChooser(modelsDir.toFile());
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("GGUF models", "gguf"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            loadLocal(file.toPath(), new ModelItem(file.getName().replace(".gguf", ""), file.getName(), null, null, false, ""));
        }
    }

    private void chooseModelFolder(ActionEvent event) {
        JFileChooser chooser = new JFileChooser(modelsDir.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose model download folder");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            modelsDir = chooser.getSelectedFile().toPath();
            prefs.put("models_dir", modelsDir.toString());
            try {
                Files.createDirectories(modelsDir);
            } catch (IOException e) {
                appendSystem("Could not create model folder: " + e.getMessage());
            }
            modelFolderField.setText(compactPath(modelsDir));
            modelFolderField.setToolTipText(modelsDir.toString());
            setStatus("Model folder updated");
        }
    }

    private Path modelPath(ModelItem model) {
        return modelsDir.resolve(model.filename);
    }

    private void loadLocal(Path modelPath, ModelItem model) {
        if (!ensureRuntime()) return;
        unloadLocal();
        runAsync("Starting local runtime", () -> {
            Path exe = runtimeExe().orElseThrow();
            List<String> command = optimizedServerCommand(exe, modelPath);
            ProcessBuilder pb = optimizedProcessBuilder(exe, command);
            llamaServer = pb.start();
            raiseProcessPriority(llamaServer);
            loadedModel = model;
            Thread.sleep(3500);
            if (!llamaServer.isAlive()) {
                String output = new String(llamaServer.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("llama.cpp exited while loading. " + output.trim());
            }
            appendSystem("Loaded local model with max GPU offload + RAM preload: " + model.name);
        });
    }

    private List<String> optimizedServerCommand(Path exe, Path modelPath) {
        String help = llamaServerHelp(exe);
        int cpuCount = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(2, Math.min(cpuCount, cpuCount <= 8 ? cpuCount : cpuCount - 2));

        List<String> command = new ArrayList<>();
        command.add(exe.toString());
        command.add("-m"); command.add(modelPath.toString());
        command.add("--host"); command.add("127.0.0.1");
        command.add("--port"); command.add(String.valueOf(LLAMA_PORT));
        command.add("--threads"); command.add(String.valueOf(threads));
        command.add("--threads-batch"); command.add(String.valueOf(threads));
        command.add("--n-gpu-layers"); command.add("999");
        addIfSupported(command, help, "--split-mode", "layer");
        addIfSupported(command, help, "--main-gpu", "0");
        command.add("--ctx-size"); command.add("4096");
        command.add("--batch-size"); command.add("2048");
        command.add("--ubatch-size"); command.add("512");
        command.add("--parallel"); command.add("1");
        addIfSupported(command, help, "--cont-batching");
        addIfSupported(command, help, "--flash-attn");
        addIfSupported(command, help, "--cache-type-k", "q8_0");
        addIfSupported(command, help, "--cache-type-v", "q8_0");
        addIfSupported(command, help, "--no-mmap");
        addIfSupported(command, help, "--mlock");
        return command;
    }

    private ProcessBuilder optimizedProcessBuilder(Path exe, List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(exe.getParent().toFile());
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        String threads = commandValue(command, "--threads").orElse("4");
        env.put("OMP_NUM_THREADS", threads);
        return pb;
    }

    private String llamaServerHelp(Path exe) {
        try {
            Process process = new ProcessBuilder(exe.toString(), "--help")
                .directory(exe.getParent().toFile())
                .redirectErrorStream(true)
                .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return output;
        } catch (Exception ignored) {
            return "";
        }
    }

    private void addIfSupported(List<String> command, String help, String flag, String value) {
        if (help.isBlank() || help.contains(flag)) {
            command.add(flag);
            command.add(value);
        }
    }

    private void addIfSupported(List<String> command, String help, String flag) {
        if (help.isBlank() || help.contains(flag)) {
            command.add(flag);
        }
    }

    private Optional<String> commandValue(List<String> command, String flag) {
        int index = command.indexOf(flag);
        if (index < 0 || index + 1 >= command.size()) return Optional.empty();
        return Optional.of(command.get(index + 1));
    }

    private void raiseProcessPriority(Process process) {
        long pid = process.pid();
        try {
            new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "$p = Get-Process -Id " + pid + " -ErrorAction SilentlyContinue; if ($p) { $p.PriorityClass = 'High' }"
            ).redirectErrorStream(true).start();
        } catch (IOException ignored) {
            // Best effort only. The llama-server flags still handle the main speed path.
        }
    }

    private boolean ensureRuntime() {
        if (runtimeExe().isPresent()) return true;
        int choice = JOptionPane.showConfirmDialog(this, "llama.cpp runtime is not installed. Install it now?", APP_NAME, JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            installRuntime(null);
        }
        return false;
    }

    private Optional<Path> runtimeExe() {
        Optional<Path> marked = readRuntimeMarker();
        if (marked.isPresent()) return marked;
        Path saved = Paths.get(prefs.get("llama_server", runtimeDir.resolve("llama-server.exe").toString()));
        if (Files.exists(saved)) return Optional.of(saved);
        try (var stream = Files.walk(runtimeDir)) {
            return stream
                .filter(p -> p.getFileName().toString().equalsIgnoreCase("llama-server.exe"))
                .max(Comparator.comparingLong(this::lastModifiedMillis));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Path> readRuntimeMarker() {
        try {
            if (!Files.exists(runtimeMarker)) return Optional.empty();
            Path marked = Paths.get(Files.readString(runtimeMarker, StandardCharsets.UTF_8).trim());
            return Files.exists(marked) ? Optional.of(marked) : Optional.empty();
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private void installRuntime(ActionEvent event) {
        runAsync("Installing llama.cpp runtime", () -> {
            Path script = appData.resolve("install-llama-runtime.ps1");
            Files.writeString(script, INSTALL_SCRIPT, StandardCharsets.UTF_8);
            Process process = new ProcessBuilder(
                "powershell.exe", "-ExecutionPolicy", "Bypass", "-File", script.toString(), "-InstallDir", runtimeDir.toString()
            ).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) throw new IOException(output);
            Path installed = readRuntimeMarker().orElseGet(() -> runtimeExe().orElse(null));
            if (installed != null) prefs.put("llama_server", installed.toString());
            appendSystem("llama.cpp runtime installed");
        });
    }

    private void sendPrompt(ActionEvent event) {
        String prompt = inputField.getText().trim();
        if (prompt.isEmpty()) return;
        inputField.setText("");
        append("You", prompt);
        ModelItem model = loadedModel != null ? loadedModel : selectedModel();
        runAsync("Generating", () -> {
            String response = model.online ? sendOnline(model, prompt) : sendLocal(prompt);
            append("NEO", response);
        });
    }

    private String sendLocal(String prompt) throws Exception {
        if (llamaServer == null || !llamaServer.isAlive()) {
            throw new IllegalStateException("Load a local model or choose an online model first.");
        }
        return chatCompletions(URI.create("http://127.0.0.1:" + LLAMA_PORT + "/v1/chat/completions"), null, "local", prompt);
    }

    private String sendOnline(ModelItem model, String prompt) throws Exception {
        if ("openrouter".equals(model.provider)) {
            String key = openRouterApiKeyField.getText().trim();
            if (key.isEmpty()) throw new IllegalStateException("Save an OpenRouter API key first.");
            return chatCompletions(URI.create("https://openrouter.ai/api/v1/chat/completions"), key, model.onlineId, prompt, true);
        }
        String key = huggingFaceTokenField.getText().trim();
        if (key.isEmpty()) throw new IllegalStateException("Save a Hugging Face token first.");
        return chatCompletions(URI.create("https://router.huggingface.co/v1/chat/completions"), key, model.onlineId, prompt, true);
    }

    private String chatCompletions(URI uri, String key, String model, String prompt) throws Exception {
        return chatCompletions(uri, key, model, prompt, false);
    }

    private String chatCompletions(URI uri, String key, String model, String prompt, boolean forceJsonResponse) throws Exception {
        String cacheKey = model + "\n" + prompt;
        synchronized (responseCache) {
            String cached = responseCache.get(cacheKey);
            if (cached != null) return cached;
        }
        boolean local = key == null;
        String body = "{\"model\":\"" + json(model) + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + json(prompt) + "\"}]" +
            (local ? ",\"cache_prompt\":true" : "") +
            (forceJsonResponse ? ",\"stream\":false,\"max_tokens\":2048" : "") +
            "}";
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMinutes(10))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (key != null) {
            builder.header("Authorization", "Bearer " + key);
            if (uri.getHost() != null && uri.getHost().contains("openrouter.ai")) {
                builder.header("HTTP-Referer", "https://github.com/diazneoones82/Neo-Local-LLM");
                builder.header("X-Title", APP_NAME);
            }
        }
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IOException("LLM request failed: " + response.body());
        String content = extractContent(response.body());
        synchronized (responseCache) {
            responseCache.put(cacheKey, content);
        }
        return content;
    }

    private String extractContent(String json) {
        int key = json.indexOf("\"content\"");
        if (key < 0) return json;
        int colon = json.indexOf(':', key);
        int start = json.indexOf('"', colon + 1);
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                out.append(switch (c) { case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> c; });
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return out.toString();
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void unloadLocal() {
        if (llamaServer != null) {
            llamaServer.destroy();
            llamaServer = null;
        }
        if (loadedModel != null && !loadedModel.online) loadedModel = null;
        setStatus("Local model unloaded");
    }

    private void copyLastAssistant() {
        String text = chatArea.getText();
        int index = text.lastIndexOf("\nNEO:\n");
        if (index < 0) return;
        StringSelection selection = new StringSelection(text.substring(index + 6).trim());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        setStatus("Last response copied");
    }

    private ModelItem selectedModel() {
        return Objects.requireNonNull((ModelItem) modelCombo.getSelectedItem());
    }

    private void runAsync(String status, ThrowingRunnable runnable) {
        setStatus(status + "...");
        CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
                setStatus("Ready");
            } catch (Throwable t) {
                appendSystem("Error: " + t.getMessage());
                setStatus("Error");
            }
        });
    }

    private void append(String who, String text) {
        SwingUtilities.invokeLater(() -> chatArea.append("\n" + who + ":\n" + text.trim() + "\n"));
    }

    private void appendSystem(String text) {
        SwingUtilities.invokeLater(() -> chatArea.append("\n" + text + "\n"));
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void setDownloadProgress(int percent, long downloaded, long total) {
        SwingUtilities.invokeLater(() -> {
            if (percent < 0) {
                downloadProgress.setVisible(false);
                downloadProgress.setValue(0);
                downloadProgress.setString("");
                return;
            }
            downloadProgress.setVisible(true);
            downloadProgress.setIndeterminate(total <= 0);
            downloadProgress.setValue(Math.max(0, Math.min(100, percent)));
            downloadProgress.setString(total > 0
                ? percent + "%  " + formatBytes(downloaded) + " / " + formatBytes(total)
                : formatBytes(downloaded));
        });
    }

    private void openPath(Path path) {
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            appendSystem(e.getMessage());
        }
    }

    private void applyTheme() {
        boolean dark = darkMode.isSelected();
        Color bg = dark ? DARK_BG : LIGHT_BG;
        Color panel = dark ? DARK_PANEL : LIGHT_PANEL;
        Color field = dark ? DARK_FIELD : LIGHT_FIELD;
        Color button = dark ? DARK_BUTTON : LIGHT_BUTTON;
        Color fg = dark ? DARK_TEXT : LIGHT_TEXT;
        Color muted = dark ? DARK_MUTED : LIGHT_MUTED;
        Color border = dark ? DARK_BORDER : CHERRY;

        getContentPane().setBackground(bg);
        styleTree(getContentPane(), bg, panel, field, button, fg, muted, border, dark);
        chatArea.setBackground(new Color(22, 22, 22));
        chatArea.setForeground(fg);
        chatArea.setCaretColor(CHERRY);
        chatArea.setSelectionColor(new Color(85, 75, 98, dark ? 170 : 90));
        inputField.setBackground(field);
        inputField.setForeground(fg);
        inputField.setCaretColor(CHERRY);
        statusLabel.setForeground(muted);
        downloadProgress.setForeground(CHERRY);
        downloadProgress.setBackground(field);
        SwingUtilities.updateComponentTreeUI(this);
        styleTree(getContentPane(), bg, panel, field, button, fg, muted, border, dark);
    }

    private void styleTree(Component component, Color bg, Color panel, Color field, Color buttonColor, Color fg, Color muted, Color border, boolean dark) {
        if (component instanceof JPanel) {
            component.setBackground(component == getContentPane() ? bg : panel);
            component.setForeground(fg);
        }
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(border.getRed(), border.getGreen(), border.getBlue(), 130), 1, true));
            scrollPane.getViewport().setBackground(panel);
        }
        if (component instanceof JLabel label) {
            label.setForeground(label == statusLabel ? muted : fg);
            label.setFont(label.getFont().deriveFont(label.getFont().isBold() ? Font.BOLD : Font.PLAIN));
        }
        if (component instanceof JTextField textField) {
            textField.setBackground(field);
            textField.setForeground(fg);
            textField.setCaretColor(CHERRY);
            textField.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(border.getRed(), border.getGreen(), border.getBlue(), 135), 1, true),
                new EmptyBorder(7, 9, 7, 9)
            ));
        }
        if (component instanceof JComboBox<?> comboBox) {
            comboBox.setBackground(CONTROL_BG);
            comboBox.setForeground(CONTROL_FG);
            comboBox.setFont(UI_FONT_BOLD);
            comboBox.setOpaque(true);
            comboBox.setBorder(BorderFactory.createLineBorder(CONTROL_BORDER, 1, true));
        }
        if (component instanceof JButton button) {
            boolean accent = Boolean.TRUE.equals(button.getClientProperty("accent"));
            button.setBackground(accent ? CHERRY : CONTROL_BG);
            button.setForeground(accent ? Color.WHITE : fg);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setRolloverEnabled(true);
            button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(accent ? CHERRY_DARK : CONTROL_BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
            ));
            if (!accent) {
                button.setBackground(CONTROL_BG);
                button.setForeground(CONTROL_FG);
            }
        }
        if (component instanceof JCheckBox checkBox) {
            checkBox.setBackground(bg);
            checkBox.setForeground(fg);
            checkBox.setFont(UI_FONT_BOLD);
            checkBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        }
        if (component instanceof JProgressBar progressBar) {
            progressBar.setBackground(field);
            progressBar.setForeground(CHERRY);
            progressBar.setBorder(BorderFactory.createLineBorder(new Color(border.getRed(), border.getGreen(), border.getBlue(), 130), 1, true));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleTree(child, bg, panel, field, buttonColor, fg, muted, border, dark);
            }
        }
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing falls back to the cross-platform look and feel.
        }
        UIManager.put("Button.font", UI_FONT_BOLD);
        UIManager.put("ComboBox.font", UI_FONT_BOLD);
        UIManager.put("Label.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("TextArea.font", CHAT_FONT);
        UIManager.put("Button.background", CONTROL_BG);
        UIManager.put("Button.foreground", CONTROL_FG);
        UIManager.put("Button.select", new Color(72, 63, 82));
        UIManager.put("ComboBox.background", CONTROL_BG);
        UIManager.put("ComboBox.foreground", CONTROL_FG);
        UIManager.put("ComboBox.buttonBackground", CONTROL_BG);
        UIManager.put("ComboBox.buttonForeground", CONTROL_FG);
        UIManager.put("ComboBox.selectionBackground", CHERRY);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("List.background", CONTROL_BG);
        UIManager.put("List.foreground", CONTROL_FG);
        UIManager.put("List.selectionBackground", CHERRY);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.background", CONTROL_BG);
        UIManager.put("MenuItem.background", CONTROL_BG);
        UIManager.put("MenuItem.foreground", CONTROL_FG);
    }

    private static Image createCherryIconImage(int size) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size,
            size,
            java.awt.image.BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 240, 244));
            g.fillRoundRect(0, 0, size, size, size / 4, size / 4);
            drawCherry(g, size * 0.08, size * 0.08, size * 0.84);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void drawCherry(Graphics2D g, double x, double y, double s) {
        Graphics2D copy = (Graphics2D) g.create();
        try {
            copy.translate(x, y);
            copy.scale(s / 108.0, s / 108.0);
            copy.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            copy.setColor(new Color(255, 156, 176));
            Polygon petal = new Polygon(new int[] {38, 53, 47}, new int[] {30, 13, 34}, 3);
            copy.fill(petal);
            copy.setColor(LOGO_LEAF);
            Polygon leaf = new Polygon(new int[] {54, 82, 68}, new int[] {24, 15, 30}, 3);
            copy.fill(leaf);
            copy.setColor(LOGO_CHERRY_DARK);
            copy.drawLine(52, 25, 42, 50);
            copy.setColor(LOGO_CHERRY);
            copy.fillOval(16, 42, 58, 52);
            copy.fillOval(58, 42, 48, 52);
            copy.setColor(new Color(255, 240, 244));
            copy.fillOval(91, 58, 26, 25);
            copy.setColor(new Color(253, 232, 238, 220));
            copy.fillOval(25, 50, 28, 14);
        } finally {
            copy.dispose();
        }
    }

    private static final class CherryMark extends JComponent {
        private final int size;

        private CherryMark(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawCherry(g, 0, 0, size);
            } finally {
                g.dispose();
            }
        }
    }

    private static final class ModelRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setFont(UI_FONT_BOLD);
            label.setBorder(new EmptyBorder(7, 9, 7, 9));
            if (value instanceof ModelItem item) {
                label.setText(item.name());
                label.setToolTipText(item.name());
            }
            if (isSelected) {
                label.setBackground(CHERRY);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(CONTROL_BG);
                label.setForeground(CONTROL_FG);
            }
            return label;
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String formatBytes(long bytes) {
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        while (value >= 1024.0 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return unit == 0 ? bytes + " B" : String.format("%.1f %s", value, units[unit]);
    }

    private static Path defaultDownloadsFolder() {
        String home = System.getProperty("user.home");
        Path downloads = Paths.get(home, "Downloads");
        return Files.isDirectory(downloads) ? downloads : Paths.get(home);
    }

    private static String compactPath(Path path) {
        String value = path.toString();
        if (value.length() <= 18) return value;
        Path fileName = path.getFileName();
        Path parent = path.getParent();
        if (fileName == null) return value.substring(0, 12) + "..." + value.substring(value.length() - 16);
        String tail = fileName.toString();
        if (parent != null && parent.getFileName() != null) {
            tail = parent.getFileName() + "\\" + tail;
        }
        return "...\\" + tail;
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

private static final String INSTALL_SCRIPT = """
param([Parameter(Mandatory=$true)][string]$InstallDir)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Get-Process -Name 'llama-server' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
$release = Invoke-RestMethod -Uri 'https://api.github.com/repos/ggml-org/llama.cpp/releases/latest' -Headers @{ 'User-Agent' = 'Neo Local LLM' }
$asset = $release.assets |
  Where-Object { $_.name -match 'bin-win' -and $_.name -match 'x64' -and $_.name -match '\\.zip$' } |
  Sort-Object @{ Expression = { if ($_.name -match 'vulkan') { 0 } elseif ($_.name -match 'avx2') { 1 } else { 2 } } }, name |
  Select-Object -First 1
if (-not $asset) { throw 'Could not find a Windows x64 llama.cpp runtime asset in the latest release.' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$target = Join-Path $InstallDir "llama-$stamp"
New-Item -ItemType Directory -Force -Path $target | Out-Null
$zip = Join-Path $target $asset.name
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip -Headers @{ 'User-Agent' = 'Neo Local LLM' }
Expand-Archive -Path $zip -DestinationPath $target -Force
$server = Get-ChildItem -Path $target -Recurse -Filter 'llama-server.exe' | Select-Object -First 1
if (-not $server) { throw 'Downloaded runtime did not include llama-server.exe.' }
$marker = Join-Path $InstallDir 'llama-server.path'
Set-Content -LiteralPath $marker -Value $server.FullName -Encoding UTF8
Get-ChildItem -Path $InstallDir -Directory -Filter 'llama-*' |
  Where-Object { $_.FullName -ne $target } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -Skip 2 |
  ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }
Write-Output "Installed $($server.FullName)"
""";
}
