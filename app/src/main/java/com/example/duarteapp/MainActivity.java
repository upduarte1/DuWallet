package com.example.duarteapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String savedUsername = getSharedPreferences("DuarteAppPrefs", MODE_PRIVATE)
                .getString("username", null);

        if (savedUsername != null) {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);

        firestore = FirebaseFirestore.getInstance();

        EditText usernameEditText = findViewById(R.id.usernameEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        Button signupButton = findViewById(R.id.signupButton);

        // Sign Up
        signupButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar se o usuário já existe
            firestore.collection("users")
                    .document(username)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(MainActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            // Armazenar o usuário com senha criptografada
                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);
                            user.put("password", password); // Substituir com um hash em produção

                            firestore.collection("users")
                                    .document(username)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(MainActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                        usernameEditText.setText("");
                                        passwordEditText.setText("");
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to register user", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error checking username", Toast.LENGTH_SHORT).show());
        });

        // Login
        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            firestore.collection("users")
                    .document(username)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedPassword = documentSnapshot.getString("password");
                            if (password.equals(storedPassword)) {
                                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                getSharedPreferences("DuarteAppPrefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("username", username)
                                        .apply();
                                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                                intent.putExtra("username", username);
                                startActivity(intent);
                                finish(); // Fecha a tela de login
                            } else {
                                Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to login", Toast.LENGTH_SHORT).show());
        });
    }
}
