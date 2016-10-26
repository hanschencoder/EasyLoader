package com.hanschen.easyloader.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.hanschen.easyloader.example.adapter.ImageAdapter;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private String[] picture = new String[]{"http://192.168.60.251/1.jpg",
            "http://192.168.60.251/2.jpg",
            "http://192.168.60.251/3.jpg",
            "http://192.168.60.251/4.jpg",
            "http://192.168.60.251/5.jpg",
            "http://192.168.60.251/6.jpg",
            "http://192.168.60.251/7.jpg",
            "http://192.168.60.251/8.jpg",
            "http://192.168.60.251/9.jpg",
            "http://192.168.60.251/10.jpg",
            "http://192.168.60.251/11.jpg",
            "http://192.168.60.251/12.jpg",
            "http://192.168.60.251/13.jpg",
            "http://192.168.60.251/14.jpg",
            "http://192.168.60.251/15.jpg",};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupRecycleView();
    }

    private void setupRecycleView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.image_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ImageAdapter(MainActivity.this, Arrays.asList(picture)));
    }
}
