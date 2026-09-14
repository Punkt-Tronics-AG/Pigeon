package org.thoughtcrime.securesms.pigeon.activity;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.R;

import java.util.List;

import static pigeon.extensions.KotilinExtensionsKt.focusOnLeft;

public class ConversationSubMenuAdapter extends RecyclerView.Adapter<ConversationSubMenuAdapter.ViewHolder> {
  private final List<String> data;
  LayoutInflater    inflater;
  ItemClickListener clickListener;

  public ConversationSubMenuAdapter(Context context, ItemClickListener clickListener, List<String> data) {
    this.inflater      = LayoutInflater.from(context);
    this.clickListener = clickListener;
    this.data          = data;
    Resources res = context.getResources();
  }

  @NonNull @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = inflater.inflate(R.layout.conversation_sub_menu_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.tv.setText(data.get(position));
    focusOnLeft(holder.itemView);

    holder.itemView.setOnClickListener(v -> {
      if (clickListener != null) clickListener.onItemClick(position);
    });
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  interface ItemClickListener {
    void onItemClick(int position);
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    public TextView tv;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      tv = itemView.findViewById(R.id.sub_menu);
    }
  }
}
