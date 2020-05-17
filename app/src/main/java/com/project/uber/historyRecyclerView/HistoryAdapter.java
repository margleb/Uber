package com.project.uber.historyRecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.uber.R;

import java.util.ArrayList;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolders> {
    private ArrayList<HistoryObject> itemList;
    private Context context;

    public HistoryAdapter(ArrayList<HistoryObject> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolders onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolders rcv = new HistoryViewHolders(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolders holder, int position) {
        holder.rideId.setText(itemList.get(position).getRideId());
        holder.time.setText(itemList.get(position).getTime());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
