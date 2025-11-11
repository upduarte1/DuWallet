package com.example.duarteapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Inicializar Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Obter username do Intent
        String username = getSharedPreferences("DuarteAppPrefs", MODE_PRIVATE)
                .getString("username", null);


        // Verificar se o username foi recebido corretamente
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Invalid session. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return; // Interrompe o fluxo caso o username não seja válido
        }

        // Referências aos componentes do layout
        TextView welcomeText = findViewById(R.id.welcomeText);
        LinearLayout addItemLayout = findViewById(R.id.addItemLayout);
        LinearLayout calendarLayout = findViewById(R.id.calendarLayout);
        LinearLayout monthlySummaryLayout = findViewById(R.id.monthlySummaryLayout);
        LinearLayout categoryExpensesLayout = findViewById(R.id.categoryExpensesLayout);

        LinearLayout categoryIncomingsLayout = findViewById(R.id.categoryIncomingsLayout);
        LinearLayout logoutLayout = findViewById(R.id.logoutLayout);

        // Configurar saudação dinâmica
        welcomeText.setText(String.format("Welcome, %s", username));

        // Configurar botões
        addItemLayout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddItemActivity.class);
            intent.putExtra("username", username); // Passar username para a próxima atividade
            startActivity(intent);
        });

        calendarLayout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CalendarActivity.class);
            intent.putExtra("username", username); // Passar username para a próxima atividade
            startActivity(intent);
        });

        monthlySummaryLayout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MonthlySummaryActivity.class);
            intent.putExtra("username", username); // Passar username para a próxima atividade
            startActivity(intent);
        });

        categoryExpensesLayout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ExpensesByCategoryActivity.class);
            intent.putExtra("username", username); // Passar username para a próxima atividade
            startActivity(intent);
        });

        categoryIncomingsLayout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, IncomingsByCategoryActivity.class);
            intent.putExtra("username", username); // Passar username para a próxima atividade
            startActivity(intent);
        });

        logoutLayout.setOnClickListener(v -> {
            getSharedPreferences("DuarteAppPrefs", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Toast.makeText(DashboardActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        });
    }
}
