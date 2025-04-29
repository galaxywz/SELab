import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TextGraphProcessor extends JFrame { 
    private JButton loadFileButton;
    private JButton processButton;
    private JButton queryBridgeButton;  // 新增查询桥接词按钮
    private JButton generateTextButton; // 新增生成文本按钮
    private JButton shortestPathButton; // 新增最短路径按钮
    private JTextArea resultArea;
    private JPanel graphPanel;
    private File selectedFile;
    private Map<String, Map<String, Integer>> graph = new HashMap<>();
    private JLabel statusLabel;
    
    // 添加高亮显示相关的变量
    private String highlightWord1 = null;
    private String highlightWord2 = null;
    private List<String> highlightBridgeWords = null;
    private javax.swing.Timer highlightTimer = null;
    private final int HIGHLIGHT_DURATION = 5000; // 高亮显示持续5秒
    
    // 添加最短路径相关的变量
    private List<List<String>> shortestPaths = null;
    private Map<List<String>, Integer> pathLengths = null;
    private List<Color> pathColors = Arrays.asList(
        new Color(255, 0, 0, 200),    // 红色
        new Color(0, 128, 0, 200),    // 绿色
        new Color(0, 0, 255, 200),    // 蓝色
        new Color(255, 165, 0, 200),  // 橙色
        new Color(128, 0, 128, 200)   // 紫色
    );


    public TextGraphProcessor() {
        setTitle("文本图结构处理器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // 创建控制面板
        JPanel controlPanel = new JPanel();
        loadFileButton = new JButton("加载文本文件");
        processButton = new JButton("处理并生成图");
        queryBridgeButton = new JButton("查询桥接词");  // 新增按钮
        generateTextButton = new JButton("生成新文本"); // 新增按钮
        shortestPathButton = new JButton("计算最短路径"); // 新增按钮
        processButton.setEnabled(false);
        queryBridgeButton.setEnabled(false);  // 初始禁用
        generateTextButton.setEnabled(false); // 初始禁用
        shortestPathButton.setEnabled(false); // 初始禁用
        statusLabel = new JLabel("请选择文本文件");

        controlPanel.add(loadFileButton);
        controlPanel.add(processButton);
        controlPanel.add(queryBridgeButton);  // 添加到控制面板
        controlPanel.add(generateTextButton); // 添加到控制面板
        controlPanel.add(shortestPathButton); // 添加到控制面板
        controlPanel.add(statusLabel);
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 创建分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // 创建结果文本区域
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        splitPane.setLeftComponent(scrollPane);

        // 创建图显示面板
        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGraph(g);
            }
        };
        graphPanel.setBackground(Color.WHITE);
        JScrollPane graphScrollPane = new JScrollPane(graphPanel);
        splitPane.setRightComponent(graphScrollPane);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel);

        // 添加事件处理
        loadFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件", "txt"));
                int result = fileChooser.showOpenDialog(TextGraphProcessor.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    statusLabel.setText("已选择文件: " + selectedFile.getName());
                    processButton.setEnabled(true);
                }
            }
        });

        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedFile != null) {
                    processTextFile();
                    queryBridgeButton.setEnabled(true);  // 处理完成后启用查询按钮
                    generateTextButton.setEnabled(true); // 处理完成后启用生成文本按钮
                    shortestPathButton.setEnabled(true); // 处理完成后启用最短路径按钮
                }
            }
        });
        
        // 添加查询桥接词按钮的事件处理
        queryBridgeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showBridgeWordDialog();
            }
        });
        
        // 添加生成新文本按钮的事件处理
        generateTextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGenerateTextDialog();
            }
        });

        // 添加最短路径按钮的事件处理
        shortestPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showShortestPathDialog();
            }
        });        
    }

    // 显示查询桥接词的对话框
    private void showBridgeWordDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField word1Field = new JTextField(10);
        JTextField word2Field = new JTextField(10);
        
        panel.add(new JLabel("第一个单词:"));
        panel.add(word1Field);
        panel.add(new JLabel("第二个单词:"));
        panel.add(word2Field);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "查询桥接词", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String word1 = word1Field.getText().trim().toLowerCase();
            String word2 = word2Field.getText().trim().toLowerCase();
            
            if (word1.isEmpty() || word2.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入两个单词", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            findBridgeWords(word1, word2);
        }
    }
    
    // 查找桥接词的方法
    private void findBridgeWords(String word1, String word2) {
        // 检查两个单词是否都在图中
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            resultArea.append("\n查询结果: No " + 
                    ((!graph.containsKey(word1) && !graph.containsKey(word2)) ? "word1 or word2" : 
                     (!graph.containsKey(word1) ? "word1" : "word2")) + 
                    " in the graph!\n");
            return;
        }
        
        // 查找所有从word1出发能到达word2的桥接词
        List<String> bridgeWords = new ArrayList<>();
        Map<String, Integer> outEdges = graph.get(word1);
        
        for (String potentialBridge : outEdges.keySet()) {
            // 检查潜在桥接词是否有指向word2的边
            if (graph.containsKey(potentialBridge) && 
                graph.get(potentialBridge).containsKey(word2)) {
                bridgeWords.add(potentialBridge);
            }
        }
        
        // 根据结果输出不同信息
        if (bridgeWords.isEmpty()) {
            resultArea.append("\n查询结果: No bridge words from " + word1 + " to " + word2 + "!\n");
        } else {
            StringBuilder result = new StringBuilder("\n查询结果: The bridge words from " + word1 + " to " + word2 + " are: ");
            
            if (bridgeWords.size() == 1) {
                result.append(bridgeWords.get(0)).append(".");
            } else {
                for (int i = 0; i < bridgeWords.size() - 1; i++) {
                    result.append(bridgeWords.get(i)).append(", ");
                }
                result.append("and ").append(bridgeWords.get(bridgeWords.size() - 1)).append(".");
            }
            
            resultArea.append(result.toString() + "\n");
            
            // 高亮显示桥接词路径
            highlightBridgeWords(word1, word2, bridgeWords);
        }
    }
    
    private void highlightBridgeWords(String word1, String word2, List<String> bridgeWords) {
        // 设置高亮显示的单词和桥接词
        highlightWord1 = word1;
        highlightWord2 = word2;
        highlightBridgeWords = new ArrayList<>(bridgeWords);
        
        // 重绘图形面板以显示高亮路径
        graphPanel.repaint();
        
        // 如果已有计时器在运行，先停止它
        if (highlightTimer != null && highlightTimer.isRunning()) {
            highlightTimer.stop();
        }
        
        // 创建一个计时器，在指定时间后取消高亮显示
        highlightTimer = new javax.swing.Timer(HIGHLIGHT_DURATION, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 清除高亮显示
                highlightWord1 = null;
                highlightWord2 = null;
                highlightBridgeWords = null;
                graphPanel.repaint();
                highlightTimer.stop();
            }
        });
        highlightTimer.setRepeats(false);
        highlightTimer.start();
    }

    // 显示计算最短路径的对话框
    private void showShortestPathDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField word1Field = new JTextField(10);
        JTextField word2Field = new JTextField(10);
        
        panel.add(new JLabel("起始单词:"));
        panel.add(word1Field);
        panel.add(new JLabel("目标单词:"));
        panel.add(word2Field);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "计算最短路径", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String word1 = word1Field.getText().trim().toLowerCase();
            String word2 = word2Field.getText().trim().toLowerCase();
            
            if (word1.isEmpty() || word2.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入两个单词", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            findShortestPaths(word1, word2);
        }
    }
    
    // 查找最短路径的方法
    private void findShortestPaths(String startWord, String endWord) {
        // 清除之前的高亮显示
        highlightWord1 = null;
        highlightWord2 = null;
        highlightBridgeWords = null;
        
        // 检查两个单词是否都在图中
        if (!graph.containsKey(startWord) || !graph.containsKey(endWord)) {
            resultArea.append("\n计算结果: 输入的单词 " + 
                    ((!graph.containsKey(startWord) && !graph.containsKey(endWord)) ? startWord + " 和 " + endWord : 
                     (!graph.containsKey(startWord) ? startWord : endWord)) + 
                    " 不在图中!\n");
            return;
        }
        
        // 使用Dijkstra算法找到所有最短路径
        Map<String, Integer> distances = new HashMap<>();
        Map<String, List<List<String>>> paths = new HashMap<>();
        
        // 初始化
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
            paths.put(node, new ArrayList<>());
        }
        distances.put(startWord, 0);
        List<String> initialPath = new ArrayList<>();
        initialPath.add(startWord);
        paths.get(startWord).add(initialPath);
        
        // 优先队列，按距离排序
        PriorityQueue<String> queue = new PriorityQueue<>(
            (a, b) -> Integer.compare(distances.get(a), distances.get(b))
        );
        queue.add(startWord);
        
        // 记录已处理的节点
        Set<String> processed = new HashSet<>();
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            // 如果已经处理过，跳过
            if (processed.contains(current)) {
                continue;
            }
            
            // 如果已经到达终点，不需要继续处理
            if (current.equals(endWord)) {
                break;
            }
            
            processed.add(current);
            
            // 处理所有邻居
            if (graph.containsKey(current)) {
                for (Map.Entry<String, Integer> edge : graph.get(current).entrySet()) {
                    String neighbor = edge.getKey();
                    int weight = edge.getValue();
                    int newDistance = distances.get(current) + weight;
                    
                    // 如果找到更短的路径
                    if (newDistance < distances.get(neighbor)) {
                        distances.put(neighbor, newDistance);
                        
                        // 更新路径
                        List<List<String>> newPaths = new ArrayList<>();
                        for (List<String> path : paths.get(current)) {
                            List<String> newPath = new ArrayList<>(path);
                            newPath.add(neighbor);
                            newPaths.add(newPath);
                        }
                        paths.put(neighbor, newPaths);
                        
                        // 添加到队列中继续处理
                        queue.add(neighbor);
                    }
                    // 如果找到相同长度的路径
                    else if (newDistance == distances.get(neighbor)) {
                        // 添加新的路径
                        for (List<String> path : paths.get(current)) {
                            List<String> newPath = new ArrayList<>(path);
                            newPath.add(neighbor);
                            paths.get(neighbor).add(newPath);
                        }
                    }
                }
            }
        }
        
        // 获取最短路径
        if (distances.get(endWord) == Integer.MAX_VALUE) {
            resultArea.append("\n计算结果: 从 " + startWord + " 到 " + endWord + " 不可达!\n");
            return;
        }
        
        // 保存所有最短路径和路径长度
        shortestPaths = paths.get(endWord);
        pathLengths = new HashMap<>();
        
        for (List<String> path : shortestPaths) {
            int pathLength = calculatePathLength(path);
            pathLengths.put(path, pathLength);
        }
        
        // 输出结果
        resultArea.append("\n计算结果: 从 " + startWord + " 到 " + endWord + " 的最短路径:\n");
        
        for (int i = 0; i < shortestPaths.size(); i++) {
            List<String> path = shortestPaths.get(i);
            int pathLength = pathLengths.get(path);
            
            StringBuilder pathStr = new StringBuilder();
            for (int j = 0; j < path.size() - 1; j++) {
                pathStr.append(path.get(j)).append("→");
            }
            pathStr.append(path.get(path.size() - 1));
            
            resultArea.append("路径 " + (i + 1) + ": " + pathStr.toString() + ", 长度: " + pathLength + "\n");
        }
        
        // 重绘图形面板以显示最短路径
        graphPanel.repaint();
        
        // 如果已有计时器在运行，先停止它
        if (highlightTimer != null && highlightTimer.isRunning()) {
            highlightTimer.stop();
        }
        
        // 创建一个计时器，在指定时间后取消高亮显示
        highlightTimer = new javax.swing.Timer(HIGHLIGHT_DURATION * 2, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 清除高亮显示
                shortestPaths = null;
                pathLengths = null;
                graphPanel.repaint();
                highlightTimer.stop();
            }
        });
        highlightTimer.setRepeats(false);
        highlightTimer.start();
    }
    
    // 计算路径长度
    private int calculatePathLength(List<String> path) {
        int length = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String current = path.get(i);
            String next = path.get(i + 1);
            length += graph.get(current).get(next);
        }
        return length;
    }

    private void processTextFile() {
        resultArea.setText("");
        graph.clear();

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(" ");
                }
            }

            // 处理文本，忽略非字母字符，标点符号当作空格
            String processedText = content.toString().replaceAll("[^a-zA-Z]", " ").toLowerCase();
            resultArea.append("处理后的文本:\n" + processedText + "\n\n");

            // 分词并构建图
            String[] words = processedText.split("\\s+");
            List<String> wordList = new ArrayList<>();
            for (String word : words) {
                if (!word.isEmpty()) {
                    wordList.add(word);
                }
            }

            resultArea.append("提取的单词列表:\n" + wordList + "\n\n");

            // 构建有向图
            for (int i = 0; i < wordList.size() - 1; i++) {
                String currentWord = wordList.get(i);
                String nextWord = wordList.get(i + 1);

                if (!graph.containsKey(currentWord)) {
                    graph.put(currentWord, new HashMap<>());
                }

                Map<String, Integer> edges = graph.get(currentWord);
                edges.put(nextWord, edges.getOrDefault(nextWord, 0) + 1);
            }

            // 输出图结构
            resultArea.append("生成的有向图结构:\n");
            for (String node : graph.keySet()) {
                resultArea.append(node + " -> ");
                Map<String, Integer> edges = graph.get(node);
                for (Map.Entry<String, Integer> edge : edges.entrySet()) {
                    resultArea.append(edge.getKey() + "(" + edge.getValue() + ") ");
                }
                resultArea.append("\n");
            }

            // 更新图面板
            graphPanel.repaint();
            statusLabel.setText("图生成完成，共 " + graph.keySet().size() + " 个节点");

        } catch (IOException ex) {
            resultArea.setText("处理文件时出错: " + ex.getMessage());
            statusLabel.setText("处理失败");
        }
    }

    private void drawGraph(Graphics g) {
        if (graph.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 设置节点位置
        Map<String, Point> nodePositions = new HashMap<>();
        List<String> nodes = new ArrayList<>(graph.keySet());
        int nodeCount = nodes.size();

        // 圆形布局
        int radius = Math.min(graphPanel.getWidth(), graphPanel.getHeight()) / 2 - 80;
        int centerX = graphPanel.getWidth() / 2;
        int centerY = graphPanel.getHeight() / 2;

        for (int i = 0; i < nodeCount; i++) {
            double angle = 2 * Math.PI * i / nodeCount;
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));
            nodePositions.put(nodes.get(i), new Point(x, y));
        }

        // 绘制边
        for (String fromNode : graph.keySet()) {
            Point fromPoint = nodePositions.get(fromNode);
            Map<String, Integer> edges = graph.get(fromNode);

            for (Map.Entry<String, Integer> edge : edges.entrySet()) {
                String toNode = edge.getKey();
                int weight = edge.getValue();

                // 检查目标节点是否存在于布局中
                if (nodePositions.containsKey(toNode)) {
                    Point toPoint = nodePositions.get(toNode);

                    // 检查是否需要高亮显示这条边
                    boolean isHighlightedEdge = false;
                    boolean isShortestPathEdge = false;
                    int pathIndex = -1;
                    
                    // 检查是否是桥接词路径
                    if (highlightWord1 != null && highlightWord2 != null && highlightBridgeWords != null) {
                        // 高亮显示从word1到桥接词的边
                        if (fromNode.equals(highlightWord1) && highlightBridgeWords.contains(toNode)) {
                            isHighlightedEdge = true;
                        }
                        // 高亮显示从桥接词到word2的边
                        else if (highlightBridgeWords.contains(fromNode) && toNode.equals(highlightWord2)) {
                            isHighlightedEdge = true;
                        }
                    }
                    
                    // 检查是否是最短路径
                    if (shortestPaths != null) {
                        for (int i = 0; i < shortestPaths.size(); i++) {
                            List<String> path = shortestPaths.get(i);
                            for (int j = 0; j < path.size() - 1; j++) {
                                if (path.get(j).equals(fromNode) && path.get(j + 1).equals(toNode)) {
                                    isShortestPathEdge = true;
                                    pathIndex = i;
                                    break;
                                }
                            }
                            if (isShortestPathEdge) {
                                break;
                            }
                        }
                    }

                    // 设置边的颜色和粗细基于权重和是否高亮
                    float thickness = Math.min(weight, 5);
                    if (isShortestPathEdge) {
                        thickness += 2; // 最短路径边更粗
                        g2d.setStroke(new BasicStroke(thickness));
                        // 使用不同颜色区分多条最短路径
                        g2d.setColor(pathColors.get(pathIndex % pathColors.size()));
                    } else if (isHighlightedEdge) {
                        thickness += 2; // 高亮边更粗
                        g2d.setStroke(new BasicStroke(thickness));
                        g2d.setColor(new Color(255, 0, 0, 200)); // 高亮边为红色
                    } else {
                        g2d.setStroke(new BasicStroke(thickness));
                        g2d.setColor(new Color(0, 0, 255, 150)); // 普通边为蓝色
                    }

                    // 画线
                    g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);

                    // 绘制权重标签
                    int labelX = (fromPoint.x + toPoint.x) / 2;
                    int labelY = (fromPoint.y + toPoint.y) / 2;
                    g2d.setColor(Color.RED);
                    g2d.drawString(String.valueOf(weight), labelX, labelY);

                    // 绘制箭头
                    drawArrow(g2d, fromPoint, toPoint, isHighlightedEdge, isShortestPathEdge, pathIndex);
                }
            }
        }

        // 绘制节点
        g2d.setStroke(new BasicStroke(1.0f));
        for (String node : nodePositions.keySet()) {
            Point point = nodePositions.get(node);
            int nodeSize = 30;
            
            // 检查是否需要高亮显示这个节点
            boolean isHighlightedNode = false;
            boolean isShortestPathNode = false;
            Set<Integer> pathIndices = new HashSet<>();
            
            // 检查是否是桥接词节点
            if (highlightWord1 != null && highlightWord2 != null && highlightBridgeWords != null) {
                if (node.equals(highlightWord1) || node.equals(highlightWord2) || highlightBridgeWords.contains(node)) {
                    isHighlightedNode = true;
                }
            }
            
            // 检查是否是最短路径节点
            if (shortestPaths != null) {
                for (int i = 0; i < shortestPaths.size(); i++) {
                    if (shortestPaths.get(i).contains(node)) {
                        isShortestPathNode = true;
                        pathIndices.add(i);
                    }
                }
            }

            // 节点圆形
            if (isShortestPathNode) {
                // 如果节点在多条路径上，使用特殊颜色
                if (pathIndices.size() > 1) {
                    g2d.setColor(new Color(255, 255, 0, 200)); // 多条路径共享节点为黄色
                } else {
                    // 使用对应路径的颜色
                    int pathIndex = pathIndices.iterator().next();
                    g2d.setColor(pathColors.get(pathIndex % pathColors.size()));
                }
            } else if (isHighlightedNode) {
                g2d.setColor(new Color(255, 200, 200)); // 高亮节点为浅红色
            } else {
                g2d.setColor(new Color(200, 200, 255)); // 普通节点为浅蓝色
            }
            g2d.fillOval(point.x - nodeSize/2, point.y - nodeSize/2, nodeSize, nodeSize);
            
            if (isShortestPathNode) {
                g2d.setColor(new Color(0, 0, 0)); // 最短路径节点边框为黑色
                g2d.setStroke(new BasicStroke(2.0f)); // 最短路径节点边框更粗
            } else if (isHighlightedNode) {
                g2d.setColor(new Color(255, 0, 0)); // 高亮节点边框为红色
                g2d.setStroke(new BasicStroke(2.0f)); // 高亮节点边框更粗
            } else {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1.0f));
            }
            g2d.drawOval(point.x - nodeSize/2, point.y - nodeSize/2, nodeSize, nodeSize);

            // 节点标签
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(node);
            if (isShortestPathNode) {
                g2d.setColor(new Color(0, 0, 0)); // 最短路径节点文字为黑色
            } else if (isHighlightedNode) {
                g2d.setColor(new Color(255, 0, 0)); // 高亮节点文字为红色
            } else {
                g2d.setColor(Color.BLACK);
            }
            g2d.drawString(node, point.x - textWidth/2, point.y + 5);
        }
    }

    // 修改drawArrow方法以支持最短路径显示
    private void drawArrow(Graphics2D g2d, Point from, Point to, boolean isHighlighted, boolean isShortestPath, int pathIndex) {
        int arrowSize = 10;
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double angle = Math.atan2(dy, dx);
        int len = (int) Math.sqrt(dx*dx + dy*dy);

        // 调整箭头位置使其不覆盖节点
        int nodeRadius = 15;
        int x2 = from.x + (int) ((len - nodeRadius) * Math.cos(angle));
        int y2 = from.y + (int) ((len - nodeRadius) * Math.sin(angle));

        // 箭头的两个点
        int x3 = x2 - (int) (arrowSize * Math.cos(angle - Math.PI/6));
        int y3 = y2 - (int) (arrowSize * Math.sin(angle - Math.PI/6));
        int x4 = x2 - (int) (arrowSize * Math.cos(angle + Math.PI/6));
        int y4 = y2 - (int) (arrowSize * Math.sin(angle + Math.PI/6));

        // 绘制箭头
        if (isShortestPath) {
            g2d.setColor(pathColors.get(pathIndex % pathColors.size())); // 最短路径箭头使用对应颜色
        } else if (isHighlighted) {
            g2d.setColor(new Color(255, 0, 0)); // 高亮箭头为红色
        } else {
            g2d.setColor(Color.BLACK);
        }
        g2d.fillPolygon(new int[]{x2, x3, x4}, new int[]{y2, y3, y4}, 3);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TextGraphProcessor app = new TextGraphProcessor();
                app.setVisible(true);

                // 检查是否通过命令行参数提供了文件路径
                if (args.length > 0) {
                    File file = new File(args[0]);
                    if (file.exists() && file.isFile()) {
                        app.selectedFile = file;
                        app.statusLabel.setText("已通过参数加载文件: " + file.getName());
                        app.processButton.setEnabled(true);
                    }
                }
            }
        });
    }

    // 显示生成新文本的对话框
    private void showGenerateTextDialog() {
    JPanel panel = new JPanel(new BorderLayout());
    JTextArea inputTextArea = new JTextArea(5, 30);
    inputTextArea.setLineWrap(true);
    inputTextArea.setWrapStyleWord(true);
    
    panel.add(new JLabel("请输入文本:"), BorderLayout.NORTH);
    panel.add(new JScrollPane(inputTextArea), BorderLayout.CENTER);
    
    int result = JOptionPane.showConfirmDialog(this, panel, "生成新文本", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
    if (result == JOptionPane.OK_OPTION) {
        String inputText = inputTextArea.getText().trim();
        
        if (inputText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入文本", "输入错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        generateNewText(inputText);
    }
}

    // 根据桥接词生成新文本
    private void generateNewText(String inputText) {
    // 处理输入文本，忽略非字母字符，标点符号当作空格
    String processedText = inputText.replaceAll("[^a-zA-Z]", " ").toLowerCase();
    
    // 分词
    String[] words = processedText.split("\\s+");
    List<String> wordList = new ArrayList<>();
    for (String word : words) {
        if (!word.isEmpty()) {
            wordList.add(word);
        }
    }
    
    if (wordList.size() < 2) {
        resultArea.append("\n生成新文本结果: 输入文本中单词数量不足，无法生成新文本。\n");
        return;
    }
    
    // 生成新文本
    List<String> newTextList = new ArrayList<>();
    newTextList.add(wordList.get(0));
    
    for (int i = 0; i < wordList.size() - 1; i++) {
        String currentWord = wordList.get(i);
        String nextWord = wordList.get(i + 1);
        
        // 查找桥接词
        List<String> bridgeWords = findBridgeWordsForGeneration(currentWord, nextWord);
        
        // 如果存在桥接词，随机选择一个插入
        if (!bridgeWords.isEmpty()) {
            Random random = new Random();
            String selectedBridge = bridgeWords.get(random.nextInt(bridgeWords.size()));
            newTextList.add(selectedBridge);
        }
        
        // 添加下一个单词
        newTextList.add(nextWord);
    }
    
    // 将单词列表转换为文本
    StringBuilder newText = new StringBuilder();
    for (String word : newTextList) {
        newText.append(word).append(" ");
    }
    
    resultArea.append("\n生成新文本结果:\n原文本: " + inputText + "\n新文本: " + newText.toString().trim() + "\n");
}

    // 查找用于生成新文本的桥接词
    private List<String> findBridgeWordsForGeneration(String word1, String word2) {
        List<String> bridgeWords = new ArrayList<>();
        
        // 检查两个单词是否都在图中
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return bridgeWords; // 返回空列表
        }
        
        // 查找所有从word1出发能到达word2的桥接词
        Map<String, Integer> outEdges = graph.get(word1);
        
        for (String potentialBridge : outEdges.keySet()) {
            // 检查潜在桥接词是否有指向word2的边
            if (graph.containsKey(potentialBridge) && 
                graph.get(potentialBridge).containsKey(word2)) {
                bridgeWords.add(potentialBridge);
            }
        }
        
        return bridgeWords;
    }
}

