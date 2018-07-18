Image Paint Android App
=======================

Overview
--------
This app allows users to either load and image from the device storage or take a picture with their camera.  This picture is displayed on the screen, and users can annotate on the picture.  The annotations can be cleared if the user chooses to do so, and the color of the stroke can also be selected with a color picker.  After the image is annotated, it can be saved on the device storage.

Process
-------
To start annotating on the canvas, I started [directly drawing on it](https://medium.com/@ssaurel/learn-to-create-a-paint-application-for-android-5b16968063f8), but it was hard to get a bitmap with the strokes and the original image.  [Here](https://stackoverflow.com/questions/5176441/drawable-image-on-a-canvas) is how to set a background of a canvas with the original setup.

Then, I went a different route and used an image view to capture my touch events and also use it to display the canvas.  The app could take an image file either from the camera or storage, and turn it into a bitmap.  Then, when a touch event is detected on the ImageView, the canvas could be updated by drawing lines which also updates the bitmap.  When the user decides to save the bitmap, it can just be passed onto the save function.  Looking back, I could have adapted my first annotating project to have a universal bitmap that was being updated to be saved later by the user.

[This site](http://android-er.blogspot.com/2015/12/open-image-free-draw-something-on.html) helped me with saving the bitmap, and I used the [Android Developers page on taking photos](https://developer.android.com/training/camera/photobasics) to help render it into the application. 

For the color picker, I used [Daniel Nilsson's color picker](https://github.com/danielnilsson9/color-picker-view).  I added a seek bar that controls the stroke width to turn it into a settings page.

I also used [Butterknife](https://github.com/JakeWharton/butterknife) for quick view and method binding.
