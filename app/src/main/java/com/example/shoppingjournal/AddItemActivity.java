package com.example.shoppingjournal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class AddItemActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        EditText etName = findViewById(R.id.etName);
        EditText etPrice = findViewById(R.id.etPrice);
        Button btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String price = etPrice.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Lūdzu, ievadi preces nosaukumu", Toast.LENGTH_SHORT).show();
                etName.requestFocus();
                return;
            }

            if (price.isEmpty()) {
                Toast.makeText(this, "Lūdzu, ievadi cenu", Toast.LENGTH_SHORT).show();
                etPrice.requestFocus();
                return;
            }

            Intent data = new Intent();
            data.putExtra("name", name);
            data.putExtra("price", price);
            setResult(RESULT_OK, data);
            finish();
        });

    }
}
