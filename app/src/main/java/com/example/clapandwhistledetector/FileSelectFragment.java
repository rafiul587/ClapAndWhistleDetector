package com.example.clapandwhistledetector;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.example.clapandwhistledetector.databinding.FragmentFileListBinding;
import com.example.clapandwhistledetector.databinding.FragmentFileSelectBinding;

public class FileSelectFragment extends Fragment {

    FragmentFileSelectBinding binding;
    PreferenceUtil prefUtil;
    Uri uri;

    public FileSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFileSelectBinding.inflate(inflater, container, false);
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
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TAG", "onResume: ");
        String stringUri = prefUtil.readString(FileSelectActivity.SELECTED_FILE_URI, "");
        if(stringUri.length() != 0) {
            uri = Uri.parse(stringUri);
            binding.selectedFileName.setText(uri.getQueryParameter("title"));
            binding.radioButton.setChecked(false);
        }else {
            binding.selectedFileName.setText("None");
            binding.radioButton.setChecked(true);
        }
        binding.radioButton.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                prefUtil.saveString(FileSelectActivity.SELECTED_FILE_URI, "");
                binding.selectedFileName.setText("None");

            }
        });
    }
}