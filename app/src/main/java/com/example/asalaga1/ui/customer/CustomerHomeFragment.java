package com.example.asalaga1.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asalaga1.R;
import com.example.asalaga1.database.AppRepository;
import com.example.asalaga1.model.Venue;
import com.example.asalaga1.ui.ProfileActivity;
import com.bumptech.glide.Glide;
import com.example.asalaga1.util.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class CustomerHomeFragment extends Fragment {

    private TextView tvUserName;
    private EditText etSearch;
    private RecyclerView rvVenues;
    private TextView tvEmptyState;
    private ImageButton btnProfile;
    private AppRepository repository;
    private List<Venue> allVenues = new ArrayList<>();
    private List<Venue> filteredVenues = new ArrayList<>();
    private VenueAdapter adapter;
    private String selectedCategory = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        repository = new AppRepository(requireContext());
        SessionManager session = new SessionManager(requireContext());

        tvUserName = view.findViewById(R.id.tv_home_user_name);
        etSearch = view.findViewById(R.id.et_home_search);
        rvVenues = view.findViewById(R.id.rv_home_venues);
        tvEmptyState = view.findViewById(R.id.tv_home_empty_state);
        btnProfile = view.findViewById(R.id.btn_customer_profile);

        tvUserName.setText(session.getUserName());

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileActivity.class));
        });

        rvVenues.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VenueAdapter(filteredVenues, venue -> {
            Intent intent = new Intent(requireActivity(), VenueDetailsActivity.class);
            intent.putExtra("VENUE_ID", venue.getId());
            startActivity(intent);
        });
        rvVenues.setAdapter(adapter);

        setupCategories(view);
        setupSearch();
        loadVenues();

        return view;
    }

    private void setupCategories(View view) {
        TextView btnFutsal = view.findViewById(R.id.btn_cat_futsal);
        TextView btnBadminton = view.findViewById(R.id.btn_cat_badminton);
        TextView btnBasketball = view.findViewById(R.id.btn_cat_basketball);
        TextView btnTennis = view.findViewById(R.id.btn_cat_tennis);

        View.OnClickListener listener = v -> {
            int id = v.getId();
            String clickedCat = "";
            if (id == R.id.btn_cat_futsal) clickedCat = "Futsal";
            else if (id == R.id.btn_cat_badminton) clickedCat = "Badminton";
            else if (id == R.id.btn_cat_basketball) clickedCat = "Basketball";
            else if (id == R.id.btn_cat_tennis) clickedCat = "Tennis";

            // Reset backgrounds
            btnFutsal.setBackgroundResource(R.drawable.bg_rounded_card);
            btnBadminton.setBackgroundResource(R.drawable.bg_rounded_card);
            btnBasketball.setBackgroundResource(R.drawable.bg_rounded_card);
            btnTennis.setBackgroundResource(R.drawable.bg_rounded_card);

            if (selectedCategory.equals(clickedCat)) {
                // Deselect category
                selectedCategory = "";
            } else {
                selectedCategory = clickedCat;
                v.setBackgroundResource(R.drawable.bg_tag_confirmed);
            }
            loadVenues();
        };

        btnFutsal.setOnClickListener(listener);
        btnBadminton.setOnClickListener(listener);
        btnBasketball.setOnClickListener(listener);
        btnTennis.setOnClickListener(listener);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadVenues() {
        repository.getExecutor().execute(() -> {
            List<Venue> venues;
            if (selectedCategory.isEmpty()) {
                venues = repository.getVenueDao().getAllApprovedVenues();
            } else {
                venues = repository.getVenueDao().getApprovedVenuesBySportType(selectedCategory);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allVenues.clear();
                    allVenues.addAll(venues);
                    filterData();
                });
            }
        });
    }

    private void filterData() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        filteredVenues.clear();

        for (Venue v : allVenues) {
            boolean matchesSearch = v.getName().toLowerCase().contains(query);
            if (matchesSearch) {
                filteredVenues.add(v);
            }
        }

        adapter.notifyDataSetChanged();
        if (filteredVenues.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvVenues.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvVenues.setVisibility(View.VISIBLE);
        }
    }

    // Inside Class VenueAdapter
    public static class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.VenueViewHolder> {
        private final List<Venue> venues;
        private final OnVenueClickListener listener;

        public interface OnVenueClickListener {
            void onVenueClick(Venue venue);
        }

        public VenueAdapter(List<Venue> venues, OnVenueClickListener listener) {
            this.venues = venues;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_venue, parent, false);
            return new VenueViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VenueViewHolder holder, int position) {
            Venue venue = venues.get(position);
            holder.bind(venue, listener);
        }

        @Override
        public int getItemCount() {
            return venues.size();
        }

        public static class VenueViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName, tvAddress, tvDesc;
            private final android.widget.ImageView imgVenue;

            public VenueViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_venue_name);
                tvAddress = itemView.findViewById(R.id.tv_item_venue_address);
                tvDesc = itemView.findViewById(R.id.tv_item_venue_desc);
                imgVenue = itemView.findViewById(R.id.img_item_venue);
            }

            public void bind(Venue venue, OnVenueClickListener listener) {
                tvName.setText(venue.getName());
                tvAddress.setText("📍 " + venue.getAddress());
                tvDesc.setText(venue.getDescription());

                String imgUrl = com.example.asalaga1.util.ImageHelper.getImageUrl(venue.getImagePath());

                Glide.with(itemView.getContext())
                     .load(imgUrl)
                     .placeholder(R.drawable.bg_gradient_blue)
                     .error(R.drawable.bg_gradient_blue)
                     .into(imgVenue);

                itemView.setOnClickListener(v -> listener.onVenueClick(venue));
            }
        }
    }
}
