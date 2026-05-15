package com.dkonak.dartat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockLoweringView extends View {
    private static final int BOARD = 10;
    private static final int FALL_W = 10;
    private static final int FALL_H = 15;
    private static final int EMPTY = -1;
    private static final long LOWERING_FRAME_MS = 50L;
    private static final float START_FALL_DELAY = 0.46f;
    private static final int[] COLORS = {
            Color.rgb(89, 118, 239),
            Color.rgb(95, 205, 87),
            Color.rgb(255, 200, 61),
            Color.rgb(171, 96, 226),
            Color.rgb(238, 84, 85),
            Color.rgb(62, 199, 207),
            Color.rgb(246, 145, 57)
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final int[][] board = new int[BOARD][BOARD];
    private final int[][] fallingBoard = new int[FALL_H][FALL_W];
    private final List<BlockPiece> tray = new ArrayList<>();
    private final RectF blastModeRect = new RectF();
    private final RectF loweringModeRect = new RectF();
    private final RectF restartRect = new RectF();
    private final RectF rotateRect = new RectF();
    private final RectF optionBlastRect = new RectF();
    private final RectF optionLoweringRect = new RectF();
    private final RectF pauseMenuRect = new RectF();
    private final RectF pausePanelRect = new RectF();
    private final RectF resumeRect = new RectF();
    private final RectF leaveRect = new RectF();
    private final RectF newGameRect = new RectF();
    private final RectF adContinueRect = new RectF();
    private final RectF leftRect = new RectF();
    private final RectF rightRect = new RectF();
    private final RectF downRect = new RectF();
    private final RectF dropRect = new RectF();
    private final RectF[] trayRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final ToneGenerator tones = null;
    private final Vibrator vibrator;

    private Mode mode = Mode.BLAST;
    private Screen screen = Screen.MENU;
    private BlockPiece selectedPiece;
    private BlockPiece fallingPiece;
    private int selectedTray = -1;
    private int fallingX;
    private int fallingY;
    private int score;
    private int bestScore;
    private int lines;
    private boolean gameOver;
    private boolean rewardedContinueUsed;
    private boolean draggingPiece;
    private boolean attachedToWindow;
    private boolean windowVisible = true;
    private long lastFrame;
    private float fallTimer;
    private float fallDelay = START_FALL_DELAY;
    private float dragX;
    private float dragY;
    private int previewCol = -1;
    private int previewRow = -1;

    public BlockLoweringView(Context context) {
        super(context);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        textPaint.setAntiAlias(true);
        setFocusable(true);
        setWillNotDraw(false);
        resetAll();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachedToWindow = true;
        windowVisible = getWindowVisibility() == VISIBLE;
        lastFrame = SystemClock.elapsedRealtime();
        postInvalidateOnAnimation();
        if ((mode == Mode.LOWERING && !gameOver) || draggingPiece) {
            scheduleNextFrame();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attachedToWindow = false;
        if (tones != null) {
            tones.release();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        windowVisible = visibility == VISIBLE;
        if (windowVisible) {
            lastFrame = SystemClock.elapsedRealtime();
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        if (action == MotionEvent.ACTION_MOVE) {
            if (screen == Screen.PLAYING && mode == Mode.BLAST && draggingPiece) {
                updateDragPreview(x, y);
                invalidate();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (screen == Screen.PLAYING && mode == Mode.BLAST && draggingPiece) {
                finishBlastDrag(action == MotionEvent.ACTION_UP);
                invalidate();
            }
            return true;
        }
        if (action != MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (screen == Screen.MENU) {
            if (optionBlastRect.contains(x, y)) {
                startMode(Mode.BLAST);
            } else if (optionLoweringRect.contains(x, y)) {
                startMode(Mode.LOWERING);
            }
            invalidate();
            return true;
        }

        if (screen == Screen.PAUSE) {
            if (!gameOver && resumeRect.contains(x, y)) {
                screen = Screen.PLAYING;
                lastFrame = SystemClock.elapsedRealtime();
            } else if (leaveRect.contains(x, y)) {
                clearBlastSelection();
                screen = Screen.MENU;
            } else if (newGameRect.contains(x, y)) {
                startMode(mode);
            } else if (gameOver && !rewardedContinueUsed && adContinueRect.contains(x, y)) {
                continueAfterAd();
            }
            invalidate();
            return true;
        }

        if (pauseMenuRect.contains(x, y)) {
            screen = Screen.PAUSE;
            cancelBlastDrag();
            invalidate();
            return true;
        }

        if (restartRect.contains(x, y)) {
            startMode(mode);
            invalidate();
            return true;
        }
        if (mode == Mode.BLAST) {
            handleBlastTouch(x, y);
        } else {
            handleLoweringTouch(x, y);
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.elapsedRealtime();
        float dt = Math.min((now - lastFrame) / 1000f, 0.04f);
        lastFrame = now;
        if (screen == Screen.PLAYING && mode == Mode.LOWERING && !gameOver) {
            updateFalling(dt);
        }
        drawBackground(canvas);
        if (screen == Screen.MENU) {
            drawGameOptions(canvas);
        } else if (mode == Mode.BLAST) {
            drawBlast(canvas);
        } else {
            drawLoweringConsole(canvas);
        }
        if (screen == Screen.PAUSE) {
            drawPauseMenu(canvas);
        }
        if (screen == Screen.PLAYING && ((mode == Mode.LOWERING && !gameOver) || draggingPiece)) {
            scheduleNextFrame();
        }
    }

    private void scheduleNextFrame() {
        if (!attachedToWindow || !windowVisible) {
            return;
        }
        if (mode == Mode.LOWERING && !draggingPiece) {
            postInvalidateDelayed(LOWERING_FRAME_MS);
        } else {
            postInvalidateOnAnimation();
        }
    }

    private void resetAll() {
        clearGrid(board);
        clearGrid(fallingBoard);
        score = 0;
        lines = 0;
        fallDelay = START_FALL_DELAY;
        selectedTray = -1;
        selectedPiece = null;
        cancelBlastDrag();
        gameOver = false;
        rewardedContinueUsed = false;
        seedBlastBoard();
        refillTray();
        startLoweringMode();
        screen = Screen.MENU;
    }

    private void startMode(Mode selectedMode) {
        mode = selectedMode;
        gameOver = false;
        rewardedContinueUsed = false;
        clearBlastSelection();
        if (mode == Mode.BLAST) {
            clearGrid(board);
            seedBlastBoard();
            refillTray();
        } else {
            startLoweringMode();
        }
        lastFrame = SystemClock.elapsedRealtime();
        screen = Screen.PLAYING;
        if (mode == Mode.LOWERING) {
            scheduleNextFrame();
        }
    }

    private void startLoweringMode() {
        clearGrid(fallingBoard);
        fallingPiece = randomPiece();
        fallingX = FALL_W / 2 - 2;
        fallingY = 0;
        fallTimer = 0f;
        fallDelay = Math.min(fallDelay, START_FALL_DELAY);
        gameOver = false;
        rewardedContinueUsed = false;
        if (!canPlace(fallingBoard, fallingPiece, fallingX, fallingY)) {
            gameOver = true;
            screen = Screen.PAUSE;
        }
    }

    private void handleBlastTouch(float x, float y) {
        if (gameOver) {
            return;
        }
        for (int i = 0; i < trayRects.length; i++) {
            if (trayRects[i].contains(x, y) && i < tray.size()) {
                selectedTray = i;
                selectedPiece = tray.get(i);
                draggingPiece = true;
                updateDragPreview(x, y);
                play(ToneGenerator.TONE_PROP_BEEP);
                return;
            }
        }
        if (rotateRect.contains(x, y) && selectedPiece != null && selectedTray >= 0 && selectedTray < tray.size()) {
            selectedPiece = selectedPiece.rotated();
            tray.set(selectedTray, selectedPiece);
            play(ToneGenerator.TONE_PROP_BEEP2);
            return;
        }

        updateDragPreview(x, y);
    }

    private void updateDragPreview(float x, float y) {
        dragX = x;
        dragY = y;
        previewCol = -1;
        previewRow = -1;
        if (selectedPiece == null) {
            return;
        }
        BoardArea area = blastBoardArea();
        previewCol = Math.round((x - area.bounds.left - (selectedPiece.width * area.cell / 2f)) / area.cell);
        previewRow = Math.round((y - area.bounds.top - (selectedPiece.height * area.cell / 2f)) / area.cell);
    }

    private void finishBlastDrag(boolean shouldPlace) {
        if (!shouldPlace || selectedPiece == null) {
            clearBlastSelection();
            return;
        }
        if (selectedTray >= 0 && selectedTray < tray.size() && canPlace(board, selectedPiece, previewCol, previewRow)) {
            placePiece(board, selectedPiece, previewCol, previewRow);
            int cleared = clearFullLines(board);
            score += selectedPiece.size() * 10 + cleared * cleared * 80;
            lines += cleared;
            bestScore = Math.max(bestScore, score);
            tray.remove(selectedTray);
            selectedTray = -1;
            selectedPiece = null;
            draggingPiece = false;
            previewCol = -1;
            previewRow = -1;
            if (tray.isEmpty()) {
                refillTray();
                score += 45;
            }
            gameOver = !hasAnyMove();
            if (gameOver) {
                screen = Screen.PAUSE;
            }
            vibrate(24);
            play(cleared > 0 ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_BEEP);
        } else {
            vibrate(55);
            play(ToneGenerator.TONE_SUP_ERROR);
            clearBlastSelection();
        }
    }

    private void cancelBlastDrag() {
        draggingPiece = false;
        previewCol = -1;
        previewRow = -1;
    }

    private void clearBlastSelection() {
        cancelBlastDrag();
        selectedTray = -1;
        selectedPiece = null;
    }

    private void handleLoweringTouch(float x, float y) {
        if (gameOver) {
            return;
        }
        if (rotateRect.contains(x, y)) {
            BlockPiece rotated = fallingPiece.rotated();
            if (canPlace(fallingBoard, rotated, fallingX, fallingY)) {
                fallingPiece = rotated;
                play(ToneGenerator.TONE_PROP_BEEP2);
            }
        } else if (leftRect.contains(x, y) && canPlace(fallingBoard, fallingPiece, fallingX - 1, fallingY)) {
            fallingX--;
        } else if (rightRect.contains(x, y) && canPlace(fallingBoard, fallingPiece, fallingX + 1, fallingY)) {
            fallingX++;
        } else if (downRect.contains(x, y)) {
            stepDown();
        } else if (dropRect.contains(x, y)) {
            while (canPlace(fallingBoard, fallingPiece, fallingX, fallingY + 1)) {
                fallingY++;
                score += 1;
            }
            lockFalling();
        }
    }

    private void updateFalling(float dt) {
        fallTimer += dt;
        if (fallTimer >= fallDelay) {
            fallTimer = 0f;
            stepDown();
        }
    }

    private void stepDown() {
        if (canPlace(fallingBoard, fallingPiece, fallingX, fallingY + 1)) {
            fallingY++;
            score += 1;
        } else {
            lockFalling();
        }
    }

    private void lockFalling() {
        placePiece(fallingBoard, fallingPiece, fallingX, fallingY);
        int cleared = clearFallingRows();
        score += 30 + cleared * cleared * 120;
        lines += cleared;
        bestScore = Math.max(bestScore, score);
        fallDelay = Math.max(0.14f, START_FALL_DELAY - (lines * 0.018f));
        fallingPiece = randomPiece();
        fallingX = FALL_W / 2 - 2;
        fallingY = 0;
        if (!canPlace(fallingBoard, fallingPiece, fallingX, fallingY)) {
            gameOver = true;
            screen = Screen.PAUSE;
            vibrate(110);
            play(ToneGenerator.TONE_SUP_ERROR);
        } else {
            play(cleared > 0 ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_BEEP);
        }
    }

    private void continueAfterAd() {
        rewardedContinueUsed = true;
        gameOver = false;
        screen = Screen.PLAYING;
        if (mode == Mode.LOWERING) {
            clearTopRows();
            fallingPiece = randomPiece();
            fallingX = FALL_W / 2 - 2;
            fallingY = 0;
            fallTimer = 0f;
            if (!canPlace(fallingBoard, fallingPiece, fallingX, fallingY)) {
                clearGrid(fallingBoard);
            }
            scheduleNextFrame();
        } else {
            refillTray();
            if (!hasAnyMove()) {
                clearGrid(board);
                seedBlastBoard();
                refillTray();
            }
        }
        vibrate(35);
        play(ToneGenerator.TONE_PROP_ACK);
    }

    private void clearTopRows() {
        int rowsToClear = Math.min(4, FALL_H);
        for (int r = 0; r < rowsToClear; r++) {
            for (int c = 0; c < FALL_W; c++) {
                fallingBoard[r][c] = EMPTY;
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        paint.setShader(null);
        paint.setColor(mode == Mode.BLAST ? Color.rgb(72, 103, 169) : Color.rgb(235, 235, 230));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        if (mode == Mode.BLAST) {
            paint.setColor(Color.argb(42, 255, 255, 255));
            canvas.drawCircle(getWidth() * 0.22f, getHeight() * 0.15f, dp(92), paint);
            canvas.drawCircle(getWidth() * 0.86f, getHeight() * 0.78f, dp(120), paint);
        }
    }

    private void drawTopBar(Canvas canvas, boolean onConsole) {
        float top = dp(24);
        setButton(pauseMenuRect, getWidth() - dp(54), top + dp(24), dp(78), dp(38));
        drawPill(canvas, pauseMenuRect, "Menu", false);
        if (onConsole) {
            return;
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(18));
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Block Lowering", dp(18), top + dp(78), textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(sp(15));
        canvas.drawText("Score " + score, getWidth() - dp(18), top + dp(78), textPaint);
    }

    private void drawGameOptions(Canvas canvas) {
        drawBackground(canvas);
        float centerX = getWidth() / 2f;
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(34));
        textPaint.setColor(Color.WHITE);
        canvas.drawText("Block Lowering", centerX, getHeight() * 0.2f, textPaint);

        textPaint.setTextSize(sp(20));
        canvas.drawText("Oyun Secenekleri", centerX, getHeight() * 0.28f, textPaint);

        setButton(optionBlastRect, centerX, getHeight() * 0.42f, getWidth() * 0.72f, dp(64));
        setButton(optionLoweringRect, centerX, getHeight() * 0.52f, getWidth() * 0.72f, dp(64));
        drawPill(canvas, optionBlastRect, "Block Blast", true);
        drawPill(canvas, optionLoweringRect, "Yukardan Blok", false);
    }

    private void drawPauseMenu(Canvas canvas) {
        paint.setColor(Color.argb(185, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        float centerX = getWidth() / 2f;
        float menuW = Math.min(getWidth() * 0.82f, dp(330));
        boolean over = gameOver;
        float menuH = over ? dp(344) : dp(300);
        pausePanelRect.set(centerX - menuW / 2f, getHeight() * 0.5f - menuH / 2f, centerX + menuW / 2f, getHeight() * 0.5f + menuH / 2f);
        paint.setColor(Color.rgb(244, 244, 238));
        canvas.drawRoundRect(pausePanelRect, dp(16), dp(16), paint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(24));
        textPaint.setColor(Color.rgb(32, 33, 34));
        canvas.drawText(over ? "Oyun Bitti" : "Oyun Durdu", centerX, pausePanelRect.top + dp(48), textPaint);

        if (over) {
            setButton(adContinueRect, centerX, pausePanelRect.top + dp(104), menuW - dp(54), dp(46));
            setButton(newGameRect, centerX, pausePanelRect.top + dp(164), menuW - dp(54), dp(46));
            setButton(leaveRect, centerX, pausePanelRect.top + dp(224), menuW - dp(54), dp(46));
            drawMenuButton(canvas, adContinueRect, rewardedContinueUsed ? "Devam Hakki Kullanildi" : "Reklam Izle ve Devam Et", !rewardedContinueUsed);
            drawMenuButton(canvas, newGameRect, "Yeniden Baslat", false);
            drawMenuButton(canvas, leaveRect, "Oyundan Ayril", false);
        } else {
            setButton(resumeRect, centerX, pausePanelRect.top + dp(108), menuW - dp(54), dp(48));
            setButton(newGameRect, centerX, pausePanelRect.top + dp(168), menuW - dp(54), dp(48));
            setButton(leaveRect, centerX, pausePanelRect.top + dp(228), menuW - dp(54), dp(48));
            drawMenuButton(canvas, resumeRect, "Devam Et", true);
            drawMenuButton(canvas, newGameRect, "Yeni Oyun Baslat", false);
            drawMenuButton(canvas, leaveRect, "Oyundan Ayril", false);
        }
    }

    private void drawMenuButton(Canvas canvas, RectF r, String label, boolean primary) {
        paint.setColor(primary ? Color.rgb(255, 205, 54) : Color.rgb(224, 226, 221));
        canvas.drawRoundRect(r, r.height() / 2f, r.height() / 2f, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(13));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.rgb(24, 25, 28));
        drawCentered(canvas, label, r.centerX(), r.centerY(), textPaint);
    }

    private void drawBlast(Canvas canvas) {
        drawTopBar(canvas, false);
        BoardArea area = blastBoardArea();
        drawBoard(canvas, board, area, true);
        drawBlastPreview(canvas, area);
        drawTray(canvas, area.bounds.bottom + dp(28));
        setButton(rotateRect, getWidth() * 0.5f, getHeight() - dp(44), dp(150), dp(44));
        drawPill(canvas, rotateRect, "Rotate", selectedPiece != null);
        drawDraggedPiece(canvas, area);
        if (gameOver) {
            screen = Screen.PAUSE;
            drawPauseMenu(canvas);
        }
    }

    private void drawLoweringConsole(Canvas canvas) {
        float consoleW = Math.min(getWidth() * 0.86f, dp(360));
        float consoleH = getHeight() - dp(92);
        float left = (getWidth() - consoleW) / 2f;
        float top = dp(46);
        rect.set(left, top, left + consoleW, top + consoleH);
        paint.setColor(Color.rgb(31, 33, 34));
        canvas.drawRoundRect(rect, dp(16), dp(16), paint);
        paint.setColor(Color.rgb(78, 82, 84));
        canvas.drawRoundRect(left + dp(4), top + dp(4), left + consoleW - dp(4), top + dp(128), dp(12), dp(12), paint);

        drawTopBar(canvas, true);

        float screenW = consoleW - dp(78);
        float screenH = Math.min(dp(340), consoleH * 0.42f);
        RectF screen = new RectF(left + dp(38), top + dp(58), left + dp(38) + screenW, top + dp(58) + screenH);
        paint.setColor(Color.rgb(143, 160, 139));
        canvas.drawRect(screen, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.rgb(83, 96, 82));
        canvas.drawRect(screen, paint);
        paint.setStyle(Paint.Style.FILL);

        float cell = Math.min((screen.width() - dp(74)) / FALL_W, (screen.height() - dp(30)) / FALL_H);
        BoardArea area = new BoardArea(new RectF(screen.left + dp(12), screen.top + dp(15), screen.left + dp(12) + cell * FALL_W, screen.top + dp(15) + cell * FALL_H), cell);
        drawBoard(canvas, fallingBoard, area, false);
        drawPiece(canvas, fallingPiece, area.bounds.left + fallingX * area.cell, area.bounds.top + fallingY * area.cell, area.cell, true);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.rgb(32, 45, 39));
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(11));
        canvas.drawText("SCORE", screen.right - dp(14), screen.top + dp(28), textPaint);
        textPaint.setTextSize(sp(18));
        canvas.drawText(String.valueOf(score), screen.right - dp(14), screen.top + dp(48), textPaint);
        textPaint.setTextSize(sp(11));
        canvas.drawText("LINES", screen.right - dp(14), screen.top + dp(78), textPaint);
        textPaint.setTextSize(sp(18));
        canvas.drawText(String.valueOf(lines), screen.right - dp(14), screen.top + dp(98), textPaint);

        drawConsoleControls(canvas, left, top, consoleW, consoleH);
        if (gameOver) {
            this.screen = Screen.PAUSE;
            drawPauseMenu(canvas);
        }
    }

    private void drawConsoleControls(Canvas canvas, float left, float top, float consoleW, float consoleH) {
        float baseY = top + consoleH * 0.7f;
        float cx = left + consoleW * 0.27f;
        setButton(leftRect, cx - dp(39), baseY, dp(42), dp(42));
        setButton(rightRect, cx + dp(39), baseY, dp(42), dp(42));
        setButton(downRect, cx, baseY + dp(38), dp(42), dp(42));
        setButton(rotateRect, left + consoleW * 0.73f, baseY - dp(5), dp(78), dp(38));
        setButton(dropRect, left + consoleW * 0.73f, baseY + dp(46), dp(78), dp(38));
        drawRoundButton(canvas, leftRect, "<");
        drawRoundButton(canvas, rightRect, ">");
        drawRoundButton(canvas, downRect, "v");
        drawPill(canvas, rotateRect, "Rotate", false);
        drawPill(canvas, dropRect, "Drop", false);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sp(22));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.save();
        canvas.rotate(-28, left + consoleW * 0.47f, top + consoleH * 0.87f);
        canvas.drawText("BRICK GAME", left + consoleW * 0.47f, top + consoleH * 0.87f, textPaint);
        canvas.restore();
    }

    private void drawTray(Canvas canvas, float top) {
        float slot = Math.min(dp(88), getWidth() / 3.55f);
        float gap = dp(10);
        float start = (getWidth() - slot * 3 - gap * 2) / 2f;
        for (int i = 0; i < 3; i++) {
            RectF slotRect = trayRects[i];
            slotRect.set(start + i * (slot + gap), top, start + i * (slot + gap) + slot, top + slot);
            paint.setColor(i == selectedTray ? Color.argb(95, 255, 255, 255) : Color.argb(42, 255, 255, 255));
            canvas.drawRoundRect(slotRect, dp(8), dp(8), paint);
            if (i < tray.size() && !(draggingPiece && i == selectedTray)) {
                BlockPiece piece = tray.get(i);
                float cell = slot / 5f;
                float px = slotRect.centerX() - piece.width * cell / 2f;
                float py = slotRect.centerY() - piece.height * cell / 2f;
                drawPiece(canvas, piece, px, py, cell, true);
            }
        }
    }

    private void drawBlastPreview(Canvas canvas, BoardArea area) {
        if (!draggingPiece || selectedPiece == null || previewCol == -1 || previewRow == -1) {
            return;
        }
        boolean valid = canPlace(board, selectedPiece, previewCol, previewRow);
        int previewColor = valid ? Color.rgb(120, 235, 128) : Color.rgb(255, 92, 92);
        for (Cell cellPos : selectedPiece.cells) {
            int col = previewCol + cellPos.x;
            int row = previewRow + cellPos.y;
            if (col < 0 || col >= BOARD || row < 0 || row >= BOARD) {
                continue;
            }
            float x = area.bounds.left + col * area.cell;
            float y = area.bounds.top + row * area.cell;
            paint.setColor(Color.argb(90, Color.red(previewColor), Color.green(previewColor), Color.blue(previewColor)));
            canvas.drawRect(x + dp(1), y + dp(1), x + area.cell - dp(1), y + area.cell - dp(1), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(210, Color.red(previewColor), Color.green(previewColor), Color.blue(previewColor)));
            canvas.drawRect(x + dp(2), y + dp(2), x + area.cell - dp(2), y + area.cell - dp(2), paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawDraggedPiece(Canvas canvas, BoardArea area) {
        if (!draggingPiece || selectedPiece == null) {
            return;
        }
        float cell = area.cell * 0.92f;
        float left = dragX - selectedPiece.width * cell / 2f;
        float top = dragY - selectedPiece.height * cell / 2f;
        paint.setAlpha(210);
        drawPiece(canvas, selectedPiece, left, top, cell, true);
        paint.setAlpha(255);
    }

    private void drawBoard(Canvas canvas, int[][] grid, BoardArea area, boolean jewel) {
        paint.setColor(jewel ? Color.rgb(44, 50, 88) : Color.rgb(125, 145, 122));
        canvas.drawRect(area.bounds, paint);
        int rows = grid.length;
        int cols = grid[0].length;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = area.bounds.left + c * area.cell;
                float y = area.bounds.top + r * area.cell;
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(jewel ? Color.rgb(30, 35, 63) : Color.rgb(104, 124, 103));
                canvas.drawRect(x, y, x + area.cell, y + area.cell, paint);
                paint.setStyle(Paint.Style.FILL);
                if (grid[r][c] != EMPTY) {
                    drawBlock(canvas, x, y, area.cell, COLORS[grid[r][c] % COLORS.length], jewel);
                }
            }
        }
    }

    private void drawPiece(Canvas canvas, BlockPiece piece, float left, float top, float cell, boolean jewel) {
        if (piece == null) {
            return;
        }
        for (Cell cellPos : piece.cells) {
            drawBlock(canvas, left + cellPos.x * cell, top + cellPos.y * cell, cell, COLORS[piece.color], jewel);
        }
    }

    private void drawBlock(Canvas canvas, float x, float y, float size, int color, boolean jewel) {
        float inset = Math.max(1f, size * 0.08f);
        rect.set(x + inset, y + inset, x + size - inset, y + size - inset);
        paint.setColor(jewel ? darken(color) : Color.rgb(44, 58, 49));
        canvas.drawRoundRect(rect, jewel ? size * 0.12f : 0f, jewel ? size * 0.12f : 0f, paint);
        rect.inset(size * 0.08f, size * 0.08f);
        paint.setColor(jewel ? color : Color.rgb(32, 44, 36));
        canvas.drawRoundRect(rect, jewel ? size * 0.1f : 0f, jewel ? size * 0.1f : 0f, paint);
        if (jewel) {
            paint.setColor(Color.argb(90, 255, 255, 255));
            canvas.drawRect(rect.left, rect.top, rect.right, rect.top + size * 0.16f, paint);
        }
    }

    private void drawPill(Canvas canvas, RectF r, String label, boolean active) {
        paint.setColor(active ? Color.rgb(255, 205, 54) : Color.argb(78, 255, 255, 255));
        canvas.drawRoundRect(r, r.height() / 2f, r.height() / 2f, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(13));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(active ? Color.rgb(39, 40, 43) : Color.WHITE);
        drawCentered(canvas, label, r.centerX(), r.centerY(), textPaint);
    }

    private void drawRoundButton(Canvas canvas, RectF r, String label) {
        paint.setColor(Color.rgb(255, 235, 39));
        canvas.drawOval(r, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(22));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.rgb(35, 35, 35));
        drawCentered(canvas, label, r.centerX(), r.centerY(), textPaint);
    }

    private void drawGameOver(Canvas canvas, String label) {
        paint.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(34));
        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, getWidth() / 2f, getHeight() * 0.44f, textPaint);
        setButton(restartRect, getWidth() / 2f, getHeight() * 0.54f, dp(170), dp(52));
        drawPill(canvas, restartRect, "New game", true);
    }

    private BoardArea blastBoardArea() {
        float size = Math.min(getWidth() - dp(36), getHeight() * 0.48f);
        float left = (getWidth() - size) / 2f;
        float top = dp(128);
        return new BoardArea(new RectF(left, top, left + size, top + size), size / BOARD);
    }

    private void refillTray() {
        tray.clear();
        for (int i = 0; i < 3; i++) {
            tray.add(randomPiece());
        }
    }

    private void seedBlastBoard() {
        int[][] seeds = {
                {0, 0, 0}, {0, 1, 0}, {1, 0, 0}, {2, 7, 1}, {3, 7, 1},
                {8, 3, 2}, {9, 3, 2}, {8, 4, 2}, {9, 4, 2}, {8, 5, 2}, {9, 5, 2},
                {5, 8, 3}, {6, 8, 3}, {7, 8, 3}, {8, 8, 3}, {9, 8, 3}
        };
        for (int[] seed : seeds) {
            board[seed[1]][seed[0]] = seed[2];
        }
    }

    private boolean hasAnyMove() {
        for (BlockPiece piece : tray) {
            for (int y = 0; y < BOARD; y++) {
                for (int x = 0; x < BOARD; x++) {
                    if (canPlace(board, piece, x, y)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canPlace(int[][] grid, BlockPiece piece, int x, int y) {
        int rows = grid.length;
        int cols = grid[0].length;
        for (Cell cell : piece.cells) {
            int px = x + cell.x;
            int py = y + cell.y;
            if (px < 0 || px >= cols || py < 0 || py >= rows || grid[py][px] != EMPTY) {
                return false;
            }
        }
        return true;
    }

    private void placePiece(int[][] grid, BlockPiece piece, int x, int y) {
        for (Cell cell : piece.cells) {
            grid[y + cell.y][x + cell.x] = piece.color;
        }
    }

    private int clearFullLines(int[][] grid) {
        boolean[] fullRows = new boolean[BOARD];
        boolean[] fullCols = new boolean[BOARD];
        int count = 0;
        for (int r = 0; r < BOARD; r++) {
            fullRows[r] = true;
            fullCols[r] = true;
            for (int c = 0; c < BOARD; c++) {
                fullRows[r] &= grid[r][c] != EMPTY;
                fullCols[r] &= grid[c][r] != EMPTY;
            }
            if (fullRows[r]) count++;
            if (fullCols[r]) count++;
        }
        for (int r = 0; r < BOARD; r++) {
            for (int c = 0; c < BOARD; c++) {
                if (fullRows[r] || fullCols[c]) {
                    grid[r][c] = EMPTY;
                }
            }
        }
        return count;
    }

    private int clearFallingRows() {
        int cleared = 0;
        for (int r = FALL_H - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < FALL_W; c++) {
                if (fallingBoard[r][c] == EMPTY) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cleared++;
                for (int rr = r; rr > 0; rr--) {
                    System.arraycopy(fallingBoard[rr - 1], 0, fallingBoard[rr], 0, FALL_W);
                }
                for (int c = 0; c < FALL_W; c++) {
                    fallingBoard[0][c] = EMPTY;
                }
                r++;
            }
        }
        return cleared;
    }

    private void clearGrid(int[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c] = EMPTY;
            }
        }
    }

    private BlockPiece randomPiece() {
        int[][][] shapes = {
                {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
                {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
                {{0, 0}, {0, 1}, {0, 2}, {1, 2}},
                {{1, 0}, {1, 1}, {0, 1}, {0, 2}},
                {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
                {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
                {{0, 0}, {1, 0}, {2, 0}},
                {{0, 0}, {0, 1}, {0, 2}},
                {{0, 0}, {1, 0}, {2, 0}, {2, 1}},
                {{0, 0}, {1, 0}, {0, 1}}
        };
        int pick = random.nextInt(shapes.length);
        return new BlockPiece(shapes[pick], random.nextInt(COLORS.length));
    }

    private int darken(int color) {
        return Color.rgb((int) (Color.red(color) * 0.7f), (int) (Color.green(color) * 0.7f), (int) (Color.blue(color) * 0.7f));
    }

    private void setButton(RectF r, float cx, float cy, float w, float h) {
        r.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    private void drawCentered(Canvas canvas, String text, float cx, float cy, Paint p) {
        Paint.FontMetrics metrics = p.getFontMetrics();
        canvas.drawText(text, cx, cy - (metrics.ascent + metrics.descent) / 2f, p);
    }

    private void vibrate(long durationMs) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    private void play(int tone) {
        if (tones != null) {
            tones.startTone(tone, 80);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private enum Mode {
        BLAST,
        LOWERING
    }

    private enum Screen {
        MENU,
        PLAYING,
        PAUSE
    }

    private static final class BoardArea {
        final RectF bounds;
        final float cell;

        BoardArea(RectF bounds, float cell) {
            this.bounds = bounds;
            this.cell = cell;
        }
    }

    private static final class Cell {
        final int x;
        final int y;

        Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class BlockPiece {
        final List<Cell> cells = new ArrayList<>();
        final int color;
        int width;
        int height;

        BlockPiece(int[][] points, int color) {
            this.color = color;
            int maxX = 0;
            int maxY = 0;
            for (int[] point : points) {
                cells.add(new Cell(point[0], point[1]));
                maxX = Math.max(maxX, point[0]);
                maxY = Math.max(maxY, point[1]);
            }
            width = maxX + 1;
            height = maxY + 1;
        }

        BlockPiece rotated() {
            int[][] points = new int[cells.size()][2];
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            for (int i = 0; i < cells.size(); i++) {
                Cell cell = cells.get(i);
                int nx = height - 1 - cell.y;
                int ny = cell.x;
                points[i][0] = nx;
                points[i][1] = ny;
                minX = Math.min(minX, nx);
                minY = Math.min(minY, ny);
            }
            for (int[] point : points) {
                point[0] -= minX;
                point[1] -= minY;
            }
            return new BlockPiece(points, color);
        }

        int size() {
            return cells.size();
        }
    }
}
