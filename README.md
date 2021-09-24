# Video Referee
An extremely simple app whose only purpose is to take a (high speed) video and then lets the user to analyze it - slow it down, step one frame at a time.
The use case is mainly as a poor-man's eagle eye, i.e. for video review of sport situations, especially HEMA (Historical European Martial Arts) fencing actions.

The app does not save the video (or, more precisely, once the user is done with the analysis and returns to the viewfinder, the video file is deleted).

## Workflow
The app has the following workflow:

0. Lanuch the app.
1. Ask for permissions (if not already granted).
2. Select capture resolution and frame rate.
3. Viewfinder - capture video.
4. Analysis - seek through, slow down, step frame by frame...
5. Repeat from step 3. or 2.