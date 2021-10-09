# Video Referee
An extremely simple app whose only purpose is to take a (high speed) video and then lets the user to analyze it - slow it down, step one frame at a time.
The use case is mainly as a poor-man's eagle eye, i.e. for video review of sport situations, especially HEMA (Historical European Martial Arts) fencing actions.

The app does not save the video (or, more precisely, once the user is done with the analysis and returns to the viewfinder, the video file is deleted).

## Workflow
The app has the following workflow:

0. Lanuch the app.
1. Ask for permissions (if not already granted).
2. Select capture resolution and frame rate.
3. Capture video.
4. Seek through, slow down...
5. Tap the ✓ button to go back to step 3, or press the native back button to go to step 2.

## Download and installation
Scan this QR code to download the APK:
![https://github.com/zegkljan/videoreferee/releases/latest/download/VideoReferee-release.apk](download-qr.png?raw=true "APK download")

Or download manually:
1. Head to the [latest release](https://github.com/zegkljan/videoreferee/releases/latest).
2. Download the APK.
3. Install on your Android device.

You will need to enable installation from unknown sources, as this is not Google Play.

## Notes
I'm no android app developer.
I just was not happy with any of the apps that I have found for this job, so I just went ahead and made my own, heavily based on [Camera2SlowMotion and Camera2Video samples](https://github.com/android/camera-samples).