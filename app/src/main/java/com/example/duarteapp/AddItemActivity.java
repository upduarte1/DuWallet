package com.example.duarteapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    private String username;
    private RadioGroup typeRadioGroup;
    private EditText amountEditText;
    private Button dateButton;
    private Spinner categorySpinner;
    private EditText descriptionEditText;
    private String selectedDate;
    private FirebaseFirestore firestore;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    btnBack.setColorFilter(Color.GRAY); // Muda para cinza quando pressionada
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    btnBack.setColorFilter(Color.WHITE); // Volta ao branco quando solta
                    break;
            }
            return false;
        });
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddItemActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        username = getIntent().getStringExtra("username");
        firestore = FirebaseFirestore.getInstance();

        typeRadioGroup = findViewById(R.id.typeRadioGroup);
        amountEditText = findViewById(R.id.amountEditText);
        dateButton = findViewById(R.id.dateButton);
        categorySpinner = findViewById(R.id.categorySpinner);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        Button saveButton = findViewById(R.id.saveButton);

        dateButton.setOnClickListener(v -> showDatePicker());
        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateCategorySpinner(checkedId));
        saveButton.setOnClickListener(v -> {
            saveItem();
            Intent intent = new Intent(AddItemActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            dateButton.setText(selectedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void updateCategorySpinner(int checkedId) {
        String[] categories;
        if (checkedId == R.id.radioExpense) {
            categories = new String[]{"Refeição", "Propina", "Supermercado", "Cafés", "Viagens", "Farmácia", "Gota", "Outros"};
        } else {
            categories = new String[]{"Bolsa", "Trabalhos", "Extras", "Betano"};
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);
    }

    private void saveItem() {
        int selectedTypeId = typeRadioGroup.getCheckedRadioButtonId();
        if (selectedTypeId == -1 || selectedDate == null || amountEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }
        String type = (selectedTypeId == R.id.radioExpense) ? "Gasto" : "Ganho";
        double amount = Double.parseDouble(amountEditText.getText().toString());
        String category = categorySpinner.getSelectedItem().toString();
        String description = descriptionEditText.getText().toString();

        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("amount", amount);
        item.put("date", selectedDate);
        item.put("category", category);
        item.put("description", description);

        firestore.collection("users")
                .document(username) // Documento do usuário
                .collection("items") // Subcoleção de itens
                .add(item)
                .addOnSuccessListener(documentReference -> Toast.makeText(this, "Item salvo com sucesso", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao salvar item", Toast.LENGTH_SHORT).show());
    }
}