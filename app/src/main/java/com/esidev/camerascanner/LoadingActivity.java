package com.esidev.camerascanner;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

public class LoadingActivity extends AppCompatActivity {
    private LottieAnimationView loading_bar_lottie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        changeBarsColors();
        loading_bar_lottie = findViewById(R.id.loading_bar_lotie);
        loading_bar_lottie.setRepeatCount(0); // Play the animation once
        loading_bar_lottie.playAnimation();

        // Control animation programmatically
        loading_bar_lottie.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Code to run when animation starts
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(LoadingActivity.this, RobotHeyActivity.class);
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
