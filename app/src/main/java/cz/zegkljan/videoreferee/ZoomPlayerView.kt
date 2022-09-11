package cz.zegkljan.videoreferee

import android.content.Context
import android.graphics.Matrix
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.video.VideoSize
import com.otaliastudios.zoom.ZoomEngine
import com.otaliastudios.zoom.ZoomSurfaceView

class ZoomPlayerView(context: Context, attrs: AttributeSet?) : PlayerView(context, attrs) {
    private var isZoomedIn: Boolean = false
    private val surfaceView: ZoomSurfaceView = findViewById(R.id.player_surface_view)

    init {
        val zoomEngineListener = object : ZoomEngine.Listener {
            override fun onIdle(engine: ZoomEngine) {
                showController()
            }

            override fun onUpdate(engine: ZoomEngine, matrix: Matrix) {
                hideController()
                /*
                isZoomedIn = engine.zoom > 1

                val finalResizeMode = if (isZoomedIn)
                    AspectRatioFrameLayout.RESIZE_MODE_FILL
                else
                    AspectRatioFrameLayout.RESIZE_MODE_FIT

                if (finalResizeMode != resizeMode) {
                    resizeMode = finalResizeMode
                }
                */
            }
        }

        val playerSurfaceViewListener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                surfaceView.setContentSize(videoSize.width.toFloat(), videoSize.height.toFloat())
            }
        }

        surfaceView.addCallback(object : ZoomSurfaceView.Callback {
            override fun onZoomSurfaceCreated(view: ZoomSurfaceView) {
                view.engine.addListener(zoomEngineListener)
                player?.setVideoSurface(view.surface)
                player?.addListener(playerSurfaceViewListener)
            }

            override fun onZoomSurfaceDestroyed(view: ZoomSurfaceView) {
                //view.engine.removeListener(zoomEngineListener)
                player?.removeListener(playerSurfaceViewListener)
                player?.setVideoSurface(null)
            }
        })
    }

    override fun setPlayer(player: Player?) {
        super.setPlayer(player)

        if (player == null) {
            return
        }
        if (player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) {
            if (surfaceView is SurfaceView) {
                player.setVideoSurfaceView(surfaceView as SurfaceView)
            }
        }
        if (player.isCommandAvailable(Player.COMMAND_GET_TEXT)) {
            subtitleView?.setCues(player.currentCues.cues)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (surfaceView is SurfaceView) {
            // Work around https://github.com/google/ExoPlayer/issues/3160.
            surfaceView.visibility = visibility
        }
    }

    override fun getVideoSurfaceView(): View {
        return surfaceView
    }

    override fun onResume() {
        if (surfaceView is GLSurfaceView) {
            (surfaceView as GLSurfaceView).onResume()
        }
    }

    override fun onPause() {
        if (surfaceView is GLSurfaceView) {
            (surfaceView as GLSurfaceView).onPause()
        }
    }
}