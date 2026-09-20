package com.p.l;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GMapActivity extends AppCompatActivity implements OnMapReadyCallback {
	
	private static final int PICK_FILE = 101;
	
	private GoogleMap mMap;
	private List<LatLng> routePoints = new ArrayList<>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gmap);
		
		SupportMapFragment mapFragment =
		(SupportMapFragment) getSupportFragmentManager()
		.findFragmentById(R.id.map);
		
		mapFragment.getMapAsync(this);
		
		// Auto open file picker
		pickJsonFile();
	}
	
	// ================= FILE PICKER =================
	private void pickJsonFile() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType("application/json");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, PICK_FILE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
			Uri uri = data.getData();
			loadJsonFromUri(uri);
		}
	}
	
	// ================= LOAD JSON SAFELY =================
	private void loadJsonFromUri(Uri uri) {
		try {
			InputStream inputStream = getContentResolver().openInputStream(uri);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			
			StringBuilder sb = new StringBuilder();
			String line;
			
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			
			String json = sb.toString();
			
			parsePoints(json);
			
			drawRoute();
			
			} catch (Exception e) {
			Toast.makeText(this, "File read failed", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}
	
	// ================= SAFE PARSER =================
	private void parsePoints(String json) {
		routePoints.clear();
		
		try {
			Gson gson = new Gson();
			
			TrackData track = gson.fromJson(json, TrackData.class);
			
			if (track == null || track.points == null) {
				Toast.makeText(this, "No valid data", Toast.LENGTH_SHORT).show();
				return;
			}
			
			for (TrackPoint p : track.points) {
				if (p == null) continue;
				
				routePoints.add(new LatLng(p.lat, p.lng));
			}
			
			Toast.makeText(this,
			"Loaded points: " + routePoints.size(),
			Toast.LENGTH_SHORT).show();
			
			} catch (Exception e) {
			Toast.makeText(this,
			"Partial data loaded (some points skipped)",
			Toast.LENGTH_SHORT).show();
		}
	}
	
	// ================= MAP READY =================
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
	}
	
	// ================= DRAW ROUTE =================
	private void drawRoute() {
		if (mMap == null || routePoints.isEmpty()) return;
		
		mMap.clear();
		
		PolylineOptions polyline = new PolylineOptions()
		.addAll(routePoints)
		.width(8f)
		.color(Color.BLUE);
		
		mMap.addPolyline(polyline);
		
		// Start marker
		mMap.addMarker(new MarkerOptions()
		.position(routePoints.get(0))
		.title("Start"));
		
		// End marker
		mMap.addMarker(new MarkerOptions()
		.position(routePoints.get(routePoints.size() - 1))
		.title("End"));
		
		// Camera focus
		mMap.moveCamera(
		CameraUpdateFactory.newLatLngZoom(routePoints.get(0), 15f)
		);
	}
}