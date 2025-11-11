package com.example.duarteapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.MonthViewHolder> {

    private final ArrayList<String> months;
    private final Context context;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final OnMonthSelectedListener listener;

    public interface OnMonthSelectedListener {
        void onMonthSelected(int position);
    }

    public MonthAdapter(Context context, ArrayList<String> months, OnMonthSelectedListener listener) {
        this.context = context;
        this.months = months;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        String month = months.get(position % months.size()); // Para permitir rolagem infinita
        holder.textView.setText(month);
        holder.textView.setTextSize(16);
        holder.textView.setTextColor(selectedPosition == position ? context.getResources().getColor(android.R.color.white) : context.getResources().getColor(android.R.color.black));
        holder.textView.setBackgroundColor(selectedPosition == position ? context.getResources().getColor(android.R.color.holo_blue_light) : android.R.color.transparent);
        holder.textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        holder.textView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onMonthSelected(selectedPosition % months.size()); // Notifica o mês selecionado
        });
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE; // Para rolagem infinita
    }

    public static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
