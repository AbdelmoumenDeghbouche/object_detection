package com.esidev.camerascanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.esidev.camerascanner.R;
import com.esidev.camerascanner.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.TensorFlowLite;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    List<String> labels;
    List<Integer> colors = List.of(
            Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
            Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED);
    Paint paint = new Paint();
    ImageProcessor imageProcessor;
    Bitmap bitmap;
    ImageView imageView;
    CameraDevice cameraDevice;
    Handler handler;
    CameraManager cameraManager;
    TextureView textureView;
    Model model;
    TextToSpeech tts;
    Queue<String> labelQueue = new LinkedList<>();
    String lastSpokenLabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeBarsColors();
        get_permission();

        tts = new TextToSpeech(this, this);

        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize ImageProcessor
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        try {
            model = Model.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HandlerThread handlerThread = new HandlerThread("videoThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        imageView = findViewById(R.id.imageView);
        textureView = findViewById(R.id.textureView);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                open_camera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                bitmap = textureView.getBitmap();
                TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
                tensorImage = imageProcessor.process(tensorImage);

                ByteBuffer byteBuffer = tensorImage.getBuffer();

                try {
                    Model model = Model.newInstance(getApplicationContext());

                    // Create input tensor buffer
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 320, 320, 3}, DataType.UINT8);
                    inputFeature0.loadBuffer(byteBuffer);

                    // Run model inference and get result
                    Model.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                    TensorBuffer outputFeature1 = outputs.getOutputFeature1AsTensorBuffer();
                    TensorBuffer outputFeature2 = outputs.getOutputFeature2AsTensorBuffer();
                    TensorBuffer outputFeature3 = outputs.getOutputFeature3AsTensorBuffer();


                    // Get the results from the model
                    float[] locations = outputFeature0.getFloatArray();
                    float[] classes = outputFeature1.getFloatArray();
                    float[] scores = outputFeature2.getFloatArray();
                    float[] numberOfDetections = outputFeature3.getFloatArray();

                    // Create a mutable copy of the bitmap to draw on
                    Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutable);

                    int h = mutable.getHeight();
                    int w = mutable.getWidth();
                    paint.setTextSize(h / 15f);
                    paint.setStrokeWidth(h / 85f);

                    int x;
                    for (int i = 0; i < scores.length; i++) {
                        float score = scores[i];
                        x = i * 4;
                        if (score > 0.59) {
                            String label = labels.get((int) classes[i]);
                            if (label.equals("chair") || label.equals("couch") || label.equals("dining table") || label.equals("cup") || label.equals("wine glass")) {
                                if (label.equals("dining table")) {
                                    label = "table";
                                }

                                if (label.equals("wine glass")) {
                                    label = "glass";
                                }
                                paint.setColor(colors.get(i % colors.size())); // Avoid index out of bounds
                                paint.setStyle(Paint.Style.STROKE);
                                canvas.drawRect(new RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint);
                                paint.setStyle(Paint.Style.FILL);
                                canvas.drawText(label + " ", locations[x + 1] * w, locations[x] * h, paint);

                                if (!labelQueue.contains(label) && !label.equals(lastSpokenLabel)) {
                                    labelQueue.add(label);
                                }
                            }
                        }
                    }

                    imageView.setImageBitmap(mutable);
                    speakNextLabel();

                    // Releases model resources if no longer used
                    model.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (model != null) {
            model.close();
        }
    }

    @SuppressLint("MissingPermission")
    private void open_camera() {
        try {
            cameraManager.openCamera(cameraManager.getCameraIdList()[0], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;

                    SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                    Surface surface = new Surface(surfaceTexture);

                    try {
                        final CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(surface);

                        camera.createCaptureSession(List.of(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                try {
                                    CaptureRequest captureRequest = captureRequestBuilder.build();
                                    session.setRepeatingRequest(captureRequest, null, handler);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, handler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                }
            }, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void get_permission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle the error
            } else {
                // Set a high-quality voice
                Set<Voice> voices = tts.getVoices();
                for (Voice v : voices) {
                    if (v.getQuality() == Voice.QUALITY_VERY_HIGH) {
                        tts.setVoice(v);
                        break;
                    }
                }
            }
        } else {
            // Initialization failed
        }
    }

    private void speakNextLabel() {
        if (!tts.isSpeaking() && !labelQueue.isEmpty()) {
            String label = labelQueue.poll();

            lastSpokenLabel = label;
            tts.speak("There is a " + label + " in front of you", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void changeBarsColors() {
        // Change status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#010101"));
        }

        // Change navigation bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#010101"));
        }
    }
}
