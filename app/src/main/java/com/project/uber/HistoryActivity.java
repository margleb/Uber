package com.project.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.uber.historyRecyclerView.HistoryAdapter;
import com.project.uber.historyRecyclerView.HistoryObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView mHistoryRecycleView;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private String customerOrDriver, userId;
    private TextView mbalance;
    private Double balance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mbalance = findViewById(R.id.balance);

        mHistoryRecycleView = (findViewById(R.id.historyRecyclerView));
        mHistoryRecycleView.setNestedScrollingEnabled(false); // плавный скролл
        mHistoryRecycleView.setHasFixedSize(true); // дочерние элементы имеют фиксированную высоту и ширину

        mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecycleView.setLayoutManager(mHistoryLayoutManager); // линейный слой
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(), HistoryActivity.this); // адаптер recycle view
        mHistoryRecycleView.setAdapter(mHistoryAdapter);

        customerOrDriver = getIntent().getExtras().getString("customerOrDriver"); // водитель или клиент

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        getUserHistoryIds();

        if(customerOrDriver.equals("Drivers")) {
            mbalance.setVisibility(View.VISIBLE);
        }

    }

    private void getUserHistoryIds() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(userId).child("history");
        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    for(DataSnapshot history: dataSnapshot.getChildren()) {
                        fetchRideInformation(history.getKey());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void fetchRideInformation(String ridekey) {
        DatabaseReference historyDatabase = FirebaseDatabase.getInstance().getReference().child("history").child(ridekey);
        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    
                    String rideId = dataSnapshot.getKey();
                    Long timestamp = 0L;
                    String distance = "";
                    Double ridePrice = 0.0;

                        if(dataSnapshot.child("timestamp").getValue() != null) {
                            timestamp = Long.valueOf(dataSnapshot.child("timestamp").getValue().toString());
                        }

                        // высчитывается текущий баланс водителя
                        if(dataSnapshot.child("customerPaid").getValue() != null && dataSnapshot.child("driverPaidOut").getValue() == null) {
                            if(dataSnapshot.child("distance").getValue() != null) {
                                 distance = dataSnapshot.child("distance").getValue().toString();
                                 ridePrice = (Double.valueOf(distance) * 0.4);
                                 balance += ridePrice;
                                 mbalance.setText("balance: " + balance + " $");
                            }
                        }

                    HistoryObject obj = new HistoryObject(rideId, getDate(timestamp));
                    resultHistory.add(obj);
                    // уведомляет адаптер об изменениях и подгружает новый список
                    mHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HistoryActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp*10000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();
        return date;
    }

    private ArrayList resultHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultHistory;
    };
}
