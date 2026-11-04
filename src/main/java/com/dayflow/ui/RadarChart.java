package com.dayflow.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 六维雷达图：展示任务的 6 个优先级维度。
 *
 * <p>纯 JavaFX Canvas 绘制，无第三方依赖。
 */
public class RadarChart extends VBox {

    private static final double SIZE = 220;

    private static final String[] AXES = {
            "重要", "紧急", "难度", "耗时", "进度", "约束"
    };

    private final Canvas canvas;

    // 六个维度的值（0-1 归一化）
    private double importance;
    private double urgency;
    private double difficulty;
    private double timeCost;    // 耗时（归一化，越大越费时间）
    private double progress;
    private double constraint;  // 0 或 1

    public RadarChart() {
        canvas = new Canvas(SIZE, SIZE);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(4));
        getChildren().add(canvas);
    }

    /**
     * 设置六维度原始值并绘制。
     *
     * @param importance 1-5
     * @param urgency    1-5
     * @param difficulty 1-5
     * @param minutes    预计耗时分钟（15-480）
     * @param progress   0-100
     * @param constraint 是否硬截止
     */
    public void setValues(int importance, int urgency, int difficulty,
                          int minutes, int progress, boolean constraint) {
        this.importance = clamp01((importance - 1) / 4.0);
        this.urgency = clamp01((urgency - 1) / 4.0);
        this.difficulty = clamp01((difficulty - 1) / 4.0);
        // 耗时取对数后归一化：15min→0, 480min→1
        this.timeCost = clamp01(
                (Math.log(minutes / 15.0) / Math.log(480.0 / 15.0)));
        this.progress = clamp01(progress / 100.0);
        this.constraint = constraint ? 1.0 : 0.0;
        draw();
    }

    private double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cx = SIZE / 2;
        double cy = SIZE / 2;
        double radius = SIZE / 2 - 24; // 留给标签
        double angleStep = 2 * Math.PI / AXES.length;

        gc.clearRect(0, 0, SIZE, SIZE);

        // —— 画 5 层同心六边形 ——
        gc.setLineWidth(1);
        gc.setStroke(Color.web("#D0D5DD"));
        for (int ring = 1; ring <= 5; ring++) {
            double r = radius * ring / 5.0;
            gc.beginPath();
            for (int i = 0; i <= AXES.length; i++) {
                double angle = i * angleStep - Math.PI / 2;
                double x = cx + Math.cos(angle) * r;
                double y = cy + Math.sin(angle) * r;
                if (i == 0) gc.moveTo(x, y);
                else gc.lineTo(x, y);
            }
            gc.closePath();
            gc.stroke();
        }

        // —— 画 6 条轴线 ——
        gc.setStroke(Color.web("#98A2B3"));
        for (int i = 0; i < AXES.length; i++) {
            double angle = i * angleStep - Math.PI / 2;
            double ex = cx + Math.cos(angle) * radius;
            double ey = cy + Math.sin(angle) * radius;
            gc.beginPath();
            gc.moveTo(cx, cy);
            gc.lineTo(ex, ey);
            gc.stroke();
        }

        // —— 画数据多边形 ——
        double[] values = {
                importance, urgency, difficulty, timeCost, progress, constraint
        };

        gc.beginPath();
        for (int i = 0; i <= AXES.length; i++) {
            int idx = i % AXES.length;
            double angle = idx * angleStep - Math.PI / 2;
            double r = radius * values[idx];
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }
        gc.closePath();
        gc.setFill(Color.web("#315DB5", 0.35));
        gc.fill();
        gc.setStroke(Color.web("#315DB5"));
        gc.setLineWidth(2);
        gc.stroke();

        // —— 画 6 个数据点 ——
        for (int i = 0; i < AXES.length; i++) {
            double angle = i * angleStep - Math.PI / 2;
            double r = radius * values[i];
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            gc.setFill(Color.web("#315DB5"));
            gc.fillOval(x - 3.5, y - 3.5, 7, 7);
        }

        // —— 画轴标签 ——
        gc.setFont(Font.font("Segoe UI", 12));
        gc.setFill(Color.web("#202B3C"));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < AXES.length; i++) {
            double angle = i * angleStep - Math.PI / 2;
            double tr = radius + 16;
            double x = cx + Math.cos(angle) * tr;
            double y = cy + Math.sin(angle) * tr + 4;
            gc.fillText(AXES[i], x, y);
        }
    }
}
