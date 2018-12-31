package com.example.m.kulka2d;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class MainActivity extends Activity implements SensorEventListener {

    private GLSurfaceView mGLView;

    private GameView mGameView;
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private WakeLock mWakeLock;
    private boolean DEBUG = false;
    private float targetX;
    private float targetY;
    float obstacleX;
    float obstacleY;
    private int score = 0;
    private float currentDegree = 0f;
    //compass with the help of https://www.javacodegeeks.com/2013/09/android-compass-code-example.html
    private ImageView compassImage;
    float yOrientation;
    float zOrientation;

    final float BORDER_THICKNESS = 30f;
    final boolean TWO_D = false;

    private GameView.GolfBall golfBall;
    private GameView.Obstacle obstacle;
    private float mCubeRotationAngleXBall;
    private float mCubeRotationAngleYBall;
    private float mCubeRotationAngleXObstacle;
    private float mCubeRotationAngleYObstacle;

    protected int crateTextureDataHandle;
    protected ShaderProgram colShaders;
    protected ShaderProgram texShaders;

    private OpenGLView mOpenGLView;

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
        addContentView(compassImage, new ViewGroup.LayoutParams(250, 250));

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        GLSurfaceView view = new GLSurfaceView(this);
//        view.setRenderer(new OpenGLRenderer());
//        addContentView(view, new ViewGroup.LayoutParams(250, 250));
        //setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWakeLock.acquire();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
        mGameView.startSimulation();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameView.stopSimulation();
        mWakeLock.release();
        mSensorManager.unregisterListener(this);
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

        class Obstacle extends GLSurfaceView implements GLSurfaceView.Renderer {
            float positionX = originX;//(float) Math.random();
            float positionY = (float) Math.random();
            float velocityX = 0.1f;
            float velocityY = 0.1f;
            double directionY = -1f;
            double directionX = -1f;

            //from this point on for 3d rendering
            private Cube mCube = new Cube();

            public Obstacle(Context context) {
                super(context);
                setZOrderOnTop(true);
                setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                getHolder().setFormat(PixelFormat.TRANSLUCENT);
            }

            public Obstacle(Context context, AttributeSet attrs) {
                super(context, attrs);
                setZOrderOnTop(true);
                setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                getHolder().setFormat(PixelFormat.TRANSLUCENT);
            }

            public void computePhysics() {
                positionX = (float) (positionX + (directionX * 0.001f));
                positionY = (float) (positionY + (directionY * 0.001f));

                velocityX = java.lang.Math.signum(velocityX) * 0.1f;
                velocityY = java.lang.Math.signum(velocityY) * 0.1f;

                //mCubeRotationAngleXObstacle -= 0.45f;
                mCubeRotationAngleYObstacle -= 0.45f;

                obstacleX = positionX;
                obstacleY = positionY;
            }

            public void resolveCollisionWithBounds() {
                final float xMax = horizontalBoundary;
                final float yMax = verticalBoundary;
                final float x = positionX;
                final float y = positionY;

                float newXPosition = CalculateCoordsOnScreen(BORDER_THICKNESS, true);
                float newYPosition = CalculateCoordsOnScreen(BORDER_THICKNESS, false);
                float obstacleThickness = CalculateCoordsOnScreen(dstWidth, true);

                if (x + obstacleThickness > xMax - newXPosition) {
                    positionX = xMax - newXPosition - obstacleThickness;
                    velocityX = (float) (-velocityX + Helpers.UniversalRandom(-1, 1)) * 3;
                    directionX = -java.lang.Math.signum(directionX) * Helpers.UniversalRandom(-1, 1) * 2;
                } else if (x < -xMax + newXPosition) {
                    positionX = -xMax + newXPosition;
                    velocityX = (float) (-velocityX + Helpers.UniversalRandom(-1, 1)) * 3;
                    directionX = -java.lang.Math.signum(directionX) * Helpers.UniversalRandom(-1, 1) * 2;
                }
                if (y  + obstacleThickness> yMax - newYPosition) {
                    positionY = yMax - newYPosition - obstacleThickness;
                    velocityY = (float) (-velocityY + Helpers.UniversalRandom(-1, 1)) * 3;
                    directionY = -java.lang.Math.signum(directionY) * Helpers.UniversalRandom(-1, 1) * 2;
                } else if (y < -yMax + newYPosition) {
                    positionY = -yMax + newYPosition;
                    velocityY = (float) (-velocityY + Helpers.UniversalRandom(-1, 1)) * 3;
                    directionY = -java.lang.Math.signum(directionY) * Helpers.UniversalRandom(-1, 1) * 2;
                }
            }

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

                gl.glClearDepthf(1.0f);
                gl.glEnable(GL10.GL_DEPTH_TEST);
                gl.glDepthFunc(GL10.GL_LEQUAL);

                gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                        GL10.GL_NICEST);

                gl.glDisable(GL10.GL_DITHER);
                gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                        GL10.GL_FASTEST);

                //loadTexture(this.getContext());
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES10.glActiveTexture(GLES10.GL_TEXTURE0);
                GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, crateTextureDataHandle);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
                gl.glLoadIdentity();

                gl.glTranslatef(0.0f, 0.0f, -10.0f);

                float mCubeRotationAngleY = mCubeRotationAngleYObstacle;
                float mCubeRotationAngleX = mCubeRotationAngleXObstacle;
                gl.glRotatef(mCubeRotationAngleY, 0f, 1f, 0.0f);
                gl.glRotatef(mCubeRotationAngleX, 1f, 0f, 0.0f);

                mCube.draw(gl);

                gl.glLoadIdentity();
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                gl.glViewport(0, 0, width, height);
                gl.glMatrixMode(GL10.GL_PROJECTION);
                gl.glLoadIdentity();
                GLU.gluPerspective(gl, 20.0f, (float) width / (float) height, 0.1f, 100.0f);
                gl.glViewport(0, 0, width, height);

                gl.glMatrixMode(GL10.GL_MODELVIEW);
                gl.glLoadIdentity();
            }
        }


        class GolfBall extends GLSurfaceView implements GLSurfaceView.Renderer {
            private float positionX = (float) Math.random();
            private float positionY = (float) Math.random();
            private float velocityX = 0.5f;
            private float velocityY = 0.5f;
            private boolean isObstacle;

//            /** Store our model data in a float buffer. */
//            private final FloatBuffer mCubeTextureCoordinates;
//            /** This will be used to pass in the texture. */
//            private int mTextureUniformHandle;
//            /** This will be used to pass in model texture coordinate information. */
//            private int mTextureCoordinateHandle;
//            /** Size of the texture coordinate data in elements. */
//            private final int mTextureCoordinateDataSize = 2;
//            /** This is a handle to our texture data. */
//            private int mTextureDataHandle;

            public GolfBall(Context context, boolean isObstacle) {
                super(context);
                setZOrderOnTop(true);
                setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                getHolder().setFormat(PixelFormat.TRANSLUCENT);
                this.isObstacle = isObstacle;
            }

            public GolfBall(Context context, AttributeSet attrs, boolean isObstacle) {
                super(context, attrs);
                setZOrderOnTop(true);
                setEGLConfigChooser(8, 8, 8, 8, 16, 0);
                getHolder().setFormat(PixelFormat.TRANSLUCENT);
                this.isObstacle = isObstacle;
            }

//            public GolfBall(Context context, AttributeSet attrs, int defStyleAttr) {
//                super(context, attrs, defStyleAttr);
//            }

            public void computePhysics(float sx, float sy, float dT) {

                final float ax = -sx / 5;
                final float ay = -sy / 5;

                if (!isObstacle) {
                    positionX += velocityX * dT + ax * dT * dT / 2;
                    positionY += velocityY * dT + ay * dT * dT / 2;

                    velocityX += ax * dT;
                    velocityY += ay * dT;

                    mCubeRotationAngleXBall -= 200 * velocityY;
                    mCubeRotationAngleYBall -= 200 * -velocityX;
                } else {
                    positionX += positionX * 0.1;
                    positionY += positionY * 0.1;

                    velocityX = java.lang.Math.signum(velocityX) * 0.1f;
                    velocityY = java.lang.Math.signum(velocityY) * 0.1f;

                    mCubeRotationAngleXObstacle -= 0.15f;
                    mCubeRotationAngleYObstacle -= 0.15f;
                }
            }

            public void resolveCollisionWithBounds() {
                final float xMax = horizontalBoundary;
                final float yMax = verticalBoundary;
                final float x = positionX;
                final float y = positionY;

                float newXPosition = CalculateCoordsOnScreen(BORDER_THICKNESS, true);
                float newYPosition = CalculateCoordsOnScreen(BORDER_THICKNESS, false);

                if (x > xMax - newXPosition) {
                    positionX = xMax - newXPosition;
                    if (!isObstacle) velocityX = velocityX * -0.5f;
                    else velocityX = -velocityX;
                } else if (x < -xMax + newXPosition) {
                    positionX = -xMax + newXPosition;
                    if (!isObstacle) velocityX = velocityX * -0.5f;
                    else velocityX = -velocityX;
                }
                if (y > yMax - newYPosition) {
                    positionY = yMax - newYPosition;
                    if (!isObstacle) velocityY = velocityY * -0.5f;
                    else velocityY = -velocityY;
                } else if (y < -yMax + newYPosition) {
                    positionY = -yMax + newYPosition;
                    if (!isObstacle) velocityY = velocityY * -0.5f;
                    else velocityY = -velocityY;
                }
            }

            public void resolveCollisionWithObstacles() {
//                final float obstacleX = horizontalBoundary / 2;
//                final float obstacleY = verticalBoundary / 2;
                float leftBorder = obstacleX - CalculateCoordsOnScreen(dstWidth * 1, true);
                float rightBorder = obstacleX + CalculateCoordsOnScreen(dstWidth * 1, true);
                float topBorder = -obstacleY - CalculateCoordsOnScreen(dstHeight, true);
                float bottomBorder = -obstacleY + CalculateCoordsOnScreen(dstHeight, true);
                final float x = positionX;
                final float y = positionY;

                if (Helpers.IsBetween(x, leftBorder, rightBorder) &&
                        Helpers.IsBetween(y, topBorder, bottomBorder))
                //minuses before obstacles due to some bug in setting up the position
                {
                    Map<String, Float> borders = new HashMap<String, Float>();
                    borders.put("leftBorder", abs(leftBorder) - abs(x));
                    borders.put("rightBorder", abs(rightBorder) - abs(x));
                    borders.put("topBorder", abs(topBorder) - abs(y));
                    borders.put("bottomBorder", abs(bottomBorder) - abs(y));

                    Map.Entry<String, Float> obstacleAt = null;
                    for (Map.Entry<String, Float> entry : borders.entrySet()) {
                        if (obstacleAt == null || entry.getValue().compareTo(obstacleAt.getValue()) < 0) {
                            obstacleAt = entry;
                        }
                    }

                    if (obstacleAt.getKey() == "bottomBorder") {
                        positionY = bottomBorder;
                        velocityY = velocityY * -0.5f;
                    } else if (obstacleAt.getKey() == "topBorder") {
                        positionY = topBorder;
                        velocityY = velocityY * -0.5f;
                    } else if (obstacleAt.getKey() == "leftBorder") {
                        positionX = leftBorder;
                        velocityX = velocityX * -0.5f;
                    } else if (obstacleAt.getKey() == "rightBorder") {
                        positionX = rightBorder;
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

            //from this point on for 3d rendering
            private Cube mCube = new Cube();
            //private Sphere mSphere = new Sphere();

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                GLES20.glClearColor(0.2f, 0.5f, 0.1f, 1.0f);

                GLES20.glEnable(GLES20.GL_CULL_FACE);
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                GLES20.glDepthMask(true);


                loadTexture(this.getContext());//,R.drawable.golfball);

                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

                gl.glClearDepthf(1.0f);
                gl.glEnable(GL10.GL_DEPTH_TEST);
                gl.glDepthFunc(GL10.GL_LEQUAL);

                gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                        GL10.GL_NICEST);

                gl.glDisable(GL10.GL_DITHER);
                gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                        GL10.GL_FASTEST);

                colShaders = new ShaderProgram();
                String[] colShadersAttributes = new String[]{"vertexPosition", "vertexColour", "vertexNormal"};
                colShaders.init(R.raw.col_vertex_shader, R.raw.col_fragment_shader, colShadersAttributes, this.getContext(), "kolorowanie");

                texShaders = new ShaderProgram();
                String[] texShadersAttributes = new String[]{"vertexPosition", "vertexTexCoord", "vertexNormal"};
                texShaders.init(R.raw.tex_vertex_shader, R.raw.tex_fragment_shader, texShadersAttributes, this.getContext(), "teksturowanie");
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                GLES10.glActiveTexture(GLES10.GL_TEXTURE0);
                GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, crateTextureDataHandle);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
                gl.glLoadIdentity();

                gl.glTranslatef(0.0f, 0.0f, -10.0f);

                float mCubeRotationAngleY = mCubeRotationAngleYBall;
                float mCubeRotationAngleX = mCubeRotationAngleXBall;
                if (isObstacle) {
                    mCubeRotationAngleY = mCubeRotationAngleYObstacle;
                    mCubeRotationAngleX = mCubeRotationAngleXObstacle;
                }
                gl.glRotatef(mCubeRotationAngleY, 0f, 1f, 0.0f);
                gl.glRotatef(mCubeRotationAngleX, 1f, 0f, 0.0f);

                mCube.draw(gl);
                //mSphere.draw(gl);

                gl.glLoadIdentity();
            }

            protected float[] projectionMatrix = new float[16];

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                gl.glViewport(0, 0, width, height);
                gl.glMatrixMode(GL10.GL_PROJECTION);
                gl.glLoadIdentity();
                GLU.gluPerspective(gl, 20.0f, (float) width / (float) height, 0.1f, 100.0f);
                gl.glViewport(0, 0, width, height);

                gl.glMatrixMode(GL10.GL_MODELVIEW);
                gl.glLoadIdentity();


                GLES20.glViewport(0, 0, width, height);

                // Przygotowanie macierzy projekcji perspektywicznej z uwzględnieniem Field of View.
                final float ratio = (float) width / height;
                final float fov = 60;
                final float near = 1.0f;
                final float far = 10000.0f;
                final float top = (float) (Math.tan(fov * Math.PI / 360.0f) * near);
                final float bottom = -top;
                final float left = ratio * bottom;
                final float right = ratio * top;
                Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
            }

            public void loadTexture(Context context) {
                final int[] textureHandle = new int[1];
                GLES10.glGenTextures(1, textureHandle, 0);
                final Options options = new Options();
                options.inScaled = true;
                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.golfball, options);

                GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureHandle[0]);
                GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
                GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
                GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
                crateTextureDataHandle = textureHandle[0];
                bitmap.recycle();
            }


