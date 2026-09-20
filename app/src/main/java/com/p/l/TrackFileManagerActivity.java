package com.p.l;







import android.content.Intent;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TrackFileManagerActivity extends AppCompatActivity {
	
	private boolean selectionMode = false;
	private List<Boolean> selected = new ArrayList<>();
	
	private Button btnSelectAll, btnDelete, btnExport;
	
	
	//--
	private ListView listView;
	private File trackDir;


	private List<File> files = new ArrayList<>();
	private ArrayAdapter<String> adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(createLayout());
		
		trackDir = new File(getExternalFilesDir(null), "tracks");
		
		loadFiles();
		
		listView.setOnItemClickListener((p, v, pos, id) ->
		showOptions(files.get(pos))
		);
	}
	
	
	
	
	///
	
	
	
	
	private void importFile() {
		
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType("application/json");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		
		startActivityForResult(intent, 101);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == 101 && resultCode == RESULT_OK) {
			
			if (data != null && data.getData() != null) {
				
				try {
					copyUriToInternal(data.getData());
					toast("File Imported");
					loadFiles();
					
					} catch (Exception e) {
					toast("Import failed");
				}
			}
		}
	}
	
	
	private void copyUriToInternal(android.net.Uri uri) throws Exception {
		
		InputStream in = getContentResolver().openInputStream(uri);
		
		if (!trackDir.exists()) trackDir.mkdirs();
		
		String fileName = "import_" + System.currentTimeMillis() + ".json";
		File outFile = new File(trackDir, fileName);
		
		OutputStream out = new FileOutputStream(outFile);
		
		byte[] buf = new byte[4096];
		int len;
		
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		
		in.close();
		out.close();
	}
	
	
	
	
	
	
	//___
	
	
	private void exitSelectionMode() {
		selectionMode = false;
		
		for (int i = 0; i < selected.size(); i++) {
			selected.set(i, false);
		}
		
		loadFiles();
		updateButtons();
	}
	
	
	private void deleteSelected() {
		new AlertDialog.Builder(this)
		.setTitle("Delete Selected Files?")
		.setPositiveButton("Delete", (d, w) -> {
			
			for (int i = files.size() - 1; i >= 0; i--) {
				if (selected.get(i)) {
					files.get(i).delete();
				}
			}
			
			toast("Deleted selected files");
			
			exitSelectionMode(); // 🔥 IMPORTANT
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	
	private void exportSelected() {
		try {
			File destDir = new File(
			Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS),
			"MyTracks"
			);
			
			if (!destDir.exists()) destDir.mkdirs();
			
			for (int i = 0; i < files.size(); i++) {
				if (selected.get(i)) {
					File src = files.get(i);
					File dest = new File(destDir, src.getName());
					copy(src, dest);
				}
			}
			
			toast("Export completed");
			
			exitSelectionMode(); // 🔥 IMPORTANT
			
			} catch (Exception e) {
			toast("Export failed");
		}
	}
	
	
	private void updateButtons() {
		if (!selectionMode) {
			btnSelectAll.setVisibility(View.GONE);
			btnDelete.setVisibility(View.GONE);
			btnExport.setVisibility(View.GONE);
			return;
		}
		
		btnSelectAll.setVisibility(View.VISIBLE);
		btnDelete.setVisibility(View.VISIBLE);
		btnExport.setVisibility(View.VISIBLE);
		
		boolean anySelected = false;
		boolean allSelected = selected.size() > 0;
		
		for (boolean b : selected) {
			if (b) anySelected = true;
			else allSelected = false;
		}
		
		btnDelete.setEnabled(anySelected);
		btnExport.setEnabled(anySelected);
		
		btnSelectAll.setText(allSelected ? "Unselect All" : "Select All");
	}
	

	
	//+++
	
	/*
	private void updateButtons() {
		if (!selectionMode) {
			btnSelectAll.setVisibility(View.GONE);
			btnDelete.setVisibility(View.GONE);
			btnExport.setVisibility(View.GONE);
			return;
		}
		
		btnSelectAll.setVisibility(View.VISIBLE);
		btnDelete.setVisibility(View.VISIBLE);
		btnExport.setVisibility(View.VISIBLE);
		
		boolean anySelected = false;
		boolean allSelected = true;
		
		for (boolean b : selected) {
			if (b) anySelected = true;
			else allSelected = false;
		}
		
		// 🔥 Correct toggle logic
		if (allSelected && selected.size() > 0) {
			btnSelectAll.setText("Unselect All");
			} else {
			btnSelectAll.setText("Select All");
		}
		
		// Delete/Export only when at least 1 selected
		btnDelete.setEnabled(anySelected);
		btnExport.setEnabled(anySelected);
	}
	
	*/
	
	private void toggleSelectAll() {
		
		boolean allSelected = true;
		
		for (boolean b : selected) {
			if (!b) {
				allSelected = false;
				break;
			}
		}
		
		boolean newState = !allSelected;
		
		for (int i = 0; i < selected.size(); i++) {
			selected.set(i, newState);
			listView.setItemChecked(i, newState);
		}
		
		updateButtons();
	}
	
	@Override
	public void onBackPressed() {
		if (selectionMode) {
			selectionMode = false;
			
			// clear selection
			for (int i = 0; i < selected.size(); i++) {
				selected.set(i, false);
			}
			
			loadFiles();   // refresh UI
			updateButtons();
			return;
		}
		
		super.onBackPressed();
	}
	
	/*
	private void deleteSelected() {
		new AlertDialog.Builder(this)
		.setTitle("Delete Selected Files?")
		.setMessage("Selected files will be permanently deleted.")
		.setPositiveButton("Delete", (d, w) -> {
			
			for (int i = files.size() - 1; i >= 0; i--) {
				if (selected.get(i)) {
					files.get(i).delete();
				}
			}
			
			toast("Deleted selected files");
			selectionMode = false;
			loadFiles();
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	
	private void exportSelected() {
		try {
			File destDir = new File(
			Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS),
			"MyTracks"
			);
			
			if (!destDir.exists()) destDir.mkdirs();
			
			for (int i = 0; i < files.size(); i++) {
				if (selected.get(i)) {
					File src = files.get(i);
					File dest = new File(destDir, src.getName());
					copy(src, dest);
				}
			}
			
			toast("Export completed");
			
			selectionMode = false;
			loadFiles();
			
			} catch (Exception e) {
			toast("Export failed");
		}
	}
	
	*/
	
	private View createLayout() {
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		
		TextView title = new TextView(this);
		title.setText("Saved Tracks");
		title.setTextSize(20);
		title.setPadding(20, 20, 20, 20);
		
		LinearLayout actions = new LinearLayout(this);
		
		btnSelectAll = new Button(this);
		btnSelectAll.setText("Select All");
		btnSelectAll.setOnClickListener(v -> toggleSelectAll());
		
		btnDelete = new Button(this);
		btnDelete.setText("Delete");
		btnDelete.setOnClickListener(v -> deleteSelected());
		
		btnExport = new Button(this);
		btnExport.setText("Export");
		btnExport.setOnClickListener(v -> exportSelected());
		
	
		Button btnImport = new Button(this);
		btnImport.setText("Import");
		
		btnImport.setOnClickListener(v -> importFile());
		
		actions.addView(btnImport);
		
		
		actions.addView(btnSelectAll);
		actions.addView(btnDelete);
		actions.addView(btnExport);
		
		listView = new ListView(this);
		
		root.addView(title);
		root.addView(actions);
		root.addView(listView,
		new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT,
		LinearLayout.LayoutParams.MATCH_PARENT));
		
		updateButtons(); // 👈 initial hide
		
		return root;
	}
	
	
	
	
	private void loadFiles() {
		files.clear();
		selected.clear();
		List<String> names = new ArrayList<>();
		
		if (trackDir.exists()) {
			File[] arr = trackDir.listFiles((d, n) -> n.endsWith(".json"));
			if (arr != null) {
				for (File f : arr) {
					files.add(f);
					names.add(f.getName());
					selected.add(false);
				}
			}
		}
		
		int layout = selectionMode
		? android.R.layout.simple_list_item_multiple_choice
		: android.R.layout.simple_list_item_1;
		
		adapter = new ArrayAdapter<>(this, layout, names);
		listView.setAdapter(adapter);
		
		listView.setChoiceMode(selectionMode
		? ListView.CHOICE_MODE_MULTIPLE
		: ListView.CHOICE_MODE_NONE);
		
		// CLICK
		listView.setOnItemClickListener((p, v, pos, id) -> {
			if (selectionMode) {
				selected.set(pos, listView.isItemChecked(pos));
				updateButtons();
				} else {
				showOptions(files.get(pos)); // 👈 old behavior
			}
		});
		
		// LONG CLICK
		listView.setOnItemLongClickListener((p, v, pos, id) -> {
			if (!selectionMode) {
				selectionMode = true;
				loadFiles();
				
				listView.setItemChecked(pos, true);
				selected.set(pos, true);
				
				updateButtons();
			}
			return true;
		});
	}
	
	
	
	///
	
	// ================= LOAD FILES =================
	
	/*
	private void loadFiles() {
		files.clear();
		List<String> names = new ArrayList<>();
		
		if (trackDir.exists()) {
			File[] arr = trackDir.listFiles((d, n) -> n.endsWith(".json"));
			if (arr != null) {
				for (File f : arr) {
					files.add(f);
					names.add(f.getName());
				}
			}
		}
		
		adapter = new ArrayAdapter<>(this,
		android.R.layout.simple_list_item_1, names);
		listView.setAdapter(adapter);
	}
	
	*/
	
	
	// ================= OPTIONS =================
	
	
	
	private void showOptions(File file) {
		String[] opts = {
			"▶ Open",
			"📄 Copy to Downloads",
			"✏ Rename",
			"🗑 Delete"
		};
		
		new AlertDialog.Builder(this)
		.setTitle(file.getName())
		.setItems(opts, (d, i) -> {
			if (i == 0) openFile(file);
			if (i == 1) copyFile(file);
			if (i == 2) renameFile(file);
			if (i == 3) deleteFile(file);
		})
		.show();
	}
	
	
	
	private void openFile(File selectedFile) {
		Intent i = new Intent(this, SavedMapActivity.class);
		i.putExtra("trackFile", selectedFile.getAbsolutePath());
		startActivity(i);
	}
	
	
	private void DEshowOptions(File file) {
		String[] opts = {
			"📄 Copy to Downloads",
			"✏ Rename",
			"🗑 Delete"
		};
		
		new AlertDialog.Builder(this)
		.setTitle(file.getName())
		.setItems(opts, (d, i) -> {
			if (i == 0) copyFile(file);
			if (i == 1) renameFile(file);
			if (i == 2) deleteFile(file);
		})
		.show();
	}
	
	// ================= COPY =================
	
	
	/*	File selectedFile = files[which];
	
	Intent i = new Intent(this, SavedMapActivity.class);
	i.putExtra("trackFile", selectedFile.getAbsolutePath());
	startActivity(i);.
	*/
	
	private void copyFile(File src) {
		try {
			File destDir = new File(
			Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS
			),
			"MyTracks"
			);
			
			if (!destDir.exists()) destDir.mkdirs();
			
			File dest = new File(destDir, src.getName());
			copy(src, dest);
			
			toast("Copied to Downloads/MyTracks");
			
			} catch (Exception e) {
			toast("Copy failed");
		}
	}
	
	private void copy(File a, File b) throws Exception {
		InputStream in = new FileInputStream(a);
		OutputStream out = new FileOutputStream(b);
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
		in.close();
		out.close();
	}
	
	// ================= RENAME =================
	
	private void renameFile(File file) {
		EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setText(file.getName());
		
		new AlertDialog.Builder(this)
		.setTitle("Rename File")
		.setView(input)
		.setPositiveButton("Rename", (d, w) -> {
			File newFile = new File(trackDir, input.getText().toString());
			if (file.renameTo(newFile)) {
				toast("Renamed");
				loadFiles();
			} else toast("Rename failed");
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// ================= DELETE =================
	
	private void deleteFile(File file) {
		new AlertDialog.Builder(this)
		.setTitle("Delete?")
		.setMessage(file.getName())
		.setPositiveButton("Delete", (d, w) -> {
			if (file.delete()) {
				toast("Deleted");
				loadFiles();
			} else toast("Delete failed");
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// ================= UI =================
	
	/*
	
	
	private View createLayout() {
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		
		TextView title = new TextView(this);
		title.setText("Saved Tracks");
		title.setTextSize(20);
		title.setPadding(20, 20, 20, 20);
		
		listView = new ListView(this);
		
		root.addView(title);
		root.addView(listView,
		new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT,
		LinearLayout.LayoutParams.MATCH_PARENT));
		
		return root;
	}
	
*/
	
	
	
	
	private void toast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}
	
	
	
	
	
}