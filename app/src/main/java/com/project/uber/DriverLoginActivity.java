package com.project.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DriverLoginActivity extends AppCompatActivity {
    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;

    private FirebaseAuth mAuth;
    // измнение состояния авторизация
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user =  firebaseAuth.getCurrentUser(); // состояние об авторизации пользователя
                if(user!=null) {
                    signInLikeCustomer();
                }
            }
        };

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);

        mLogin = (Button) findViewById(R.id.login);
        mRegistration = (Button) findViewById(R.id.registration);

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            // Проверка в случае ошибки
                            // Objects.requireNonNull(task.getException()).printStackTrace();
                            Toast.makeText(DriverLoginActivity.this, "Sing up error", Toast.LENGTH_SHORT).show();
                        } else {
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("name");
                            current_user_db.setValue(email); // сохраняем пользователя (водителя) в базу
                        }
                    }
                });
            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                if(!email.isEmpty() && !password.isEmpty()) {
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()) {
                                // Objects.requireNonNull(task.getException()).printStackTrace();
                                Toast.makeText(DriverLoginActivity.this, "Sing in failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(DriverLoginActivity.this, "Поля логин и пароль должны быть заполенены!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void signInLikeCustomer() {
        FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String userId = mAuth.getCurrentUser().getUid();
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                    Map<String, String> customerIds = new HashMap<>();
                    for (DataSnapshot currentId : dataSnapshot.getChildren()) {
                        customerIds.put(currentId.getKey(), "key");
                    }
                    if(customerIds.get(userId) != null) {
                        Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    mAuth.signOut();
                    Toast.makeText(DriverLoginActivity.this, "Данный пользователь не является водителем. Зарегестрируйтесь как клиент", Toast.LENGTH_SHORT).show();
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
        //  добавляем слушатель авторизации
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // удляем слушатель авторизации
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
