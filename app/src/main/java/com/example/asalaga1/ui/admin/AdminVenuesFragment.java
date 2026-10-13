package com.example.asalaga1.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Venue;
import android.content.Intent;
import com.bumptech.glide.Glide;
import com.example.asalaga1.util.ImageHelper;
import com.example.asalaga1.ui.customer.VenueDetailsActivity;
import java.util.ArrayList;
import java.util.List;

public class AdminVenuesFragment extends Fragment {

    private RecyclerView rvVenues;
    private TextView tvEmptyState;
    private EditText etSearch;
    private TextView tvTabPending;
    private TextView tvTabConfirmed;
    
    private AppRepository repository;
    private List<Venue> allVenues = new ArrayList<>();
    private List<Venue> displayedVenues = new ArrayList<>();
    private AdminVenueAdapter adapter;
    
    private boolean isFilterPending = true;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_venues, container, false);

        repository = new AppRepository(requireContext());

        rvVenues = view.findViewById(R.id.rv_admin_venues);
        tvEmptyState = view.findViewById(R.id.tv_admin_venues_empty);
        etSearch = view.findViewById(R.id.et_admin_venues_search);
        tvTabPending = view.findViewById(R.id.tv_tab_pending);
        tvTabConfirmed = view.findViewById(R.id.tv_tab_confirmed);

        rvVenues.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminVenueAdapter(displayedVenues, new AdminVenueAdapter.OnVenueDecisionListener() {
            @Override
            public void onApprove(Venue venue) {
                updateVenue(venue.getId(), "APPROVED", "Venue berhasil disetujui!");
            }

            @Override
            public void onReject(Venue venue) {
                updateVenue(venue.getId(), "REJECTED", "Pengajuan venue berhasil ditolak.");
            }
        });
        rvVenues.setAdapter(adapter);

        // Tab selection click listeners
        tvTabPending.setOnClickListener(v -> setFilterPending(true));
        tvTabConfirmed.setOnClickListener(v -> setFilterPending(false));

        // Real-time search query listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                filterVenues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadAllVenues();

        return view;
    }

    private void setFilterPending(boolean pending) {
        isFilterPending = pending;
        if (pending) {
            tvTabPending.setBackgroundResource(R.drawable.bg_button_blue);
            tvTabPending.setTextColor(getResources().getColor(R.color.white));
            tvTabConfirmed.setBackgroundResource(android.R.color.transparent);
            tvTabConfirmed.setTextColor(getResources().getColor(R.color.text_muted));
        } else {
            tvTabPending.setBackgroundResource(android.R.color.transparent);
            tvTabPending.setTextColor(getResources().getColor(R.color.text_muted));
            tvTabConfirmed.setBackgroundResource(R.drawable.bg_button_blue);
            tvTabConfirmed.setTextColor(getResources().getColor(R.color.white));
        }
        filterVenues();
    }

    private void loadAllVenues() {
        repository.getExecutor().execute(() -> {
            List<Venue> venues = repository.getVenueDao().getAllVenues();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allVenues.clear();
                    allVenues.addAll(venues);
                    filterVenues();
                });
            }
        });
    }

    private void filterVenues() {
        displayedVenues.clear();
        String query = currentSearchQuery.toLowerCase().trim();
        for (Venue v : allVenues) {
            boolean statusMatches;
            if (isFilterPending) {
                statusMatches = "PENDING".equals(v.getStatus());
            } else {
                statusMatches = "APPROVED".equals(v.getStatus()) || "REJECTED".equals(v.getStatus());
            }

            if (statusMatches) {
                boolean queryMatches = query.isEmpty() 
                        || (v.getName() != null && v.getName().toLowerCase().contains(query))
                        || (v.getAddress() != null && v.getAddress().toLowerCase().contains(query));
                if (queryMatches) {
                    displayedVenues.add(v);
                }
            }
        }
        adapter.notifyDataSetChanged();

        if (displayedVenues.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            if (isFilterPending) {
                tvEmptyState.setText(query.isEmpty() ? "Semua pengajuan venue telah disetujui!" : "Tidak ada pengajuan venue cocok.");
            } else {
                tvEmptyState.setText(query.isEmpty() ? "Tidak ada venue terkonfirmasi." : "Tidak ada venue cocok.");
            }
            rvVenues.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvVenues.setVisibility(View.VISIBLE);
        }
    }

    private void updateVenue(int venueId, String status, String toastMessage) {
        repository.getExecutor().execute(() -> {
            repository.getVenueDao().updateVenueStatus(venueId, status);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();
                    loadAllVenues();
                });
            }
        });
    }

    // Embedded Adapter for Admin pending verification list
    public static class AdminVenueAdapter extends RecyclerView.Adapter<AdminVenueAdapter.AdminVenueViewHolder> {
        private final List<Venue> venues;
        private final OnVenueDecisionListener listener;

        public interface OnVenueDecisionListener {
            void onApprove(Venue venue);
            void onReject(Venue venue);
        }

        public AdminVenueAdapter(List<Venue> venues, OnVenueDecisionListener listener) {
            this.venues = venues;
            this.listener = listener;
        }

        @NonNull
        @Override
        public AdminVenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_venue, parent, false);
            return new AdminVenueViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminVenueViewHolder holder, int position) {
            Venue venue = venues.get(position);
            holder.bind(venue, listener);
        }

        @Override
        public int getItemCount() {
            return venues.size();
        }

        public static class AdminVenueViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvAddress, tvDesc;
            private final View layoutAdminActions;
            private final Button btnApprove, btnReject;
            private final android.widget.ImageView imgVenue;
            
            // Status Tag Views
            private final View layoutStatusBadge;
            private final TextView tvStatusText;

            public AdminVenueViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_venue_name);
                tvAddress = itemView.findViewById(R.id.tv_item_venue_address);
                tvDesc = itemView.findViewById(R.id.tv_item_venue_desc);
                layoutAdminActions = itemView.findViewById(R.id.layout_admin_venue_actions);
                btnApprove = itemView.findViewById(R.id.btn_admin_approve);
                btnReject = itemView.findViewById(R.id.btn_admin_reject);
                imgVenue = itemView.findViewById(R.id.img_item_venue);
                
                layoutStatusBadge = itemView.findViewById(R.id.layout_venue_status_badge);
                tvStatusText = itemView.findViewById(R.id.tv_item_venue_status);
            }

            public void bind(Venue venue, OnVenueDecisionListener listener) {
                tvName.setText(venue.getName());
                tvAddress.setText("📍 " + venue.getAddress());
                tvDesc.setText(venue.getDescription());

                String imgId = venue.getImagePath();
                String imgUrl = ImageHelper.getImageUrl(imgId);

                Glide.with(itemView.getContext())
                     .load(imgUrl)
                     .placeholder(R.drawable.bg_gradient_blue)
                     .error(R.drawable.bg_gradient_blue)
                     .into(imgVenue);

                // Show action buttons or status badge based on state
                if ("PENDING".equals(venue.getStatus())) {
                    layoutAdminActions.setVisibility(View.VISIBLE);
                    layoutStatusBadge.setVisibility(View.GONE);
                } else {
                    layoutAdminActions.setVisibility(View.GONE);
                    layoutStatusBadge.setVisibility(View.VISIBLE);
                    tvStatusText.setText(venue.getStatus());
                    
                    if ("APPROVED".equals(venue.getStatus())) {
                        layoutStatusBadge.setBackgroundResource(R.drawable.bg_tag_confirmed);
                        tvStatusText.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                    } else { // REJECTED
                        layoutStatusBadge.setBackgroundResource(R.drawable.bg_tag_cancelled);
                        tvStatusText.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                    }
                }

                btnApprove.setOnClickListener(v -> listener.onApprove(venue));
                btnReject.setOnClickListener(v -> listener.onReject(venue));

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), VenueDetailsActivity.class);
                    intent.putExtra("VENUE_ID", venue.getId());
                    itemView.getContext().startActivity(intent);
                });
            }
        }
    }
}
