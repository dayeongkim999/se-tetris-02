package com.example.game.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MenuOverlay {
    
    public enum MenuType {
        PAUSE_MENU,
        GAME_OVER_MENU,
        SETTINGS_MENU
    }
    
    private StackPane overlay;
    private VBox menuContainer;
    private Rectangle background;
    
    // 키보드 네비게이션을 위한 변수들 추가
    private List<Button> menuButtons;
    private int selectedButtonIndex = 0;
    
    // 콜백 인터페이스
    public interface MenuCallback {
        void onResume();
        void onRestart();
        void onSettings();
        void onMainMenu();
        void onExit();
    }
    
    public MenuOverlay() {
        createOverlay();
        setupKeyboardNavigation();
    }
    
    private void createOverlay() {
        overlay = new StackPane();
        overlay.setVisible(false);
        
        // 반투명 배경
        background = new Rectangle();
        background.setFill(Color.color(0, 0, 0, 0.8));
        background.widthProperty().bind(overlay.widthProperty());
        background.heightProperty().bind(overlay.heightProperty());
        
        // 메뉴 컨테이너
        menuContainer = new VBox(15);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(40));
        menuContainer.getStyleClass().add("menu-overlay");
        
        overlay.getChildren().addAll(background, menuContainer);
    }
    
    // 키보드 네비게이션 설정 추가
    private void setupKeyboardNavigation() {
        overlay.setFocusTraversable(true);
        overlay.setOnKeyPressed(event -> {
            if (!overlay.isVisible() || menuButtons == null || menuButtons.isEmpty()) {
                return;
            }
            
            KeyCode code = event.getCode();
            switch (code) {
                case UP:
                case W:
                    navigateUp();
                    break;
                case DOWN:
                case S:
                    navigateDown();
                    break;
                case ENTER:
                case SPACE:
                    selectCurrentButton();
                    break;
                case ESCAPE:
                    handleEscape();
                    break;
            }
            event.consume();
        });
    }
    
    private void navigateUp() {
        if (menuButtons.isEmpty()) return;
        
        updateButtonStyle(menuButtons.get(selectedButtonIndex), false);
        selectedButtonIndex = (selectedButtonIndex - 1 + menuButtons.size()) % menuButtons.size();
        updateButtonStyle(menuButtons.get(selectedButtonIndex), true);
    }
    
    private void navigateDown() {
        if (menuButtons.isEmpty()) return;
        
        updateButtonStyle(menuButtons.get(selectedButtonIndex), false);
        selectedButtonIndex = (selectedButtonIndex + 1) % menuButtons.size();
        updateButtonStyle(menuButtons.get(selectedButtonIndex), true);
    }
    
    private void selectCurrentButton() {
        if (!menuButtons.isEmpty() && selectedButtonIndex < menuButtons.size()) {
            menuButtons.get(selectedButtonIndex).fire();
        }
    }
    
    private void updateButtonStyle(Button button, boolean selected) {
        if (selected) {
            button.setStyle(
                "-fx-background-color: #3bb78f; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #0abab5; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(59,183,143,0.8), 15, 0, 0, 0);"
            );
        } else {
            button.setStyle(
                "-fx-background-color: #16213e; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #3bb78f; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;"
            );
        }
    }
    
    public void showPauseMenu(MenuCallback callback) {
        showMenu(MenuType.PAUSE_MENU, callback, null);
    }
    
    public void showGameOverMenu(MenuCallback callback, int finalScore) {
        showMenu(MenuType.GAME_OVER_MENU, callback, finalScore);
    }
    
    public void showSettingsMenu(MenuCallback callback) {
        showMenu(MenuType.SETTINGS_MENU, callback, null);
    }
    
    private void showMenu(MenuType menuType, MenuCallback callback, Integer finalScore) {
        menuContainer.getChildren().clear();
        menuButtons = new ArrayList<>(); // 버튼 리스트 초기화
        selectedButtonIndex = 0;
        
        // 타이틀
        Text title = createTitle(menuType, finalScore);
        if (!title.getText().isEmpty()) {
            menuContainer.getChildren().add(title);
        }
        
        // 버튼들
        switch (menuType) {
            case PAUSE_MENU:
                addPauseMenuButtons(callback);
                break;
            case GAME_OVER_MENU:
                addGameOverMenuButtons(callback);
                break;
            case SETTINGS_MENU:
                addSettingsMenuButtons(callback);
                break;
        }
        
        // 첫 번째 버튼 선택 상태로 설정
        if (!menuButtons.isEmpty()) {
            updateButtonStyle(menuButtons.get(0), true);
        }
        
        overlay.setVisible(true);
        overlay.toFront();
        overlay.requestFocus();
    }
    
    private Text createTitle(MenuType menuType, Integer finalScore) {
        Text title = new Text();
        title.getStyleClass().add("menu-title");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.WHITE);
        
        switch (menuType) {
            case PAUSE_MENU:
                title.setText("PAUSED");
                break;
            case GAME_OVER_MENU:
                title.setText("GAME OVER");
                if (finalScore != null) {
                    Text scoreText = new Text("Final Score: " + String.format("%,d", finalScore));
                    scoreText.getStyleClass().add("menu-score");
                    scoreText.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
                    scoreText.setFill(Color.LIGHTGRAY);
                    
                    VBox titleContainer = new VBox(10);
                    titleContainer.setAlignment(Pos.CENTER);
                    titleContainer.getChildren().addAll(title, scoreText);
                    
                    menuContainer.getChildren().add(titleContainer);
                    return new Text(); // 빈 텍스트 반환 (이미 컨테이너에 추가됨)
                }
                break;
            case SETTINGS_MENU:
                title.setText("SETTINGS");
                break;
        }
        
        return title;
    }
    
    private void addPauseMenuButtons(MenuCallback callback) {
        Button resumeBtn = createMenuButton("Resume", () -> {
            hide();
            callback.onResume();
        });
        
        Button settingsBtn = createMenuButton("Settings", () -> {
            callback.onSettings();
        });
        
        Button restartBtn = createMenuButton("Restart", () -> {
            hide();
            callback.onRestart();
        });
        
        Button mainMenuBtn = createMenuButton("Main Menu", () -> {
            hide();
            callback.onMainMenu();
        });
        
        // 버튼들을 리스트에 추가
        menuButtons.add(resumeBtn);
        menuButtons.add(settingsBtn);
        menuButtons.add(restartBtn);
        menuButtons.add(mainMenuBtn);
        
        menuContainer.getChildren().addAll(resumeBtn, settingsBtn, restartBtn, mainMenuBtn);
    }
    
    private void addGameOverMenuButtons(MenuCallback callback) {
        Button restartBtn = createMenuButton("Play Again", () -> {
            hide();
            callback.onRestart();
        });
        
        Button mainMenuBtn = createMenuButton("Main Menu", () -> {
            hide();
            callback.onMainMenu();
        });
        
        Button exitBtn = createMenuButton("Exit", () -> {
            callback.onExit();
        });
        
        // 버튼들을 리스트에 추가
        menuButtons.add(restartBtn);
        menuButtons.add(mainMenuBtn);
        menuButtons.add(exitBtn);
        
        menuContainer.getChildren().addAll(restartBtn, mainMenuBtn, exitBtn);
    }
    
    private void addSettingsMenuButtons(MenuCallback callback) {
        Button colorSchemeBtn = createMenuButton("Color Scheme", () -> {
            // 컬러 스킴 변경 로직
        });
        
        Button controlsBtn = createMenuButton("Controls", () -> {
            // 컨트롤 설정 로직
        });
        
        Button backBtn = createMenuButton("Back", () -> {
            hide();
            callback.onResume();
        });
        
        // 버튼들을 리스트에 추가
        menuButtons.add(colorSchemeBtn);
        menuButtons.add(controlsBtn);
        menuButtons.add(backBtn);
        
        menuContainer.getChildren().addAll(colorSchemeBtn, controlsBtn, backBtn);
    }
    
    private Button createMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setPrefWidth(200);
        button.setPrefHeight(50);
        button.setOnAction(e -> action.run());
        
        // 마우스 호버 이벤트 (키보드 선택과 연동)
        button.setOnMouseEntered(e -> {
            if (menuButtons != null && menuButtons.contains(button)) {
                updateButtonStyle(menuButtons.get(selectedButtonIndex), false);
                selectedButtonIndex = menuButtons.indexOf(button);
                updateButtonStyle(button, true);
            }
        });
        
        return button;
    }
    
    private void handleEscape() {
        // ESC 키 처리 - Resume 버튼이 있으면 Resume 실행, 없으면 첫 번째 버튼 실행
        if (!menuButtons.isEmpty()) {
            // Resume 버튼을 찾아서 실행
            for (Button button : menuButtons) {
                if (button.getText().equals("Resume")) {
                    button.fire();
                    return;
                }
            }
            // Resume 버튼이 없으면 첫 번째 버튼 실행
            menuButtons.get(0).fire();
        }
    }
    
    public void hide() {
        overlay.setVisible(false);
        if (menuButtons != null) {
            menuButtons.clear();
        }
        selectedButtonIndex = 0;
    }
    
    public StackPane getOverlay() {
        return overlay;
    }
    
    public boolean isVisible() {
        return overlay.isVisible();
    }
}