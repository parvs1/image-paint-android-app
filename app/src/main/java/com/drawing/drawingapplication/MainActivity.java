package com.drawing.drawingapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.color_picker.dialog.ColorPickerDialogFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

import static android.os.Environment.DIRECTORY_PICTURES;

public class MainActivity extends AppCompatActivity implements Serializable, ColorPickerDialogFragment.ColorPickerDialogListener
{
	@BindView(R.id.image_result)
	ImageView imageResult;

	private Bitmap masterBitmap;
	private Canvas masterCanvas;

	private int previousX, previousY;

	private Paint paintDraw;
	private boolean loadImage;

	private static final int REQUEST_IMAGE_CAPTURE = 1;
	private static final int REQUEST_LOAD_IMAGE = 2;

	private Uri photoUri;

	private void dispatchTakePictureIntent()
	{
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null)
		{
			// Create the File where the photo should go
			File photoFile = null;
			try
			{
				photoFile = createImageFile();
			} catch (IOException ex)
			{
				// Error occurred while creating the File
			}
			// Continue only if the File was successfully created
			if (photoFile != null)
			{
				photoUri = FileProvider.getUriForFile(this,
						"com.drawing.drawingapplication.fileprovider",
						photoFile);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
				startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			}
		}
	}

	private void loadImageIntent()
	{
		Intent intent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, REQUEST_LOAD_IMAGE);
	}

	public void onColorSelected(int dialogId, int color, int strokeWidth)
	{
		paintDraw.setColor(color);
		paintDraw.setStrokeWidth(strokeWidth);
	}

	private File createImageFile() throws IOException
	{
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = getExternalFilesDir(DIRECTORY_PICTURES);
		File image = File.createTempFile(
				imageFileName,  /* prefix */
				".jpg",         /* suffix */
				storageDir      /* directory */
		);

		return image;
	}

	@OnClick(R.id.button_take_picture)
	public void onClickTakePicture()
	{
		dispatchTakePictureIntent();
	}

	@OnClick(R.id.button_load_image)
	public void onClickLoadImage()
	{
		loadImageIntent();
	}

	@OnClick(R.id.button_save_image)
	public void onClickSaveImage()
	{
		if (masterBitmap != null)
		{
			saveBitmap(masterBitmap);
		}
	}

	@OnClick(R.id.button_undo)
	public void onClickUndo()
	{
		if (!paths.isEmpty())
		{
			updateCanvasAndImageFromUri();
			paths.remove(paths.size() - 1);
			imageResult.invalidate();
			for (PaintPath paintPath : paths)
			{
				masterCanvas.drawPath(paintPath.path, paintPath.paint);
			}
		}
	}

	@OnClick(R.id.button_advanced_settings)
	public void onClickAdavancedSettings()
	{
		ColorPickerDialogFragment colorPickerDialogFragment = ColorPickerDialogFragment
				.newInstance(0, "Advanced Color Picker", null, paintDraw.getColor(), false, paintDraw.getStrokeWidth(), this);
		colorPickerDialogFragment.show(getFragmentManager(), "Color Picker");
	}

	@OnClick(R.id.button_clear)
	public void onClickClear()
	{
		if (masterBitmap != null)
		{
			updateCanvasAndImageFromUri();
			paths.clear();
			imageResult.invalidate();
		}
	}

	@OnTouch(R.id.image_result)
	public boolean onTouchImageResult(View view, MotionEvent event)
	{
		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				touchStart(x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				touchMove(x, y);
				break;
			case MotionEvent.ACTION_UP:
				touchUp();
				break;
		}

		for (PaintPath paintPath : paths)
		{
			masterCanvas.drawPath(paintPath.path, paintPath.paint);
		}

		imageResult.invalidate();


		/*
		 * Return 'true' to indicate that the event have been consumed.
		 * If auto-generated 'false', your code can detect ACTION_DOWN only,
		 * cannot detect ACTION_MOVE and ACTION_UP.
		 */
		return true;
	}

	private float mX, mY;
	private Path mPath;
	private ArrayList<PaintPath> paths = new ArrayList<>();
	private static final float TOUCH_TOLERANCE = 4;

	private void touchStart(float x, float y)
	{
		mPath = new Path();
		paths.add(new PaintPath(new Paint(paintDraw), mPath));

		mPath.reset();
		mPath.moveTo(x, y);
		mX = x;
		mY = y;
	}

	private void touchMove(float x, float y)
	{
		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);

		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
		{
			mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
			mX = x;
			mY = y;
		}
	}

	private void touchUp()
	{
		mPath.lineTo(mX, mY);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		paintDraw = new Paint();
		paintDraw.setStyle(Paint.Style.STROKE);
		paintDraw.setColor(Color.WHITE);
		paintDraw.setStrokeWidth(10);

		ButterKnife.bind(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK)
		{
			switch (requestCode)
			{
				case REQUEST_IMAGE_CAPTURE:
					loadImage = false;
					updateCanvasAndImageFromUri();
					paths.clear();
					imageResult.invalidate();
					break;
				case REQUEST_LOAD_IMAGE:
					loadImage = true;
					photoUri = data.getData();
					updateCanvasAndImageFromUri();
					paths.clear();
					imageResult.invalidate();
					break;
			}
		}
	}

	private void updateCanvasAndImageFromUri()
	{
		Bitmap immutableBitmap;
		double scaleFactor;
		try
		{
			if (loadImage)
			{
				immutableBitmap = BitmapFactory.decodeStream(
						getContentResolver().openInputStream(photoUri));
				scaleFactor = 1;
			}
			else
			{
				immutableBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
				scaleFactor = 0.5;
			}

			int canvasWidth = (int) Math.round(immutableBitmap.getWidth() * scaleFactor);
			int canvasHeight = (int) Math.round(immutableBitmap.getHeight() * scaleFactor);

			//masterBitmap is mutable
			masterBitmap = Bitmap.createBitmap(
					canvasWidth,
					canvasHeight,
					Bitmap.Config.ARGB_8888);

			masterCanvas = new Canvas(masterBitmap);
			masterCanvas.drawBitmap(immutableBitmap, null, new RectF(0, 0, canvasWidth, canvasHeight), null);

			imageResult.setImageBitmap(masterBitmap);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void saveBitmap(Bitmap bm)
	{
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "IMAGE_PAINT_APP_JPG_" + timeStamp + ".jpg";

		File file = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);
		File newFile = new File(file, imageFileName);

		try
		{
			FileOutputStream fileOutputStream = new FileOutputStream(newFile);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
			fileOutputStream.flush();
			fileOutputStream.close();
			Toast.makeText(MainActivity.this,
					"Save Bitmap: " + fileOutputStream.toString(),
					Toast.LENGTH_LONG).show();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			Toast.makeText(MainActivity.this,
					"Something wrong: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		} catch (IOException e)
		{
			e.printStackTrace();
			Toast.makeText(MainActivity.this,
					"Something wrong: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
}