package com.arcadeblocks.ui;

import com.arcadeblocks.ArcadeBlocksApp;
import com.arcadeblocks.config.GameConfig;
import com.arcadeblocks.config.GameLine;
import com.arcadeblocks.localization.LocalizationManager;
import com.almasb.fxgl.dsl.FXGL;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.beans.binding.Bindings;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Экран Game Over
 */
public class GameOverView extends VBox {
    
    private final ArcadeBlocksApp app;
    private Button[] actionButtons;
    private int currentButtonIndex = 0;
    private Label finalScoreLabel;
    private final LocalizationManager localizationManager = LocalizationManager.getInstance();

    private final boolean canContinue;
    private final int continueCost;
    
    // Анимации для очистки при удалении компонента
    private Timeline glowTimeline;
    private ScaleTransition pulseAnimation;
    // КРИТИЧНО: Сохраняем ссылки на анимации кнопок для их остановки
    private java.util.Map<Button, ScaleTransition> buttonScaleTransitions = new java.util.HashMap<>();
    private java.util.Map<Button, ScaleTransition> dialogButtonScaleTransitions = new java.util.HashMap<>();
    private FadeTransition dialogFadeTransition;
    private ScaleTransition dialogScaleTransition;
    
    public GameOverView(ArcadeBlocksApp app, boolean canContinue, int continueCost) {
        this.app = app;
        this.canContinue = canContinue;
        this.continueCost = continueCost;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);

        Label gameOverLabel = createGlowingTitleLabel();

