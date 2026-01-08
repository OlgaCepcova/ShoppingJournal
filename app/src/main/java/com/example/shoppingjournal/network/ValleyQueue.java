package com.example.shoppingjournal.network;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ValleyQueue {
    private static ValleyQueue instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private ValleyQueue(Context context) {
        ctx = context.getApplicationContext();
        requestQueue = getRequestQueue();
    }

    public static synchronized ValleyQueue getInstance(Context context) {
        if (instance == null) {
            instance = new ValleyQueue(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }
}

