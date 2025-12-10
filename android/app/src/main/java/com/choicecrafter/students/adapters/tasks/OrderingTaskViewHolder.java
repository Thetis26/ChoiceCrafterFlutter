package com.choicecrafter.students.adapters.tasks;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.tasks.OrderingTask;
import com.choicecrafter.students.utils.AiHintService;
import com.choicecrafter.students.utils.HintDialogUtil;

import java.util.ArrayList;
import java.util.List;

public class OrderingTaskViewHolder extends RecyclerView.ViewHolder {
    private RecyclerView recyclerView;
    private OrderItemAdapter adapter;
    private OrderingTask currentTask;
    private Context context;
    private final TextView descriptionView;
    private final AiHintService aiHintService;
    private boolean aiHintShown;
    private boolean hintInProgress;
    private String solutionText;

    public OrderingTaskViewHolder(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        recyclerView = itemView.findViewById(R.id.order_items_recycler);
        descriptionView = itemView.findViewById(R.id.task_description);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        aiHintService = AiHintService.getInstance(itemView.getContext());
    }

    public void bind(OrderingTask task) {
        currentTask = task;
        if (descriptionView != null) {
            if (!TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }
        adapter = new OrderItemAdapter(new ArrayList<>(task.getItems()));
        recyclerView.setAdapter(adapter);
        aiHintShown = false;
        hintInProgress = false;
        solutionText = buildSolutionText();

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to = target.getAdapterPosition();
                adapter.swapItems(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder vh, int actionState) {
                super.onSelectedChanged(vh, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && vh != null) {
                    vh.itemView.setBackgroundResource(R.drawable.order_item_selected);
                    vh.itemView.animate().scaleX(1.03f).scaleY(1.03f).setDuration(150).start();
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                vh.itemView.setBackgroundResource(R.drawable.bg_order_item_default);
                vh.itemView.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    public boolean validateOrder() {
        List<String> expected = buildOrderedItems();
        boolean correct = adapter.getItems().equals(expected);
        if (!correct) {
            Toast.makeText(context, R.string.task_answer_incorrect, Toast.LENGTH_SHORT).show();
        }
        return correct;
    }

    public boolean hasUsedHint() {
        return aiHintShown;
    }

    public void showHint() {
        if (currentTask == null) {
            return;
        }

        if (hintInProgress) {
            Toast.makeText(context, R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!aiHintShown) {
            requestHintFromAi();
            return;
        }

        showFinalAnswerDialog();
    }

    private String buildPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("Provide a helpful hint for arranging the following items in the correct order. Do not reveal the final order.\n");
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            builder.append("Description: ").append(currentTask.getDescription()).append('\n');
        }
        builder.append("Items to arrange: \n");
        for (String item : currentTask.getItems()) {
            builder.append("- ").append(item).append('\n');
        }
        return builder.toString();
    }

    private String buildSolutionText() {
        StringBuilder builder = new StringBuilder();
        for (String item : buildOrderedItems()) {
            builder.append(item).append('\n');
        }
        return builder.toString().trim();
    }

    private void showDialog(String title, String message, int iconRes) {
        HintDialogUtil.showHintDialog(context, title, message, iconRes);
    }

    private void showHintDialogWithActions(String hint) {
        HintDialogUtil.showHintDialog(
                context,
                context.getString(R.string.ai_hint_title),
                hint,
                R.drawable.lightbulb_on,
                this::handleRequestAnotherHint,
                this::showFinalAnswerDialog);
    }

    private void requestHintFromAi() {
        hintInProgress = true;
        Toast.makeText(context, R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
        aiHintService.requestHint(buildPrompt(), new AiHintService.HintCallback() {
            @Override
            public void onSuccess(String hint) {
                hintInProgress = false;
                aiHintShown = true;
                showHintDialogWithActions(hint);
            }

            @Override
            public void onError(String errorMessage) {
                hintInProgress = false;
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRequestAnotherHint() {
        if (hintInProgress) {
            Toast.makeText(context, R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
            return;
        }
        requestHintFromAi();
    }

    private void showFinalAnswerDialog() {
        showDialog(context.getString(R.string.ai_answer_title), solutionText, R.drawable.trophy);
    }

    private List<String> buildOrderedItems() {
        List<String> orderedItems = new ArrayList<>();
        if (currentTask == null) {
            return orderedItems;
        }

        List<String> items = currentTask.getItems();
        List<Integer> positions = currentTask.getCorrectOrder();
        if (items == null || positions == null) {
            return orderedItems;
        }

        for (int position : positions) {
            if (position >= 0 && position < items.size()) {
                orderedItems.add(items.get(position));
            }
        }
        return orderedItems;
    }

    private static class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder> {
        private final List<String> items;
        OrderItemAdapter(List<String> items) { this.items = items; }
        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false);
            return new ItemViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            holder.itemView.setBackgroundResource(R.drawable.bg_order_item_default);
            holder.text.setText(items.get(position));
            holder.rewardIcon.setRotation(position % 2 == 0 ? 0f : -8f);
            holder.rewardIcon.setScaleX(1f);
            holder.rewardIcon.setScaleY(1f);
            holder.itemView.setScaleX(1f);
            holder.itemView.setScaleY(1f);
        }
        @Override
        public int getItemCount() { return items.size(); }
        List<String> getItems() { return items; }
        void swapItems(int from, int to) {
            if (from == to || from < 0 || to < 0 || from >= items.size() || to >= items.size()) {
                return;
            }

            String movedItem = items.remove(from);
            items.add(to, movedItem);
            notifyItemMoved(from, to);

            int start = Math.min(from, to);
            int end = Math.max(from, to);
            notifyItemRangeChanged(start, end - start + 1);
        }
        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ImageView rewardIcon;
            ItemViewHolder(View item) {
                super(item);
                text = item.findViewById(R.id.item_text);
                rewardIcon = item.findViewById(R.id.item_reward_icon);
            }
        }
    }
}
