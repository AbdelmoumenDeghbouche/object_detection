package com.esidev.camerascanner;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

public class RobotHeyActivity extends AppCompatActivity {
    private LottieAnimationView robot_hello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_robot_hey);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        robot_hello = (LottieAnimationView) findViewById(R.id.robot_hello);
        changeBarsColors();
        robot_hello.setRepeatCount(0); // Play the animation once
        robot_hello.playAnimation();

        // Control animation programmatically
        robot_hello.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Code to run when animation starts
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(RobotHeyActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: close the current activity
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Code to run when animation is cancelled
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Code to run when animation repeats
            }
        });
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