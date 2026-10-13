package com.example.asalaga1.ui.owner;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.bumptech.glide.Glide;
import com.example.asalaga1.util.ImageHelper;
import com.example.asalaga1.util.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class OwnerVenuesFragment extends Fragment {

    private RecyclerView rvVenues;
    private TextView tvEmptyState;
    private FloatingActionButton fabAdd;

    private AppRepository repository;
    private SessionManager session;
    private List<Venue> venueList = new ArrayList<>();
    private OwnerVenueAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_venues, container, false);

        repository = new AppRepository(requireContext());
        session = new SessionManager(requireContext());

        rvVenues = view.findViewById(R.id.rv_owner_venues);
        tvEmptyState = view.findViewById(R.id.tv_owner_venues_empty);
        fabAdd = view.findViewById(R.id.fab_add_venue);

        rvVenues.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OwnerVenueAdapter(venueList, venue -> {
            Intent intent = new Intent(requireActivity(), OwnerCourtsActivity.class);
            intent.putExtra("VENUE_ID", venue.getId());
            intent.putExtra("VENUE_NAME", venue.getName());
            startActivity(intent);
        });
        rvVenues.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddVenueDialog());

        loadVenues();

        return view;
    }

    private void loadVenues() {
        int ownerId = session.getUserId();
        repository.getExecutor().execute(() -> {
            List<Venue> venues = repository.getVenueDao().getVenuesByOwner(ownerId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    venueList.clear();
                    venueList.addAll(venues);
                    adapter.notifyDataSetChanged();

                    if (venueList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvVenues.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvVenues.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void showAddVenueDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_venue, null);
        EditText etName = dialogView.findViewById(R.id.et_dialog_venue_name);
        EditText etAddress = dialogView.findViewById(R.id.et_dialog_venue_address);
        EditText etDesc = dialogView.findViewById(R.id.et_dialog_venue_desc);

        new AlertDialog.Builder(requireContext())
                .setTitle("Daftarkan Venue Olahraga")
                .setView(dialogView)
                .setPositiveButton("Daftarkan", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String address = etAddress.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (name.isEmpty() || address.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(requireContext(), "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int ownerId = session.getUserId();
                    repository.getExecutor().execute(() -> {
                        Venue newVenue = new Venue(ownerId, name, address, desc, "venue_placeholder", "PENDING");
                        repository.getVenueDao().insertVenue(newVenue);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Venue berhasil diajukan! Menunggu persetujuan Admin.", Toast.LENGTH_LONG).show();
                                loadVenues();
                            });
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // Embedded Adapter specifically for Owner view
    public static class OwnerVenueAdapter extends RecyclerView.Adapter<OwnerVenueAdapter.OwnerVenueViewHolder> {
        private final List<Venue> venues;
        private final OnManageCourtsListener listener;

        public interface OnManageCourtsListener {
            void onManageCourts(Venue venue);
        }

        public OwnerVenueAdapter(List<Venue> venues, OnManageCourtsListener listener) {
            this.venues = venues;
            this.listener = listener;
        }

        @NonNull
        @Override
        public OwnerVenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_venue, parent, false);
            return new OwnerVenueViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OwnerVenueViewHolder holder, int position) {
            Venue venue = venues.get(position);
            holder.bind(venue, listener);
        }

        @Override
        public int getItemCount() {
            return venues.size();
        }

        public static class OwnerVenueViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvAddress, tvDesc, tvStatus;
            private final View statusBadge;
            private final View layoutActions;
            private final Button btnManage;
            private final android.widget.ImageView imgVenue;

            public OwnerVenueViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_venue_name);
                tvAddress = itemView.findViewById(R.id.tv_item_venue_address);
                tvDesc = itemView.findViewById(R.id.tv_item_venue_desc);
                tvStatus = itemView.findViewById(R.id.tv_item_venue_status);
                statusBadge = itemView.findViewById(R.id.layout_venue_status_badge);
                layoutActions = itemView.findViewById(R.id.layout_owner_venue_actions);
                btnManage = itemView.findViewById(R.id.btn_manage_courts);
                imgVenue = itemView.findViewById(R.id.img_item_venue);
            }

            public void bind(Venue venue, OnManageCourtsListener listener) {
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
                
                statusBadge.setVisibility(View.VISIBLE);
                tvStatus.setText(venue.getStatus());

                if ("APPROVED".equals(venue.getStatus())) {
                    statusBadge.setBackgroundResource(R.drawable.bg_tag_confirmed);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_confirmed));
                    layoutActions.setVisibility(View.VISIBLE);
                    btnManage.setOnClickListener(v -> listener.onManageCourts(venue));
                } else if ("PENDING".equals(venue.getStatus())) {
                    statusBadge.setBackgroundResource(R.drawable.bg_tag_pending);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                    layoutActions.setVisibility(View.GONE);
                } else {
                    statusBadge.setBackgroundResource(R.drawable.bg_tag_cancelled);
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_cancelled));
                    layoutActions.setVisibility(View.GONE);
                }
            }
        }
    }
}
