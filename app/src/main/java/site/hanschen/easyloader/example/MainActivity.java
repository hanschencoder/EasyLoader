package site.hanschen.easyloader.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;

import java.util.Arrays;

import site.hanschen.easyloader.example.adapter.ImageAdapter;

public class MainActivity extends AppCompatActivity {

    private String[] picture = new String[]{
            "http://192.168.60.251/1.jpg",
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

    private String[] networkPictures = new String[]{
            "http://a.hiphotos.baidu.com/image/pic/item/e61190ef76c6a7efce7315cef9faaf51f2de6684.jpg",
            "http://e.hiphotos.baidu.com/image/pic/item/472309f79052982266172a03d3ca7bcb0b46d4cd.jpg",
            "http://e.hiphotos.baidu.com/image/pic/item/9f2f070828381f30e55cb18eac014c086e06f04b.jpg",
            "http://c.hiphotos.baidu.com/image/pic/item/8718367adab44aed2cab83c3b61c8701a18bfb2b.jpg",
            "http://b.hiphotos.baidu.com/image/pic/item/d043ad4bd11373f0639a52b8a10f4bfbfbed046b.jpg",
            "http://g.hiphotos.baidu.com/image/pic/item/0df3d7ca7bcb0a46c4c6b61e6e63f6246b60af34.jpg",
            "http://b.hiphotos.baidu.com/image/pic/item/b7fd5266d0160924421866dbd00735fae6cd3435.jpg",
            "http://c.hiphotos.baidu.com/image/pic/item/91ef76c6a7efce1bf7d3e22cab51f3deb58f655f.jpg",
            "http://a.hiphotos.baidu.com/image/pic/item/11385343fbf2b211d35090a8ce8065380dd78edd.jpg",
            "http://d.hiphotos.baidu.com/image/pic/item/9f2f070828381f305a5bdb76ad014c086f06f0ab.jpg",
            "http://g.hiphotos.baidu.com/image/pic/item/d439b6003af33a8787e2df2ec35c10385343b573.jpg",
            "http://f.hiphotos.baidu.com/image/pic/item/aa18972bd40735fa4bc04a549a510fb30f24082e.jpg",
            "http://b.hiphotos.baidu.com/image/pic/item/32fa828ba61ea8d33cc8ace8930a304e241f5870.jpg",
            "http://h.hiphotos.baidu.com/image/pic/item/9f510fb30f2442a72eaa2549d543ad4bd01302b2.jpg",
            "http://f.hiphotos.baidu.com/image/pic/item/359b033b5bb5c9ea8898800fd139b6003bf3b3f7.jpg",
            "http://a.hiphotos.baidu.com/image/pic/item/d53f8794a4c27d1ecad6b8281fd5ad6edcc4386d.jpg    ",
            "http://c.hiphotos.baidu.com/image/pic/item/cdbf6c81800a19d886f0dfda37fa828ba71e4679.jpg",
            "http://f.hiphotos.baidu.com/image/pic/item/91529822720e0cf3c2c75c120e46f21fbe09aa22.jpg",
            "http://e.hiphotos.baidu.com/image/pic/item/3c6d55fbb2fb4316613c689324a4462309f7d333.jpg",
            "http://a.hiphotos.baidu.com/image/pic/item/dcc451da81cb39db067174bfd4160924aa1830a1.jpg",
            "http://h.hiphotos.baidu.com/image/pic/item/359b033b5bb5c9eaea606234d139b6003bf3b352.jpg",
            "http://g.hiphotos.baidu.com/image/pic/item/810a19d8bc3eb1356555d66ca21ea8d3fc1f4452.jpg",
            "http://c.hiphotos.baidu.com/image/pic/item/cb8065380cd79123856df5a3a9345982b3b78053.jpg    ",
            "http://c.hiphotos.baidu.com/image/pic/item/cb8065380cd79123856df5a3a9345982b3b78053.jpg",
            "http://a.hiphotos.baidu.com/image/pic/item/9825bc315c6034a8d6b99d40cf13495408237693.jpg",
            "http://g.hiphotos.baidu.com/image/pic/item/5fdf8db1cb13495477a9c0cc534e9258d0094ae6.jpg",
            "http://b.hiphotos.baidu.com/image/pic/item/377adab44aed2e73eaa818ff8301a18b86d6faa4.jpg",
            "http://e.hiphotos.baidu.com/image/pic/item/b3119313b07eca8031e302d2942397dda1448324.jpg"};

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
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(new ImageAdapter(MainActivity.this, Arrays.asList(networkPictures)));
    }
}
