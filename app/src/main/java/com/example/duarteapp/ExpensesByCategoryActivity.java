package com.example.duarteapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpensesByCategoryActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private FirebaseFirestore firestore;
    private String username;
    private PieChart pieChart;
    private TableLayout detailsTable;
    private int selectedMonth, selectedYear;
    private Map<String, List<String>> categoryDetails;
    private TextView monthSummaryText;
    private TextView selectedCategoryText;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_by_category);

        ImageView btnBack = findViewById(R.id.btnBack);
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
            Intent intent = new Intent(ExpensesByCategoryActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        selectedCategoryText = findViewById(R.id.selectedCategoryText);
        firestore = FirebaseFirestore.getInstance();
        username = getIntent().getStringExtra("username");

        pieChart = findViewById(R.id.pieChart);
        detailsTable = findViewById(R.id.detailsTable);
        monthSummaryText = findViewById(R.id.monthSummaryText);
        Button previousMonthButton = findViewById(R.id.previousMonthButton);
        Button nextMonthButton = findViewById(R.id.nextMonthButton);

        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH) + 1;
        selectedYear = calendar.get(Calendar.YEAR);
        updateMonthSummaryText();

        previousMonthButton.setOnClickListener(v -> navigateToPreviousMonth());
        nextMonthButton.setOnClickListener(v -> navigateToNextMonth());

        fetchExpenseData();
    }

    @Override
    public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
        if (e instanceof PieEntry) {
            PieEntry pieEntry = (PieEntry) e;
            selectedCategoryText.setText("Categoria Selecionada: " + pieEntry.getLabel());
            setupDetailsTable(pieEntry.getLabel());
        }
    }

    @Override
    public void onNothingSelected() {
        detailsTable.removeAllViews();
    }

    private void updateMonthSummaryText() {
        String monthName = getMonthName(selectedMonth);
        monthSummaryText.setText(String.format("Mês: %s/%d", monthName, selectedYear));
    }

    private String getMonthName(int month) {
        String[] months = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};
        return months[month - 1];
    }

    private void navigateToPreviousMonth() {
        selectedMonth--;
        if (selectedMonth < 1) {
            selectedMonth = 12;
            selectedYear--;
        }
        updateMonthSummaryText();
        fetchExpenseData();
    }

    private void navigateToNextMonth() {
        selectedMonth++;
        if (selectedMonth > 12) {
            selectedMonth = 1;
            selectedYear++;
        }
        updateMonthSummaryText();
        fetchExpenseData();
    }

    private void fetchExpenseData() {
        firestore.collection("users")
                .document(username)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Double> categoryTotals = new HashMap<>();
                    categoryDetails = new HashMap<>();

                    for (var document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        String type = document.getString("type");
                        String category = document.getString("category");
                        double amount = document.getDouble("amount");

                        if (date != null && date.matches("\\d+/\\d+/\\d+")) {
                            String[] dateParts = date.split("/");
                            int month = Integer.parseInt(dateParts[1]);
                            int year = Integer.parseInt(dateParts[2]);

                            if (month == selectedMonth && year == selectedYear && "Gasto".equals(type)) {
                                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);

                                String description = document.getString("description"); // esta linha vai buscar a descrição do Firestore
                                if (description == null) description = ""; // para evitar null

                                categoryDetails
                                        .computeIfAbsent(category, k -> new ArrayList<>())
                                        .add(date + " - " + description + " - " + amount + "€");

                            }
                        }
                    }

                    setupPieChart(categoryTotals);
                });
    }

    private void setupPieChart(Map<String, Double> categoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Despesas");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextColor(Color.WHITE);

        pieChart.setOnChartValueSelectedListener(this);
    }

    private void setupDetailsTable(String category) {
        detailsTable.removeAllViews();

        TableRow headerRow = new TableRow(this);

        TextView headerDate = new TextView(this);
        headerDate.setText("Data");
        headerDate.setPadding(16, 16, 16, 16);
        headerDate.setTextColor(Color.WHITE);
        headerDate.setTextSize(20);
        headerDate.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView headerDescription = new TextView(this);
        headerDescription.setText("Descrição");
        headerDescription.setPadding(16, 16, 16, 16);
        headerDescription.setTextColor(Color.WHITE);
        headerDescription.setTextSize(20);
        headerDescription.setTypeface(null, android.graphics.Typeface.BOLD);
        headerDescription.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        TextView headerValue = new TextView(this);
        headerValue.setText("Valor");
        headerValue.setPadding(16, 16, 16, 16);
        headerValue.setTextColor(Color.WHITE);
        headerValue.setTextSize(20);
        headerValue.setTypeface(null, android.graphics.Typeface.BOLD);
        headerValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        headerRow.addView(headerDate);
        headerRow.addView(headerDescription);
        headerRow.addView(headerValue);
        detailsTable.addView(headerRow);

        View divider = new View(this);
        divider.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                2
        ));
        divider.setBackgroundColor(Color.WHITE);
        detailsTable.addView(divider);

        if (!categoryDetails.containsKey(category)) return;

        List<String> sortedDetails = new ArrayList<>(Objects.requireNonNull(categoryDetails.get(category)));
        Collections.sort(sortedDetails, (detail1, detail2) -> {
            String date1 = detail1.split(" - ")[0];
            String date2 = detail2.split(" - ")[0];
            return date2.compareTo(date1);
        });

        for (String detail : sortedDetails) {

            String[] parts = detail.split(" - ");
            if (parts.length < 3) continue;

            String date = parts[0];
            String description = parts[1];
            String value = parts[2];



            TableRow row = new TableRow(this);

            TextView dateTextView = new TextView(this);
            dateTextView.setText(date);
            dateTextView.setPadding(16, 16, 16, 16);
            dateTextView.setTextColor(Color.WHITE);

            TextView descriptionTextView = new TextView(this);
            descriptionTextView.setText(description);
            descriptionTextView.setTextColor(Color.WHITE);
            descriptionTextView.setPadding(16, 16, 16, 16);
            descriptionTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            TextView valueTextView = new TextView(this);
            valueTextView.setText(value);
            valueTextView.setTextColor(Color.WHITE);
            valueTextView.setPadding(16, 16, 16, 16);
            valueTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            row.addView(dateTextView);
            row.addView(descriptionTextView);
            row.addView(valueTextView);
            detailsTable.addView(row);
        }
    }
}
