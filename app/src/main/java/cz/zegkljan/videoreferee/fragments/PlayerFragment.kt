/*
 * Copyright 2020 The Android Open Source Project
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

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.io.File
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
    private val playbackStateListener: Player.Listener = object: Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
                else -> "UNKNOWN_STATE"
            }
            Log.d(TAG, "playback state changed to $stateString")
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

    private fun initializePlayer() {
        mspf = (1000f / args.fps).roundToInt()

        player = SimpleExoPlayer.Builder(requireContext())
            .build()
            .also { exoPlayer ->
                fragmentPlayerBinding.playerView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(args.filename)))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = plWhenReady
                exoPlayer.seekTo(currentWindow, playbackPosition)
                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }

        // control listeners
        fragmentPlayerBinding.speed1.isChecked = true
        fragmentPlayerBinding.playbackSpeedSelect.setOnCheckedChangeListener listener@{ _, checkedId ->
            if (player == null) {
                return@listener
            }
            val p = player!!
            if (p.isPlaying) {
                p.pause()
            }
            try {
                when (checkedId) {
                    fragmentPlayerBinding.speed1.id -> p.playbackParameters = p.playbackParameters.withSpeed(1f)
                    fragmentPlayerBinding.speed12.id -> p.playbackParameters = p.playbackParameters.withSpeed(1f / 2f)
                    fragmentPlayerBinding.speed14.id -> p.playbackParameters = p.playbackParameters.withSpeed(1f / 4f)
                    fragmentPlayerBinding.speed18.id -> p.playbackParameters = p.playbackParameters.withSpeed(1f / 8f)
                    fragmentPlayerBinding.speed116.id -> p.playbackParameters = p.playbackParameters.withSpeed(1f / 16f)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Unsupported playback speed: ${checkedId}.")
            }
            if (p.isPlaying) {
                p.pause()
            }
        }

        // navigation out
        fragmentPlayerBinding.doneButton.setOnClickListener {
            val file = File(args.filename)
            if (!file.delete()) {
                Log.e(TAG, "Failed to delete file $file")
            }
            val navDirections: NavDirections
            if (args.isHighSpeed) {
                navDirections = PlayerFragmentDirections.actionPlayerToHighSpeedCamera(args.cameraId, args.width, args.height, args.fps)
            } else {
                navDirections = PlayerFragmentDirections.actionPlayerToNormalSpeedCamera(args.cameraId, args.width, args.height, args.fps)
            }
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