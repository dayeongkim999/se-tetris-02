package com.example.game.component;

import java.util.Map;

import com.example.Router;
import com.example.settings.GameSettings;
import com.example.game.component.MenuOverlay.MenuCallback;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Board {
    public static final int CELL_SIZE = 25;
    
    private StackPane mainContainer; // 메인 컨테이너 (오버레이 포함)
    private BorderPane root; // 게임 보드 레이아웃
    private Canvas canvas;
    private GraphicsContext gc;
    private GameLogic gameLogic;
    private ScorePanel scorePanel;
    private GameSettings gameSettings;
    private MenuOverlay menuOverlay; // 메뉴 오버레이 추가
    
    private AnimationTimer gameLoop;
    private long lastUpdate = 0;
    private final long baseDropInterval = 1_000_000_000L; // 1초 (기본 속도)
    private boolean isPaused = false;
    
    public Board() {
        // 컴포넌츠 초기화
        gameSettings = GameSettings.getInstance();
        gameLogic = new GameLogic();
        scorePanel = new ScorePanel();
        menuOverlay = new MenuOverlay(); // 오버레이 초기화
        
        // UI 초기화
        initializeUI();
        // 키 입력 처리 설정
        setupKeyHandling();
        // 게임 루프 시작
        startGameLoop();
        // 초기 보드 그리기
        drawBoard();
    }
    
    // UI 초기화
    private void initializeUI() {
        // 메인 컨테이너 생성 (오버레이를 위한 StackPane)
        mainContainer = new StackPane();
        
        // 게임 보드 레이아웃
        root = new BorderPane();
        root.getStyleClass().add("game-root");
        
        // 점수판 설정
        scorePanel.getPanel().getStyleClass().add("side-panel");
        canvas = new Canvas(GameLogic.WIDTH * CELL_SIZE, GameLogic.HEIGHT * CELL_SIZE);
        gc = canvas.getGraphicsContext2D();

        // 레이아웃 설정
        root.setCenter(canvas);
        root.setRight(scorePanel.getPanel());
        root.setPadding(new Insets(20));
        
        // 메인 컨테이너에 게임 보드와 오버레이 추가
        mainContainer.getChildren().addAll(root, menuOverlay.getOverlay());
    }
    
    private void setupKeyHandling() {
        mainContainer.setFocusTraversable(true);
        mainContainer.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            
            // 메뉴가 열려있을 때 ESC 키 처리
            if (menuOverlay.isVisible()) {
                if (code == KeyCode.ESCAPE) {
                    // 메뉴가 열려있을 때 ESC를 누르면 메뉴 닫고 게임 재개
                    resumeGame();
                }
                // 다른 키들은 MenuOverlay에서 처리
                return;
            }
            
            // 게임 오버 상태에서는 ESC 키만 허용 (게임 오버 메뉴 표시)
            if (gameLogic.isGameOver()) {
                if (code == KeyCode.ESCAPE) {
                    showGameOverMenu();
                }
                return;
            }
            
            // 일반 게임 중 키 처리
            switch (code) {
                case LEFT:
                    if (!isPaused) gameLogic.moveLeft();
                    break;
                case RIGHT:
                    if (!isPaused) gameLogic.moveRight();
                    break;
                case DOWN:
                    if (!isPaused) handleMoveDown();
                    break;
                case UP:
                    if (!isPaused) gameLogic.rotateBlock();
                    break;
                case ESCAPE:
                    // ESC 키 토글: 일시정지 ↔ 재개
                    togglePause();
                    break;
                case SPACE:
                    if (!isPaused && !gameLogic.isGameOver()) {
                        hardDrop();
                    }
                    break;
            }
            
            // 메뉴가 보이지 않을 때만 보드 다시 그리기
            if (!menuOverlay.isVisible()) {
                drawBoard();
            }
        });
    }

    // ESC 키 처리를 토글 방식으로 변경
    private void togglePause() {
        if (isPaused) {
            // 현재 일시정지 상태 → 재개
            resumeGame();
        } else {
            // 현재 게임 중 → 일시정지 및 메뉴 표시
            pauseGame();
        }
    }
    
    // 게임 일시정지 및 메뉴 표시
    private void pauseGame() {
        isPaused = true;
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        menuOverlay.showPauseMenu(new MenuCallback() {
            @Override
            public void onResume() {
                resumeGame();
            }
            
            @Override
            public void onRestart() {
                restartGame();
            }
            
            @Override
            public void onSettings() {
                showSettingsMenu();
            }
            
            @Override
            public void onMainMenu() {
                goToMainMenu();
            }
            
            @Override
            public void onExit() {
                exitGame();
            }
        });
    }
    
    // 게임 재개
    private void resumeGame() {
        isPaused = false;
        menuOverlay.hide();
        if (!gameLogic.isGameOver()) {
            startGameLoop();
        }
        // 포커스를 다시 게임으로 이동
        mainContainer.requestFocus();
    }
    
    // 게임 오버 메뉴 표시
    private void showGameOverMenu() {
        menuOverlay.showGameOverMenu(new MenuCallback() {
            @Override
            public void onResume() {
                // 게임 오버에서는 재개 불가
            }
            
            @Override
            public void onRestart() {
                restartGame();
            }
            
            @Override
            public void onSettings() {
                showSettingsMenu();
            }
            
            @Override
            public void onMainMenu() {
                goToMainMenu();
            }
            
            @Override
            public void onExit() {
                exitGame();
            }
        }, scorePanel.getScore());
    }
    
    // 설정 메뉴 표시
    private void showSettingsMenu() {
        menuOverlay.showSettingsMenu(new MenuCallback() {
            @Override
            public void onResume() {
                if (gameLogic.isGameOver()) {
                    showGameOverMenu();
                } else {
                    resumeGame();
                }
            }
            
            @Override
            public void onRestart() {
                restartGame();
            }
            
            @Override
            public void onSettings() {
                // 이미 설정 메뉴에 있음
            }
            
            @Override
            public void onMainMenu() {
                goToMainMenu();
            }
            
            @Override
            public void onExit() {
                exitGame();
            }
        });
    }
    
    // 메인 메뉴로 이동
    private void goToMainMenu() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        Stage stage = (Stage) mainContainer.getScene().getWindow();
        Router router = new Router(stage);
        router.showStartMenu();
    }
    
    // 게임 종료
    private void exitGame() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        Stage stage = (Stage) mainContainer.getScene().getWindow();
        stage.close();
    }
    
    private void startGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isPaused || menuOverlay.isVisible()) return;
                
                // 동적으로 계산된 드롭 간격 사용
                long currentDropInterval = gameLogic.getDropInterval(baseDropInterval);
                
                if (now - lastUpdate >= currentDropInterval) {
                    handleMoveDown();
                    // 보드 다시 그리기
                    drawBoard();
                    lastUpdate = now;
                    
                    // 속도 정보 업데이트
                    updateSpeedDisplay();
                }
            }
        };
        gameLoop.start();
    }

    // 속도 표시 업데이트
    private void updateSpeedDisplay() {
        double speedMultiplier = gameLogic.getSpeedMultiplier();
        int speedLevel = gameLogic.getSpeedLevel();
        scorePanel.updateSpeed(speedMultiplier, speedLevel);
    }

    // 블록 아래로 이동 처리
    private void handleMoveDown() {
        boolean moved = gameLogic.moveDown();
        
        if (!moved) {
            // 라인 제거 및 점수 계산
            int linesCleared = gameLogic.clearLines();
            if (linesCleared > 0) {
                scorePanel.calculateLineScore(linesCleared);
                // 속도가 변경되었을 수 있으므로 업데이트
                updateSpeedDisplay();
            }
            
            // 게임 오버 체크
            if (gameLogic.isGameOver()) {
                gameOver();
            }
        }
    }
    
    private void gameOver() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        isPaused = true;
        
        // 자동으로 게임 오버 메뉴 표시
        showGameOverMenu();
    }
    
    // 보드 그리기
    private void drawBoard() {
        Map<String, Color> currentColors = gameSettings.getCurrentColors();
        
        // 배경 그리기
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        drawGrid();
        drawPlacedBlocks(currentColors);
        
        // 일시정지 상태가 아닐 때만 현재 블록 그리기
        if (!isPaused) {
            drawCurrentBlock(currentColors);
        }

        // 일시정지 오버레이 (메뉴가 열려있지 않을 때만)
        if (isPaused && !menuOverlay.isVisible()) {
            drawPauseOverlay();
        }

        scorePanel.updateNextBlock(gameLogic.getNextBlock());
    }
    
    // 일시정지 오버레이 그리기
    private void drawPauseOverlay() {
        // 반투명 배경
        gc.setFill(Color.color(0, 0, 0, 0.7));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // "PAUSED" 텍스트
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        
        String pauseText = "PAUSED";
        javafx.scene.text.Text tempText = new javafx.scene.text.Text(pauseText);
        tempText.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        double textWidth = tempText.getBoundsInLocal().getWidth();
        double textHeight = tempText.getBoundsInLocal().getHeight();
        
        double x = (canvas.getWidth() - textWidth) / 2;
        double y = (canvas.getHeight() + textHeight) / 2;
        
        gc.fillText(pauseText, x, y);
        
        // 안내 메시지
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        String instructionText = "Press ESC to resume";
        javafx.scene.text.Text tempInstruction = new javafx.scene.text.Text(instructionText);
        tempInstruction.setFont(javafx.scene.text.Font.font("Arial", 14));
        double instructionWidth = tempInstruction.getBoundsInLocal().getWidth();
        
        double instructionX = (canvas.getWidth() - instructionWidth) / 2;
        double instructionY = y + 40;
        
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText(instructionText, instructionX, instructionY);
    }

    // 그리드 그리기
    private void drawGrid() {
        gc.setStroke(Color.web("#16213e"));
        gc.setLineWidth(1);
        
        // 세로 선
        for (int i = 0; i <= GameLogic.WIDTH; i++) {
            gc.strokeLine(i * CELL_SIZE, 0, i * CELL_SIZE, GameLogic.HEIGHT * CELL_SIZE);
        }

        // 가로 선
        for (int i = 0; i <= GameLogic.HEIGHT; i++) {
            gc.strokeLine(0, i * CELL_SIZE, GameLogic.WIDTH * CELL_SIZE, i * CELL_SIZE);
        }
    }
    
    // 놓여진 블록 그리기
    private void drawPlacedBlocks(Map<String, Color> colorMap) {
        // 보드 상태 가져오기
        int[][] board = gameLogic.getBoard();
        String[][] blockTypes = gameLogic.getBlockTypes();
        
        // 놓여진 블록 그리기
        for (int row = 0; row < GameLogic.HEIGHT; row++) {
            for (int col = 0; col < GameLogic.WIDTH; col++) {
                if (board[row][col] == 1) {
                    // 블록 색상 결정
                    String cssClass = blockTypes[row][col];
                    // cssClass에 해당하는 색상을 가져오고, 없으면 기본 색상 사용
                    Color blockColor = colorMap.getOrDefault(cssClass, colorMap.get("block-default"));
                    // 셀 그리기
                    drawCell(col * CELL_SIZE, row * CELL_SIZE, blockColor);
                }
            }
        }
    }
    
    // 현재 떨어지는 블록 그리기
    private void drawCurrentBlock(Map<String, Color> colorMap) {
        // 현재 블록 정보 가져오기
        var currentBlock = gameLogic.getCurrentBlock();
        if (currentBlock == null) return;
        
        // 블록 색상 결정
        Color blockColor = colorMap.get(currentBlock.getCssClass());
        int currentX = gameLogic.getCurrentX();
        int currentY = gameLogic.getCurrentY();
        
        // 현재 블록 그리기
        for (int i = 0; i < currentBlock.width(); i++) {
            for (int j = 0; j < currentBlock.height(); j++) {
                if (currentBlock.getShape(i, j) == 1) {
                    // 셀 위치 계산
                    int drawX = (currentX + i) * CELL_SIZE;
                    int drawY = (currentY + j) * CELL_SIZE;
                    // 셀 그리기
                    drawCell(drawX, drawY, blockColor);
                }
            }
        }
    }
    
    // 개별 셀 그리기
    private void drawCell(double x, double y, Color color) {
        // 메인 셀
        gc.setFill(color);
        gc.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        
        // 하이라이트 효과
        gc.setFill(color.brighter());
        gc.fillRect(x + 2, y + 2, CELL_SIZE - 4, 3);
        gc.fillRect(x + 2, y + 2, 3, CELL_SIZE - 4);

        // 그림자 효과
        gc.setFill(color.darker());
        gc.fillRect(x + 2, y + CELL_SIZE - 5, CELL_SIZE - 4, 3);
        gc.fillRect(x + CELL_SIZE - 5, y + 2, 3, CELL_SIZE - 4);
    }
    
    // 메인 컨테이너 반환 (오버레이 포함)
    public StackPane getRoot() {
        return mainContainer;
    }
    
    // 게임 재시작
    public void restartGame() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        gameLogic.resetGame();
        scorePanel.resetScore();
        isPaused = false;
        menuOverlay.hide();
        
        startGameLoop();
        drawBoard();
        mainContainer.requestFocus();
    }

    // 하드 드롭 (블록을 즉시 바닥까지 떨어뜨리기)
    private void hardDrop() {
        while (gameLogic.moveDown()) {
            // 더 이상 내려갈 수 없을 때까지 반복
        }
        
        int linesCleared = gameLogic.clearLines();
        if (linesCleared > 0) {
            scorePanel.calculateLineScore(linesCleared);
            updateSpeedDisplay();
        }
    }
}
