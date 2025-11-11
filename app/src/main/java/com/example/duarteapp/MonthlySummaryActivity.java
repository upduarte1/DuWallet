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
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonthlySummaryActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private String username;

    private PieChart pieChart;
    private TableLayout categoryTable;
    private TextView monthSummaryText;

    private int selectedMonth;
    private int selectedYear;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_summary);

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
            Intent intent = new Intent(MonthlySummaryActivity.this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        firestore = FirebaseFirestore.getInstance();
        username = getIntent().getStringExtra("username");

        pieChart = findViewById(R.id.pieChart);
        categoryTable = findViewById(R.id.categoryTable);
        monthSummaryText = findViewById(R.id.monthSummaryText);

        Button previousMonthButton = findViewById(R.id.previousMonthButton);
        Button nextMonthButton = findViewById(R.id.nextMonthButton);

        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH) + 1;
        selectedYear = calendar.get(Calendar.YEAR);

        updateMonthSummaryText();
        previousMonthButton.setOnClickListener(v -> navigateToPreviousMonth());
        nextMonthButton.setOnClickListener(v -> navigateToNextMonth());
        fetchMonthlyData();

    }

    @SuppressLint("DefaultLocale")
    private void updateMonthSummaryText() {
        String monthName = getMonthName(selectedMonth);
        monthSummaryText.setText(String.format("Mês: %s/%d", monthName, selectedYear));
        monthSummaryText.setTypeface(null, android.graphics.Typeface.BOLD);
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
        fetchMonthlyData();
    }

    private void navigateToNextMonth() {
        selectedMonth++;
        if (selectedMonth > 12) {
            selectedMonth = 1;
            selectedYear++;
        }
        updateMonthSummaryText();
        fetchMonthlyData();
    }

    private void fetchMonthlyData() {
        firestore.collection("users")
                .document(username)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalGains = 0;
                    double totalExpenses = 0;

                    Map<String, Double> categoryTotals = new HashMap<>();

                    for (var document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        String type = document.getString("type");
                        String category = document.getString("category");
                        double amount = document.getDouble("amount");

                        // Filtrar pelo mês e ano selecionados
                        if (date != null && date.matches("\\d+/\\d+/\\d+")) {
                            String[] dateParts = date.split("/");
                            int month = Integer.parseInt(dateParts[1]);
                            int year = Integer.parseInt(dateParts[2]);

                            if (month == selectedMonth && year == selectedYear) {
                                if ("Ganho".equals(type)) {
                                    totalGains += amount;
                                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                                } else if ("Gasto".equals(type)) {
                                    totalExpenses += amount;
                                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) - amount);
                                }
                            }
                        }
                    }

                    setupPieChart(totalGains, totalExpenses);
                    setupCategoryTable(categoryTotals);
                });
    }
    private void setupPieChart(double totalGains, double totalExpenses) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) totalGains, "Ganhos"));
        entries.add(new PieEntry((float) totalExpenses, "Gastos"));

        PieDataSet dataSet = new PieDataSet(entries, "Resumo do Mês");
        // dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        List<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.my1));
        colors.add(getResources().getColor(R.color.my2));
        dataSet.setColors(colors);



        PieData pieData = new PieData(dataSet);
        pieData.setValueTextSize(14f);
        pieData.setValueTextColor(Color.BLACK);

        // Formatar valores como total (€)
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f€", value);
            }
        });

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextColor(Color.WHITE);

        pieChart.invalidate();
    }

    private void setupCategoryTable(Map<String, Double> categoryTotals) {
        categoryTable.removeAllViews();

        TableRow headerRow = new TableRow(this);

        TextView headerCategory = new TextView(this);
        headerCategory.setText("Categoria");
        headerCategory.setPadding(16, 16, 16, 16);
        headerCategory.setTextColor(Color.WHITE);
        headerCategory.setTextSize(20);
        headerCategory.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView headerTotal = new TextView(this);
        headerTotal.setText("Total");
        headerTotal.setPadding(16, 16, 16, 16);
        headerTotal.setTextColor(Color.WHITE);
        headerTotal.setTextSize(20);
        headerTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        headerTotal.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        headerRow.addView(headerCategory);
        headerRow.addView(headerTotal);

        categoryTable.addView(headerRow);
        View divider = new View(this);
        divider.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                2
        ));
        divider.setBackgroundColor(Color.WHITE);
        categoryTable.addView(divider);

        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(categoryTotals.entrySet());
        sortedEntries.sort((entry1, entry2) -> Double.compare(Math.abs(entry2.getValue()), Math.abs(entry1.getValue())));

        for (Map.Entry<String, Double> entry : sortedEntries) {
            String category = entry.getKey();
            double total = entry.getValue();

            TableRow row = new TableRow(this);

            TextView categoryTextView = new TextView(this);
            categoryTextView.setText(category);
            categoryTextView.setPadding(16, 16, 16, 16);
            categoryTextView.setTextColor(Color.WHITE);

            TextView totalTextView = new TextView(this);
            totalTextView.setText(String.format(total > 0 ? "+%.2f" : "%.2f", total));
            totalTextView.setTextColor(total > 0 ? Color.GREEN : Color.RED);
            totalTextView.setPadding(16, 16, 16, 16);
            totalTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            row.addView(categoryTextView);
            row.addView(totalTextView);

            categoryTable.addView(row);
        }
    }
}
