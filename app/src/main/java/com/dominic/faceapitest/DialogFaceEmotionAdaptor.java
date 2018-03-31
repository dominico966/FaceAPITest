package com.dominic.faceapitest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dominic.skuface.FaceApi;

import java.lang.reflect.Field;

/**
 * Created by 박우영 on 2018-03-23.
 */

public class DialogFaceEmotionAdaptor extends BaseAdapter {

    private Context context = null;

    private static final int FACE_EMOTION_ATT_CNT = 8;

    private FaceApi.Face face;

    public DialogFaceEmotionAdaptor(Context context, FaceApi.Face face) {
        this.context = context;
        this.face = face;
    }

    @Override
    public int getCount() {
        return FACE_EMOTION_ATT_CNT;
    }

    @Override
    public Object getItem(int i) {
        return new ItemData(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).hashCode();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View item = view;

        if (item == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
            item = layoutInflater.inflate(R.layout.dialog_face_emotion_list_view_item, viewGroup, false);
        }

        TextView name = item.findViewById(R.id.name);
        TextView value = item.findViewById(R.id.value);

        ItemData data = (ItemData) getItem(i);
        name.setText(data.getName());
        value.setText(String.valueOf(data.getValue()));

        return item;
    }

    class ItemData {
        private String name;
        private double value;

        public ItemData(int i) {
            try {
                Field field = face.getEmotion().getClass().getDeclaredFields()[i];
                name = field.getName();
                value = field.getDouble(face.getEmotion());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public String getName() {
            return name;
        }

        public double getValue() {
            return value;
        }
    }
}