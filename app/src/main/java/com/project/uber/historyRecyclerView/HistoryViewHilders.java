package com.project.uber.historyRecyclerView;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.uber.R;

public class HistoryViewHilders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView rideId;

    public HistoryViewHilders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        rideId = (TextView) itemView.findViewById(R.id.rideId);
    }

    @Override
    public void onClick(View v) {

    }
}
