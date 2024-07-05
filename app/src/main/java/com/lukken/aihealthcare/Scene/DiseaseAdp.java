package com.lukken.aihealthcare.Scene;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lukken.aihealthcare.R;

import java.util.List;

public class DiseaseAdp extends RecyclerView.Adapter<DiseaseAdp.BoardViewHolder> {
    private Context context;
    private List<SceneHealthZone.GHealthInfo> datas;

    public DiseaseAdp(Context context, List<SceneHealthZone.GHealthInfo> datas) {
        this.context = context;
        this.datas = datas;
    }

    @Override
    public BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BoardViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_ghealth_info, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BoardViewHolder holder, int position) {
        SceneHealthZone.GHealthInfo data = datas.get(position);
        holder.v1.setText(data.getTraitnm());
        holder.v2.setText(data.getValue2());

        if(data.getValue5().equals("danger")) {
            holder.bg.setBackgroundResource(R.drawable.bg_rectangle_round_strok_red);
            holder.v3.setBackgroundResource(R.drawable.bg_circle_red);
            holder.v2.setTextColor(context.getColor(R.color.vred));
            holder.v3.setText("나쁨");
        }else if(data.getValue5().equals("info")) {
            holder.bg.setBackgroundResource(R.drawable.bg_rectangle_round_strok_green);
            holder.v3.setBackgroundResource(R.drawable.bg_circle_green);
            holder.v2.setTextColor(context.getColor(R.color.vgreen));
            holder.v3.setText("보통");
        }else if(data.getValue5().equals("success")) {
            holder.bg.setBackgroundResource(R.drawable.bg_rectangle_round_strok_blue);
            holder.v3.setBackgroundResource(R.drawable.bg_circle_blue);
            holder.v2.setTextColor(context.getColor(R.color.vblue));
            holder.v3.setText("좋음");
        }

    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public class BoardViewHolder extends RecyclerView.ViewHolder {
        private View bg;
        private TextView v1;
        private TextView v2;
        private TextView v3;

        public BoardViewHolder(@NonNull View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.bg);
            v1 = itemView.findViewById(R.id.v1);
            v2 = itemView.findViewById(R.id.v2);
            v3 = itemView.findViewById(R.id.v3);
        }
    }
}
