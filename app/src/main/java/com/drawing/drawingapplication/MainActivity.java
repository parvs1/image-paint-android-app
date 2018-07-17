package com.drawing.drawingapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MainActivity extends AppCompatActivity implements Serializable, ColorPickerDialogFragment.ColorPickerDialogListener
{
	@BindView(R.id.image_result) ImageView imageResult;

	private Bitmap masterBitmap;
	private Canvas masterCanvas;

	private int previousX, previousY;

	private Paint paintDraw;

	private static final int REQUEST_IMAGE_CAPTURE = 1;

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
		File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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

	@OnClick(R.id.button_save_image)
	public void onClickSaveImage()
	{
		if (masterBitmap != null)
		{
			saveBitmap(masterBitmap);
		}
	}

	@OnClick(R.id.button_advanced_settings)
	public void onClickAdavancedSettings()
	{
		ColorPickerDialogFragment colorPickerDialogFragment = ColorPickerDialogFragment
				.newInstance(0, "Advanced Color Picker", null, Color.WHITE, false, this);
		colorPickerDialogFragment.show(getFragmentManager(), "Color Picker");
	}

	@OnClick(R.id.button_clear)
	public void onClickClear()
	{
		if (masterBitmap != null)
		{
			updateCanvasAndImageFromUri();
		}
	}

	@OnTouch(R.id.image_result)
	public boolean onTouchImageResult(View view, MotionEvent event)
	{
		int action = event.getAction();
		int x = (int) event.getX();
		int y = (int) event.getY();
		switch (action)
		{
			case MotionEvent.ACTION_DOWN:
				previousX = x;
				previousY = y;
				drawOnProjectedBitMap((ImageView) view, masterBitmap, previousX, previousY, x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				drawOnProjectedBitMap((ImageView) view, masterBitmap, previousX, previousY, x, y);
				previousX = x;
				previousY = y;
				break;
			case MotionEvent.ACTION_UP:
				drawOnProjectedBitMap((ImageView) view, masterBitmap, previousX, previousY, x, y);
				break;
		}

		/*
		 * Return 'true' to indicate that the event have been consumed.
		 * If auto-generated 'false', your code can detect ACTION_DOWN only,
		 * cannot detect ACTION_MOVE and ACTION_UP.
		 */
		return true;
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

	/*
	Project position on ImageView to position on Bitmap draw on it
	 */
	private void drawOnProjectedBitMap(ImageView iv, Bitmap bm,
									   float x0, float y0, float x, float y)
	{
		if (x < 0 || y < 0 || x > iv.getWidth() || y > iv.getHeight())
		{
			//outside ImageView
			return;
		} else
		{

			float ratioWidth = (float) bm.getWidth() / (float) iv.getWidth();
			float ratioHeight = (float) bm.getHeight() / (float) iv.getHeight();

			masterCanvas.drawLine(
					x0 * ratioWidth,
					y0 * ratioHeight,
					x * ratioWidth,
					y * ratioHeight,
					paintDraw);
			imageResult.invalidate();
		}
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
					updateCanvasAndImageFromUri();
					break;
			}
		}
	}

	private void updateCanvasAndImageFromUri()
	{
		Bitmap tempBitmap = null;

		try
		{
			tempBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

			Bitmap.Config config;
			if (tempBitmap.getConfig() != null)
			{
				config = tempBitmap.getConfig();
			} else
			{
				config = Bitmap.Config.ARGB_8888;
			}

			//masterBitmap is mutable
			masterBitmap = Bitmap.createBitmap(
					tempBitmap.getWidth(),
					tempBitmap.getHeight(),
					config);

			masterCanvas = new Canvas(masterBitmap);
			masterCanvas.drawBitmap(tempBitmap, 0, 0, null);

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
		String imageFileName = "JPEG_" + timeStamp + "_";

		File file = Environment.getExternalStorageDirectory();
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