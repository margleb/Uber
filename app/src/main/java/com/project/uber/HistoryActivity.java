package com.project.uber;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.project.uber.historyRecyclerView.HistoryAdapter;
import com.project.uber.historyRecyclerView.HistoryObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView mHistoryRecycleView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mHistoryRecycleView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        mHistoryRecycleView.setNestedScrollingEnabled(false);
        mHistoryRecycleView.setHasFixedSize(true);

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecycleView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this);
        mHistoryRecycleView.setAdapter(mHistoryAdapter);

        for(int i=0; i< 100;i++) {
            HistoryObject obj = new HistoryObject(Integer.toString(i));
            resultHistory.add(obj);
        }
        mHistoryAdapter.notifyDataSetChanged();
    }

    private ArrayList resultHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultHistory;
    };
}
