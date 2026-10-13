package com.example.asalaga1.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.User;
import com.example.asalaga1.ui.auth.LoginActivity;
import com.example.asalaga1.util.SessionManager;

public class CustomerProfileFragment extends Fragment {

    private TextView tvInitial, tvName, tvEmail, tvPhone, tvRole;
    private Button btnLogout;
    private SessionManager session;
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        session = new SessionManager(requireContext());
        repository = new AppRepository(requireContext());

        tvInitial = view.findViewById(R.id.tv_profile_initial);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvRole = view.findViewById(R.id.tv_profile_role);
        btnLogout = view.findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> handleLogout());

        loadUserData();

        return view;
    }

    private void loadUserData() {
        int userId = session.getUserId();
        tvName.setText(session.getUserName());
        tvEmail.setText(session.getUserEmail());
        tvRole.setText(session.getUserRole());

        if (!session.getUserName().isEmpty()) {
            tvInitial.setText(String.valueOf(session.getUserName().charAt(0)).toUpperCase());
        }

        repository.getExecutor().execute(() -> {
            User user = repository.getUserDao().getUserById(userId);
            if (user != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvPhone.setText(user.getPhone());
                });
            }
        });
    }

    private void handleLogout() {
        session.logout();
        Toast.makeText(requireContext(), "Berhasil keluar akun!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
