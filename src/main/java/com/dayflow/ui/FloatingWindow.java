package com.dayflow.ui;

import com.dayflow.model.Task;
import com.dayflow.model.TaskPriority;
import com.dayflow.model.TaskView;
import com.dayflow.store.TaskStore;
import com.dayflow.util.AppPreferences;
import com.dayflow.util.Win32Window;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 半透明桌面悬浮窗：常驻最前，显示今天待完成的任务。
 *
 * <p>支持拖动、直接勾选完成、随数据自动刷新；按设置里的快捷键可切换
 * 「鼠标穿透」，穿透后窗口仍然可见但完全不拦截鼠标。
 */
public final class FloatingWindow {

    private static final String TITLE = "DayFlow 悬浮窗";
    /** 留够空间给 DropShadow，否则圆角阴影会被窗口边缘切掉。 */
    private static final int OUTER_PADDING = 20;
    /** 边缘拖拽热区厚度。 */
    private static final int GRIP = 7;
    private static final int MIN_WIDTH = 280;
    private static final int MIN_HEIGHT = 130;

    private static FloatingWindow instance;

    private final TaskStore store;
    private final Stage stage = new Stage(StageStyle.TRANSPARENT);

    private final VBox card = new VBox(8);
    private final VBox listBox = new VBox(6);
    private final ScrollPane listScroll = new ScrollPane(listBox);
    private final Label titleLabel = new Label("今天");
    private final Label countLabel = new Label();
    private final Label dateLabel = new Label();
    private final Label stateLabel = new Label();
    private final Button closeButton = new Button("✕");

    private boolean clickThrough;
    private com.sun.jna.platform.win32.WinDef.HWND hwnd;

    private double dragOffsetX;
    private double dragOffsetY;

    private double resizeStartX;
    private double resizeStartY;
    private double resizeStartW;
    private double resizeStartH;
    private double resizeStartStageX;
    private double resizeStartStageY;
    private boolean resizingLeft;
    private boolean resizingRight;
    private boolean resizingTop;
    private boolean resizingBottom;

    // 外观设置（可在设置对话框里实时修改）
    private int width = AppPreferences.getFloatWidth();
    /** 窗口高度；0 表示跟随内容自动变化。 */
    private int userHeight = AppPreferences.getFloatHeight();
    private int maxRows = AppPreferences.getFloatRows();
    private double backgroundOpacity = AppPreferences.getFloatOpacity();
    private String backgroundHex = AppPreferences.getFloatColor();

    private FloatingWindow(TaskStore store) {
        this.store = store;

        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        countLabel.setStyle("-fx-font-size: 12px;");
        dateLabel.setStyle("-fx-font-size: 11px;");

        closeButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 2 0 2;"
        );
        closeButton.setOnAction(e -> stage.hide());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(8, titleLabel, countLabel, headerSpacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        listScroll.setMinHeight(0);
        listScroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;"
        );
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        card.getChildren().addAll(header, dateLabel, listScroll, stateLabel);
        card.setPadding(new Insets(16, 18, 14, 18));
        card.setEffect(new DropShadow(14, 5, 5, Color.rgb(0, 0, 0, 0.22)));
        // 让卡片撑满窗口：用户拖大窗口时列表区跟着伸展，拖小时压缩并出现滚动条
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane root = new StackPane(card, buildResizer());
        root.setPadding(new Insets(OUTER_PADDING));
        root.setStyle("-fx-background-color: transparent;");

