package com.example.asalaga1.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.User;
import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private TextView tvEmptyState;
    private EditText etSearch;
    
    private AppRepository repository;
    private List<User> allUsers = new ArrayList<>();
    private List<User> displayedUsers = new ArrayList<>();
    private UserAdapter adapter;
    
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        repository = new AppRepository(requireContext());

        rvUsers = view.findViewById(R.id.rv_admin_users);
        tvEmptyState = view.findViewById(R.id.tv_admin_users_empty);
        etSearch = view.findViewById(R.id.et_admin_users_search);

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserAdapter(displayedUsers, this::showDeleteConfirmDialog);
        rvUsers.setAdapter(adapter);

        // Real-time search query listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                filterUsers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadUsers();

        return view;
    }

    private void loadUsers() {
        repository.getExecutor().execute(() -> {
            List<User> users = repository.getUserDao().getAllUsersExceptAdmin();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allUsers.clear();
                    allUsers.addAll(users);
                    filterUsers();
                });
            }
        });
    }

    private void filterUsers() {
        displayedUsers.clear();
        String query = currentSearchQuery.toLowerCase().trim();
        for (User u : allUsers) {
            boolean matches = query.isEmpty()
                    || (u.getName() != null && u.getName().toLowerCase().contains(query))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query))
                    || (u.getRole() != null && u.getRole().toLowerCase().contains(query));
            if (matches) {
                displayedUsers.add(u);
            }
        }
        adapter.notifyDataSetChanged();

        if (displayedUsers.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(query.isEmpty() ? "Tidak ada pengguna terdaftar" : "Tidak ada pengguna cocok.");
            rvUsers.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteConfirmDialog(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus Pengguna")
                .setMessage("Apakah Anda yakin ingin menghapus akun " + user.getName() + " (" + user.getRole() + ") secara permanen?")
                .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                    repository.getExecutor().execute(() -> {
                        repository.getUserDao().deleteUser(user.getId());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Akun berhasil dihapus secara permanen.", Toast.LENGTH_SHORT).show();
                                loadUsers();
                            });
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // Embedded Adapter for Admin users list
    public static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<User> users;
        private final OnUserDeleteListener listener;

        public interface OnUserDeleteListener {
            void onDelete(User user);
        }

        public UserAdapter(List<User> users, OnUserDeleteListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.bind(user, listener);
        }

        @Override
        public int getItemCount() {
            return Math.max(users.size(), 0);
        }

        public static class UserViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvEmail, tvPhone, tvRole;
            private final ImageButton btnDelete;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_user_name);
                tvEmail = itemView.findViewById(R.id.tv_item_user_email);
                tvPhone = itemView.findViewById(R.id.tv_item_user_phone);
                tvRole = itemView.findViewById(R.id.tv_item_user_role);
                btnDelete = itemView.findViewById(R.id.btn_delete_user);
            }

            public void bind(User user, OnUserDeleteListener listener) {
                tvName.setText(user.getName());
                tvEmail.setText(user.getEmail());
                tvPhone.setText("📞 " + user.getPhone());
                tvRole.setText(user.getRole());

                // Style the badge based on role
                if ("OWNER".equals(user.getRole())) {
                    tvRole.setBackgroundResource(R.drawable.bg_tag_pending);
                    tvRole.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                } else {
                    tvRole.setBackgroundResource(R.drawable.bg_tag_confirmed);
                    tvRole.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                }

                btnDelete.setOnClickListener(v -> listener.onDelete(user));
            }
        }
    }
}
