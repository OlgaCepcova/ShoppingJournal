package com.example.shoppingjournal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoppingjournal.data.AppDatabase;
import com.example.shoppingjournal.data.ShoppingItem;
import com.example.shoppingjournal.data.ShoppingItemDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private ListView lvItems;
    private TextView tvTotal;
    private TextView tvApiResponse;
    private Button btnSendApi;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> uiItems = new ArrayList<>();

    private AppDatabase db;
    private ShoppingItemDao dao;

    private final ActivityResultLauncher<Intent> addItemLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                    String name = result.getData().getStringExtra("name");
                    String priceStr = result.getData().getStringExtra("price");

                    if (name == null) name = "";
                    if (priceStr == null) priceStr = "";

                    String cleanName = name.trim();
                    String cleanPrice = priceStr.trim().replace(",", ".");

                    if (cleanName.isEmpty() || cleanPrice.isEmpty()) {
                        return; // AddItemActivity jau vajadzētu validēt, bet drošībai
                    }

                    double price;
                    try {
                        price = Double.parseDouble(cleanPrice);
                    } catch (Exception e) {
                        return;
                    }

                    String finalName = cleanName;

                    new Thread(() -> {
                        dao.insert(new ShoppingItem(finalName, price));
                        runOnUiThread(this::loadFromDb);
                    }).start();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvItems = findViewById(R.id.lvItems);
        tvTotal = findViewById(R.id.tvTotal);
        Button btnAdd = findViewById(R.id.btnAdd);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, uiItems);
        lvItems.setAdapter(adapter);

        db = AppDatabase.getInstance(this);
        dao = db.shoppingItemDao();

        // --- API UI ---
        TextView tvApiResponse = findViewById(R.id.tvApiResponse);
        Button btnSendApi = findViewById(R.id.btnSendApi);

// --- Volley queue ---
        com.android.volley.RequestQueue requestQueue =
                com.example.shoppingjournal.network.ValleyQueue
                        .getInstance(getApplicationContext())
                        .getRequestQueue();

        String url = "https://postman-echo.com/post"; // echo API

        btnSendApi.setOnClickListener(v -> {
            tvApiResponse.setText("API atbilde: sūtu...");

            com.android.volley.toolbox.StringRequest postRequest =
                    new com.android.volley.toolbox.StringRequest(
                            com.android.volley.Request.Method.POST,
                            url,
                            response -> {
                                try {
                                    org.json.JSONObject obj = new org.json.JSONObject(response);
                                    org.json.JSONObject json = obj.getJSONObject("json");

                                    String total = json.optString("total", "");
                                    String itemsCount = json.optString("itemsCount", "");

                                    tvApiResponse.setText("API: OK ✅\n" +
                                            "Nosūtīts: " + total + "\n" +
                                            "Ierakstu skaits: " + itemsCount);
                                } catch (Exception e) {
                                    tvApiResponse.setText("API atbilde (raw):\n" + response);
                                }
                            },
                            error -> tvApiResponse.setText("API kļūda:\n" + error.toString())
                    ) {
                        @Override
                        protected java.util.Map<String, String> getParams() {
                            java.util.Map<String, String> params = new java.util.HashMap<>();
                            params.put("total", tvTotal.getText().toString());
                            params.put("itemsCount", String.valueOf(uiItems.size()));
                            return params;
                        }
                    };

            requestQueue.add(postRequest);
        });


        btnAdd.setOnClickListener(v -> addItemLauncher.launch(new Intent(this, AddItemActivity.class)));

        loadFromDb();
    }

    private void loadFromDb() {
        new Thread(() -> {
            List<ShoppingItem> items = dao.getAll();
            Double total = dao.getTotal();
            if (total == null) total = 0.0;

            ArrayList<String> newUi = new ArrayList<>();
            for (ShoppingItem it : items) {
                newUi.add(it.name + " — " + String.format(Locale.US, "%.2f", it.price) + " €");
            }

            double finalTotal = total;

            runOnUiThread(() -> {
                uiItems.clear();
                uiItems.addAll(newUi);
                adapter.notifyDataSetChanged();
                tvTotal.setText("Kopā: " + String.format(Locale.US, "%.2f", finalTotal) + " €");
            });
        }).start();
    }
}


