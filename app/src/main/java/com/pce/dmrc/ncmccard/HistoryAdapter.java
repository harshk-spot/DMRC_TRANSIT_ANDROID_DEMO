package com.pce.dmrc.ncmccard;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter
        extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<HistoryItem> historyList;

    public HistoryAdapter(
            Context context,
            List<HistoryItem> historyList
    ) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.history_item,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        HistoryItem item = historyList.get(position);

        holder.tvType.setText(item.getType());

        if (item.getType().equalsIgnoreCase("Entry")) {

            holder.tvType.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.green
                    )
            );

        } else {

            holder.tvType.setTextColor(
                    ContextCompat.getColor(
                            context,
                            R.color.red
                    )
            );
        }

        holder.tvStation.setText(item.getStation());

        holder.tvDateTime.setText(
                item.getDateTime()
        );

        holder.tvFare.setText(
                "Fare : " + item.getFare()
        );

        holder.tvBalance.setText(
                "Balance : " + item.getBalance()
        );
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvType;
        TextView tvStation;
        TextView tvFare;
        TextView tvBalance;
        TextView tvDateTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvType = itemView.findViewById(R.id.tvType);
            tvStation = itemView.findViewById(R.id.tvStation);
            tvFare = itemView.findViewById(R.id.tvFare);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }
}
