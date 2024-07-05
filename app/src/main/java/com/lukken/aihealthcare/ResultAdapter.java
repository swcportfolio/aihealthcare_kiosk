package com.lukken.aihealthcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Result> resultItems;

    private OnItemClickListener mListener;

    // 아이템 클릭 이벤트를 처리하기 위한 인터페이스 정의
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    // 클릭 리스너 등록 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }


    public ResultAdapter(Context context, ArrayList<Result> resultItems) {
        this.context = context;
        this.resultItems = resultItems;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView device_name;
        TextView date_result;
        ImageView image_result;

        Button btn_result;

        public ViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            device_name = itemView.findViewById(R.id.device_name);
            image_result = itemView.findViewById(R.id.image_result);
            //date_result = itemView.findViewById(R.id.date_result);
            btn_result = itemView.findViewById(R.id.btn_result);


            btn_result.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // 클릭 이벤트 처리
                    listener.onItemClick(position);
                }
            });
                // 아이템 뷰에 클릭 리스너 등록
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            // 클릭 이벤트 처리
                            listener.onItemClick(position);
                        }
                        //listener.onItemClick(position);

                    }
                });

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_result_list, parent, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.device_name.setText(resultItems.get(position).deviceName);
        //holder.date_result.setText(resultItems.get(position).inspectionDate);
        holder.image_result.setImageResource(imageNameToResId(resultItems.get(position).inspectionIdx));
    }

    @Override
    public int getItemCount() {
        return resultItems.size();
    }

    public int imageNameToResId(String name){
        return R.drawable.deivce_default;
//        switch (name){
//            case "1" : return R.drawable.device1;
//            case "2" : return R.drawable.device2;
//            case "3" : return R.drawable.device3;
//            case "4" : return R.drawable.device4;
//            case "5" : return R.drawable.device5;
//            case "6" : return R.drawable.device6;
//            case "7" : return R.drawable.device7;
//            case "8" : return R.drawable.device8;
//            case "9" : return R.drawable.device9;
//            case "10" : return R.drawable.device10;
//            case "11" : return R.drawable.device11;
//            case "12" :return R.drawable.device12;
//            case "13" : return R.drawable.device13;
//            case "14" : return R.drawable.device14;
//            case "15" : return R.drawable.device15;
//            case "16" : return R.drawable.device16;
//            case "17" : return R.drawable.device17;
//            case "18" : return R.drawable.device18;
//
//
//            default:
//        }
    }
}
