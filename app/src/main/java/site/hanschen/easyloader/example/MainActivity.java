package site.hanschen.easyloader.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import java.util.Arrays;

import site.hanschen.easyloader.example.adapter.ImageAdapter;

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
            "http://192.168.60.251/15.jpg",
            "http://192.168.60.251/16.jpg",
            "http://192.168.60.251/17.jpg",
            "http://192.168.60.251/18.jpg",
            "http://192.168.60.251/19.jpg",
            "http://192.168.60.251/20.jpg",
            "http://192.168.60.251/21.jpg",
            "http://192.168.60.251/22.jpg",};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(site.hanschen.easyloader.example.R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(site.hanschen.easyloader.example.R.id.toolbar);
        setSupportActionBar(toolbar);
        setupRecycleView();
    }

    private void setupRecycleView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(site.hanschen.easyloader.example.R.id.image_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ImageAdapter(MainActivity.this, Arrays.asList(picture)));
    }
}