//            public  int loadTexture(final Context context, final int resourceId)
//            {
//                final int[] textureHandle = new int[1];
//
//                GLES20.glGenTextures(1, textureHandle, 0);
//
//                if (textureHandle[0] != 0)
//                {
//                    final BitmapFactory.Options options = new BitmapFactory.Options();
//                    options.inScaled = false;   // No pre-scaling
//
//                    // Read in the resource
//                    final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
//
//                    // Bind to the texture in OpenGL
//                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
//
//                    // Set filtering
//                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
//
//                    // Load the bitmap into the bound texture.
//                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//
//                    // Recycle the bitmap, since its data has been loaded into OpenGL.
//                    bitmap.recycle();
//                }
//
//                if (textureHandle[0] == 0)
//                {
//                    throw new RuntimeException("Error loading texture.");
//                }
//
//                return textureHandle[0];
//            }
        }

        class Environment {
            Environment() {
                golfBall = new GolfBall(getContext(), false);
                //for 2d golf ball
                if (TWO_D) {
//                    golfBall.setBackgroundResource(R.drawable.golfball);
//                    golfBall.setLayerType(LAYER_TYPE_HARDWARE, null);
                    golfBall.setRenderer(new GolfBall(getContext(), false));
                } else {
                    golfBall.setRenderer(new OpenGLRenderer4());
                }
                addView(golfBall, new ViewGroup.LayoutParams(dstWidth, dstHeight));

                obstacle = new Obstacle(getContext());
                obstacle.setRenderer(new Obstacle(getContext()));
                addView(obstacle, new ViewGroup.LayoutParams(dstWidth * 2, dstHeight * 2));

            }

            private void updatePositions(float sx, float sy, long timestamp) {
                final long t = timestamp;
                if (lastT != 0) {
                    final float dT = (float) (t - lastT) / 1000.f;
                    GolfBall ball = golfBall;
                    ball.computePhysics(sx, sy, dT);
                    obstacle.computePhysics();
                }
                lastT = t;
            }

            public void update(float sx, float sy, long now) {
                // update the system's positions
                updatePositions(sx, sy, now);
                obstacle.resolveCollisionWithBounds();
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
        public void draw(Canvas canvas) {
            final int width = this.getWidth() / 2;
            final int height = this.getHeight() / 2;

            super.draw(canvas);
            //left
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rocks)
                    , null, new RectF(0, 0
                            , 30, height * 2), null);
            //right
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rocks)
                    , null, new RectF(width * 2 - 30, 0
                            , width * 2, height * 2), null);
            //top
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rocks)
                    , null, new RectF(0, 0
                            , width * 2, 30), null);
            //bottom
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rocks)
                    , null, new RectF(0, height * 2 - 30
                            , width * 2, height * 2), null);
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

