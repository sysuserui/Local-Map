package com.p.l;








import android.content.Context;
import android.graphics.*;
import android.location.Location;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class GraphView extends View {
	
	// ================= DATA =================
	
	
	
	
	static class PointD {
		double x, y;              // lng, lat
		float speed;              // m/s
		
		double segmentDistance;   // previous point → this point
		double totalDistance;     // start → this point
		
		long time;                // millis
		
		PointD(double x, double y,
		float speed,
		double segmentDistance,
		double totalDistance,
		long time) {
			
			this.x = x;
			this.y = y;
			this.speed = speed;
			this.segmentDistance = segmentDistance;
			this.totalDistance = totalDistance;
			this.time = time;
		}
	}
	
	
	private final List<PointD> points = new ArrayList<>();
	private final List<PointF> screenPts = new ArrayList<>();
	
	// ================= PAINT =================
	
	private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint startPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint middlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint endPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	// ================= SCALE =================
	
	private double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
	private double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
	
	private double zoom = 1.0;
	private double offsetX = 0, offsetY = 0;
	
	// ================= FLAGS =================
	
	private boolean showArrows = true;
	private boolean showNumbers = true;
	
	// ================= TOUCH =================
	
	private float lastX, lastY;
	private final ScaleGestureDetector scaleDetector;
	
	private float arrowPhase = 0f;
	
	
	
	
	
	
	
	// ================= CONSTRUCTOR =================
	
	public GraphView(Context c, AttributeSet a) {
		super(c, a);
		
		linePaint.setColor(Color.BLUE);
		linePaint.setStrokeWidth(5);
		linePaint.setStyle(Paint.Style.STROKE);
		
		startPaint.setColor(Color.GREEN);
		middlePaint.setColor(Color.rgb(255, 165, 0));
		endPaint.setColor(Color.RED);
		
		arrowPaint.setColor(Color.BLUE);
		arrowPaint.setStrokeWidth(3);
		
		textPaint.setColor(Color.BLACK);
		textPaint.setTextSize(28);
		
		borderPaint.setColor(Color.LTGRAY);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(2);
		
		scaleDetector = new ScaleGestureDetector(c, new ScaleListener());
	}
	
	// ================= PUBLIC API =================
	
	
	
	
	public void addPoint(
	double lon,
	double lat,
	float speed,
	double segmentDistance,
	double totalDistance,
	long time
	) {
		
		points.add(new PointD(
		lon, lat,
		speed,
		segmentDistance,
		totalDistance,
		time
		));
		
		minX = Math.min(minX, lon);
		maxX = Math.max(maxX, lon);
		minY = Math.min(minY, lat);
		maxY = Math.max(maxY, lat);
		
		invalidate();
	}
	
	
	
	
	// ================= DRAW =================
	
	
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
		
		if (points.size() < 1) return;
		
		double rangeX = maxX - minX;
		double rangeY = maxY - minY;
		if (rangeX == 0) rangeX = 1e-6;
		if (rangeY == 0) rangeY = 1e-6;
		
		float sx = (float)((getWidth() - 100) / rangeX);
		float sy = (float)((getHeight() - 100) / rangeY);
		float scale = (float)(Math.min(sx, sy) * zoom);
		
		screenPts.clear();
		
		for (PointD p : points) {
			float x = 50 + (float)((p.x - minX) * scale) + (float)offsetX;
			float y = getHeight() - 50 - (float)((p.y - minY) * scale) + (float)offsetY;
			screenPts.add(new PointF(x, y));
		}
		
		//	/* ---- PATH ----
		if (screenPts.size() > 1) {
			Path path = new Path();
			path.moveTo(screenPts.get(0).x, screenPts.get(0).y);
			for (int i = 1; i < screenPts.size(); i++)
			path.lineTo(screenPts.get(i).x, screenPts.get(i).y);
			canvas.drawPath(path, linePaint);
		}
		
		//	/* ---- ARROWS ----
		if (showArrows && zoom > 1.5) {
			for (int i = 0; i < screenPts.size() - 1; i++) {
				drawArrow(canvas, screenPts.get(i), screenPts.get(i + 1));
			}
			arrowPhase += 2;
			postInvalidateOnAnimation();
		}
		
		//* ---- POINTS ----
		for (int i = 0; i < screenPts.size(); i++) {
			PointF p = screenPts.get(i);
			Paint paint = (i == 0) ? startPaint :
			(i == screenPts.size() - 1) ? endPaint :
			middlePaint;
			
			canvas.drawCircle(p.x, p.y,
			(i == 0 || i == screenPts.size() - 1) ? 12 : 7,
			paint);
			
			if (showNumbers && zoom > 2) {
				canvas.drawText(String.valueOf(i + 1),
				p.x + 10, p.y - 10, textPaint);
			}
		}
	}
	
	
	
	
	
	// ================= ARROW =================
	
	private void drawArrow(Canvas c, PointF a, PointF b) {
		float t = (arrowPhase % 100) / 100f;
		float x = a.x + (b.x - a.x) * t;
		float y = a.y + (b.y - a.y) * t;
		
		float angle = (float) Math.atan2(b.y - a.y, b.x - a.x);
		float len = 20;
		float ang = (float) Math.toRadians(30);
		
		c.drawLine(x, y,
		x - len * (float) Math.cos(angle - ang),
		y - len * (float) Math.sin(angle - ang),
		arrowPaint);
		
		c.drawLine(x, y,
		x - len * (float) Math.cos(angle + ang),
		y - len * (float) Math.sin(angle + ang),
		arrowPaint);
	}
	
	// ================= TOUCH =================
	
	
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		scaleDetector.onTouchEvent(e);
		
		if (!scaleDetector.isInProgress()) {
			if (e.getAction() == MotionEvent.ACTION_MOVE) {
				offsetX += e.getX() - lastX;
				offsetY += e.getY() - lastY;
				invalidate();
			}
		}
		
		if (e.getAction() == MotionEvent.ACTION_UP) {
			checkTap(e.getX(), e.getY());
		}
		
		lastX = e.getX();
		lastY = e.getY();
		return true;
	}
	
	
	
	private void checkTap(float x, float y) {
		for (int i = 0; i < screenPts.size(); i++) {
			if (Math.hypot(
			x - screenPts.get(i).x,
			y - screenPts.get(i).y
			) < 30) {
				
				PointD p = points.get(i);
				
				java.text.SimpleDateFormat sdf =
				new java.text.SimpleDateFormat(
				"dd MMM yyyy, HH:mm:ss",
				java.util.Locale.getDefault()
				);
				
				String timeStr = sdf.format(new java.util.Date(p.time));
				
				String info =
				"Point: " + (i + 1) +
				"\nLat: " + p.y +
				"\nLng: " + p.x +
				"\nSpeed: " + String.format("%.2f", p.speed * 3.6) + " km/h" +
				"\nSegment Distance: " + String.format("%.1f", p.segmentDistance) + " m" +
				"\nTotal Distance: " + String.format("%.1f", p.totalDistance) + " m" +
				"\nTime: " + timeStr;
				
				Toast.makeText(
				getContext(),
				info,
				Toast.LENGTH_LONG
				).show();
				break;
			}
		}
	}
	
	
	
	
	
	
	// ================= SCALE =================
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector d) {
			zoom *= d.getScaleFactor();
			zoom = Math.max(0.2, Math.min(zoom, 20));
			invalidate();
			return true;
		}
	}
	
	//	/* ================= EXPORT =================
	
	public Bitmap exportBitmap() {
		Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(),
		Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		draw(c);
		return b;
	}
	
	// niche
	public void focusLast() {
		if (points.isEmpty() || getWidth() == 0 || getHeight() == 0) return;
		
		PointD last = points.get(points.size() - 1);
		
		double rangeX = maxX - minX;
		double rangeY = maxY - minY;
		if (rangeX == 0) rangeX = 1e-6;
		if (rangeY == 0) rangeY = 1e-6;
		
		float sx = (float)((getWidth() - 100) / rangeX);
		float sy = (float)((getHeight() - 100) / rangeY);
		float scale = (float)(Math.min(sx, sy) * zoom);
		
		zoom = 4.0;
		
		float screenX = 50 + (float)((last.x - minX) * scale);
		float screenY = getHeight() - 50 - (float)((last.y - minY) * scale);
		
		offsetX = getWidth() / 2f - screenX;
		offsetY = getHeight() / 2f - screenY;
		
		invalidate();
	}
	
	
	
	
	public void clear() {
		points.clear();
		screenPts.clear();
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;
		zoom = 1.0;
		offsetX = offsetY = 0;
		invalidate();
	}
	
	
}