package com.example.shoppingjournal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvLastAdded;
    private TextView tvTotal;

    private final ActivityResultLauncher<Intent> addItemLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String name = result.getData().getStringExtra("name");
                    String price = result.getData().getStringExtra("price");

                    if (name == null) name = "";
                    if (price == null) price = "";

                    tvLastAdded.setText("Pēdējais pievienotais: " + name + " (" + price + " €)");

                    // pagaidām tikai parādam tekstu, DB pieslēgsim nākamajā solī
                    tvTotal.setText("Kopā: (DB vēl nav pieslēgta)");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAdd = findViewById(R.id.btnAdd);
        tvLastAdded = findViewById(R.id.tvLastAdded);
        tvTotal = findViewById(R.id.tvTotal);

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
            addItemLauncher.launch(intent);
        });

        tvTotal.setText("Kopā: (DB vēl nav pieslēgta)");
    }
}

