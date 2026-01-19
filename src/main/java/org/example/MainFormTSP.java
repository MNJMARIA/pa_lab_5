package org.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MainFormTSP extends JFrame {

    // Константи задачі (варіант 20)
    private static final int N_VERTICES = 200;
    private static final int DEFAULT_MAX_ITER = 1000;
    private static final int DEFAULT_STEP = 20;

    private double[][] distances;
    private double[][] pheromones;
    private double greedyMinLength;

    // GUI компоненти
    private JPanel mainPanel;
    private JTextField tfVertices, tfAlpha, tfBeta, tfRho, tfNumAnts, tfElite, tfMaxIter, tfStep;
    private JButton btnGenerate, btnRunACO, btnStop, btnSaveBest;
    private JTextArea taBestTour;
    private JTable tableProgress;
    private DefaultTableModel tableModel;
    private JPanel chartContainer;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    // Стан виконання
    private volatile boolean isRunning = false;
    private ChartPanel chartPanel;

    public MainFormTSP() {
        super("Лабораторна №5 • Мурашиний алгоритм • Задача комівояжера • Варіант 20");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        // Стиль як у попередній лабораторній
        try {
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Exception ignored) {}

        initComponents();
        setupLayout();
        setupListeners();

        setContentPane(mainPanel);
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 245, 245));

        // Поля параметрів
        tfVertices = new JTextField(String.valueOf(N_VERTICES));     tfVertices.setEditable(false);
        tfAlpha     = new JTextField("3.0");
        tfBeta      = new JTextField("2.0");
        tfRho       = new JTextField("0.7");
        tfNumAnts   = new JTextField("45");
        tfElite     = new JTextField("10");
        tfMaxIter   = new JTextField(String.valueOf(DEFAULT_MAX_ITER));
        tfStep      = new JTextField(String.valueOf(DEFAULT_STEP));

        btnGenerate = new JButton("Згенерувати граф");
        btnRunACO   = new JButton("Запустити ACO");
        btnStop     = new JButton("Зупинити");
        btnSaveBest = new JButton("Зберегти найкращий шлях");

        styleButton(btnGenerate, new Color(0, 102, 204));
        styleButton(btnRunACO,   new Color(0, 153, 76));
        styleButton(btnStop,     new Color(220, 53, 69));
        styleButton(btnSaveBest, new Color(108, 117, 125));

        taBestTour = new JTextArea(10, 60);
        taBestTour.setEditable(false);
        taBestTour.setFont(new Font("Consolas", Font.PLAIN, 13));
        taBestTour.setLineWrap(true);

        tableModel = new DefaultTableModel(new Object[]{"Ітерація", "Найкраща довжина"}, 0);
        tableProgress = new JTable(tableModel);
        tableProgress.setRowHeight(24);
        tableProgress.setShowGrid(true);

        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createTitledBorder("Залежність найкращої довжини від ітерацій"));

        lblStatus = new JLabel("Готовий до роботи", SwingConstants.CENTER);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(235, 245, 235));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(180, 42));
    }

    private void setupLayout() {
        // Заголовок
        JLabel title = new JLabel("Мурашиний алгоритм • Задача комівояжера • Варіант 20", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(0, 80, 160));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(title, BorderLayout.NORTH);

        // Панель параметрів (ліворуч)
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Параметри алгоритму"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        addParamField(paramsPanel, gbc, 0, "Кількість вершин:", tfVertices);
        addParamField(paramsPanel, gbc, 1, "α (вага феромону):", tfAlpha);
        addParamField(paramsPanel, gbc, 2, "β (вага евристики):", tfBeta);
        addParamField(paramsPanel, gbc, 3, "ρ (випаровування):", tfRho);
        addParamField(paramsPanel, gbc, 4, "Кількість мурах:", tfNumAnts);
        addParamField(paramsPanel, gbc, 5, "Елітні мурахи:", tfElite);
        addParamField(paramsPanel, gbc, 6, "Максимум ітерацій:", tfMaxIter);
        addParamField(paramsPanel, gbc, 7, "Крок фіксації:", tfStep);

        gbc.gridy = 8; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        paramsPanel.add(btnGenerate, gbc);
        gbc.gridy = 9; paramsPanel.add(btnRunACO, gbc);
        gbc.gridy = 10; paramsPanel.add(btnStop, gbc);
        gbc.gridy = 11; paramsPanel.add(btnSaveBest, gbc);

        mainPanel.add(paramsPanel, BorderLayout.WEST);

        // Центральна частина
        JPanel center = new JPanel(new BorderLayout(10, 10));

        JScrollPane scrollTour = new JScrollPane(taBestTour);
        scrollTour.setBorder(BorderFactory.createTitledBorder("Найкраще знайдене рішення"));
        center.add(scrollTour, BorderLayout.NORTH);

        JScrollPane scrollTable = new JScrollPane(tableProgress);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Прогрес ітерацій"));
        center.add(scrollTable, BorderLayout.CENTER);

        mainPanel.add(center, BorderLayout.CENTER);

        // Роздільник між центром і графіком
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(center);
        splitPane.setRightComponent(chartContainer);
        splitPane.setDividerLocation(600); // початкове положення роздільника (можна міняти)
        splitPane.setResizeWeight(0.6);    // центр займатиме більше місця спочатку
        splitPane.setOneTouchExpandable(true);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        // Графік
        //mainPanel.add(chartContainer, BorderLayout.EAST);

        // Статус-бар
        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.add(lblStatus, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
    }

    private void addParamField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
        field.setPreferredSize(new Dimension(120, 28));
    }

    private void setupListeners() {
        btnGenerate.addActionListener(e -> generateGraph());
        btnRunACO.addActionListener(e -> startACOThread());
        btnStop.addActionListener(e -> isRunning = false);
        btnSaveBest.addActionListener(e -> saveBestTour());
    }

    private void generateGraph() {
        distances = new double[N_VERTICES][N_VERTICES];
        Random rand = new Random();

        for (int i = 0; i < N_VERTICES; i++) {
            for (int j = i + 1; j < N_VERTICES; j++) {
                int d = rand.nextInt(40) + 1; // 1..40
                distances[i][j] = d;
                distances[j][i] = d;
            }
        }

        greedyMinLength = calculateGreedyTour();
        double initPheromone = 1.0 / (N_VERTICES * greedyMinLength);

        pheromones = new double[N_VERTICES][N_VERTICES];
        for (int i = 0; i < N_VERTICES; i++) {
            for (int j = 0; j < N_VERTICES; j++) {
                if (i != j) pheromones[i][j] = initPheromone;
            }
        }

        lblStatus.setText("Граф згенеровано. Жадібна оцінка: " + String.format("%.1f", greedyMinLength));
        taBestTour.setText("");
        tableModel.setRowCount(0);
        chartContainer.removeAll();
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    private double calculateGreedyTour() {
        boolean[] visited = new boolean[N_VERTICES];
        List<Integer> tour = new ArrayList<>();
        tour.add(0);
        visited[0] = true;
        double total = 0;

        for (int i = 1; i < N_VERTICES; i++) {
            int curr = tour.get(tour.size() - 1);
            double minDist = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < N_VERTICES; j++) {
                if (!visited[j] && distances[curr][j] < minDist) {
                    minDist = distances[curr][j];
                    next = j;
                }
            }
            tour.add(next);
            visited[next] = true;
            total += minDist;
        }
        total += distances[tour.get(tour.size()-1)][0];
        return total;
    }

    private void startACOThread() {
        if (distances == null) {
            JOptionPane.showMessageDialog(this, "Спочатку згенеруйте граф!", "Помилка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(this::runACO).start();
    }

    private void runACO() {
        isRunning = true;
        progressBar.setValue(0);
        lblStatus.setText("Виконується мурашиний алгоритм...");
        btnRunACO.setEnabled(false);
        btnStop.setEnabled(true);

        tableModel.setRowCount(0);    // Очистити таблицю перед новим запуском
        taBestTour.setText("");     // Очистити поле найкращого рішення
        chartContainer.removeAll();    // Очистити графік
        chartContainer.revalidate();
        chartContainer.repaint();

        lblStatus.setForeground(new Color(0, 0, 0)); // повертаємо чорний/стандартний колір
        boolean completedSuccessfully = false;

        try {
            double alpha = Double.parseDouble(tfAlpha.getText());
            double beta = Double.parseDouble(tfBeta.getText());
            double rho = Double.parseDouble(tfRho.getText());
            int numAnts = Integer.parseInt(tfNumAnts.getText());
            int elite = Integer.parseInt(tfElite.getText());
            int maxIter = Integer.parseInt(tfMaxIter.getText());
            int step = Integer.parseInt(tfStep.getText());

            double bestLength = Double.MAX_VALUE;
            List<Integer> bestTour = null;

            Random rand = new Random();

            for (int iter = 1; iter <= maxIter && isRunning; iter++) {
                List<List<Integer>> tours = new ArrayList<>();
                List<Double> lengths = new ArrayList<>();

                for (int k = 0; k < numAnts; k++) {
                    int start = rand.nextInt(N_VERTICES);
                    List<Integer> tour = buildTour(start, alpha, beta);
                    double len = calculateTourLength(tour);
                    tours.add(tour);
                    lengths.add(len);

                    if (len < bestLength) {
                        bestLength = len;
                        bestTour = new ArrayList<>(tour);
                    }
                }

                // Випаровування
                for (int i = 0; i < N_VERTICES; i++)
                    for (int j = 0; j < N_VERTICES; j++)
                        if (i != j) pheromones[i][j] *= (1 - rho);

                // Оновлення феромону
                for (int k = 0; k < numAnts; k++) {
                    List<Integer> tour = tours.get(k);
                    double delta = greedyMinLength / lengths.get(k);
                    double mult = (k < elite) ? 2.0 : 1.0;

                    for (int p = 0; p < tour.size() - 1; p++) {
                        int a = tour.get(p);
                        int b = tour.get(p + 1);
                        pheromones[a][b] += mult * delta;
                        pheromones[b][a] += mult * delta;
                    }
                }

                if (iter % step == 0 || iter == maxIter) {
                    //tableModel.addRow(new Object[]{iter, String.format("%.1f", bestLength)});
                    tableModel.addRow(new Object[]{iter, bestLength});   // double, а не String
                }

                progressBar.setValue((int)(iter * 100.0 / maxIter));
            }

            if (bestTour != null) {
                taBestTour.setText("Найкращий тур:\n" + bestTour + "\n\nДовжина: " + String.format("%.2f", bestLength));
                drawChart();
            }

            lblStatus.setText(isRunning ?
                    "Завершено. Найкраща довжина: " + String.format("%.1f", bestLength) :
                    "Обчислення зупинено користувачем");

            completedSuccessfully = true;
        } catch (Exception ex) {
            lblStatus.setText("Помилка: " + ex.getMessage());
            if (ex != null) {  // якщо була помилка
                lblStatus.setForeground(new Color(220, 53, 69)); // червоний
            } else {
                lblStatus.setForeground(new Color(0, 120, 0));   // зелений
            }

        } finally {
            isRunning = false;
            btnRunACO.setEnabled(true);
            btnStop.setEnabled(false);

            if (completedSuccessfully) {
                lblStatus.setForeground(new Color(0, 120, 0));   // зелений
            } else {
                lblStatus.setForeground(new Color(220, 53, 69)); // червоний
            }
        }
    }

    private List<Integer> buildTour(int start, double alpha, double beta) {
        List<Integer> tour = new ArrayList<>();
        boolean[] visited = new boolean[N_VERTICES];
        tour.add(start);
        visited[start] = true;

        while (tour.size() < N_VERTICES) {
            int curr = tour.get(tour.size() - 1);
            double[] probs = new double[N_VERTICES];
            double sum = 0.0;

            for (int next = 0; next < N_VERTICES; next++) {
                if (!visited[next]) {
                    double tau = Math.pow(pheromones[curr][next], alpha);
                    double eta = Math.pow(1.0 / distances[curr][next], beta);
                    probs[next] = tau * eta;
                    sum += probs[next];
                }
            }

            if (sum == 0) {
                for (int i = 0; i < N_VERTICES; i++)
                    if (!visited[i]) {
                        tour.add(i);
                        visited[i] = true;
                        break;
                    }
            } else {
                double r = Math.random() * sum;
                double partial = 0;
                for (int j = 0; j < N_VERTICES; j++) {
                    if (!visited[j]) {
                        partial += probs[j];
                        if (partial >= r) {
                            tour.add(j);
                            visited[j] = true;
                            break;
                        }
                    }
                }
            }
        }

        tour.add(start);
        return tour;
    }

    private double calculateTourLength(List<Integer> tour) {
        double len = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            len += distances[tour.get(i)][tour.get(i + 1)];
        }
        return len;
    }

    private void drawChart() {
        XYSeries series = new XYSeries("Найкраща довжина");
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int iter = Integer.parseInt(tableModel.getValueAt(i, 0).toString());
            //double val = Double.parseDouble(tableModel.getValueAt(i, 1).toString());
            /*double val = Double.parseDouble(
                    tableModel.getValueAt(i, 1).toString().replace(',', '.')
            );*/

            double val = (Double) tableModel.getValueAt(i, 1);
            series.add(iter, val);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Залежність найкращої довжини від ітерацій",
                "Ітерація", "Довжина туру", new XYSeriesCollection(series)
        );

        if (chartPanel != null) chartContainer.remove(chartPanel);
        chartPanel = new ChartPanel(chart);
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    private void saveBestTour() {
        String text = taBestTour.getText();
        if (text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Немає результату для збереження", "Попередження", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                fw.write(text);
                lblStatus.setText("Найкращий шлях збережено у файл");
            } catch (IOException ex) {
                lblStatus.setText("Помилка збереження: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFormTSP().setVisible(true));
    }
}