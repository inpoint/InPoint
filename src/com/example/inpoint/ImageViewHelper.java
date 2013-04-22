package com.example.inpoint;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ImageViewHelper {
	private ImageView imageView;
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private Bitmap bitmap;

	private float minScaleR;//

	private static final int NONE = 0;// 
	private static final int DRAG = 1;// 
	private static final int ZOOM = 2;//
	private int mode = NONE;

	private PointF prev = new PointF();
	private PointF mid = new PointF();
	private float dist = 1f;
	private DisplayMetrics dm;
	private ImageButton zoomInButton;
	private ImageButton zoomOutButton;

	public ImageViewHelper(DisplayMetrics dm, ImageView imageView,
			Bitmap bitmap, ImageButton zoomInButton, ImageButton zoomOutButton) {
		this.dm = dm;
		this.imageView = imageView;
		this.zoomInButton = zoomInButton;
		this.zoomOutButton = zoomOutButton;
		this.bitmap = bitmap;
		setImageSize();
		minZoom();
		// center();
		imageView.setImageMatrix(matrix);

	}

	public Matrix getMatrix() {
		return matrix;
	}

	public void setZoomIn() {

		minScaleR = Math.min(
				(float) dm.widthPixels / (float) bitmap.getWidth(),
				(float) dm.heightPixels / (float) bitmap.getHeight());
		if (minScaleR < 1.0) {
			matrix.postScale(minScaleR + 1f, minScaleR + 1f);
		} else {
			matrix.postScale(minScaleR, minScaleR);
		}
	}

	public void setZoomOut() {
		minScaleR = Math.max(
				(float) dm.widthPixels / (float) bitmap.getWidth(),
				(float) dm.heightPixels / (float) bitmap.getHeight());
		if (minScaleR > 1.0) {
			matrix.postScale((minScaleR - (int) minScaleR),
					(minScaleR - (int) minScaleR));
		} else {
			matrix.postScale(minScaleR, minScaleR);
		}
	}

	/*
	 * <get the minimum ratio>, if the figure is bigger than screen, then
	 * (screen_width/screen_height) / (figure_width/figure_height) < 1 and then
	 * zoom in the figure
	 * 
	 * vice versa
	 */
	public void minZoom() {
		minScaleR = Math.min(
				(float) dm.widthPixels / (float) bitmap.getWidth(),
				(float) dm.heightPixels / (float) bitmap.getHeight());
		if (minScaleR <= 1.0) {
			matrix.postScale(minScaleR, minScaleR);
		} else {
			matrix.postScale(1.5f, 1.5f);
		}
	}

	public void center() {
		center(true, true);

	}

	// reset the figure to the screen center once it go over the display
	// boundary
	public void center(boolean horizontal, boolean vertical) {

		Matrix m = new Matrix();
		m.set(matrix);
		RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
		m.mapRect(rect);

		float height = rect.height();
		float width = rect.width();

		float deltaX = 0, deltaY = 0;

		if (vertical) {

			int screenHeight = dm.heightPixels;
			if (height < screenHeight) {
				deltaY = (screenHeight - height) / 2 - rect.top;
			} else if (rect.top > 0) {
				deltaY = -rect.top;
			} else if (rect.bottom < screenHeight) {
				deltaY = imageView.getHeight() - rect.bottom;
			}
		}

		if (horizontal) {
			int screenWidth = dm.widthPixels;
			if (width < screenWidth) {
				deltaX = (screenWidth - width) / 2 - rect.left;
			} else if (rect.left > 0) {
				deltaX = -rect.left;
			} else if (rect.right < screenWidth) {
				deltaX = screenWidth - rect.right;
			}
		}
		matrix.postTranslate(deltaX, deltaY);
	}

	// distance between two points
	public float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	// middle point of two points
	public void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	public void setImageSize() {
		zoomInButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setZoomIn();
				// center();
				imageView.setImageMatrix(matrix);
			}

		});
		zoomOutButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setZoomOut();
				// center();
				imageView.setImageMatrix(matrix);
			}

		});

		imageView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				switch (event.getAction() & MotionEvent.ACTION_MASK) {

				case MotionEvent.ACTION_DOWN:
					savedMatrix.set(matrix);
					prev.set(event.getX(), event.getY());
					mode = DRAG;
					break;

				case MotionEvent.ACTION_POINTER_DOWN:
					dist = spacing(event);
					if (spacing(event) > 10f) {
						savedMatrix.set(matrix);
						midPoint(mid, event);
						mode = ZOOM;
					}
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					mode = NONE;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mode == DRAG) {
						matrix.set(savedMatrix);
						matrix.postTranslate(event.getX() - prev.x,
								event.getY() - prev.y);
					} else if (mode == ZOOM) {
						float newDist = spacing(event);
						if (newDist > 10f) {
							matrix.set(savedMatrix);
							float tScale = newDist / dist;
							matrix.postScale(tScale, tScale, mid.x, mid.y);
						}

					}
					break;
				}
				imageView.setImageMatrix(matrix);
				// center();
				return true;
			}

		});
	}
}
