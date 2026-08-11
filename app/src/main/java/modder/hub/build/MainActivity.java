package modder.hub.build;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;

import modder.hub.dexeditor.views.FastScrollerRecyclerView;
import modder.hub.fastscroller.FastScrollerListView;

public class MainActivity extends Activity {

    private FastScrollerListView listView;
    private FastScrollerRecyclerView recyclerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ListView
        listView = findViewById(R.id.listView);

        listView.setFastScrollEnabled(true);
        listView.setTransparentTrackBackground(false);

        // Пример данных для ListView
        String[] items = new String[100];

        for (int i = 0; i < items.length; i++) {
            items[i] = "Item " + (i + 1);
        }

        ArrayAdapter<String> listAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        items
                );

        listView.setAdapter(listAdapter);


        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerView.setTrackVisible(true);

        // Здесь устанавливается твой RecyclerView Adapter.
        // Например:
        //
        // recyclerView.setAdapter(new MyAdapter(...));
    }
}