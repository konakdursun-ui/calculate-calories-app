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
    private static final int BOARD = 9;
    private static final int FALL_W = 10;
    private static final int FALL_H = 15;
    private static final int EMPTY = -1;
    private static final long LOWERING_FRAME_MS = 50L;
    private static final float START_FALL_DELAY = 0.46f;
    private static final int[] COLORS = {
            Color.rgb(255, 223, 158),
            Color.rgb(0, 218, 243),
            Color.rgb(189, 194, 255),
            Color.rgb(255, 180, 171),
            Color.rgb(250, 189, 0),
            Color.rgb(156, 240, 255),
            Color.rgb(134, 144, 238)
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
    private int blastColorIndex;
    private int combo;
    private int movesSinceClear;
    private int blastMoves;
    private int lastScoreGain;
    private int clearFlashCount;
    private long clearFlashUntil;
    private long failFlashUntil;
    private boolean gameOver;
    private boolean rewardedContinueUsed;
    private boolean draggingPiece;
    private boolean attachedToWindow;
    private boolean windowVisible = true;
    private long lastTapTime;
    private float touchStartX;
    private float touchStartY;
    private float lastTapX;
    private float lastTapY;
    private float lastMoveY;
    private int lastLoweringDirection;
    private long directionFlashUntil;
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
            } else if (screen == Screen.PLAYING && mode == Mode.LOWERING) {
                handleLoweringDrag(y);
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
            touchStartX = x;
            touchStartY = y;
            lastMoveY = y;
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
        if (screen == Screen.PLAYING && ((mode == Mode.LOWERING && !gameOver) || draggingPiece || SystemClock.elapsedRealtime() < clearFlashUntil || SystemClock.elapsedRealtime() < failFlashUntil)) {
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
        combo = 0;
        movesSinceClear = 0;
        blastMoves = 0;
        lastScoreGain = 0;
        clearFlashCount = 0;
        clearFlashUntil = 0L;
        failFlashUntil = 0L;
        blastColorIndex = random.nextInt(COLORS.length);
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
            combo = 0;
            movesSinceClear = 0;
            blastMoves = 0;
            lastScoreGain = 0;
            clearFlashCount = 0;
            clearFlashUntil = 0L;
            failFlashUntil = 0L;
            blastColorIndex = random.nextInt(COLORS.length);
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
        float liftedY = y - dp(96);
        previewCol = Math.round((x - area.bounds.left - (selectedPiece.width * area.cell / 2f)) / area.cell);
        previewRow = Math.round((liftedY - area.bounds.top - (selectedPiece.height * area.cell / 2f)) / area.cell);
    }

    private void finishBlastDrag(boolean shouldPlace) {
        if (!shouldPlace || selectedPiece == null) {
            clearBlastSelection();
            return;
        }
        if (selectedTray >= 0 && selectedTray < tray.size() && canPlace(board, selectedPiece, previewCol, previewRow)) {
            placePiece(board, selectedPiece, previewCol, previewRow);
            int cleared = clearFullLines(board);
            int placementScore = selectedPiece.size() * 10 + cleared * cleared * 80;
            if (cleared > 0) {
                combo = movesSinceClear <= 1 ? combo + 1 : 1;
                movesSinceClear = 0;
                int multiplier = combo + 1;
                placementScore *= multiplier;
                clearFlashCount = cleared;
                clearFlashUntil = SystemClock.elapsedRealtime() + 520L;
                if (isBoardEmpty(board)) {
                    nextBlastColor();
                    placementScore += 400;
                }
            } else {
                movesSinceClear++;
                if (movesSinceClear > 2) {
                    combo = 0;
                }
            }
            lastScoreGain = placementScore;
            score += placementScore;
            lines += cleared;
            bestScore = Math.max(bestScore, score);
            blastMoves++;
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
                failFlashUntil = SystemClock.elapsedRealtime() + 650L;
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
        long now = SystemClock.elapsedRealtime();
        if (now - lastTapTime < 280L && Math.abs(x - lastTapX) < dp(42) && Math.abs(y - lastTapY) < dp(42)) {
            rotateFallingPiece();
            lastTapTime = 0L;
            return;
        }
        lastTapTime = now;
        lastTapX = x;
        lastTapY = y;
        BoardArea area = loweringBoardArea();
        float pieceCenterX = area.bounds.left + (fallingX + fallingPiece.width / 2f) * area.cell;
        if (x < pieceCenterX) {
            moveFalling(-1);
        } else {
            moveFalling(1);
        }
    }

    private void handleLoweringDrag(float y) {
        if (gameOver || y <= lastMoveY + dp(16)) {
            return;
        }
        int steps = Math.min(5, Math.max(1, (int) ((y - lastMoveY) / dp(18))));
        for (int i = 0; i < steps && !gameOver; i++) {
            stepDown();
        }
        lastMoveY = y;
    }

    private void moveFalling(int direction) {
        lastLoweringDirection = direction;
        directionFlashUntil = SystemClock.elapsedRealtime() + 220L;
        if (canPlace(fallingBoard, fallingPiece, fallingX + direction, fallingY)) {
            fallingX += direction;
            play(ToneGenerator.TONE_PROP_BEEP);
        }
    }

    private void rotateFallingPiece() {
        BlockPiece rotated = fallingPiece.rotated();
        if (canPlace(fallingBoard, rotated, fallingX, fallingY)) {
            fallingPiece = rotated;
            play(ToneGenerator.TONE_PROP_BEEP2);
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
        paint.setColor(mode == Mode.BLAST || screen == Screen.MENU ? Color.rgb(18, 20, 20) : Color.rgb(18, 20, 20));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.argb(44, 57, 73, 171));
        canvas.drawCircle(getWidth() * 0.14f, getHeight() * 0.22f, dp(160), paint);
        paint.setColor(Color.argb(34, 0, 218, 243));
        canvas.drawCircle(getWidth() * 0.9f, getHeight() * 0.78f, dp(150), paint);
    }

    private void drawTopBar(Canvas canvas, boolean onConsole) {
        float top = dp(24);
        float barBottom = dp(94);
        paint.setColor(Color.argb(64, 12, 15, 15));
        canvas.drawRect(0, 0, getWidth(), barBottom, paint);
        paint.setColor(Color.argb(30, 255, 255, 255));
        canvas.drawRect(0, barBottom - dp(1), getWidth(), barBottom, paint);

        setButton(pauseMenuRect, dp(36), top + dp(30), dp(48), dp(44));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(30));
        textPaint.setColor(Color.rgb(198, 197, 255));
        drawCentered(canvas, "=", pauseMenuRect.centerX(), pauseMenuRect.centerY() - dp(2), textPaint);

        textPaint.setTextAlign(screen == Screen.MENU ? Paint.Align.CENTER : Paint.Align.LEFT);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(screen == Screen.MENU ? 24 : 22));
        textPaint.setColor(Color.rgb(189, 194, 255));
        canvas.drawText("Block Blast", screen == Screen.MENU ? getWidth() / 2f : dp(76), top + dp(38), textPaint);

        if (screen == Screen.MENU) {
            return;
        }
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(sp(onConsole ? 20 : 22));
        textPaint.setColor(onConsole ? Color.rgb(198, 197, 212) : Color.rgb(255, 223, 158));
        canvas.drawText(onConsole ? "Score: " + score : "Rekor: " + bestScore, getWidth() - dp(24), top + dp(38), textPaint);
        if (!onConsole && combo > 0) {
            setButton(rect, getWidth() - dp(78), top + dp(68), dp(104), dp(28));
            paint.setColor(Color.argb(52, 0, 218, 243));
            canvas.drawRoundRect(rect, dp(14), dp(14), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.argb(150, 0, 218, 243));
            canvas.drawRoundRect(rect, dp(14), dp(14), paint);
            paint.setStyle(Paint.Style.FILL);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(sp(14));
            textPaint.setColor(Color.rgb(0, 218, 243));
            drawCentered(canvas, "Combo x" + (combo + 1), rect.centerX(), rect.centerY(), textPaint);
        }
    }

    private void drawGameOptions(Canvas canvas) {
        drawBackground(canvas);
        float centerX = getWidth() / 2f;
        drawTopBar(canvas, false);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(16));
        textPaint.setColor(Color.rgb(198, 197, 212));
        canvas.drawText("Choose a mode", centerX, dp(124), textPaint);

        float cardW = getWidth() - dp(48);
        float cardH = Math.min(dp(270), getHeight() * 0.29f);
        optionBlastRect.set(dp(24), dp(150), dp(24) + cardW, dp(150) + cardH);
        optionLoweringRect.set(dp(24), optionBlastRect.bottom + dp(22), dp(24) + cardW, optionBlastRect.bottom + dp(22) + cardH);
        drawModeCard(canvas, optionBlastRect, "Block Blast", "Fill the grid, clear the lines.", true);
        drawModeCard(canvas, optionLoweringRect, "Falling Blocks", "Gesture brick game challenge.", false);
    }

    private void drawPauseMenu(Canvas canvas) {
        paint.setColor(Color.argb(210, 0, 0, 0));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        float centerX = getWidth() / 2f;
        float menuW = Math.min(getWidth() * 0.86f, dp(360));
        boolean over = gameOver;
        float menuH = over ? dp(430) : dp(330);
        pausePanelRect.set(centerX - menuW / 2f, getHeight() * 0.5f - menuH / 2f, centerX + menuW / 2f, getHeight() * 0.5f + menuH / 2f);
        paint.setColor(Color.rgb(30, 32, 32));
        canvas.drawRoundRect(pausePanelRect, dp(22), dp(22), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(pausePanelRect, dp(22), dp(22), paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(over ? 44 : 40));
        textPaint.setColor(Color.rgb(189, 194, 255));
        canvas.drawText(over ? "Game Over!" : "Paused", centerX, pausePanelRect.top + dp(over ? 86 : 74), textPaint);

        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(18));
        textPaint.setColor(Color.rgb(226, 226, 226));
        canvas.drawText(over ? "Final Score" : "Score: " + score, centerX, pausePanelRect.top + dp(over ? 130 : 114), textPaint);
        if (over) {
            rect.set(pausePanelRect.left + dp(34), pausePanelRect.top + dp(148), pausePanelRect.right - dp(34), pausePanelRect.top + dp(216));
            paint.setColor(Color.rgb(40, 42, 43));
            canvas.drawRoundRect(rect, dp(12), dp(12), paint);
            textPaint.setFakeBoldText(true);
            textPaint.setTextSize(sp(40));
            textPaint.setColor(Color.rgb(0, 218, 243));
            drawCentered(canvas, String.valueOf(score), rect.centerX(), rect.centerY() + dp(4), textPaint);
        }

        if (over) {
            setButton(adContinueRect, centerX, pausePanelRect.top + dp(258), menuW - dp(68), dp(58));
            setButton(newGameRect, centerX, pausePanelRect.top + dp(330), menuW - dp(68), dp(54));
            setButton(leaveRect, centerX, pausePanelRect.top + dp(392), menuW - dp(68), dp(42));
            drawMenuButton(canvas, adContinueRect, rewardedContinueUsed ? "Continue Used" : "Watch Ad & Continue", true);
            drawMenuButton(canvas, newGameRect, "New Game", false);
            drawTextButton(canvas, leaveRect, "Exit Game");
        } else {
            setButton(resumeRect, centerX, pausePanelRect.top + dp(166), menuW - dp(62), dp(58));
            setButton(newGameRect, centerX, pausePanelRect.top + dp(238), menuW - dp(62), dp(58));
            setButton(leaveRect, centerX, pausePanelRect.top + dp(306), menuW - dp(62), dp(54));
            drawMenuButton(canvas, resumeRect, "Continue", true);
            drawMenuButton(canvas, newGameRect, "New Game", false);
            drawMenuButton(canvas, leaveRect, "Exit Game", false);
        }
    }

    private void drawMenuButton(Canvas canvas, RectF r, String label, boolean primary) {
        paint.setColor(primary ? Color.rgb(255, 223, 158) : Color.rgb(40, 42, 43));
        canvas.drawRoundRect(r, dp(10), dp(10), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(primary ? Color.rgb(250, 189, 0) : Color.argb(88, 255, 255, 255));
        canvas.drawRoundRect(r, dp(10), dp(10), paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sp(22));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(primary ? Color.rgb(38, 26, 0) : Color.rgb(226, 226, 226));
        drawCentered(canvas, label, r.centerX(), r.centerY(), textPaint);
    }

    private void drawModeCard(Canvas canvas, RectF card, String title, String subtitle, boolean blastCard) {
        paint.setColor(Color.argb(190, 30, 32, 32));
        canvas.drawRoundRect(card, dp(22), dp(22), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(75, 255, 255, 255));
        canvas.drawRoundRect(card, dp(22), dp(22), paint);
        paint.setStyle(Paint.Style.FILL);

        float previewSize = Math.min(card.width() * 0.42f, card.height() * 0.34f);
        RectF preview = new RectF(card.centerX() - previewSize / 2f, card.top + dp(20), card.centerX() + previewSize / 2f, card.top + dp(20) + previewSize);
        paint.setColor(Color.rgb(36, 39, 40));
        canvas.drawRoundRect(preview, dp(12), dp(12), paint);
        int miniCols = blastCard ? 8 : 5;
        int miniRows = blastCard ? 6 : 6;
        float miniCell = preview.width() / miniCols;
        for (int r = 0; r < miniRows; r++) {
            for (int c = 0; c < miniCols; c++) {
                if ((blastCard && ((r + c) % 4 == 0 || (r == 4 && c < 3) || (r == 1 && c > 4)))
                        || (!blastCard && (r == 0 || r == 2 || (c == 2 && r < 4)))) {
                    drawBlock(canvas, preview.left + c * miniCell, preview.top + r * miniCell, miniCell, COLORS[(r + c) % COLORS.length], true);
                }
            }
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(24));
        textPaint.setColor(Color.rgb(226, 226, 226));
        canvas.drawText(title, card.centerX(), preview.bottom + dp(36), textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(13));
        textPaint.setColor(Color.rgb(198, 197, 212));
        canvas.drawText(subtitle, card.centerX(), preview.bottom + dp(58), textPaint);

        float buttonTop = card.bottom - dp(58);
        rect.set(card.left + dp(28), buttonTop, card.right - dp(28), buttonTop + dp(42));
        drawMenuButton(canvas, rect, blastCard ? "Play Classic" : "Play Falling", blastCard);
    }

    private void drawTextButton(Canvas canvas, RectF r, String label) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(16));
        textPaint.setColor(Color.rgb(226, 226, 226));
        drawCentered(canvas, label, r.centerX(), r.centerY(), textPaint);
    }

    private void drawBlast(Canvas canvas) {
        drawTopBar(canvas, false);
        BoardArea area = blastBoardArea();
        drawBoard(canvas, board, area, true);
        drawBlastPreview(canvas, area);
        drawBlastScoreHud(canvas, area);
        drawClearFlash(canvas, area);
        drawTray(canvas, getHeight() - dp(172));
        rotateRect.setEmpty();
        drawDraggedPiece(canvas, area);
        if (gameOver) {
            if (SystemClock.elapsedRealtime() >= failFlashUntil) {
                screen = Screen.PAUSE;
                drawPauseMenu(canvas);
            }
        }
    }

    private void drawBlastScoreHud(Canvas canvas, BoardArea area) {
        rect.set(area.bounds.left + dp(64), area.bounds.top - dp(46), area.bounds.right - dp(64), area.bounds.top - dp(10));
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(rect, dp(18), dp(18), paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(17));
        textPaint.setColor(Color.rgb(226, 226, 226));
        drawCentered(canvas, "Skor " + score + (lastScoreGain > 0 ? "  +" + lastScoreGain : ""), rect.centerX(), rect.centerY(), textPaint);
    }

    private void drawClearFlash(Canvas canvas, BoardArea area) {
        long now = SystemClock.elapsedRealtime();
        if (now < clearFlashUntil) {
            float alpha = (clearFlashUntil - now) / 520f;
            paint.setColor(Color.argb((int) (150 * alpha), 0, 218, 243));
            canvas.drawRoundRect(area.bounds, dp(10), dp(10), paint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);
            textPaint.setTextSize(sp(28));
            textPaint.setColor(Color.argb((int) (255 * alpha), 255, 223, 158));
            canvas.drawText("PATLAMA x" + clearFlashCount, area.bounds.centerX(), area.bounds.centerY(), textPaint);
        }
        if (now < failFlashUntil) {
            paint.setColor(Color.argb(125, 255, 55, 55));
            canvas.drawRoundRect(area.bounds, dp(10), dp(10), paint);
        }
    }

    private void drawLoweringConsole(Canvas canvas) {
        drawTopBar(canvas, true);
        BoardArea area = loweringBoardArea();
        RectF screen = area.bounds;
        paint.setColor(Color.rgb(143, 160, 139));
        canvas.drawRoundRect(screen, dp(8), dp(8), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(screen, dp(8), dp(8), paint);
        paint.setStyle(Paint.Style.FILL);

        drawBoard(canvas, fallingBoard, area, false);
        drawPiece(canvas, fallingPiece, area.bounds.left + fallingX * area.cell, area.bounds.top + fallingY * area.cell, area.cell, true);

        float scoreX = screen.right + dp(64);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.rgb(198, 197, 212));
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(14));
        canvas.drawText("SCORE", scoreX, screen.top + dp(44), textPaint);
        textPaint.setTextSize(sp(32));
        textPaint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(score), scoreX, screen.top + dp(82), textPaint);
        textPaint.setTextSize(sp(14));
        textPaint.setColor(Color.rgb(198, 197, 212));
        canvas.drawText("LINES", scoreX, screen.top + dp(142), textPaint);
        textPaint.setTextSize(sp(32));
        textPaint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(lines), scoreX, screen.top + dp(180), textPaint);

        drawConsoleLabel(canvas);
        drawLoweringControlHints(canvas, area);
        if (gameOver) {
            this.screen = Screen.PAUSE;
            drawPauseMenu(canvas);
        }
    }

    private BoardArea loweringBoardArea() {
        float top = dp(126);
        float sideScore = dp(92);
        float screenW = getWidth() - dp(48) - sideScore;
        float screenH = getHeight() - top - dp(70);
        RectF screen = new RectF(dp(22), top, dp(22) + screenW, top + screenH);
        float cell = Math.min((screen.width() - dp(14)) / FALL_W, (screen.height() - dp(14)) / FALL_H);
        return new BoardArea(new RectF(screen.left + dp(7), screen.top + dp(7), screen.left + dp(7) + cell * FALL_W, screen.top + dp(7) + cell * FALL_H), cell);
    }

    private void drawLoweringControlHints(Canvas canvas, BoardArea area) {
        float y = area.bounds.bottom + dp(22);
        float buttonW = Math.min(dp(116), area.bounds.width() * 0.36f);
        float buttonH = dp(40);
        leftRect.set(area.bounds.left, y, area.bounds.left + buttonW, y + buttonH);
        rightRect.set(area.bounds.right - buttonW, y, area.bounds.right, y + buttonH);
        drawGhostControl(canvas, leftRect, "< SOL", lastLoweringDirection < 0 && SystemClock.elapsedRealtime() < directionFlashUntil);
        drawGhostControl(canvas, rightRect, "SAG >", lastLoweringDirection > 0 && SystemClock.elapsedRealtime() < directionFlashUntil);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(sp(11));
        textPaint.setColor(Color.argb(170, 198, 197, 212));
        canvas.drawText("Parcanin soluna/sagina dokun - cift tikla dondur - asagi surukle", area.bounds.centerX(), y + buttonH + dp(22), textPaint);
    }

    private void drawGhostControl(Canvas canvas, RectF bounds, String label, boolean active) {
        paint.setColor(active ? Color.argb(92, 0, 218, 243) : Color.argb(42, 255, 255, 255));
        canvas.drawRoundRect(bounds, dp(20), dp(20), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(active ? Color.rgb(0, 218, 243) : Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(bounds, dp(20), dp(20), paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sp(13));
        textPaint.setColor(active ? Color.rgb(156, 240, 255) : Color.rgb(198, 197, 212));
        drawCentered(canvas, label, bounds.centerX(), bounds.centerY(), textPaint);
    }

    private void drawConsoleLabel(Canvas canvas) {
        textPaint.setColor(Color.argb(70, 226, 226, 226));
        textPaint.setTextSize(sp(44));
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.save();
        canvas.rotate(-12, getWidth() * 0.64f, getHeight() - dp(40));
        canvas.drawText("BRICK GAME", getWidth() * 0.64f, getHeight() - dp(40), textPaint);
        canvas.restore();
    }

    private void drawTray(Canvas canvas, float top) {
        float panelW = getWidth() - dp(48);
        float panelH = dp(124);
        float panelLeft = dp(24);
        rect.set(panelLeft, top, panelLeft + panelW, top + panelH);
        paint.setColor(Color.rgb(31, 35, 80));
        canvas.drawRoundRect(rect, dp(16), dp(16), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.argb(75, 255, 255, 255));
        canvas.drawRoundRect(rect, dp(16), dp(16), paint);
        paint.setStyle(Paint.Style.FILL);

        float slot = Math.min(dp(86), (panelW - dp(64)) / 3f);
        float gap = (panelW - slot * 3) / 4f;
        float start = panelLeft + gap;
        for (int i = 0; i < 3; i++) {
            RectF slotRect = trayRects[i];
            float slotTop = top + (panelH - slot) / 2f;
            slotRect.set(start + i * (slot + gap), slotTop, start + i * (slot + gap) + slot, slotTop + slot);
            paint.setColor(Color.rgb(18, 20, 34));
            canvas.drawRoundRect(slotRect, dp(14), dp(14), paint);
            if (i == selectedTray) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(Color.argb(150, 0, 218, 243));
                canvas.drawRoundRect(slotRect, dp(14), dp(14), paint);
                paint.setStyle(Paint.Style.FILL);
            }
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
        if (valid) {
            highlightClearingLines(canvas, area, selectedPiece, previewCol, previewRow);
        }
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

    private void highlightClearingLines(Canvas canvas, BoardArea area, BlockPiece piece, int placeX, int placeY) {
        boolean[] rows = new boolean[BOARD];
        boolean[] cols = new boolean[BOARD];
        findFullLinesAfterPlace(piece, placeX, placeY, rows, cols);
        paint.setColor(Color.argb(86, 0, 218, 243));
        for (int r = 0; r < BOARD; r++) {
            if (rows[r]) {
                rect.set(area.bounds.left, area.bounds.top + r * area.cell, area.bounds.right, area.bounds.top + (r + 1) * area.cell);
                canvas.drawRect(rect, paint);
            }
        }
        paint.setColor(Color.argb(86, 255, 223, 158));
        for (int c = 0; c < BOARD; c++) {
            if (cols[c]) {
                rect.set(area.bounds.left + c * area.cell, area.bounds.top, area.bounds.left + (c + 1) * area.cell, area.bounds.bottom);
                canvas.drawRect(rect, paint);
            }
        }
    }

    private void findFullLinesAfterPlace(BlockPiece piece, int placeX, int placeY, boolean[] rows, boolean[] cols) {
        int[][] copy = new int[BOARD][BOARD];
        for (int r = 0; r < BOARD; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, BOARD);
        }
        placePiece(copy, piece, placeX, placeY);
        for (int r = 0; r < BOARD; r++) {
            rows[r] = true;
            cols[r] = true;
            for (int c = 0; c < BOARD; c++) {
                rows[r] &= copy[r][c] != EMPTY;
                cols[r] &= copy[c][r] != EMPTY;
            }
        }
    }

    private void drawDraggedPiece(Canvas canvas, BoardArea area) {
        if (!draggingPiece || selectedPiece == null) {
            return;
        }
        float cell = area.cell * 0.92f;
        float left = dragX - selectedPiece.width * cell / 2f;
        float top = dragY - dp(96) - selectedPiece.height * cell / 2f;
        paint.setAlpha(210);
        drawPiece(canvas, selectedPiece, left, top, cell, true);
        paint.setAlpha(255);
    }

    private void drawBoard(Canvas canvas, int[][] grid, BoardArea area, boolean jewel) {
        paint.setColor(jewel ? Color.rgb(25, 27, 34) : Color.rgb(143, 160, 139));
        canvas.drawRoundRect(area.bounds, jewel ? dp(10) : dp(6), jewel ? dp(10) : dp(6), paint);
        int rows = grid.length;
        int cols = grid[0].length;
        float gap = jewel ? dp(2) : dp(1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = area.bounds.left + c * area.cell;
                float y = area.bounds.top + r * area.cell;
                rect.set(x + gap / 2f, y + gap / 2f, x + area.cell - gap / 2f, y + area.cell - gap / 2f);
                paint.setColor(jewel ? Color.rgb(19, 22, 31) : Color.rgb(131, 151, 128));
                canvas.drawRoundRect(rect, jewel ? dp(2) : 0f, jewel ? dp(2) : 0f, paint);
                if (grid[r][c] != EMPTY) {
                    drawBlock(canvas, x, y, area.cell, colorFor(grid[r][c]), jewel);
                }
            }
        }
    }

    private void drawPiece(Canvas canvas, BlockPiece piece, float left, float top, float cell, boolean jewel) {
        if (piece == null) {
            return;
        }
        for (Cell cellPos : piece.cells) {
            drawBlock(canvas, left + cellPos.x * cell, top + cellPos.y * cell, cell, colorFor(piece.color), jewel);
        }
    }

    private void drawBlock(Canvas canvas, float x, float y, float size, int color, boolean jewel) {
        float inset = Math.max(1f, size * (jewel ? 0.045f : 0.04f));
        rect.set(x + inset, y + inset, x + size - inset, y + size - inset);
        paint.setColor(Color.argb(jewel ? 150 : 80, 0, 0, 0));
        canvas.drawRoundRect(rect.left + dp(2), rect.top + dp(3), rect.right + dp(2), rect.bottom + dp(3), jewel ? size * 0.12f : dp(3), jewel ? size * 0.12f : dp(3), paint);
        paint.setColor(jewel ? darken(color) : Color.rgb(42, 56, 47));
        canvas.drawRoundRect(rect, jewel ? size * 0.12f : dp(3), jewel ? size * 0.12f : dp(3), paint);
        rect.inset(size * 0.055f, size * 0.055f);
        paint.setColor(jewel ? color : Color.rgb(255, 205, 63));
        canvas.drawRoundRect(rect, jewel ? size * 0.1f : dp(3), jewel ? size * 0.1f : dp(3), paint);
        if (jewel) {
            paint.setColor(Color.argb(105, 255, 255, 255));
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
        float size = Math.min(getWidth() - dp(48), getHeight() * 0.44f);
        float left = (getWidth() - size) / 2f;
        float top = dp(126);
        return new BoardArea(new RectF(left, top, left + size, top + size), size / BOARD);
    }

    private void refillTray() {
        tray.clear();
        if (mode == Mode.BLAST && blastMoves < 9) {
            refillSmartBlastTray();
            return;
        }
        for (int i = 0; i < 3; i++) {
            tray.add(randomPiece());
        }
    }

    private void refillSmartBlastTray() {
        List<BlockPiece> clearingPieces = new ArrayList<>();
        List<BlockPiece> fittingPieces = new ArrayList<>();
        for (int[][] shape : pieceShapes()) {
            BlockPiece piece = new BlockPiece(shape, blastColorIndex);
            addUsefulBlastPiece(piece, clearingPieces, fittingPieces);
            BlockPiece rotated = piece.rotated();
            addUsefulBlastPiece(rotated, clearingPieces, fittingPieces);
        }

        while (tray.size() < 3 && !clearingPieces.isEmpty()) {
            tray.add(clearingPieces.remove(random.nextInt(clearingPieces.size())));
        }
        while (tray.size() < 3 && !fittingPieces.isEmpty()) {
            tray.add(fittingPieces.remove(random.nextInt(fittingPieces.size())));
        }
        while (tray.size() < 3) {
            tray.add(randomPiece());
        }
    }

    private void addUsefulBlastPiece(BlockPiece piece, List<BlockPiece> clearingPieces, List<BlockPiece> fittingPieces) {
        boolean fits = false;
        boolean clears = false;
        for (int y = 0; y < BOARD; y++) {
            for (int x = 0; x < BOARD; x++) {
                if (canPlace(board, piece, x, y)) {
                    fits = true;
                    if (wouldClearAfterPlace(piece, x, y)) {
                        clears = true;
                    }
                }
            }
        }
        if (clears) {
            clearingPieces.add(piece);
        } else if (fits) {
            fittingPieces.add(piece);
        }
    }

    private boolean wouldClearAfterPlace(BlockPiece piece, int x, int y) {
        int[][] copy = new int[BOARD][BOARD];
        for (int r = 0; r < BOARD; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, BOARD);
        }
        placePiece(copy, piece, x, y);
        return countFullLines(copy) > 0;
    }

    private void seedBlastBoard() {
        for (int c = 0; c < BOARD; c++) {
            if (c < 6) {
                board[8][c] = blastColorIndex;
            }
        }
        for (int r = 3; r < BOARD; r++) {
            if (r != 6) {
                board[r][0] = blastColorIndex;
            }
        }
        int[][] seeds = {
                {0, 0, 0}, {1, 0, 0}, {7, 2, 2}, {8, 2, 2},
                {7, 3, 2}, {8, 3, 2}, {7, 4, 2}, {8, 4, 2},
                {5, 7, 3}, {6, 7, 3}, {7, 7, 3}
        };
        for (int[] seed : seeds) {
            board[seed[1]][seed[0]] = blastColorIndex;
        }
    }

    private void nextBlastColor() {
        int next = random.nextInt(COLORS.length - 1);
        if (next >= blastColorIndex) {
            next++;
        }
        blastColorIndex = next;
    }

    private boolean isBoardEmpty(int[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] != EMPTY) {
                    return false;
                }
            }
        }
        return true;
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
        int count = countFullLines(grid);
        boolean[] fullRows = new boolean[BOARD];
        boolean[] fullCols = new boolean[BOARD];
        for (int r = 0; r < BOARD; r++) {
            fullRows[r] = true;
            fullCols[r] = true;
            for (int c = 0; c < BOARD; c++) {
                fullRows[r] &= grid[r][c] != EMPTY;
                fullCols[r] &= grid[c][r] != EMPTY;
            }
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

    private int countFullLines(int[][] grid) {
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
        int[][][] shapes = pieceShapes();
        int pick = random.nextInt(shapes.length);
        return new BlockPiece(shapes[pick], mode == Mode.BLAST ? blastColorIndex : random.nextInt(COLORS.length));
    }

    private int[][][] pieceShapes() {
        return new int[][][]{
                {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
                {{0, 0}, {1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}},
                {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {0, 2}, {1, 2}},
                {{0, 0}, {1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}, {0, 2}, {1, 2}, {2, 2}},
                {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
                {{0, 0}, {0, 1}, {0, 2}, {1, 2}},
                {{1, 0}, {1, 1}, {0, 1}, {0, 2}},
                {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
                {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
                {{0, 0}, {1, 0}, {2, 0}},
                {{0, 0}, {0, 1}, {0, 2}},
                {{0, 0}, {1, 0}, {2, 0}, {2, 1}},
                {{0, 0}, {1, 0}, {0, 1}},
                {{0, 0}, {1, 0}},
                {{0, 0}, {0, 1}},
                {{0, 0}}
        };
    }

    private int colorFor(int colorIndex) {
        return COLORS[(mode == Mode.BLAST ? blastColorIndex : colorIndex) % COLORS.length];
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
