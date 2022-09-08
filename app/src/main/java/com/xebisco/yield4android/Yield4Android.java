package com.xebisco.yield4android;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.xebisco.yield.AudioClip;
import com.xebisco.yield.AudioPlayer;
import com.xebisco.yield.Filter;
import com.xebisco.yield.MultiThread;
import com.xebisco.yield.RelativeFile;
import com.xebisco.yield.SampleGraphics;
import com.xebisco.yield.SampleWindow;
import com.xebisco.yield.Texture;
import com.xebisco.yield.Vector2;
import com.xebisco.yield.Vector3;
import com.xebisco.yield.YldB;
import com.xebisco.yield.YldGame;
import com.xebisco.yield.YldPair;
import com.xebisco.yield.config.WindowConfiguration;
import com.xebisco.yield.exceptions.NotSupportedException;
import com.xebisco.yield.render.ExceptionThrower;
import com.xebisco.yield.render.MultipleFingerPointers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Yield4Android extends View implements com.xebisco.yield.render.RenderMaster, ExceptionThrower, MultipleFingerPointers {

    private RectF rect = new RectF(0, 0, 0, 0);
    public static final String version = "v0.0.2";
    private Rect textBounds = new Rect();
    private final Paint paint = new Paint();
    private com.xebisco.yield.View view;
    private Bitmap viewBitmap;
    private Canvas drawCanvas;
    private Rect viewRect, viewOut;
    private boolean canStart;
    private Map<Integer, Bitmap> bitmaps = new HashMap<>();
    private Map<String, YldPair<Typeface, Float>> typefaces = new HashMap<>();
    private Set<Integer> keys = new HashSet<>();
    private Map<Integer, YldPair<MediaPlayer, Float>> mediaPlayers = new HashMap<>();
    private final Set<Vector3> fingerPointers = new HashSet<>();
    private long actual, last;
    private float lMouseX, lMouseY, fps;

    public static int yieldColor(com.xebisco.yield.Color color) {
        return Color.argb(color.getA(), color.getR(), color.getG(), color.getB());
    }

    public Yield4Android(Context context) {
        super(context);
        if (context instanceof Activity)
            ((Activity) context).setContentView(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        actual = System.currentTimeMillis();
        fps = 1000 / (float) (actual - last);
        canvas.drawBitmap(viewBitmap, null, viewOut, paint);
        last = System.currentTimeMillis();
    }

    @Override
    public boolean isInTouchMode() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        final int points = event.getPointerCount();

        // Check if it's an event that a finger
        // was removed, if so, set removedPoint
        int removedPoint = -1;
        final int action = event.getAction() & MotionEvent.ACTION_MASK;

        if(action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP)
            removedPoint = 0;


        StringBuilder out = new StringBuilder();

        fingerPointers.clear();

        for(int i = 0; i < points; i++){
            // Find out pointer ID
            int pointerID = event.getPointerId(i);
            if(pointerID == MotionEvent.INVALID_POINTER_ID){
                out.append("\tPoint ").append(pointerID).append(" INVALID\n");
                continue;
            }

            // Check if it's the removed finger
            if(removedPoint == i){
                out.append("\tPoint ").append(pointerID).append(" REMOVED\n");
                continue;
            }

            out.append("\tPoint ").append(pointerID).append("\t(").append(event.getX(i)).append(", ").append(event.getY(i)).append(")\n");
            fingerPointers.add(new Vector3(event.getX(i), event.getY(i), pointerID));
        }

        Log.i(TAG, out.toString());

        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public SampleGraphics initGraphics() {
        return new SampleGraphics() {
            private float lastAngle;
            private Vector2 lastPos = new Vector2();

            @Override
            public void setRotation(Vector2 vector2, float v) {
                drawCanvas.rotate(-lastAngle, lastPos.x, lastPos.y);
                lastPos = vector2.get();
                lastAngle = v;
                drawCanvas.rotate(v, vector2.x, vector2.y);
            }

            @Override
            public void drawLine(Vector2 vector2, Vector2 vector21, com.xebisco.yield.Color color) {
                paint.setColor(yieldColor(color));
                drawCanvas.drawLine(vector2.x, vector2.y, vector21.x, vector21.y, paint);
            }

            @Override
            public void drawRect(Vector2 vector2, Vector2 vector21, com.xebisco.yield.Color color, boolean b) {
                if (b)
                    paint.setStyle(Paint.Style.FILL);
                else
                    paint.setStyle(Paint.Style.STROKE);
                paint.setColor(yieldColor(color));
                rect.left = (int) (vector2.x - vector21.x / 2f);
                rect.top = (int) (vector2.y - vector21.y / 2f);
                rect.right = rect.left + (int) vector21.x;
                rect.bottom = rect.top + (int) vector21.y;
                drawCanvas.drawRect(rect, paint);
            }

            @Override
            public void drawRoundRect(Vector2 vector2, Vector2 vector21, com.xebisco.yield.Color color, boolean b, int i, int i1) {
                if (b)
                    paint.setStyle(Paint.Style.FILL);
                else
                    paint.setStyle(Paint.Style.STROKE);
                paint.setColor(yieldColor(color));
                rect.left = (int) (vector2.x - vector21.x / 2f);
                rect.top = (int) (vector2.y - vector21.y / 2f);
                rect.right = rect.left + (int) vector21.x;
                rect.bottom = rect.top + (int) vector21.y;
                drawCanvas.drawRoundRect(rect, i, i1, paint);
            }

            @Override
            public void drawOval(Vector2 vector2, Vector2 vector21, com.xebisco.yield.Color color, boolean b) {
                if (b)
                    paint.setStyle(Paint.Style.FILL);
                else
                    paint.setStyle(Paint.Style.STROKE);
                paint.setColor(yieldColor(color));
                rect.left = (int) (vector2.x - vector21.x / 2f);
                rect.top = (int) (vector2.y - vector21.y / 2f);
                rect.right = rect.left + (int) vector21.x;
                rect.bottom = rect.top + (int) vector21.y;
                drawCanvas.drawOval(rect, paint);
            }

            @Override
            public void drawArc(Vector2 vector2, Vector2 vector21, com.xebisco.yield.Color color, boolean b, int i, int i1) {
                if (b)
                    paint.setStyle(Paint.Style.FILL);
                else
                    paint.setStyle(Paint.Style.STROKE);
                paint.setColor(yieldColor(color));
                rect.left = (int) (vector2.x - vector21.x / 2f);
                rect.top = (int) (vector2.y - vector21.y / 2f);
                rect.right = rect.left + (int) vector21.x;
                rect.bottom = rect.top + (int) vector21.y;
                drawCanvas.drawArc(rect, i, i1, true, paint);
            }

            @Override
            public void drawString(String s, com.xebisco.yield.Color color, Vector2 vector2, Vector2 vector21, String s1) {
                paint.setColor(yieldColor(color));
                setFont(s1);
                paint.setTextScaleX(vector21.x);
                drawCanvas.drawText(s, vector2.x - getStringWidth(s) / 2f, vector2.y - getStringHeight(s) / 2f, paint);
            }

            @Override
            public void drawTexture(Texture texture, Vector2 vector2, Vector2 vector21) {
                Bitmap bitmap = bitmaps.get(texture.getTextureID());
                rect.left = (int) (vector2.x - vector21.x / 2f);
                rect.top = (int) (vector2.y - vector21.y / 2f);
                rect.right = rect.left + (int) vector21.x;
                rect.bottom = rect.top + (int) vector21.y;
                drawCanvas.drawBitmap(bitmap, null, rect, paint);
            }

            @Override
            public void setFilter(Filter filter) {
                paint.setAntiAlias(filter == Filter.LINEAR);
            }

            private YldPair<Typeface, Float> font;

            @Override
            public void setFont(String s) {
                font = typefaces.get(s);
                assert font != null;
                paint.setTypeface(font.getFirst());
                paint.setTextSize(font.getSecond());
            }

            @Override
            public float getStringWidth(String s) {
                paint.getTextBounds(s, 0, s.length(), textBounds);
                return textBounds.right;
            }

            @Override
            public float getStringWidth(String s, String s1) {
                setFont(s1);
                paint.getTextBounds(s, 0, s.length(), textBounds);
                return textBounds.right;
            }

            @Override
            public float getStringHeight(String s) {
                paint.getTextBounds(s, 0, s.length(), textBounds);
                return textBounds.top * -1 + textBounds.bottom;
            }

            @Override
            public float getStringHeight(String s, String s1) {
                setFont(s1);
                paint.getTextBounds(s, 0, s.length(), textBounds);
                return textBounds.top * -1 + textBounds.bottom;
            }

            @Override
            public void custom(String s, Object... objects) {

            }
        };
    }

    @Override
    public SampleGraphics specificGraphics() {
        return initGraphics();
    }

    @Override
    public void before(YldGame yldGame) {

    }

    @Override
    public SampleWindow initWindow(WindowConfiguration windowConfiguration) {
        return new SampleWindow() {
            @Override
            public int getWidth() {
                return Yield4Android.this.getWidth();
            }

            @Override
            public int getHeight() {
                return Yield4Android.this.getHeight();
            }
        };
    }

    @Override
    public void frameStart(SampleGraphics sampleGraphics, com.xebisco.yield.View view) {
        this.view = view;
        if (viewBitmap != null) {
            drawCanvas.drawColor(yieldColor(view.getBgColor()));
        }
    }

    @Override
    public void frameEnd() {
        if (viewBitmap == null || (viewBitmap.getWidth() != view.getWidth() || viewBitmap.getHeight() != view.getHeight())) {
            viewBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(viewBitmap);
            viewRect = new Rect(0, 0, view.getWidth(), view.getHeight());
            canStart = true;
        }
        viewOut = new Rect(0, 0, getWidth(), getHeight());
        invalidate();
    }

    @Override
    public boolean canStart() {
        return canStart;
    }

    @Override
    public float fpsCount() {
        return 0;
    }

    @Override
    public Set<Integer> pressing() {
        return keys;
    }

    @Override
    public int mouseX() {
        return (int) lMouseX;
    }

    @Override
    public int mouseY() {
        return (int) lMouseY;
    }

    @Override
    public void close() {

    }

    @Override
    public void loadTexture(Texture texture) {
        Bitmap bitmap = BitmapFactory.decodeStream(texture.getInputStream());
        loadTexture(texture, bitmap);
    }

    public void loadTexture(Texture texture, Bitmap bitmap) {
        bitmaps.put(texture.getTextureID(), bitmap);
        Texture iX = new Texture(""), iY = new Texture(""), iXY = new Texture("");
        Matrix matrix = new Matrix();
        matrix.setScale(-1, 1);
        Bitmap invX = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        matrix.setScale(1, -1);
        Bitmap invY = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        matrix.setScale(-1, -1);
        Bitmap invXY = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        texture.setInvertedX(iX);
        texture.setInvertedY(iY);
        texture.setInvertedXY(iXY);
        texture.setVisualUtils(this);
        bitmaps.put(iX.getTextureID(), invX);
        bitmaps.put(iY.getTextureID(), invY);
        bitmaps.put(iXY.getTextureID(), invXY);
        if (texture.isFlushAfterLoad())
            texture.flush();
    }

    @Override
    public void unloadAllTextures() {
        while (!bitmaps.isEmpty())
            bitmaps.clear();
    }

    @Override
    public void unloadTexture(Texture texture) {
        bitmaps.remove(texture.getTextureID());
    }

    @Override
    public void clearTexture(Texture texture) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID());
        assert bitmap != null;
        bitmaps.put(texture.getTextureID(), Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888));
    }

    @Override
    public void loadFont(String s, String s1, float i, int i1) {
        Typeface typeface = Typeface.create(s1, i1);
        typefaces.put(s, new YldPair<>(typeface, i));
    }

    @Override
    public void loadFont(String s, float v, float v1, int i, RelativeFile relativeFile) {
        try {
            File file = File.createTempFile("tempfont", "tmp");
            fileFromRelativeFile(relativeFile, file);
            Typeface typeface = Typeface.createFromFile(file);
            typefaces.put(s, new YldPair<>(typeface, v));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fileFromRelativeFile(RelativeFile relativeFile, File file) {
        try {
            OutputStream outStream = new FileOutputStream(file);
            if (relativeFile.getInputStream() == null)
                relativeFile.setInputStream(getContext().getAssets().open(relativeFile.getCachedPath()));
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = relativeFile.getInputStream().read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            if (relativeFile.isFlushAfterLoad())
                relativeFile.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unloadFont(String s) {
        typefaces.remove(s);
    }

    @Override
    public com.xebisco.yield.Color[][] getTextureColors(Texture texture) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID());
        assert bitmap != null;
        com.xebisco.yield.Color[][] colors = new com.xebisco.yield.Color[bitmap.getWidth()][bitmap.getHeight()];
        for (int x = 0; x < colors.length; x++)
            for (int y = 0; y < colors[0].length; y++)
                colors[x][y] = new com.xebisco.yield.Color(Color.valueOf(bitmap.getPixel(x, y)).toArgb());
        return colors;
    }

    @Override
    public void setTextureColors(Texture texture, com.xebisco.yield.Color[][] colors) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID());
        assert bitmap != null;
        for (int x = 0; x < colors.length; x++)
            for (int y = 0; y < colors[0].length; y++)
                bitmap.setPixel(x, y, yieldColor(colors[x][y]));
    }

    @Override
    public void setPixel(Texture texture, com.xebisco.yield.Color color, Vector2 vector2) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID());
        assert bitmap != null;
        bitmap.setPixel((int) vector2.x, (int) vector2.y, yieldColor(color));
    }

    @Override
    public Texture cutTexture(Texture texture, int i, int i1, int i2, int i3) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID()), b2 = Bitmap.createBitmap(i2, i3, Bitmap.Config.ARGB_8888);
        assert bitmap != null;
        Canvas canvas = new Canvas(b2);
        rect.bottom = bitmap.getHeight() - i1;
        rect.right = bitmap.getWidth() - i;
        rect.top = -i1;
        rect.left = -i;
        canvas.drawBitmap(bitmap, null, rect, paint);
        Texture texture1 = new Texture(texture.getCachedPath());
        loadTexture(texture1, b2);
        return texture1;
    }

    @Override
    public Texture duplicate(Texture texture) {
        Texture texture1 = new Texture(texture.getCachedPath());
        loadTexture(texture1, bitmaps.get(texture.getTextureID()));
        return texture1;
    }

    @Override
    public Texture overlayTexture(Texture texture, Texture texture1, Vector2 vector2, Vector2 vector21) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID()), bitmap2 = bitmaps.get(texture1.getTextureID());
        assert bitmap != null && bitmap2 != null;
        Bitmap b2 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b2);
        rect.top = 0;
        rect.left = 0;
        rect.bottom = bitmap.getHeight();
        rect.right = bitmap.getWidth();
        canvas.drawBitmap(bitmap, null, rect, paint);
        rect.bottom = vector21.y;
        rect.right = vector21.x;
        rect.top = vector2.y;
        rect.left = vector2.x;
        canvas.drawBitmap(bitmap2, null, rect, paint);
        Texture texture11 = new Texture(texture.getCachedPath());
        loadTexture(texture11, b2);
        return texture11;
    }

    @Override
    public Texture scaleTexture(Texture texture, int i, int i1) {
        Bitmap bitmap = bitmaps.get(texture.getTextureID());
        assert bitmap != null;
        Bitmap b2 = Bitmap.createScaledBitmap(bitmap, i, i1, false);
        Texture texture1 = new Texture(texture.getCachedPath());
        loadTexture(texture1, b2);
        return texture1;
    }

    @Override
    public int getTextureWidth(int i) {
        Bitmap bitmap = bitmaps.get(i);
        assert bitmap != null;
        return bitmap.getWidth();
    }

    @Override
    public int getTextureHeight(int i) {
        Bitmap bitmap = bitmaps.get(i);
        assert bitmap != null;
        return bitmap.getHeight();
    }

    @Override
    public void loadAudioClip(AudioClip audioClip, AudioPlayer audioPlayer, MultiThread multiThread, YldB yldB) {
        try {
            AssetFileDescriptor afd = assetFileDescriptorFromRelativeFile(audioClip);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.setVolume(1f, 1f);
            mediaPlayers.put(audioPlayer.getPlayerID(), new YldPair<>(player, 1f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AssetFileDescriptor assetFileDescriptorFromRelativeFile(RelativeFile relativeFile) {
        try {
            return getContext().getAssets().openFd(relativeFile.getCachedPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setMicrosecondPosition(AudioPlayer audioPlayer, long l) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        MediaPlayer player = pair.getFirst();
        player.seekTo((int) l);
    }

    @Override
    public long getMicrosecondPosition(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        MediaPlayer player = pair.getFirst();
        return player.getCurrentPosition();
    }

    @Override
    public long getMicrosecondLength(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        MediaPlayer player = pair.getFirst();
        return player.getDuration();
    }

    @Override
    public float getVolume(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        return pair.getSecond();
    }

    @Override
    public void setVolume(AudioPlayer audioPlayer, float v) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        MediaPlayer player = pair.getFirst();
        player.setVolume(1f, 1f);
        mediaPlayers.replace(audioPlayer.getPlayerID(), new YldPair<>(player, v));
    }

    @Override
    public void pausePlayer(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> mediaPlayer = mediaPlayers.get(audioPlayer.getPlayerID());
        assert mediaPlayer != null;
        mediaPlayer.getFirst().pause();
    }

    @Override
    public void resumePlayer(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> mediaPlayer = mediaPlayers.get(audioPlayer.getPlayerID());
        assert mediaPlayer != null;
        mediaPlayer.getFirst().start();
    }

    @Override
    public void flushPlayer(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> mediaPlayer = mediaPlayers.get(audioPlayer.getPlayerID());
        assert mediaPlayer != null;
        mediaPlayer.getFirst().release();
    }

    @Override
    public void setLoop(AudioPlayer audioPlayer, boolean b) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        MediaPlayer player = pair.getFirst();
        player.setLooping(b);
    }

    @Override
    public void setLoop(AudioPlayer audioPlayer, int i) {
        throw new NotSupportedException();
    }

    @Override
    public boolean isPlayerRunning(AudioPlayer audioPlayer) {
        YldPair<MediaPlayer, Float> pair = mediaPlayers.get(audioPlayer.getPlayerID());
        assert pair != null;
        return pair.getFirst().isPlaying();
    }

    @Override
    public void loadAudioPlayer(AudioPlayer audioPlayer) {

    }

    @Override
    public void throwException(Exception e) {
        e.printStackTrace();
    }

    public RectF getRect() {
        return rect;
    }

    public void setRect(RectF rect) {
        this.rect = rect;
    }

    public Rect getTextBounds() {
        return textBounds;
    }

    public void setTextBounds(Rect textBounds) {
        this.textBounds = textBounds;
    }

    public Paint getPaint() {
        return paint;
    }

    public com.xebisco.yield.View getView() {
        return view;
    }

    public void setView(com.xebisco.yield.View view) {
        this.view = view;
    }

    public Bitmap getViewBitmap() {
        return viewBitmap;
    }

    public void setViewBitmap(Bitmap viewBitmap) {
        this.viewBitmap = viewBitmap;
    }

    public Canvas getDrawCanvas() {
        return drawCanvas;
    }

    public void setDrawCanvas(Canvas drawCanvas) {
        this.drawCanvas = drawCanvas;
    }

    public Rect getViewRect() {
        return viewRect;
    }

    public void setViewRect(Rect viewRect) {
        this.viewRect = viewRect;
    }

    public Rect getViewOut() {
        return viewOut;
    }

    public void setViewOut(Rect viewOut) {
        this.viewOut = viewOut;
    }

    public boolean isCanStart() {
        return canStart;
    }

    public void setCanStart(boolean canStart) {
        this.canStart = canStart;
    }

    public Map<Integer, Bitmap> getBitmaps() {
        return bitmaps;
    }

    public void setBitmaps(Map<Integer, Bitmap> bitmaps) {
        this.bitmaps = bitmaps;
    }

    public Map<String, YldPair<Typeface, Float>> getTypefaces() {
        return typefaces;
    }

    public void setTypefaces(Map<String, YldPair<Typeface, Float>> typefaces) {
        this.typefaces = typefaces;
    }

    public Set<Integer> getKeys() {
        return keys;
    }

    public void setKeys(Set<Integer> keys) {
        this.keys = keys;
    }

    public Map<Integer, YldPair<MediaPlayer, Float>> getMediaPlayers() {
        return mediaPlayers;
    }

    public void setMediaPlayers(Map<Integer, YldPair<MediaPlayer, Float>> mediaPlayers) {
        this.mediaPlayers = mediaPlayers;
    }

    public long getActual() {
        return actual;
    }

    public void setActual(long actual) {
        this.actual = actual;
    }

    public long getLast() {
        return last;
    }

    public void setLast(long last) {
        this.last = last;
    }

    public float getlMouseX() {
        return lMouseX;
    }

    public void setlMouseX(float lMouseX) {
        this.lMouseX = lMouseX;
    }

    public float getlMouseY() {
        return lMouseY;
    }

    public void setlMouseY(float lMouseY) {
        this.lMouseY = lMouseY;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        this.fps = fps;
    }
    @Override
    public Set<Vector3> fingerPointers() {
        return fingerPointers;
    }
}
