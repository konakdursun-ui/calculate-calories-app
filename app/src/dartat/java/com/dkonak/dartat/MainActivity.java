package com.dkonak.dartat;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new PinGameView(this, callback -> {
            Toast.makeText(this, "Devam hakki verildi.", Toast.LENGTH_SHORT).show();
            callback.onRewardEarned();
        }));
    }
}