//            canvas.drawRect(width * 0.65f, height * 0.6f
//                    , width * 1.35f, height * 1.4f
//                    , paint);
//            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rocks)
//                    , null, new RectF(width * 0.65f, height * 0.6f
//                            , width * 1.35f, height * 1.4f), null);
//

            final float xc = originX;
            final float yc = originY;
            final float xs = metersToPixelsX;
            final float ys = metersToPixelsY;

            //canvas.drawText(Helpers.GetOrientation(yOrientation, zOrientation), xc, 600, paintOrientation);
            canvas.drawText("score: " + score, 300, 30, paint);
            float targetXTranslated = originX + (originX * targetX / horizontalBoundary);
            float targetYTranslated = originY - (originY * targetY / verticalBoundary);
            canvas.drawCircle(targetXTranslated, targetYTranslated, 50, paint);

            if (DEBUG) {
                canvas.drawText("ballX: " + golfBall.positionX, 0, 15, paint);
                canvas.drawText("ballY: " + golfBall.positionY, 0, 40, paint);
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
            golfBall.setTranslationX(x);
            golfBall.setTranslationY(y);

            obstacle.setTranslationX(xc + obstacle.positionX * xs);
            obstacle.setTranslationY(yc + obstacle.positionY * ys);

            invalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private float CalculateCoordsOnScreen(float position, boolean isX) {
            if (isX) return (position) / metersToPixelsX;
            else return (position) / metersToPixelsY;
        }
    }
}
