package com.example.clapandwhistledetector.fragments;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.clapandwhistledetector.R;
import com.example.clapandwhistledetector.activities.FileSelectActivity;
import com.example.clapandwhistledetector.databinding.FragmentFileSelectBinding;
import com.example.clapandwhistledetector.fragments.FileListFragment;
import com.example.clapandwhistledetector.util.PreferenceUtil;

public class FileSelectFragment extends Fragment {

    FragmentFileSelectBinding binding;
    PreferenceUtil prefUtil;
    Uri uri;
    MediaPlayer mediaPlayer;

    public FileSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFileSelectBinding.inflate(inflater, container, false);
        requireActivity().setActionBar(binding.toolbar);
        requireActivity().getActionBar().setTitle("Select Tone");

        requireActivity().getActionBar().setDisplayShowHomeEnabled(true);
        requireActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> requireActivity().onBackPressed());
        prefUtil = new PreferenceUtil(getActivity());

        binding.importLayout.setOnClickListener(view -> {
            if (savedInstanceState == null) {
                getParentFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container_view, new FileListFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
        binding.defaultToneList.setOnClickListener(view -> {
            if(!binding.radioButton.isChecked()) {
                binding.radioButton.setChecked(true);
                mediaPlayer = MediaPlayer.create(getActivity(), R.raw.alarm);
                mediaPlayer.start();
                prefUtil.saveString(FileSelectActivity.SELECTED_FILE_URI, "");
                prefUtil.saveString(FileSelectActivity.SELECTED_FILE_NAME, "");
                binding.selectedFileName.setText("None");
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        String name = prefUtil.readString(FileSelectActivity.SELECTED_FILE_NAME, "");
        if(name.length() != 0) {
            binding.selectedFileName.setText(name);
            binding.radioButton.setChecked(false);
        }else {
            binding.selectedFileName.setText("None");
            binding.radioButton.setChecked(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mediaPlayer != null){
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }
}