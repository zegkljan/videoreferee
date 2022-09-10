/*
 * Copyright 2021 Jan Žegklitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.zegkljan.videoreferee.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import cz.zegkljan.videoreferee.R
import cz.zegkljan.videoreferee.databinding.FragmentPlayerBinding
import cz.zegkljan.videoreferee.utils.BOUT_COUNTER_KEY
import cz.zegkljan.videoreferee.utils.EXCHANGE_COUNTER_KEY
import cz.zegkljan.videoreferee.utils.Medium
import kotlin.math.roundToInt

class PlayerFragment : Fragment() {

    private var playbackPosition = 0L
    private var currentWindow = 0
    private var plWhenReady = false

    /** Android ViewBinding */
    private lateinit var fragmentPlayerBinding: FragmentPlayerBinding

    /** AndroidX navigation arguments */
    private val args: PlayerFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    /**  Player */
    private var player: SimpleExoPlayer? = null
    private val playbackStateListener: Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
                else -> "UNKNOWN_STATE"
            }
            // Log.d(TAG, "playback state changed to $stateString")
        }
    }

    /** Milliseconds per frame. */
    private var mspf: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentPlayerBinding = FragmentPlayerBinding.inflate(inflater, container, false)
        return fragmentPlayerBinding.root
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    @SuppressLint("SetTextI18n")
    private fun initializePlayer() {
        mspf = (1000f / args.fps).roundToInt()

        player = SimpleExoPlayer.Builder(requireContext())
            .build()
            .also { exoPlayer ->
                fragmentPlayerBinding.playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(Uri.parse(args.fileuri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = plWhenReady
                exoPlayer.seekTo(currentWindow, playbackPosition)
                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }

        // control listeners
        val max = fragmentPlayerBinding.playbackSpeedSeek.max
        fragmentPlayerBinding.playbackSpeedText.text = "1"
        fragmentPlayerBinding.playbackSpeedSeek.progress =
            fragmentPlayerBinding.playbackSpeedSeek.max
        fragmentPlayerBinding.playbackSpeedSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            var progress = max
            var wasPlaying = false
            var dragging = false

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || player == null) {
                    return
                }
                this.progress = progress
                fragmentPlayerBinding.playbackSpeedText.text =
                    valueToText(progressToSpeed(seekBar, progress))
                if (!dragging) {
                    setPlaybackSpeed(seekBar)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (player == null) {
                    return
                }
                dragging = true
                val p = player!!
                wasPlaying = p.isPlaying
                p.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (player == null) {
                    return
                }
                dragging = false
                setPlaybackSpeed(seekBar)
                if (wasPlaying) {
                    player!!.play()
                }
            }

            fun progressToSpeed(seekBar: SeekBar?, progress: Int): Float {
                return progress.toFloat() / seekBar!!.max.toFloat() * 0.99f + 0.01f
            }

            fun valueToText(value: Float): String {
                return if (value == 1f) {
                    "1"
                } else {
                    "%.2f".format(value)
                }
            }

            fun setPlaybackSpeed(seekBar: SeekBar?) {
                val p = player!!
                val speed = progressToSpeed(seekBar, progress)
                p.playbackParameters = p.playbackParameters.withSpeed(speed)
            }
        })

        // navigation out
        fragmentPlayerBinding.doneButton.setOnClickListener {
            val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
            prefs.edit().putInt(EXCHANGE_COUNTER_KEY, prefs.getInt(EXCHANGE_COUNTER_KEY, 0) + 1)
                .apply()
            Log.d(TAG, "keep: bout counter: ${prefs.getInt(BOUT_COUNTER_KEY, 0)}")
            Log.d(TAG, "keep: exchange counter: ${prefs.getInt(EXCHANGE_COUNTER_KEY, 0)}")
            val navDirections: NavDirections = PlayerFragmentDirections.actionPlayerToCamera(
                args.cameraId,
                args.width,
                args.height,
                args.fps
            )
            navController.navigate(navDirections)
        }
        fragmentPlayerBinding.deleteButton.setOnClickListener {
            val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val medium = Medium.fromUri(Uri.parse(args.fileuri))
            if (!medium.remove(requireContext())) {
                // Log.e(TAG, "Failed to delete file $medium")
            }
            prefs.edit().putInt(EXCHANGE_COUNTER_KEY, prefs.getInt(EXCHANGE_COUNTER_KEY, 0) + 1)
                .apply()
            Log.d(TAG, "delete: bout counter: ${prefs.getInt(BOUT_COUNTER_KEY, 0)}")
            Log.d(TAG, "delete: exchange counter: ${prefs.getInt(EXCHANGE_COUNTER_KEY, 0)}")
            val navDirections: NavDirections = PlayerFragmentDirections.actionPlayerToCamera(
                args.cameraId,
                args.width,
                args.height,
                args.fps
            )
            navController.navigate(navDirections)
        }
    }

    private fun releasePlayer() {
        player?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            plWhenReady = this.playWhenReady
            removeListener(playbackStateListener)
            release()
        }
        player = null
    }

    companion object {
        private val TAG = PlayerFragment::class.java.simpleName
    }
}