package com.p.l;











import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import com.google.gson.Gson;
import android.widget.Button;
import java.io.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class SavedMapActivity extends AppCompatActivity {
	
	private GraphView graphView;
	private double totalDistance = 0.0; // meters
	private String currentTrackPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.s);
		
		graphView = findViewById(R.id.graphView);
		
		// Focus button only - export button removed completely
		FloatingActionButton fab = findViewById(R.id.fabFocus);
		fab.setOnClickListener(v -> graphView.focusLast());
		
		// Get track file path
		currentTrackPath = getIntent().getStringExtra("trackFile");
		if (currentTrackPath == null) {
			Toast.makeText(this, "Track file not found", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		loadAndShowTrack(currentTrackPath);
	}
	
	// ================= LOAD + DRAW TRACK =================
	
	private void DETloadAndShowTrack(String path) {
		try {
			File file = new File(path);
			
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();
			
			String json = new String(data);
			
			TrackData track = new Gson().fromJson(json, TrackData.class);
			
			if (track == null || track.points == null || track.points.isEmpty()) {
				Toast.makeText(this, path + " Empty track file", Toast.LENGTH_LONG).show();
				return;
			}
			
			// Reset before rebuild
			totalDistance = 0;
			
			graphView.clear();
			
			for (TrackPoint p : track.points) {
				// Rebuild total distance
				totalDistance += p.segmentDistance;
				//distanceFromLast;
				
				graphView.addPoint(
				p.lng,
				p.lat,
				p.speed,
				p.segmentDistance,
			//	p.distanceFromLast,  // segment distance
				totalDistance,       // total till this point
				p.time
				);
			}
			
			// Focus on last point
			graphView.post(() -> graphView.focusLast());
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Failed to load track",
			
		
			Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private void exportGraph() {
		try {
			Bitmap bitmap = graphView.exportBitmap();
			
			File dir = Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS
			);
			
			if (!dir.exists()) dir.mkdirs();
			
			File file = new File(dir,
			"graph_" + System.currentTimeMillis() + ".png");
			
			FileOutputStream fos = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.flush();
			fos.close();
			
			Toast.makeText(this,
			"Graph saved in Downloads",
			Toast.LENGTH_LONG).show();
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Export failed: " + e.getMessage(),
			Toast.LENGTH_SHORT).show();
		}
	}
	
	// ================= TRACK FILE MANAGEMENT =================
	
	// Method to show file options menu
	private void showTrackOptions() {
		File trackFile = new File(currentTrackPath);
		if (!trackFile.exists()) {
			Toast.makeText(this, "Track file not found", Toast.LENGTH_SHORT).show();
			return;
		}
		
		String[] opts = {
			"📸 Save as Image",
			"📄 Copy to Downloads",
			"✏ Rename",
			"🗑 Delete",
			"ℹ File Info"
		};
		
		new AlertDialog.Builder(this)
		.setTitle("Track Options - " + trackFile.getName())
		.setItems(opts, (d, i) -> {
			if (i == 0) exportGraph();
			if (i == 1) copyTrackFile(trackFile);
			if (i == 2) renameTrackFile(trackFile);
			if (i == 3) deleteTrackFile(trackFile);
			if (i == 4) showFileInfo(trackFile);
		})
		.show();
	}
	
	// Copy track file to Downloads
	private void copyTrackFile(File src) {
		try {
			File destDir = new File(
			Environment.getExternalStoragePublicDirectory(
			Environment.DIRECTORY_DOWNLOADS
			),
			"MyTracks"
			);
			
			if (!destDir.exists()) destDir.mkdirs();
			
			File dest = new File(destDir, src.getName());
			
			// Handle duplicate filenames
			int counter = 1;
			String nameWithoutExt = src.getName().replace(".json", "");
			String ext = ".json";
			while (dest.exists()) {
				dest = new File(destDir, nameWithoutExt + "_" + counter + ext);
				counter++;
			}
			
			copy(src, dest);
			
			Toast.makeText(this,
			"Copied to Downloads/MyTracks/" + dest.getName(),
			Toast.LENGTH_LONG).show();
			
			} catch (Exception e) {
			Toast.makeText(this, "Copy failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
	
	// Rename track file
	private void renameTrackFile(File file) {
		EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		String oldName = file.getName();
		input.setText(oldName);
		input.setSelection(0, oldName.lastIndexOf(".json"));
		
		new AlertDialog.Builder(this)
		.setTitle("Rename Track File")
		.setView(input)
		.setPositiveButton("Rename", (d, w) -> {
			String newName = input.getText().toString();
			if (!newName.endsWith(".json")) {
				newName += ".json";
			}
			
			File parent = file.getParentFile();
			File newFile = new File(parent, newName);
			
			if (file.renameTo(newFile)) {
				currentTrackPath = newFile.getAbsolutePath();
				Toast.makeText(this, "Renamed to: " + newName, Toast.LENGTH_SHORT).show();
				} else {
				Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// Delete track file
	private void deleteTrackFile(File file) {
		new AlertDialog.Builder(this)
		.setTitle("Delete Track?")
		.setMessage("Are you sure you want to delete\n" + file.getName() + "?")
		.setPositiveButton("Delete", (d, w) -> {
			if (file.delete()) {
				Toast.makeText(this, "Track deleted", Toast.LENGTH_SHORT).show();
				finish();
				} else {
				Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}
	
	// Show file information
	private void showFileInfo(File file) {
		long fileSize = file.length();
		long lastModified = file.lastModified();
		
		String info = "Name: " + file.getName() + "\n" +
		"Size: " + formatFileSize(fileSize) + "\n" +
		"Modified: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		.format(new java.util.Date(lastModified)) + "\n" +
		"Path: " + file.getAbsolutePath();
		
		new AlertDialog.Builder(this)
		.setTitle("File Information")
		.setMessage(info)
		.setPositiveButton("OK", null)
		.show();
	}
	
	// Helper method to copy file
	private void copy(File a, File b) throws Exception {
		InputStream in = new FileInputStream(a);
		OutputStream out = new FileOutputStream(b);
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
		in.close();
		out.close();
	}
	
	// Helper method to format file size
	private String formatFileSize(long size) {
		if (size < 1024) return size + " B";
		if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
		return String.format("%.1f MB", size / (1024.0 * 1024.0));
	}
	
	// Menu in action bar
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		android.view.MenuItem item = menu.add("Options");
		item.setIcon(android.R.drawable.ic_menu_more);
		item.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
		item.setOnMenuItemClickListener(item1 -> {
			showTrackOptions();
			return true;
		});
		return super.onCreateOptionsMenu(menu);
	}
	
	
	
	
	
//	private
	public List<TrackPoint> extractPointsSafely(String json) {
		List<TrackPoint> list = new ArrayList<>();
		Gson gson = new Gson();
		
		int start = json.indexOf("[");
		int end = json.lastIndexOf("]");
		
		// agar ] missing hai to end ko last tak le jao
		if (end == -1) end = json.length();
		
		if (start == -1) return list;
		
		String arrayPart = json.substring(start + 1, end);
		
		int brace = 0;
		StringBuilder current = new StringBuilder();
		
		for (int i = 0; i < arrayPart.length(); i++) {
			char c = arrayPart.charAt(i);
			
			if (c == '{') {
				brace++;
			}
			
			if (brace > 0) {
				current.append(c);
			}
			
			if (c == '}') {
				brace--;
				
				if (brace == 0) {
					// ✅ ek complete object mil gaya
					try {
						TrackPoint p = gson.fromJson(current.toString(), TrackPoint.class);
						if (p != null) list.add(p);
						} catch (Exception e) {
						// ❌ ignore broken object
					}
					current.setLength(0);
				}
			}
		}
		
		return list;
	}
	
	private void loadAndShowTrack(String path) {
		try {
			File file = new File(path);
			
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();
			
			String json = new String(data);
			
			// 🔥 SAFE PARSE
			List<TrackPoint> points = extractPointsSafely(json);
			
			if (points.isEmpty()) {
				Toast.makeText(this, "No valid points found", Toast.LENGTH_LONG).show();
				return;
			}
			
			totalDistance = 0;
			graphView.clear();
			
			for (TrackPoint p : points) {
				
				if (p == null) continue;
				
				totalDistance += p.segmentDistance;
				
				graphView.addPoint(
				p.lng,
				p.lat,
				p.speed,
				p.segmentDistance,
				totalDistance,
				p.time
				);
			}
			
			graphView.post(() -> graphView.focusLast());
			
			Toast.makeText(this,
			"Loaded " + points.size() + " valid points",
			Toast.LENGTH_SHORT).show();
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Error loading file",
			Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	
}