package com.example.clapandwhistledetector.fragments;

import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clapandwhistledetector.activities.FileSelectActivity;
import com.example.clapandwhistledetector.adapters.FileListAdapter;
import com.example.clapandwhistledetector.databinding.FragmentFileListBinding;
import com.example.clapandwhistledetector.model.FileModel;
import com.example.clapandwhistledetector.util.AppExecutors;
import com.example.clapandwhistledetector.util.PreferenceUtil;
import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileListFragment extends Fragment implements FileListAdapter.ClickListener {

    PreferenceUtil prefUtil;
    FragmentFileListBinding binding;
    FileListAdapter fileListAdapter;
    List<FileModel> fileList;
    MediaPlayer mediaPlayer;
    Ringtone ringtone;
    AppExecutors appExecutors;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFileListBinding.inflate(inflater, container, false);
        requireActivity().setActionBar(binding.toolbar);
        requireActivity().getActionBar().setTitle("Device Tones");
        // Display application icon in the toolbar
        requireActivity().getActionBar().setDisplayShowHomeEnabled(true);
        requireActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> requireActivity().onBackPressed());
        prefUtil = new PreferenceUtil(getActivity());
        appExecutors = AppExecutors.getInstance();
        fileList = new ArrayList<>();
        mediaPlayer = new MediaPlayer();
        fileListAdapter = new FileListAdapter(this, fileList);
        getFileListFromStorage();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recyclerView.addItemDecoration(new MaterialDividerItemDecoration(getActivity(), RecyclerView.VERTICAL));
        binding.recyclerView.setAdapter(fileListAdapter);
        return binding.getRoot();
    }

    public void getFileListFromStorage() {
        binding.progressBar.setVisibility(View.VISIBLE);
        appExecutors.diskIO().execute(
                () -> {
                    String uri = prefUtil.readString(FileSelectActivity.SELECTED_FILE_URI, "");
                    List<FileModel> temp = new ArrayList<>();
                    RingtoneManager manager = new RingtoneManager(getActivity());
                    manager.setType(RingtoneManager.TYPE_ALL);
                    Cursor cursor = manager.getCursor();
                    Log.d("TAG", "listRingtones: 2" + uri);
                    while (cursor.moveToNext()) {
                        String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                        Uri ringtoneURI = manager.getRingtoneUri(cursor.getPosition());
                        if (uri.length() != 0 && Uri.parse(uri).equals(ringtoneURI)) {
                            Log.d("TAG", "listRingtones: 1" + uri);
                            temp.add(new FileModel(title, ringtoneURI, true));
                        } else temp.add(new FileModel(title, ringtoneURI, false));
                        Log.d("TAG", "listRingtones: " + title);
                    }
                    ;
                    fileList.clear();
                    fileList.addAll(temp);
                    appExecutors.mainThread().execute(() -> {
                                fileListAdapter.notifyDataSetChanged();
                                binding.progressBar.setVisibility(View.GONE);
                            }
                    );
                }
        );
    }

    @Override
    public void onClick(int adapterPosition) {
        if (ringtone != null) ringtone.stop();

        Uri uri = fileList.get(adapterPosition).getUri();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        if (uri != null) {
            if (uri.toString().length() > 0) {
                ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                ringtone.setAudioAttributes(attributes);
                if (ringtone != null) {
                    ringtone.play();
                }
            }
        }
        Log.d("TAG", "onClick: " + Objects.requireNonNull(uri).getQueryParameter("title"));
        String posString = prefUtil.readString(FileSelectActivity.SELECTED_FILE_POSITION, "");

        prefUtil.saveString(FileSelectActivity.SELECTED_FILE_URI, uri.toString());
        prefUtil.saveString(FileSelectActivity.SELECTED_FILE_POSITION, String.valueOf(adapterPosition));
        fileList.get(adapterPosition).setActive(true);
        fileListAdapter.notifyItemChanged(adapterPosition);

        int position = -1;
        if (posString.length() != 0) {
            position = Integer.parseInt(posString);
        }
        if (position >= 0) {
            fileList.get(position).setActive(false);
            fileListAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
    }
}