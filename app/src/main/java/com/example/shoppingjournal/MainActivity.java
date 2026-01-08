package com.example.shoppingjournal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.shoppingjournal.data.AppDatabase;
import com.example.shoppingjournal.data.ShoppingItem;
import com.example.shoppingjournal.data.ShoppingItemDao;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private ListView lvItems;
    private TextView tvTotal;
    private TextView tvApiResponse;
    private Button btnSendApi;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> uiItems = new ArrayList<>();

    private AppDatabase db;
    private ShoppingItemDao dao;

    private Uri pendingPhotoUri = null;
    private ImageView imgLastPhoto;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean allGranted = true;
                        for (Boolean granted : result.values()) {
                            if (!Boolean.TRUE.equals(granted)) { allGranted = false; break; }
                        }
                        if (allGranted) {
                            startFullSizeCapture();
                        } else {
                            Toast.makeText(this, "Atļaujas noraidītas.", Toast.LENGTH_LONG).show();
                        }
                    }
            );

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (Boolean.TRUE.equals(success)) {
                            Toast.makeText(this, "Foto saglabāts!", Toast.LENGTH_SHORT).show();
                            // parādam bildi UI
                            imgLastPhoto.setImageURI(pendingPhotoUri);
                        } else {
                            // ja atcelts, izdzēšam tukšo ierakstu (īpaši svarīgi API 29+)
                            if (pendingPhotoUri != null) {
                                getContentResolver().delete(pendingPhotoUri, null, null);
                            }
                            Toast.makeText(this, "Uzņemšana atcelta.", Toast.LENGTH_SHORT).show();
                        }
                        pendingPhotoUri = null;
                    }
            );


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
        tvApiResponse = findViewById(R.id.tvApiResponse);
        btnSendApi = findViewById(R.id.btnSendApi);

        imgLastPhoto = findViewById(R.id.imgLastPhoto);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);

        btnTakePhoto.setOnClickListener(v -> ensureCameraAndSave());



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

    private boolean areAllGranted(String[] perms) {
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void ensureCameraAndSave() {
        String[] required = com.example.shoppingjournal.PermissionHelper.forCameraAndSaveToGallery();
        if (areAllGranted(required)) {
            startFullSizeCapture();
        } else {
            permissionLauncher.launch(required);
        }
    }

    private void startFullSizeCapture() {
        pendingPhotoUri = createDestinationUri();
        if (pendingPhotoUri == null) {
            Toast.makeText(this, "Neizdevās izveidot vietu attēlam.", Toast.LENGTH_LONG).show();
            return;
        }
        takePictureLauncher.launch(pendingPhotoUri);
    }

    private Uri createDestinationUri() {
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "IMG_" + time + ".jpg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+) - saglabājas galerijā caur MediaStore
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + "ShoppingJournal");
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

            return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            // Android 9 un zemāk (API <= 28) - fails Pictures/ShoppingJournal + FileProvider
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDir = new File(picturesDir, "ShoppingJournal");
            if (!appDir.exists() && !appDir.mkdirs()) return null;

            File file = new File(appDir, fileName);
            return FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    file);
        }
    }

}


