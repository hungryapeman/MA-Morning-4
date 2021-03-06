package com.example.note.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.note.R;
import com.example.note.adapter.NoteOverviewAdapter;
import com.example.note.controller.NoteManager;
import com.example.note.dialogs.FilterDialog;
import com.example.note.model.Note;
import com.example.note.model.NoteComparator;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NoteOverviewFragment extends Fragment {

    private NoteOverviewAdapter mAdapter;
    private List<Note> mAllNotes;
    private List<Note> mNotesCopy;
    private NoteManager mNoteManager;
    private Button mRemoveFiltersBtn;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.overview_fragment, container, false);
        Context context = inflater.getContext();

        setHasOptionsMenu(true);

        mNoteManager = NoteManager.getInstance();
        mAllNotes = new ArrayList<>(mNoteManager.getNotes());
        mNotesCopy = new ArrayList<>(mAllNotes);

        ListView listView = view.findViewById(R.id.overview_list);
        mAdapter = new NoteOverviewAdapter(context, R.layout.overview_list_item, mAllNotes, NoteOverviewFragment.this);
        mNoteManager.addObserver(mAdapter);
        listView.setAdapter(mAdapter);

        mRemoveFiltersBtn = view.findViewById(R.id.removeFilterBtn);
        mRemoveFiltersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAllNotes.clear();
                mAllNotes.addAll(mNotesCopy);
                mRemoveFiltersBtn.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
            }
        });

        FloatingActionButton new_note_btn = view.findViewById(R.id.overview_new_note_btn);
        new_note_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAllNotes.clear();
                mAllNotes.addAll(mNotesCopy);
                mRemoveFiltersBtn.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
                Bundle bundle = new Bundle();
                bundle.putInt("note_id", -1);
                NavHostFragment.findNavController(NoteOverviewFragment.this)
                        .navigate(R.id.action_overview_to_detail_fragment, bundle);
            }
        });

        Button show_done_notes = view.findViewById(R.id.show_done_notes);
        show_done_notes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(NoteOverviewFragment.this)
                        .navigate(R.id.action_overview_to_done_overview_fragment);
            }
        });

        sortNotesList("Date");
        mAdapter.notifyDataSetChanged();
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if(visible && mAdapter != null) {
            mAllNotes = mNoteManager.getNotes();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort) {
            showSortDialog();
            return true;
        }
        else if (id == R.id.filter) {
            showFilterDialog();
            return true;
        } else if (id == R.id.action_empty_notes) {
            new AlertDialog.Builder(this.getContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Empty Notes")
                    .setMessage("Are you sure you want to delete all stored notes?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mNoteManager.emptyNotes();
                            updateNotes();
                        }
                    })
                    .show();
            return true;
        } else if (id == R.id.action_export_notes) {
            int result = mNoteManager.exportNotes(this.getContext());
            if (result == 0) {
                Toast.makeText(this.getContext(), "Stored Notes to Device", Toast.LENGTH_LONG).show();
                mAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this.getContext(), "No Notes to export!", Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_import_notes) {
            File dir = this.getContext().getFilesDir();

            final String[] files = dir.list();
            Arrays.sort(files, Collections.reverseOrder());

            AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
            builder.setTitle("Select File");
            builder.setItems(files, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String fileName = files[i];
                    for(File file : getActivity().getApplicationContext().getFilesDir().listFiles()) {
                        if (file.getName().equals(fileName)) {
                            mNoteManager.importNotes(file);
                            updateNotes();
                        }
                    }
                }
            }).show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateNotes() {
        List<Note> newNotes = mNoteManager.getNotes();
        mAllNotes.clear();
        mAllNotes.addAll(newNotes);
        sortNotesList("Date");
        mAdapter.notifyDataSetChanged();
    }

    public void sortNotesList(String categorie) {
        if(categorie.equals("Title") || categorie.equals("Date"))
            Collections.sort(mAllNotes, new NoteComparator(categorie));
        else if(categorie.equals("Title desc") || categorie.equals("Date desc"))
            Collections.sort(mAllNotes, new NoteComparator(categorie).reversed());
        mAdapter.notifyDataSetChanged();
    }

    private void showSortDialog() {
        final CharSequence[] items = {"Title", "Title desc", "Date", "Date desc"};
        new AlertDialog.Builder(NoteOverviewFragment.super.getContext())
                .setTitle("Sort Notes")
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListView lw = ((AlertDialog)dialog).getListView();
                        int i = lw.getCheckedItemPosition();
                        sortNotesList(items[i].toString());                    }
                })
                .show();
    }

    private void showFilterDialog() {
        FilterDialog dialog = new FilterDialog(this);
        dialog.setTitle(R.string.filter);
        dialog.show();
    }

    public void filterByTagAndDate(String tag, Long from, Long to) {
        mNotesCopy = new ArrayList<>(mAllNotes);
        final Long fromVal = (from == null ? Long.MIN_VALUE : from);
        final Long toVal = (to == null ? Long.MAX_VALUE : to);
        mAllNotes.removeIf(note -> (!note.getTag().equals(tag) || note.getLastModification() > toVal || note.getLastModification() < fromVal));
        mRemoveFiltersBtn.setVisibility(View.VISIBLE);
        mAdapter.notifyDataSetChanged();
    }

    public List<Note> getAllNotes() {
        return mAllNotes;
    }
}