        root.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        card.setPrefWidth(width);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        stage.setTitle(TITLE);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setScene(scene);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMaxX() - width - 2 * OUTER_PADDING - 24);
        stage.setY(bounds.getMinY() + 36);

        store.addListener(() -> Platform.runLater(this::refresh));

        Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(60), e -> refresh()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();

        registerHotkey();
        refresh();
    }

    /** 主界面/快捷键入口：创建或切换悬浮窗显示状态。 */
    public static void toggle(TaskStore store) {
        FloatingWindow window = ensure(store);
        if (window.stage.isShowing()) {
            window.stage.hide();
        } else {
            window.show();
        }
    }

    /** 开机自动展示悬浮窗（主程序退到后台时仍能看到今天的任务）。 */
    public static void showAtStartup(TaskStore store) {
        FloatingWindow window = ensure(store);
        if (!window.stage.isShowing()) {
            window.show();
        }
    }

    /** 切换鼠标穿透（悬浮窗未显示时先显示出来）。 */
    public static void toggleClickThrough() {
        if (instance == null) {
            return;
        }
        FloatingWindow window = instance;

        Platform.runLater(() -> {
            if (!window.stage.isShowing()) {
                window.show();
                return;
            }
            window.setClickThrough(!window.clickThrough);
        });
    }

    /** 设置变更后立即套用新的透明度、颜色、尺寸与条数。 */
    public static void applyPreferences() {
        if (instance == null) {
            return;
        }
        FloatingWindow window = instance;

        Platform.runLater(() -> {
            window.width = AppPreferences.getFloatWidth();
            window.userHeight = AppPreferences.getFloatHeight();
            window.maxRows = AppPreferences.getFloatRows();
            window.backgroundOpacity = AppPreferences.getFloatOpacity();
            window.backgroundHex = AppPreferences.getFloatColor();
            window.registerHotkey();
            window.refresh();
        });
    }

    public static boolean isShowing() {
        return instance != null && instance.stage.isShowing();
    }

    private static FloatingWindow ensure(TaskStore store) {
        if (instance == null) {
            instance = new FloatingWindow(store);
        }
        return instance;
    }

    private void registerHotkey() {
        Win32Window.applyHotkey(AppPreferences.getHotkey(),
                () -> Platform.runLater(FloatingWindow::toggleClickThrough));
    }

    private void show() {
        stage.show();
        stage.toFront();
        // 透明窗口的尺寸必须在显示后按真实内容重算一次，否则会停在
        // 构造时 Scene 的初始高度（内容被裁掉）。
        refresh();
        resolveHandle(6);
    }

    /** 按内容实际高度设置窗口尺寸，并保证不越出屏幕可视区域。 */
    private void fitToContent() {
        double w = width + 2 * OUTER_PADDING;
        double h = userHeight > 0
                ? userHeight
                : card.prefHeight(width) + 2 * OUTER_PADDING;

        if (Double.isNaN(stage.getWidth()) || Math.abs(stage.getWidth() - w) > 0.5) {
            stage.setWidth(w);
        }
        if (Double.isNaN(stage.getHeight()) || Math.abs(stage.getHeight() - h) > 0.5) {
            stage.setHeight(h);
        }

        keepOnScreen();
    }

    /**
     * 卡片四周的透明拖拽热区：鼠标移到边缘变成缩放光标，拖动即可改尺寸，
     * 行为与普通窗口一致。
     */
    private AnchorPane buildResizer() {
        AnchorPane resizer = new AnchorPane();
        resizer.setPickOnBounds(false);
        // root 的 padding(OUTER_PADDING) 已经让内容区正好等于卡片范围，
        // 这里不能再加 margin，否则热区会缩进到卡片内部。

        Region north = grip(Cursor.N_RESIZE, false, false, true, false, GRIP, GRIP);
        AnchorPane.setTopAnchor(north, 0.0);
        AnchorPane.setLeftAnchor(north, 0.0);
        AnchorPane.setRightAnchor(north, 0.0);

        Region south = grip(Cursor.S_RESIZE, false, false, false, true, GRIP, GRIP);
        AnchorPane.setBottomAnchor(south, 0.0);
        AnchorPane.setLeftAnchor(south, 0.0);
        AnchorPane.setRightAnchor(south, 0.0);

        Region west = grip(Cursor.W_RESIZE, true, false, false, false, GRIP, GRIP);
        AnchorPane.setLeftAnchor(west, 0.0);
        AnchorPane.setTopAnchor(west, (double) GRIP);
        AnchorPane.setBottomAnchor(west, (double) GRIP);

        Region east = grip(Cursor.E_RESIZE, false, true, false, false, GRIP, GRIP);
        AnchorPane.setRightAnchor(east, 0.0);
        AnchorPane.setTopAnchor(east, (double) GRIP);
        AnchorPane.setBottomAnchor(east, (double) GRIP);

        Region nw = grip(Cursor.NW_RESIZE, true, false, true, false, GRIP, GRIP);
        AnchorPane.setTopAnchor(nw, 0.0);
        AnchorPane.setLeftAnchor(nw, 0.0);

        Region ne = grip(Cursor.NE_RESIZE, false, true, true, false, GRIP, GRIP);
        AnchorPane.setTopAnchor(ne, 0.0);
        AnchorPane.setRightAnchor(ne, 0.0);

        Region sw = grip(Cursor.SW_RESIZE, true, false, false, true, GRIP, GRIP);
        AnchorPane.setBottomAnchor(sw, 0.0);
        AnchorPane.setLeftAnchor(sw, 0.0);

        Region se = grip(Cursor.SE_RESIZE, false, true, false, true, GRIP, GRIP);
        AnchorPane.setBottomAnchor(se, 0.0);
        AnchorPane.setRightAnchor(se, 0.0);

        resizer.getChildren().addAll(north, south, west, east, nw, ne, sw, se);
        return resizer;
    }

    private Region grip(Cursor cursor, boolean left, boolean right,
                        boolean top, boolean bottom, double w, double h) {
        Region grip = new Region();
        grip.setCursor(cursor);
        grip.setStyle("-fx-background-color: transparent;");
        grip.setPrefSize(w, h);
        grip.setOnMousePressed(e -> {
            beginResize(e, left, right, top, bottom);
            e.consume();
        });
        grip.setOnMouseDragged(e -> {
            resize(e);
            e.consume();
        });
        grip.setOnMouseReleased(e -> {
            finishResize();
            e.consume();
        });
        return grip;
    }

    private void beginResize(MouseEvent e, boolean left, boolean right,
                             boolean top, boolean bottom) {
        resizeStartX = e.getScreenX();
        resizeStartY = e.getScreenY();
        resizeStartW = stage.getWidth();
        resizeStartH = stage.getHeight();
        resizeStartStageX = stage.getX();
        resizeStartStageY = stage.getY();

        resizingLeft = left;
        resizingRight = right;
        resizingTop = top;
        resizingBottom = bottom;
    }

    private void resize(MouseEvent e) {
        double dx = e.getScreenX() - resizeStartX;
        double dy = e.getScreenY() - resizeStartY;

        double w = resizeStartW;
        double h = resizeStartH;
        double x = resizeStartStageX;
        double y = resizeStartStageY;

        if (resizingRight) {
            w = resizeStartW + dx;
        }
        if (resizingLeft) {
            w = resizeStartW - dx;
        }
        if (resizingBottom) {
            h = resizeStartH + dy;
        }
        if (resizingTop) {
            h = resizeStartH - dy;
        }

        w = Math.max(MIN_WIDTH + 2 * OUTER_PADDING, w);
        h = Math.max(MIN_HEIGHT, h);

        if (resizingLeft) {
            x = resizeStartStageX + (resizeStartW - w);
        }
        if (resizingTop) {
            y = resizeStartStageY + (resizeStartH - h);
        }

        stage.setWidth(w);
        stage.setHeight(h);
        stage.setX(x);
        stage.setY(y);

        width = (int) Math.round(w - 2 * OUTER_PADDING);
        card.setPrefWidth(width);
    }

    private void finishResize() {
        width = Math.max(MIN_WIDTH, (int) Math.round(stage.getWidth() - 2 * OUTER_PADDING));
        userHeight = (int) Math.round(stage.getHeight());

        AppPreferences.setFloatWidth(width);
        AppPreferences.setFloatHeight(userHeight);

        refresh();
    }

    private void keepOnScreen() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double maxX = bounds.getMaxX() - stage.getWidth();
        double maxY = bounds.getMaxY() - stage.getHeight();

        stage.setX(Math.max(bounds.getMinX(), Math.min(stage.getX(), maxX)));
        stage.setY(Math.max(bounds.getMinY(), Math.min(stage.getY(), maxY)));
    }

    private void resolveHandle(int attemptsLeft) {
        hwnd = Win32Window.findWindow(TITLE).orElse(null);

        if (hwnd != null) {
            Win32Window.markAsToolWindow(hwnd);
            Win32Window.setClickThrough(hwnd, clickThrough);
            return;
        }

        if (attemptsLeft > 0) {
            PauseTransition retry = new PauseTransition(Duration.millis(120));
            retry.setOnFinished(e -> resolveHandle(attemptsLeft - 1));
            retry.play();
        }
    }

    private void setClickThrough(boolean on) {
        clickThrough = on;
        Win32Window.setClickThrough(hwnd, on);
        applyStyle();
        updateFooter();
    }

    /** 背景色 / 透明度 / 尺寸由偏好决定，深浅背景自动换文字颜色。 */
    private void applyStyle() {
        card.setPrefWidth(width);

        Color base = Color.web(backgroundHex);
        double alpha = Math.min(1.0,
                Math.max(0.15, backgroundOpacity + (clickThrough ? 0.10 : 0)));

        String background = String.format(Locale.US,
                "rgba(%d, %d, %d, %.2f)",
                (int) Math.round(base.getRed() * 255),
                (int) Math.round(base.getGreen() * 255),
                (int) Math.round(base.getBlue() * 255),
                alpha);

        card.setStyle(
                "-fx-background-color: " + background + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(49, 93, 181, 0.20);" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;"
        );

        boolean dark = base.getBrightness() < 0.45 && alpha > 0.45;
        String primary = dark ? "#FFFFFF" : "#202B3C";
        String muted = dark ? "rgba(255, 255, 255, 0.75)" : "#667085";

        titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + primary + ";");
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + muted + ";");
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + muted + ";");
        closeButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 2 0 2;" +
                        "-fx-text-fill: " + muted + ";"
        );
    }

    private String primaryInk() {
        Color base = Color.web(backgroundHex);
        boolean dark = base.getBrightness() < 0.45 && backgroundOpacity > 0.45;
        return dark ? "#FFFFFF" : "#263449";
    }

    private String mutedInk() {
        Color base = Color.web(backgroundHex);
        boolean dark = base.getBrightness() < 0.45 && backgroundOpacity > 0.45;
        return dark ? "rgba(255, 255, 255, 0.75)" : "#667085";
    }

    private void updateFooter() {
        String hotkey = AppPreferences.getHotkey();
        stateLabel.setText(clickThrough
                ? "👆 鼠标已穿透 · " + hotkey + " 恢复点击"
                : hotkey + " 鼠标穿透 · 按住拖动可移动");
        stateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                + (clickThrough ? "#315DB5" : mutedInk()) + ";");
    }

    private void refresh() {
        List<Task> tasks = store.visibleTasks(TaskView.TODAY);
        countLabel.setText(tasks.isEmpty() ? "" : tasks.size() + " 项未完成");
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)));

        listBox.getChildren().clear();

        if (tasks.isEmpty()) {
            Label empty = new Label("🎉 今天的任务都完成了");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: " + mutedInk() + ";");
            empty.setPadding(new Insets(6, 0, 10, 0));
            listBox.getChildren().add(empty);
        } else {
            int shown = Math.min(tasks.size(), maxRows);
            for (int i = 0; i < shown; i++) {
                listBox.getChildren().add(row(tasks.get(i)));
            }
            if (tasks.size() > shown) {
                Label more = new Label("还有 " + (tasks.size() - shown) + " 项…");
                more.setStyle("-fx-font-size: 11px; -fx-text-fill: " + mutedInk() + ";");
                listBox.getChildren().add(more);
            }
        }

        applyStyle();
        updateFooter();

        // 新加入的行（CheckBox 等）在 CSS/skin 生效前 prefHeight 偏小，
        // 先强制布局，再按真实内容高度调整窗口。
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.getRoot().applyCss();
            scene.getRoot().layout();
        }
        fitToContent();
    }

    private HBox row(Task task) {
        CheckBox done = new CheckBox();
        done.setStyle("-fx-cursor: hand;");
        done.setOnAction(e -> {
            task.setCompleted(true);
            store.touch();
        });

        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                + TaskPriority.color(task.priority()) + ";");

        Label time = new Label(task.getTime() == null ? "--:--" : task.getTime().toString());
        time.setStyle("-fx-font-size: 11px; -fx-text-fill: " + mutedInk() + ";");
        time.setMinWidth(42);

        Label name = new Label(task.getTitle());
        name.setStyle("-fx-font-size: 13px; -fx-text-fill: " + primaryInk() + ";");
        name.setMaxWidth(Math.max(120, width - 128));
        HBox.setHgrow(name, Priority.ALWAYS);

        HBox row = new HBox(5, done, dot, time, name);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));
        return row;
    }
}
