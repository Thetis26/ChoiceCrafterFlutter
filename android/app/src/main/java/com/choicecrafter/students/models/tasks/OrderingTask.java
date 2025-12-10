package com.choicecrafter.students.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class OrderingTask extends Task {
    private List<String> items;
    private List<Integer> correctOrder;

    public OrderingTask() {}

    public OrderingTask(String title, String description, String type, String status,
                        List<String> items, List<Integer> correctOrder) {
        super(title, description, type, status);
        this.items = items;
        this.correctOrder = correctOrder;
    }

    protected OrderingTask(Parcel in) {
        super(in);
        items = in.createStringArrayList();
        correctOrder = in.readArrayList(Integer.class.getClassLoader());
    }

    public static final Parcelable.Creator<OrderingTask> CREATOR = new Parcelable.Creator<OrderingTask>() {
        @Override
        public OrderingTask createFromParcel(Parcel in) {
            return new OrderingTask(in);
        }

        @Override
        public OrderingTask[] newArray(int size) {
            return new OrderingTask[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeStringList(items);
        dest.writeList(correctOrder);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<Integer> getCorrectOrder() {
        return correctOrder;
    }

    public void setCorrectOrder(List<Integer> correctOrder) {
        this.correctOrder = correctOrder;
    }
}
