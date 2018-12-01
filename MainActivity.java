package com.example.m.kulka2d;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.HashMap;
import java.util.Map;
import static java.lang.Math.abs;


public class MainActivity extends Activity implements SensorEventListener{

    private GameView mGameView;
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private WakeLock mWakeLock;
    private boolean DEBUG = false;
    private float targetX;
    private float targetY;
    private int score = 0;
    private float currentDegree = 0f;
    //compass with the help of https://www.javacodegeeks.com/2013/09/android-compass-code-example.html
    private ImageView compassImage;
    float yOrientation;
    float zOrientation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());

        mGameView = new GameView(this);
        mGameView.setBackgroundResource(R.drawable.grass);
        setContentView(mGameView);
        compassImage = new ImageView(this);
        compassImage.setImageResource(R.drawable.compass);
        addContentView(compassImage,new ViewGroup.LayoutParams(250,250));

    }

    @Override
    protected void onResume() {
        super.onResume();
        mWakeLock.acquire();
        mSensorManager.registerListener( this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
        mGameView.startSimulation();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameView.stopSimulation();
        mWakeLock.release();
        mSensorManager.unregisterListener( this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
        yOrientation = Math.round(event.values[1]);
        zOrientation = Math.round(event.values[2]);
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(currentDegree, -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        // how long the animation will take place
        ra.setDuration(210);
        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        // Start the animation
        compassImage.startAnimation(ra);
        currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    class GameView extends FrameLayout implements SensorEventListener {
        private static final float ballDiameter = 0.01f;

        private final int dstWidth;
        private final int dstHeight;

        private Sensor accelerometer;
        private long lastT;

        private float xDpi;
        private float yDpi;
        private float metersToPixelsX;
        private float metersToPixelsY;
        private float originX;
        private float originY;
        private float sensorX;
        private float sensorY;
        private float horizontalBoundary;
        private float verticalBoundary;
        private final Environment environment;

        public GameView(Context context, int dstWidth, int dstHeight, Environment environment) {
            super(context);
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;
            this.environment = environment;
        }

        public GameView(Context context, AttributeSet attrs, int dstWidth, int dstHeight, Environment environment) {
            super(context, attrs);
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;
            this.environment = environment;
        }

        public GameView(Context context, AttributeSet attrs, int defStyleAttr, int dstWidth, int dstHeight, Environment environment) {
            super(context, attrs, defStyleAttr);
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;
            this.environment = environment;
        }

        public GameView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, int dstWidth, int dstHeight, Environment environment) {
            super(context, attrs, defStyleAttr, defStyleRes);
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;
            this.environment = environment;
        }

        public void CreateTarget() {
            float[] target = Helpers.CreateRandomTargetCoordinates(horizontalBoundary, verticalBoundary
                    , -horizontalBoundary * 0.4, -verticalBoundary * 0.4
                    , horizontalBoundary * 0.4, verticalBoundary * 0.4);

            targetX = target[0];
            targetY = target[1];
        }

        class GolfBall extends View {
            private float positionX = (float) Math.random();
            private float positionY = (float) Math.random();
            private float velocityX;
            private float velocityY;

            public GolfBall(Context context) {
                super(context);
            }

            public GolfBall(Context context, AttributeSet attrs) {
                super(context, attrs);
            }

            public GolfBall(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }

            public void computePhysics(float sx, float sy, float dT) {

                final float ax = -sx / 5;
                final float ay = -sy / 5;

                positionX += velocityX * dT + ax * dT * dT / 2;
                positionY += velocityY * dT + ay * dT * dT / 2;

                velocityX += ax * dT;
                velocityY += ay * dT;
            }

            public void resolveCollisionWithBounds() {
                final float xMax = horizontalBoundary;
                final float yMax = verticalBoundary;
                final float x = positionX;
                final float y = positionY;

                if (x > xMax) {
                    positionX = xMax;
                    velocityX = velocityX * -0.5f;
                } else if (x < -xMax) {
                    positionX = -xMax;
                    velocityX = velocityX * -0.5f;
                }
                if (y > yMax) {
                    positionY = yMax;
                    velocityY = velocityY * -0.5f;
                } else if (y < -yMax) {
                    positionY = -yMax;
                    velocityY = velocityY * -0.5f;
                }
            }

            public void resolveCollisionWithObstacles() {
                final float obstacleX = horizontalBoundary / 2;
                final float obstacleY = verticalBoundary / 2;
                final float x = positionX;
                final float y = positionY;

                if (x > -obstacleX && x < obstacleX && y > -obstacleY && y < obstacleY) {
                    Map<String, Float> borders = new HashMap<String, Float>();
                    borders.put("leftBorder", abs(x - -obstacleX));
                    borders.put("rightBorder", abs(x - obstacleX));
                    borders.put("topBorder", abs(y - obstacleY));
                    borders.put("bottomBorder", abs(y - -obstacleY));

                    Map.Entry<String, Float> obstacleAt = null;
                    for (Map.Entry<String, Float> entry : borders.entrySet()) {
                        if (obstacleAt == null || entry.getValue().compareTo(obstacleAt.getValue()) < 0) {
                            obstacleAt = entry;
                        }
                    }

                    if (obstacleAt.getKey() == "bottomBorder") {
                        positionY = -obstacleY;
                        velocityY = velocityY * -0.5f;
                    } else if (obstacleAt.getKey() == "topBorder") {
                        positionY = obstacleY;
                        velocityY = velocityY * -0.5f;
                    } else if (obstacleAt.getKey() == "leftBorder") {
                        positionX = -obstacleX;
                        velocityX = velocityX * -0.5f;
                    } else if (obstacleAt.getKey() == "rightBorder") {
                        positionX = obstacleX;
                        velocityX = velocityX * -0.5f;
                    }
                }
            }

            public void resolveCollisionWithTarget() {
                final float x = positionX;
                final float y = positionY;


                if (x > targetX - horizontalBoundary * 0.1 && x < targetX + horizontalBoundary * 0.1 &&
                        y > targetY - verticalBoundary * 0.1 && y < targetY + verticalBoundary * 0.1) {
                    CreateTarget();
                    this.positionX = -positionX;
                    this.positionY = -positionY;
                    score++;
                }
            }
        }

        class Environment {
            private GolfBall golfBall;

            Environment() {
                golfBall = new GolfBall(getContext());
                golfBall.setBackgroundResource(R.drawable.golfball);
                golfBall.setLayerType(LAYER_TYPE_HARDWARE, null);
                addView(golfBall, new ViewGroup.LayoutParams(dstWidth, dstHeight));
            }

            private void updatePositions(float sx, float sy, long timestamp) {
                final long t = timestamp;
                if (lastT != 0) {
                    final float dT = (float) (t - lastT) / 1000.f;
                    GolfBall ball = golfBall;
                    ball.computePhysics(sx, sy, dT);
                }
                lastT = t;
            }

            public void update(float sx, float sy, long now) {
                // update the system's positions
                updatePositions(sx, sy, now);
                golfBall.resolveCollisionWithBounds();
                golfBall.resolveCollisionWithObstacles();
                golfBall.resolveCollisionWithTarget();
            }

            public float getPosX() {
                return golfBall.positionX;
            }

            public float getPosY() {
                return golfBall.positionY;
            }
        }

        public void startSimulation() {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }

        public GameView(Context context) {
            super(context);


            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            xDpi = metrics.xdpi;
            yDpi = metrics.ydpi;
            metersToPixelsX = xDpi / 0.0254f;
            metersToPixelsY = yDpi / 0.0254f;

            dstWidth = (int) (ballDiameter * metersToPixelsX + 0.5f);
            dstHeight = (int) (ballDiameter * metersToPixelsY + 0.5f);
            environment = new Environment();

            Options opts = new Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            originX = (w - dstWidth) * 0.5f;
            originY = (h - dstHeight) * 0.5f;
            horizontalBoundary = ((w / metersToPixelsX - ballDiameter) * 0.5f);
            verticalBoundary = ((h / metersToPixelsY - ballDiameter) * 0.5f);
            CreateTarget();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;
            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    sensorX = event.values[0];
                    sensorY = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    sensorX = -event.values[1];
                    sensorY = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    sensorX = -event.values[0];
                    sensorY = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    sensorX = event.values[1];
                    sensorY = -event.values[0];
                    break;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final Environment environment = this.environment;
            final long now = System.currentTimeMillis();
            final float sx = sensorX;
            final float sy = sensorY;
            final int width = this.getWidth() / 2;
            final int height = this.getHeight() / 2;
            Paint paint = new Paint();
            Paint paintOrientation = new Paint();

            environment.update(sx, sy, now);

            paintOrientation.setTextSize(100f);
            paintOrientation.setColor(Color.RED);
            paint.setTextSize(30f);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(3);

            canvas.drawRect(width * 0.65f, height * 0.6f
                    , width * 1.35f, height * 1.4f
                    , paint);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rocks)
                    , null, new RectF(width * 0.65f, height * 0.6f
                            , width * 1.35f, height * 1.4f), null);

            final float xc = originX;
            final float yc = originY;
            final float xs = metersToPixelsX;
            final float ys = metersToPixelsY;

            canvas.drawText(Helpers.GetOrientation(yOrientation, zOrientation), xc, 600, paintOrientation);
            canvas.drawText("score: " + score, 300, 30, paint);
            float targetXTranslated = originX + (originX * targetX / horizontalBoundary);
            float targetYTranslated = originY - (originY * targetY / verticalBoundary);
            canvas.drawCircle(targetXTranslated, targetYTranslated, 50, paint);

            if (DEBUG) {
                canvas.drawText("ballX: " + environment.golfBall.positionX, 0, 15, paint);
                canvas.drawText("ballY: " + environment.golfBall.positionY, 0, 40, paint);
                canvas.drawText("targetX: " + targetX, 0, 65, paint);
                canvas.drawText("targetY: " + targetY, 0, 90, paint);
                canvas.drawText("targetX trans: " + targetXTranslated, 0, 115, paint);
                canvas.drawText("targetY trans: " + targetYTranslated, 0, 140, paint);
                canvas.drawText("sensorX: " + sensorX, 0, 165, paint);
                canvas.drawText("sensorY: " + sensorY, 0, 190, paint);
                canvas.drawText("yOrientation: " + yOrientation, 0, 215, paint);
                canvas.drawText("zOrientation: " + zOrientation, 0, 240, paint);
            }
            final float x = xc + environment.getPosX() * xs;
            final float y = yc - environment.getPosY() * ys;
            environment.golfBall.setTranslationX(x);
            environment.golfBall.setTranslationY(y);

            invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
