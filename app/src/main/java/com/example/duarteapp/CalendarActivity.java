package com.example.duarteapp;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TableLayout itemsTable;
    private FirebaseFirestore firestore;
    private String username;
    private String selectedDate;

    @SuppressLint({"ClickableViewAccessibility", "ResourceType", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView selectedDateText = findViewById(R.id.selectedDateText);

        btnBack.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    btnBack.setColorFilter(Color.GRAY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    btnBack.setColorFilter(Color.WHITE);
                    break;
            }
            return false;
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        calendarView = findViewById(R.id.calendarView);
        itemsTable = findViewById(R.id.itemsTable);
        firestore = FirebaseFirestore.getInstance();
        username = getIntent().getStringExtra("username");

        calendarView.post(this::markEventDays);

        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clickedDay = eventDay.getCalendar();
            int year = clickedDay.get(Calendar.YEAR);
            int month = clickedDay.get(Calendar.MONTH) + 1;
            int day = clickedDay.get(Calendar.DAY_OF_MONTH);

            selectedDate = day + "/" + month + "/" + year;
            selectedDateText.setText("Data selecionada: " + selectedDate);

            Log.d("CalendarActivity", "Data selecionada: " + selectedDate);
            fetchItemsForDate(selectedDate);
        });
    }

    private void fetchItemsForDate(String date) {
        Log.d("CalendarActivity", "Buscando itens para a data: " + date);
        firestore.collection("users")
                .document(username)
                .collection("items")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        items.add(document.getData());
                    }

                    Log.d("CalendarActivity", "Itens encontrados: " + items.size());
                    displayItems(date, items);
                })
                .addOnFailureListener(e -> {
                    Log.e("CalendarActivity", "Erro ao buscar itens", e);
                    displayError("Erro ao buscar itens.");
                });
    }
    private void markEventDays() {
        firestore.collection("users")
                .document(username)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventDay> events = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String dateStr = document.getString("date");

                        if (dateStr != null && dateStr.contains("/")) {
                            try {
                                String[] parts = dateStr.split("/");
                                int day = Integer.parseInt(parts[0]);
                                int month = Integer.parseInt(parts[1]) - 1; // Calendar.MONTH starts from 0
                                int year = Integer.parseInt(parts[2]);

                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, month, day);

                                // Debugging
                                Log.d("CalendarActivity", "Marking event on: " + day + "/" + (month + 1) + "/" + year);

                                // Add event with white dot
                                events.add(new EventDay(calendar, R.drawable.white_dot));
                            } catch (Exception e) {
                                Log.e("CalendarActivity", "Error parsing date: " + dateStr, e);
                            }
                        }
                    }

                    // Ensure we update the calendar view
                    if (!events.isEmpty()) {
                        calendarView.setEvents(events);
                        Log.d("CalendarActivity", "Events set: " + events.size());
                    } else {
                        Log.d("CalendarActivity", "No events found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("CalendarActivity", "Erro ao carregar eventos", e));
    }

    private void displayItems(String date, List<Map<String, Object>> items) {
        itemsTable.removeAllViews();

        if (items.isEmpty()) {
            Log.d("CalendarActivity", "Nenhum item encontrado para " + date);
            addEmptyRow();
            return;
        }

        addSectionHeader("Ganhos");
        List<Map<String, Object>> ganhos = new ArrayList<>();
        List<Map<String, Object>> despesas = new ArrayList<>();

        for (Map<String, Object> item : items) {
            if ("Ganho".equals(item.get("type"))) {
                ganhos.add(item);
            } else {
                despesas.add(item);
            }
        }

        if (ganhos.isEmpty()) {
            addEmptyRow();
        } else {
            for (Map<String, Object> ganho : ganhos) {
                addItemRow(ganho);
            }
        }

        addSectionHeader("Despesas");
        if (despesas.isEmpty()) {
            addEmptyRow();
        } else {
            for (Map<String, Object> despesa : despesas) {
                addItemRow(despesa);
            }
        }
    }

    private void addSectionHeader(String title) {
        TableRow headerRow = new TableRow(this);

        TextView headerText = new TextView(this);
        headerText.setText(title);
        headerText.setPadding(16, 16, 16, 16);
        headerText.setTextSize(18);
        headerText.setTextColor(Color.WHITE);
        headerText.setTypeface(null, Typeface.BOLD_ITALIC);

        headerRow.addView(headerText);
        itemsTable.addView(headerRow);

        // Adiciona uma linha branca abaixo do cabeçalho
        View separator = new View(this);
        separator.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                2 // Altura da linha
        ));
        separator.setBackgroundColor(Color.WHITE);

        itemsTable.addView(separator);
    }


    private void addEmptyRow() {
        TableRow emptyRow = new TableRow(this);
        TextView emptyText = new TextView(this);
        emptyText.setText("Nenhum item para esta data.");
        emptyText.setPadding(16, 16, 16, 16);
        emptyText.setTextColor(Color.WHITE);
        emptyRow.addView(emptyText);
        itemsTable.addView(emptyRow);
    }

    private void addItemRow(Map<String, Object> item) {
        TableRow row = new TableRow(this);
        row.setGravity(Gravity.CENTER);

        TextView categoryText = new TextView(this);
        categoryText.setText(item.get("category").toString());
        categoryText.setPadding(16, 16, 16, 16);
        categoryText.setTextColor(Color.WHITE);

        TextView amountText = new TextView(this);
        double amount = Double.parseDouble(item.get("amount").toString());
        String type = item.get("type").toString();

        if ("Ganho".equals(type)) {
            amountText.setText(String.format("+%.2f", amount));
            amountText.setTextColor(Color.GREEN);
        } else {
            amountText.setText(String.format("-%.2f", amount));
            amountText.setTextColor(Color.RED);
        }

        amountText.setPadding(16, 16, 16, 16);
        amountText.setGravity(Gravity.END);

        row.addView(categoryText);
        row.addView(amountText);

        row.setOnClickListener(v -> showSingleItemDialog(item));
        itemsTable.addView(row);
    }

    private void displayError(String message) {
        itemsTable.removeAllViews();
        TableRow errorRow = new TableRow(this);
        TextView errorText = new TextView(this);
        errorText.setText(message);
        errorText.setPadding(16, 16, 16, 16);
        errorText.setTextColor(Color.WHITE);
        errorRow.addView(errorText);
        itemsTable.addView(errorRow);
    }
    private void showSingleItemDialog(Map<String, Object> item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_item_details, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView dialogType = dialogView.findViewById(R.id.dialogType);
        TextView dialogCategory = dialogView.findViewById(R.id.dialogCategory);
        TextView dialogAmount = dialogView.findViewById(R.id.dialogAmount);
        TextView dialogDescription = dialogView.findViewById(R.id.dialogDescription);
        Button deleteButton = dialogView.findViewById(R.id.btnDelete);
        Button closeButton = dialogView.findViewById(R.id.btnClose);

        String type = item.get("type").toString();
        String category = item.get("category").toString();
        double amount = Double.parseDouble(item.get("amount").toString());
        String description = item.containsKey("description") ? item.get("description").toString() : "Sem descrição";

        // Formatar texto no diálogo
        dialogType.setText(Html.fromHtml("<b>Tipo:</b> " + type));
        dialogType.setTextColor(Color.BLACK);

        dialogCategory.setText(Html.fromHtml("<b>Categoria:</b> " + category));
        dialogCategory.setTextColor(Color.BLACK);

        dialogAmount.setText(Html.fromHtml("<b>Quantia:</b> " + String.format("%.2f€", amount)));
        dialogAmount.setTextColor(Color.BLACK);

        dialogDescription.setText(Html.fromHtml("<b>Descrição:</b> " + description));
        dialogDescription.setTextColor(Color.BLACK);

        deleteButton.setOnClickListener(v -> {
            removeItem(item, dialog);
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Ajustar tamanho do diálogo
        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.8), // 80% da largura da tela
                        ViewGroup.LayoutParams.WRAP_CONTENT // Altura ajustada ao conteúdo
                );
            }
        });

        dialog.show();
    }


    private void removeItem(Map<String, Object> item, AlertDialog dialog) {
        firestore.collection("users")
                .document(username)
                .collection("items")
                .whereEqualTo("category", item.get("category"))
                .whereEqualTo("amount", item.get("amount"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                    Toast.makeText(this, "Item removido!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchItemsForDate("data_atual");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao remover item.", Toast.LENGTH_SHORT).show());
    }
}