        Label subtitleLabel = new Label();
        localizationManager.bind(subtitleLabel, "gameover.subtitle");
        subtitleLabel.setFont(Font.font("Orbitron", 24));
        subtitleLabel.setTextFill(Color.WHITE);
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);

        finalScoreLabel = createFinalScoreLabel();

        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);

        Button continueButton = createStyledButton("gameover.button.continue", 200, 50);
        continueButton.setDisable(!canContinue);
        continueButton.setOnAction(e -> {
            playMenuSelectSound();
            showContinueConfirmation();
        });

        Button startOverButton = createStyledButton("gameover.button.restart", 200, 50);
        startOverButton.setOnAction(e -> {
            playMenuSelectSound();
            app.getAudioManager().stopMusic();
            GameLine currentGameLine = GameLine.fromLevel(FXGL.geti("level"));
            
            // Явно сбрасываем флаг Game Over перед удалением UI
            app.setGameOver(false);
            // Важно: сбрасываем флаг видимости GameOverView, чтобы onUpdate снова обрабатывал ввод
            app.setGameOverViewVisible(false);
            
            // Останавливаем анимации перед удалением
            cleanup();
            
            // Явно удаляем GameOverView из сцены, чтобы освободить обработку событий
            com.almasb.fxgl.dsl.FXGL.getGameScene().removeUINode(this);
            
            if (app.getSaveManager() != null) {
                int originalSlot = app.getOriginalSaveSlot();
                app.getSaveManager().deleteSaveFileForSlot(currentGameLine, originalSlot);
            }
            if (app.isDebugMode()) {
                int currentLevel = FXGL.geti("level");
                app.startDebugLevel(currentLevel);
            } else {
                if (app.getSaveManager() != null) {
                    int originalSlot = app.getOriginalSaveSlot();
                    app.getSaveManager().setActiveSaveSlot(currentGameLine, originalSlot);
                    app.getSaveManager().resetProgressPreservingSlots();
                    app.getSaveManager().setCurrentLevel(currentGameLine.getStartLevel());
                    app.getSaveManager().setLives(GameConfig.INITIAL_LIVES);
                    app.getSaveManager().setScore(0);
                    app.getSaveManager().awaitPendingWrites();
                    app.getSaveManager().autoSaveToSlot(currentGameLine, originalSlot);
                }
                app.startLevel(currentGameLine.getStartLevel(), true);
            }
        });

        Button mainMenuButton = createStyledButton("gameover.button.main_menu", 200, 50);
        mainMenuButton.setOnAction(e -> {
            playMenuSelectSound();
            app.getAudioManager().stopMusic();
            GameLine currentGameLine = GameLine.fromLevel(FXGL.geti("level"));
            
            // КРИТИЧНО: Освобождаем фоновые изображения уровня перед возвратом в главное меню
            app.releaseLevelBackground();
            
            // Останавливаем анимации перед удалением
            cleanup();
            
            // Сбрасываем флаги Game Over, чтобы управление восстановилось при новой игре/создании слота
            app.setGameOver(false);
            app.setGameOverViewVisible(false);
            app.resetPaddleInputFlags();
            app.setSuppressLevelCompletionChecks(true);

            if (app.getSaveManager() != null) {
                int originalSlot = app.getOriginalSaveSlot();
                app.getSaveManager().clearGameSnapshot();
                app.getSaveManager().deleteSaveFileForSlot(currentGameLine, originalSlot);
            }
            java.util.List<com.almasb.fxgl.entity.Entity> entities = new java.util.ArrayList<>(com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntities());
            for (com.almasb.fxgl.entity.Entity entity : entities) {
                entity.removeFromWorld();
            }
            String currentBackground = app.getCurrentMainMenuBackground();
            app.setCurrentMainMenuBackground(currentBackground);
            app.clearUINodesSafely();
            MainMenuView mainMenuView = new MainMenuView(app, false, currentBackground);
            com.almasb.fxgl.dsl.FXGL.getGameScene().addUINode(mainMenuView);
            
            // Ensure keyboard navigation works
            javafx.application.Platform.runLater(() -> {
                mainMenuView.restoreFocus();
            });
            
            if (app.getAudioManager() != null) {
                boolean restored = app.getAudioManager().restorePreviousMusic();
                if (!restored || !app.getAudioManager().isMusicPlaying()) {
                    // Определяем состояние прогресса игры для выбора правильной музыки
                    com.arcadeblocks.config.AudioConfig.GameProgressState progressState = 
                        com.arcadeblocks.config.AudioConfig.GameProgressState.NORMAL;
                    
                    if (app.getSaveManager() != null) {
                        progressState = app.getSaveManager().getMenuProgressState();
                        System.out.println("[GameOverView] progressState=" + progressState);
                    }
                    
                    String randomMainMenuMusic = com.arcadeblocks.config.AudioConfig.getRandomMainMenuMusic(progressState);
                    app.getAudioManager().playMusic(randomMainMenuMusic, true);
                }
            }
        });

        continueButton.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> playMenuHoverSound());
        startOverButton.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> playMenuHoverSound());
        mainMenuButton.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> playMenuHoverSound());

        continueButton.focusedProperty().addListener((obs, was, isNow) -> {
            if (isNow) { playMenuHoverSound(); applyButtonHoverStyle(continueButton); } else { restoreButtonStyle(continueButton); }
        });
        startOverButton.focusedProperty().addListener((obs, was, isNow) -> {
            if (isNow) { playMenuHoverSound(); applyButtonHoverStyle(startOverButton); } else { restoreButtonStyle(startOverButton); }
        });
        mainMenuButton.focusedProperty().addListener((obs, was, isNow) -> {
            if (isNow) { playMenuHoverSound(); applyButtonHoverStyle(mainMenuButton); } else { restoreButtonStyle(mainMenuButton); }
        });

        StackPane continueWrapper = wrapButton(continueButton);
        StackPane startOverWrapper = wrapButton(startOverButton);
        StackPane mainMenuWrapper = wrapButton(mainMenuButton);
        buttonContainer.getChildren().addAll(continueWrapper, startOverWrapper, mainMenuWrapper);

        actionButtons = new Button[]{ continueButton, startOverButton, mainMenuButton };
        currentButtonIndex = 0;

        setFocusTraversable(true);
        setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP:
                    moveSelectionUp();
                    event.consume();
                    break;
                case DOWN:
                    moveSelectionDown();
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    if (actionButtons != null && currentButtonIndex >= 0 && currentButtonIndex < actionButtons.length) {
                        playMenuSelectSound();
                        actionButtons[currentButtonIndex].fire();
                    }
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        Platform.runLater(this::requestInitialFocus);
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::requestInitialFocus);
            }
        });

        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case UP:
                    moveSelectionUp();
                    event.consume();
                    break;
                case DOWN:
                    moveSelectionDown();
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        getChildren().addAll(gameOverLabel, subtitleLabel, finalScoreLabel, buttonContainer);
    }

    private void showContinueConfirmation() {
        int playerScore = getCurrentScore();
        int remainingScore = Math.max(0, playerScore - continueCost);
        boolean hasEnoughPoints = playerScore >= continueCost;

        StackPane overlay = new StackPane();
        overlay.setPrefSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(5, 8, 20, 0.82);");
        overlay.setAlignment(Pos.CENTER);

        VBox dialog = new VBox(18);
        dialog.setAlignment(Pos.CENTER);
        dialog.setSpacing(18);
        dialog.setPadding(new Insets(32, 40, 32, 40));
        dialog.setBackground(new Background(new BackgroundFill(
            Color.web("#111739", 0.94),
            new CornerRadii(26),
            Insets.EMPTY
        )));
        dialog.setBorder(new Border(new BorderStroke(
            Color.web(GameConfig.NEON_PURPLE),
            BorderStrokeStyle.SOLID,
            new CornerRadii(26),
            new BorderWidths(2)
        )));
        DropShadow dialogGlow = new DropShadow();
        dialogGlow.setColor(Color.web(GameConfig.NEON_PURPLE, 0.65));
        dialogGlow.setRadius(30);
        dialogGlow.setSpread(0.35);
        dialog.setEffect(dialogGlow);

        Label title = new Label();
        localizationManager.bind(title, "gameover.continue.title");
        title.setFont(Font.font("Orbitron", FontWeight.BOLD, 30));
        title.setTextFill(Color.web(GameConfig.NEON_CYAN));
        title.setTextAlignment(TextAlignment.CENTER);

        Label description = new Label();
        localizationManager.bind(description, "gameover.continue.description");
        description.setFont(Font.font("Orbitron", 16));
        description.setTextFill(Color.web("#DDE6FF"));
        description.setWrapText(true);
        description.setTextAlignment(TextAlignment.CENTER);
        description.setMaxWidth(440);

        VBox statsContainer = new VBox(12);
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.getChildren().addAll(
            createStatTile("gameover.continue.cost", formatScoreValue(continueCost), Color.web(GameConfig.NEON_PINK)),
            createStatTile("gameover.continue.current", formatScoreValue(playerScore), Color.web(GameConfig.NEON_CYAN)),
            createStatTile("gameover.continue.remaining", formatScoreValue(remainingScore), Color.web("#7FFF7F"))
        );

        Label infoLabel = new Label();
        localizationManager.bind(infoLabel, "gameover.continue.info");
        infoLabel.setFont(Font.font("Orbitron", 13));
        infoLabel.setTextFill(Color.web("#94A3D3"));
        infoLabel.setWrapText(true);
        infoLabel.setTextAlignment(TextAlignment.CENTER);
        infoLabel.setMaxWidth(440);

        Label warningLabel = null;
        if (!hasEnoughPoints) {
            warningLabel = new Label();
            localizationManager.bind(warningLabel, "gameover.continue.warning");
            warningLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));
            warningLabel.setTextFill(Color.web("#FF8E8E"));
            warningLabel.setWrapText(true);
            warningLabel.setTextAlignment(TextAlignment.CENTER);
            warningLabel.setMaxWidth(420);
        }

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        Button confirm = createDialogButton("gameover.continue.confirm", GameConfig.NEON_PINK, hasEnoughPoints, null);
        Button cancel = createDialogButton("gameover.continue.cancel", GameConfig.NEON_CYAN, true, null);

        Runnable confirmAction = () -> {
            if (confirm.isDisabled()) {
                playMenuBackSound();
                return;
            }
            playMenuSelectSound();
            FXGL.getGameScene().removeUINode(overlay);
            app.continueFromGameOver();
        };

        Runnable cancelAction = () -> {
            playMenuBackSound();
            FXGL.getGameScene().removeUINode(overlay);
        };

        confirm.setOnAction(e -> confirmAction.run());
        cancel.setOnAction(e -> cancelAction.run());

        buttons.getChildren().addAll(confirm, cancel);

        dialog.getChildren().add(title);
        dialog.getChildren().add(description);
        dialog.getChildren().add(statsContainer);
        if (warningLabel != null) {
            dialog.getChildren().add(warningLabel);
        }
        dialog.getChildren().add(infoLabel);
        dialog.getChildren().add(buttons);

        dialog.setOpacity(0.0);
        dialog.setScaleX(0.9);
        dialog.setScaleY(0.9);
        
        // КРИТИЧНО: Останавливаем старые анимации диалога, если они еще активны
        if (dialogFadeTransition != null) {
            try {
                dialogFadeTransition.stop();
            } catch (Exception ignored) {}
            dialogFadeTransition = null;
        }
        if (dialogScaleTransition != null) {
            try {
                dialogScaleTransition.stop();
            } catch (Exception ignored) {}
            dialogScaleTransition = null;
        }
        
        dialogFadeTransition = new FadeTransition(Duration.millis(220), dialog);
        dialogFadeTransition.setToValue(1.0);
        dialogScaleTransition = new ScaleTransition(Duration.millis(220), dialog);
        dialogScaleTransition.setToX(1.0);
        dialogScaleTransition.setToY(1.0);
        
        // КРИТИЧНО: Очищаем ссылки на анимации после их завершения
        dialogFadeTransition.setOnFinished(e -> {
            dialogFadeTransition = null;
        });
        dialogScaleTransition.setOnFinished(e -> {
            dialogScaleTransition = null;
        });
        
        ParallelTransition showAnimation = new ParallelTransition(dialogFadeTransition, dialogScaleTransition);

        overlay.getChildren().add(dialog);
        FXGL.getGameScene().addUINode(overlay);
        showAnimation.play();

        overlay.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case ESCAPE:
                    playMenuBackSound();
                    FXGL.getGameScene().removeUINode(overlay);
                    event.consume();
                    break;
                case ENTER:
                case SPACE:
                    if (!confirm.isDisabled()) {
                        confirmAction.run();
                    } else {
                        playMenuBackSound();
                    }
                    event.consume();
                    break;
                case LEFT:
                case RIGHT:
                case TAB:
                    if (confirm.isFocused()) {
                        cancel.requestFocus();
                    } else {
                        confirm.requestFocus();
                    }
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        overlay.setFocusTraversable(true);
        Platform.runLater(confirm::requestFocus);
    }
    
    private Button createStyledButton(String translationKey, double width, double height) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setPrefSize(width, height);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));
        button.setTextFill(Color.WHITE);
        
		// Единый стиль кнопки (без CSS -fx-effect, эффект задаём программно)
		String buttonStyle = String.format(
			"-fx-background-color: %s; " +
			"-fx-border-color: %s; " +
			"-fx-border-width: 1px; " +
			"-fx-border-radius: 8px; " +
			"-fx-background-radius: 8px; " +
			"-fx-cursor: hand;",
			GameConfig.DARK_BACKGROUND, GameConfig.NEON_PURPLE
		);
        
		button.setStyle(buttonStyle);
		// Базовое мягкое свечение программно (чтобы hover-эффект не перекрывался CSS)
		DropShadow baseGlow = new DropShadow();
		baseGlow.setColor(Color.web(GameConfig.NEON_PURPLE));
		baseGlow.setRadius(8);
		baseGlow.setSpread(0.3);
		button.setEffect(baseGlow);
        
        // Hover эффекты для мыши (более заметная подсветка)
        button.setOnMouseEntered(e -> applyButtonHoverStyle(button));
        
        button.setOnMouseExited(e -> restoreButtonStyle(button));
        
        return button;
    }

	private void applyButtonHoverStyle(Button button) {
		// Не перезаписываем сохранённые значения, если уже есть (например, пришли по фокусу)
		if (!button.getProperties().containsKey("prevStyle")) {
			button.getProperties().put("prevStyle", button.getStyle());
		}
		if (!button.getProperties().containsKey("prevEffect")) {
			button.getProperties().put("prevEffect", button.getEffect());
		}

		String hoverStyle = String.format(
			"-fx-background-color: %s; " +
			"-fx-border-color: %s; " +
			"-fx-border-width: 2px; " +
			"-fx-border-radius: 8px; " +
			"-fx-background-radius: 8px; " +
			"-fx-cursor: hand;",
			GameConfig.NEON_PINK, "#FFFFFF"
		);
		button.setStyle(hoverStyle);

		DropShadow glowEffect = new DropShadow();
		glowEffect.setColor(Color.web(GameConfig.NEON_PINK));
		glowEffect.setRadius(18);
		glowEffect.setSpread(0.6);
		Glow innerGlow = new Glow();
		innerGlow.setLevel(0.7);
		glowEffect.setInput(innerGlow);
		button.setEffect(glowEffect);

		button.setTextFill(Color.WHITE);

		// КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
		ScaleTransition oldTransition = buttonScaleTransitions.get(button);
		if (oldTransition != null) {
			try {
				oldTransition.stop();
			} catch (Exception ignored) {}
		}
		
		ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
		scaleUp.setToX(1.08);
		scaleUp.setToY(1.08);
		scaleUp.setOnFinished(e2 -> {
			buttonScaleTransitions.remove(button);
		});
		buttonScaleTransitions.put(button, scaleUp);
		scaleUp.play();
	}

	private void restoreButtonStyle(Button button) {
		Object prevStyle = button.getProperties().get("prevStyle");
		if (prevStyle instanceof String) {
			button.setStyle((String) prevStyle);
		}
		button.setTextFill(Color.WHITE);
		
		// КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
		ScaleTransition oldTransition = buttonScaleTransitions.get(button);
		if (oldTransition != null) {
			try {
				oldTransition.stop();
			} catch (Exception ignored) {}
			buttonScaleTransitions.remove(button);
		}
		
		ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
		scaleDown.setToX(1.0);
		scaleDown.setToY(1.0);
		scaleDown.setOnFinished(e2 -> {
			buttonScaleTransitions.remove(button);
		});
		buttonScaleTransitions.put(button, scaleDown);
		scaleDown.play();

		Object prevEffect = button.getProperties().get("prevEffect");
		if (prevEffect instanceof javafx.scene.effect.Effect) {
			button.setEffect((javafx.scene.effect.Effect) prevEffect);
		} else {
			button.setEffect(null);
		}
		button.getProperties().remove("prevEffect");
		button.getProperties().remove("prevStyle");
	}

	private void moveSelectionUp() {
		if (actionButtons == null || actionButtons.length == 0) return;
		if (currentButtonIndex > 0) {
			currentButtonIndex--;
			actionButtons[currentButtonIndex].requestFocus();
		}
	}

	private void moveSelectionDown() {
		if (actionButtons == null || actionButtons.length == 0) return;
		if (currentButtonIndex < actionButtons.length - 1) {
			currentButtonIndex++;
			actionButtons[currentButtonIndex].requestFocus();
		}
	}

    private void requestInitialFocus() {
        if (!isFocused()) {
            requestFocus();
        }
        focusCurrentButton();
    }

    private void focusCurrentButton() {
        if (actionButtons != null && actionButtons.length > 0) {
            Button currentButton = actionButtons[currentButtonIndex];
            if (!currentButton.isFocused()) {
                currentButton.requestFocus();
            }
        }
    }

	/**
	 * Обернуть кнопку в StackPane для корректного отображения внешнего свечения
	 */
	private StackPane wrapButton(Button button) {
		StackPane wrapper = new StackPane(button);
		wrapper.setPickOnBounds(false); // чтобы клики попадали на кнопку
		return wrapper;
	}

    private Label createFinalScoreLabel() {
        String resolvedName = null;
        if (app.getSaveManager() != null) {
            String storedName = app.getSaveManager().getPlayerName();
            if (storedName != null && !storedName.isBlank()) {
                resolvedName = storedName.trim();
            }
        }

        int finalScore;
        if (app.getScoreManager() != null) {
            finalScore = app.getScoreManager().getCurrentScore();
        } else {
            finalScore = FXGL.geti("score");
        }

        final String playerNameOrNull = resolvedName;
        final int displayedScore = finalScore;

        Label label = new Label();
        label.textProperty().bind(Bindings.createStringBinding(() -> {
            String playerName = playerNameOrNull != null && !playerNameOrNull.isBlank()
                ? playerNameOrNull
                : localizationManager.get("player.default");
            return localizationManager.format("gameover.summary", playerName, formatScoreValue(displayedScore));
        }, localizationManager.localeProperty()));
        label.setFont(Font.font("Orbitron", FontWeight.MEDIUM, 18));
        label.setTextFill(Color.web(GameConfig.NEON_CYAN));
        label.setOpacity(0.9);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);
        return label;
    }

    private VBox createStatTile(String captionKey, String value, Color accentColor) {
        Label captionLabel = new Label();
        localizationManager.bind(captionLabel, captionKey);
        captionLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 11));
        captionLabel.setTextFill(accentColor.deriveColor(0, 1, 1, 0.85));
        captionLabel.setOpacity(0.92);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 24));
        valueLabel.setTextFill(Color.WHITE);

        VBox tile = new VBox(6, captionLabel, valueLabel);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(14, 26, 16, 26));
        tile.setBackground(new Background(new BackgroundFill(
            Color.web("#131937", 0.94),
            new CornerRadii(18),
            Insets.EMPTY
        )));
        tile.setBorder(new Border(new BorderStroke(
            accentColor.deriveColor(0, 1, 1, 0.75),
            BorderStrokeStyle.SOLID,
            new CornerRadii(18),
            new BorderWidths(1.4)
        )));
        DropShadow shadow = new DropShadow();
        shadow.setColor(accentColor.deriveColor(0, 1, 1, 0.35));
        shadow.setRadius(21);
        shadow.setSpread(0.28);
        tile.setEffect(shadow);
        tile.setMaxWidth(320);
        return tile;
    }

    private Button createDialogButton(String translationKey, String accentColorHex, boolean enabled, Runnable action) {
        Button button = new Button();
        localizationManager.bind(button, translationKey);
        button.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        button.setTextFill(Color.WHITE);
        button.setPrefWidth(188);
        button.setPrefHeight(48);
        button.setDisable(!enabled);
        button.setOpacity(enabled ? 1.0 : 0.55);

        String baseStyle = buildDialogButtonBaseStyle(accentColorHex, enabled);
        button.setStyle(baseStyle);
        button.getProperties().put("dialogBaseStyle", baseStyle);
        button.getProperties().put("dialogAccent", accentColorHex);

        DropShadow baseGlow = new DropShadow();
        baseGlow.setColor(Color.web(accentColorHex, enabled ? 0.6 : 0.3));
        baseGlow.setRadius(20);
        baseGlow.setSpread(0.32);
        button.setEffect(baseGlow);

        if (action != null) {
            button.setOnAction(e -> {
                if (!button.isDisabled()) {
                    action.run();
                }
            });
        }

        button.setOnMouseEntered(e -> {
            if (button.isDisabled()) {
                return;
            }
            playMenuHoverSound();
            applyDialogButtonHoverStyle(button, accentColorHex);
        });
        button.setOnMouseExited(e -> {
            restoreDialogButtonStyle(button);
        });

        button.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (button.isDisabled()) {
                return;
            }
            if (isFocused) {
                applyDialogButtonHoverStyle(button, accentColorHex);
            } else if (!button.isHover()) {
                restoreDialogButtonStyle(button);
            }
        });

        return button;
    }

    private void applyDialogButtonHoverStyle(Button button, String accentColorHex) {
        String hoverStyle = String.format(
            "-fx-background-color: rgba(34, 18, 52, 0.96); " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 14px; " +
            "-fx-background-radius: 14px; " +
            "-fx-cursor: hand;",
            accentColorHex
        );
        button.setStyle(hoverStyle);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(accentColorHex));
        glow.setRadius(30);
        glow.setSpread(0.55);
        Glow innerGlow = new Glow();
        innerGlow.setLevel(0.65);
        glow.setInput(innerGlow);
        button.setEffect(glow);

        // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
        ScaleTransition oldTransition = dialogButtonScaleTransitions.get(button);
        if (oldTransition != null) {
            try {
                oldTransition.stop();
            } catch (Exception ignored) {}
        }
        
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(160), button);
        scaleUp.setToX(1.06);
        scaleUp.setToY(1.06);
        scaleUp.setOnFinished(e2 -> {
            dialogButtonScaleTransitions.remove(button);
        });
        dialogButtonScaleTransitions.put(button, scaleUp);
        scaleUp.play();
    }

    private void restoreDialogButtonStyle(Button button) {
        Object baseStyle = button.getProperties().get("dialogBaseStyle");
        String accent = (String) button.getProperties().get("dialogAccent");

        if (baseStyle instanceof String) {
            button.setStyle((String) baseStyle);
        }
        DropShadow baseGlow = new DropShadow();
        String accentHex = accent != null ? accent : GameConfig.NEON_PURPLE;
        baseGlow.setColor(Color.web(accentHex, button.isDisabled() ? 0.3 : 0.6));
        baseGlow.setRadius(20);
        baseGlow.setSpread(0.32);
        button.setEffect(baseGlow);

        // КРИТИЧНО: Останавливаем предыдущую анимацию, если она еще активна
        ScaleTransition oldTransition = dialogButtonScaleTransitions.get(button);
        if (oldTransition != null) {
            try {
                oldTransition.stop();
            } catch (Exception ignored) {}
            dialogButtonScaleTransitions.remove(button);
        }
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(160), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setOnFinished(e2 -> {
            dialogButtonScaleTransitions.remove(button);
        });
        dialogButtonScaleTransitions.put(button, scaleDown);
        scaleDown.play();
    }

    private String buildDialogButtonBaseStyle(String accentColorHex, boolean enabled) {
        double opacity = enabled ? 0.96 : 0.55;
        return String.format(
            "-fx-background-color: rgba(16, 22, 45, %.2f); " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1.6px; " +
            "-fx-border-radius: 14px; " +
            "-fx-background-radius: 14px; " +
            "-fx-cursor: hand;",
            opacity,
            accentColorHex
        );
    }

    private String formatScoreValue(int value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(Math.max(0, value));
    }

    private int getCurrentScore() {
        if (app.getScoreManager() != null) {
            return app.getScoreManager().getCurrentScore();
        }
        return FXGL.geti("score");
    }
    
    /**
     * Воспроизвести звук выбора в меню
     */
    private void playMenuSelectSound() {
        try {
            app.getAudioManager().playSFXByName("menu_select");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Воспроизвести звук наведения в меню
     */
    private void playMenuHoverSound() {
        try {
            app.getAudioManager().playSFXByName("menu_hover");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Воспроизвести звук возврата/отмены в меню
     */
    private void playMenuBackSound() {
        try {
            app.getAudioManager().playSFXByName("menu_back");
        } catch (Exception e) {
            // Игнорируем ошибки воспроизведения звука
        }
    }
    
    /**
     * Создать заголовок Game Over с эффектами подлета и свечения
     */
    private Label createGlowingTitleLabel() {
        Label titleLabel = new Label();
        localizationManager.bind(titleLabel, "gameover.title");
        titleLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 72));
        titleLabel.setTextFill(Color.web(GameConfig.NEON_PINK));
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        
        // Начальное состояние - невидимый и смещенный вверх
        titleLabel.setOpacity(0.0);
        titleLabel.setTranslateY(-100);
        
        // Эффект неонового свечения
        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.web(GameConfig.NEON_PINK));
        glowEffect.setRadius(15);
        glowEffect.setSpread(0.6);
        
        Glow neonGlow = new Glow();
        neonGlow.setLevel(0.8);
        
        glowEffect.setInput(neonGlow);
        titleLabel.setEffect(glowEffect);
        
        // Анимация подлета
        TranslateTransition flyIn = new TranslateTransition(Duration.seconds(1.2), titleLabel);
        flyIn.setFromY(-100);
        flyIn.setToY(0);
        flyIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        
        // Анимация появления
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.0), titleLabel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // Запускаем анимации одновременно
        flyIn.play();
        fadeIn.play();
        
        // Анимация пульсации свечения (как в главном меню)
        glowTimeline = new Timeline();
        glowTimeline.setCycleCount(Timeline.INDEFINITE);
        
        KeyFrame glowFrame = new KeyFrame(Duration.seconds(2), e -> {
            double time = System.currentTimeMillis() / 1000.0;
            double glow = (Math.sin(time) + 1) / 2; // От 0 до 1
            
            DropShadow dynamicGlow = new DropShadow();
            dynamicGlow.setColor(Color.web(GameConfig.NEON_PINK));
            dynamicGlow.setRadius(10 + glow * 15);
            dynamicGlow.setSpread(0.3 + glow * 0.4);
            
            Glow dynamicNeonGlow = new Glow();
            dynamicNeonGlow.setLevel(0.5 + glow * 0.5);
            
            dynamicGlow.setInput(dynamicNeonGlow);
            titleLabel.setEffect(dynamicGlow);
            
            // Изменяем прозрачность для эффекта "дыхания"
            titleLabel.setOpacity(0.8 + glow * 0.2);
        });
        
        glowTimeline.getKeyFrames().add(glowFrame);
        
        // Анимация масштабирования (как в главном меню)
        pulseAnimation = new ScaleTransition(Duration.seconds(3.0), titleLabel);
        pulseAnimation.setFromX(1.0);
        pulseAnimation.setFromY(1.0);
        pulseAnimation.setToX(1.08);
        pulseAnimation.setToY(1.08);
        pulseAnimation.setAutoReverse(true);
        pulseAnimation.setCycleCount(ScaleTransition.INDEFINITE);
        
        // Запускаем пульсацию после завершения подлета (один обработчик для обеих анимаций)
        flyIn.setOnFinished(e -> {
            if (glowTimeline != null) {
                glowTimeline.play();
            }
            if (pulseAnimation != null) {
                pulseAnimation.play();
            }
        });
        
        return titleLabel;
    }
    
    /**
     * Остановка всех анимаций для предотвращения утечек памяти
     */
    public void cleanup() {
        // КРИТИЧНО: Останавливаем анимации диалога
        if (dialogFadeTransition != null) {
            try {
                dialogFadeTransition.stop();
            } catch (Exception ignored) {}
            dialogFadeTransition = null;
        }
        if (dialogScaleTransition != null) {
            try {
                dialogScaleTransition.stop();
            } catch (Exception ignored) {}
            dialogScaleTransition = null;
        }
        
        if (glowTimeline != null) {
            glowTimeline.stop();
            glowTimeline = null;
        }
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }
        
        // КРИТИЧНО: Останавливаем все активные анимации кнопок
        // КРИТИЧНО: Останавливаем все активные ScaleTransition и очищаем карты,
        // но не обнуляем ссылки, чтобы избежать NullPointerException при событиях мыши
        if (buttonScaleTransitions != null) {
            for (ScaleTransition transition : buttonScaleTransitions.values()) {
                if (transition != null) {
                    try {
                        transition.stop();
                    } catch (Exception ignored) {}
                }
            }
            buttonScaleTransitions.clear();
            // НЕ обнуляем buttonScaleTransitions, чтобы избежать NPE при MouseExited событиях
        }
        
        // КРИТИЧНО: Останавливаем все активные анимации диалоговых кнопок
        // Не обнуляем ссылку, чтобы избежать NPE при событиях мыши
        if (dialogButtonScaleTransitions != null) {
            for (ScaleTransition transition : dialogButtonScaleTransitions.values()) {
                if (transition != null) {
                    try {
                        transition.stop();
                    } catch (Exception ignored) {}
                }
            }
            dialogButtonScaleTransitions.clear();
            // НЕ обнуляем dialogButtonScaleTransitions, чтобы избежать NPE при MouseExited событиях
        }
        
        // Очищаем обработчик клавиш
        this.setOnKeyPressed(null);
        
        // КРИТИЧНО: Сначала очищаем все обработчики кнопок перед удалением из children
        if (actionButtons != null) {
            for (Button button : actionButtons) {
                if (button != null) {
                    button.setOnAction(null);
                    // КРИТИЧНО: Отвязываем textProperty() у Button
                    button.textProperty().unbind();
                }
            }
            actionButtons = null;
        }
        
        // КРИТИЧНО: Отвязываем все textProperty() bindings от LocalizationManager
        unbindAllTextProperties(this);
        
        // КРИТИЧНО: Отвязываем ResponsiveLayoutHelper ПЕРЕД удалением children
        com.arcadeblocks.ui.util.ResponsiveLayoutHelper.unbind(this);
        
        // КРИТИЧНО: Дополнительная прямая очистка ResponsiveLayoutHelper listeners
        try {
            javafx.stage.Stage stage = com.almasb.fxgl.dsl.FXGL.getPrimaryStage();
            if (stage != null) {
                @SuppressWarnings("unchecked")
                javafx.beans.value.ChangeListener<Number> widthListener = 
                    (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveWidthListener");
                @SuppressWarnings("unchecked")
                javafx.beans.value.ChangeListener<Number> heightListener = 
                    (javafx.beans.value.ChangeListener<Number>) this.getProperties().get("responsiveHeightListener");
                
                if (widthListener != null) {
                    stage.widthProperty().removeListener(widthListener);
                    this.getProperties().remove("responsiveWidthListener");
                }
                if (heightListener != null) {
                    stage.heightProperty().removeListener(heightListener);
                    this.getProperties().remove("responsiveHeightListener");
                }
            }
            
            @SuppressWarnings("unchecked")
            javafx.beans.value.ChangeListener<javafx.scene.Scene> sceneListener = 
                (javafx.beans.value.ChangeListener<javafx.scene.Scene>) this.getProperties().get("responsiveSceneListener");
            if (sceneListener != null) {
                this.sceneProperty().removeListener(sceneListener);
                this.getProperties().remove("responsiveSceneListener");
            }
            
            this.getProperties().remove("responsiveLastWidth");
            this.getProperties().remove("responsiveLastHeight");
        } catch (Exception ignored) {}
        
        // КРИТИЧНО: Очищаем все дочерние элементы, чтобы удалить все компоненты из памяти
        getChildren().clear();
    }
    
    /**
     * Рекурсивно отвязывает все textProperty() bindings от LocalizationManager
     */
    private void unbindAllTextProperties(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        
        // Отвязываем textProperty() у Labeled компонентов
        if (node instanceof javafx.scene.control.Labeled) {
            ((javafx.scene.control.Labeled) node).textProperty().unbind();
        }
        
        // Рекурсивно обрабатываем дочерние элементы
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                unbindAllTextProperties(child);
            }
        }
    }
}
