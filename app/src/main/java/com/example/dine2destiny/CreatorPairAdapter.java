package com.example.dine2destiny;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class CreatorPairAdapter extends ArrayAdapter<Pair<Pair<String, String>, Pair<Integer, String>>> {

    public CreatorPairAdapter(@NonNull Context context, List<Pair<Pair<String, String>, Pair<Integer, String>>> creatorData) {
        super(context, 0, creatorData);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.creator_item, parent, false);
        }

        TextView creatorNameTextView = view.findViewById(R.id.creatorNameTextView);
        TextView creatorName2TextView = view.findViewById(R.id.creatorName2TextView);
        ImageView verifiedTick = view.findViewById(R.id.verifiedIcon);
        ImageView imageViewProfile = view.findViewById(R.id.imageViewProfile);

        Pair<Pair<String, String>, Pair<Integer, String>> data = getItem(position);
        if (data != null) {
            String creatorName = data.first.first;
            String creatorName2 = data.first.second;
            int verificationStatus = data.second.first;
            String profileImageUrl = data.second.second;

            creatorNameTextView.setText(creatorName);
            creatorName2TextView.setText(creatorName2);

            // Show the verified tick if verificationStatus is 1
            if (verificationStatus == 1) {
                verifiedTick.setVisibility(View.VISIBLE);
            } else {
                verifiedTick.setVisibility(View.GONE);
            }

            // Load profile image using Glide
            Glide.with(getContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .into(imageViewProfile);
        }

        return view;
    }
}
