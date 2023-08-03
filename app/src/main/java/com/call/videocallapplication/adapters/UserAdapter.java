package com.call.videocallapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.call.videocallapplication.R;
import com.call.videocallapplication.listeners.UserListener;
import com.call.videocallapplication.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> users;
    private final UserListener userListener;
    private final List<User> selectedUsers;

    public UserAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
        this.selectedUsers = new ArrayList<>();
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTextFirstChar;
        private final TextView tvTextUserName;
        private final TextView tvTextEmail;
        private final ImageView ivAudioMeeting;
        private final ImageView ivVideoMeeting;
        private final ImageView ivImageSelected;
        private final ConstraintLayout userContainer;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            // mapping xml for item view
            tvTextFirstChar = itemView.findViewById(R.id.tvTextFirstChar);
            tvTextUserName = itemView.findViewById(R.id.tvTextUserName);
            tvTextEmail = itemView.findViewById(R.id.tvTextEmail);
            ivAudioMeeting = itemView.findViewById(R.id.ivAudioMeeting);
            ivVideoMeeting = itemView.findViewById(R.id.ivVideoMeeting);
            ivImageSelected = itemView.findViewById(R.id.ivImageSelected);
            userContainer = itemView.findViewById(R.id.userContainer);
        }

        void setUserData(User user) {
            tvTextFirstChar.setText(user.getFirstname().substring(0, 1));
            tvTextUserName.setText(String.format("%s %s", user.getFirstname(), user.getLastName()));
            tvTextEmail.setText(user.getEmail());

            // Process click event of item view
            ivAudioMeeting.setOnClickListener(v -> userListener.initAudioMeeting(user));
            ivVideoMeeting.setOnClickListener(v -> userListener.initVideoMeeting(user));
            userContainer.setOnLongClickListener(v -> {
                if (ivImageSelected.getVisibility() != View.VISIBLE) {
                    selectedUsers.add(user);
                    ivImageSelected.setVisibility(View.VISIBLE);
                    ivAudioMeeting.setVisibility(View.GONE);
                    ivVideoMeeting.setVisibility(View.GONE);
                    userListener.onMultipleUserAction(true);
                }
                return true;
            });
            userContainer.setOnClickListener(v -> {
                if (ivImageSelected.getVisibility() == View.VISIBLE) {
                    selectedUsers.remove(user);
                    ivImageSelected.setVisibility(View.GONE);
                    ivAudioMeeting.setVisibility(View.VISIBLE);
                    ivVideoMeeting.setVisibility(View.VISIBLE);
                    if (selectedUsers.size() == 0) {
                        userListener.onMultipleUserAction(false);
                    }
                } else {
                    if (selectedUsers.size() > 0) {
                        selectedUsers.add(user);
                        ivImageSelected.setVisibility(View.VISIBLE);
                        ivAudioMeeting.setVisibility(View.GONE);
                        ivVideoMeeting.setVisibility(View.GONE);
                    }
                }
            });
        }
    }
}
