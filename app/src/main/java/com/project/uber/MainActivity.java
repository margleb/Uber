package com.project.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private Button mDriver, mCustomer;
    private String currentUserIs;

    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mDriver = (Button) findViewById(R.id.driver);
        mCustomer = (Button) findViewById(R.id.customer);

        // при завершении приложения удаляет AvailableDrivers из базы
        startService(new Intent(MainActivity.this, onAppKilled.class));

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener =  new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user =  firebaseAuth.getCurrentUser(); // состояние об авторизации пользователя
                if(user!=null) {
                    checkCustomerOrDrivers();
                    if(currentUserIs == "Customers") {
                        Intent intent = new Intent(MainActivity.this, CustomerMapActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(MainActivity.this, DriverMapActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        };

        mDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DriverLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CustomerLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }

    public void checkCustomerOrDrivers() {
        final String userId =  FirebaseAuth.getInstance().getCurrentUser().getUid(); // состояние об авторизации пользователя
        FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot user: dataSnapshot.getChildren()) {
                    for(DataSnapshot currentId : user.getChildren()) {
                        if(currentId.getKey().equals(userId)) {
                            currentUserIs = user.getKey();
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //  добавляем слушательавторизации
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // удляем слушатель авторизации
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
