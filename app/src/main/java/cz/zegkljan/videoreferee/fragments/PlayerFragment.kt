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

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import cz.zegkljan.videoreferee.R
import cz.zegkljan.videoreferee.databinding.FragmentPlayerBinding
import java.io.File
import kotlin.math.roundToInt

class PlayerFragment : Fragment() {

    /** Android ViewBinding */
    private var _fragmentPlayerBinding: FragmentPlayerBinding? = null

    private val fragmentPlayerBinding get() = _fragmentPlayerBinding!!

    /** AndroidX navigation arguments */
    private val args: PlayerFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    /** Media session for playback. */
    private lateinit var mediaPlayer: MediaPlayer

    /** Milliseconds per frame. */
    private var mspf: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentPlayerBinding = FragmentPlayerBinding.inflate(inflater, container, false)
        return fragmentPlayerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentPlayerBinding.playerScreen.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //val size = getVideoOutputSize(fragmentPlayerBinding.playerScreen.display)
                fragmentPlayerBinding.playerScreen.setAspectRatio(args.width, args.height)
                fragmentPlayerBinding.playerScreen.post { initializePlayer() }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
        })
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onDestroyView() {
        _fragmentPlayerBinding = null
        super.onDestroyView()
    }

    fun initializePlayer() {
        mediaPlayer = createMediaPlayer()
        mspf = (1000f / args.fps).roundToInt()

        // player listeners
        mediaPlayer.setOnCompletionListener {
            fragmentPlayerBinding.playPauseButton.isChecked = false
        }

        // control listeners
        fragmentPlayerBinding.playPauseButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mediaPlayer.start()
            } else {
                mediaPlayer.pause()
            }
        }
        fragmentPlayerBinding.rewindButton.setOnClickListener {
            mediaPlayer.seekTo(0)
        }
        fragmentPlayerBinding.speed1.isChecked = true
        fragmentPlayerBinding.playbackSpeedSelect.setOnCheckedChangeListener { _, checkedId ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
            val pp = mediaPlayer.playbackParams
            try {
                when (checkedId) {
                    fragmentPlayerBinding.speed1.id -> mediaPlayer.playbackParams =
                        PlaybackParams().setSpeed(1f)
                    fragmentPlayerBinding.speed12.id -> mediaPlayer.playbackParams =
                        PlaybackParams().setSpeed(1f / 2f)
                    fragmentPlayerBinding.speed14.id -> mediaPlayer.playbackParams =
                        PlaybackParams().setSpeed(1f / 4f)
                    fragmentPlayerBinding.speed18.id -> mediaPlayer.playbackParams =
                        PlaybackParams().setSpeed(1f / 8f)
                    fragmentPlayerBinding.speed116.id -> mediaPlayer.playbackParams =
                        PlaybackParams().setSpeed(1f / 16f)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Unsupported playback speed.")
                mediaPlayer.playbackParams = pp
            }
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }

        // navigation out
        fragmentPlayerBinding.doneButton.setOnClickListener {
            val file = File(args.filename)
            if (!file.delete()) {
                Log.e(TAG, "Failed to delete file $file")
            }
            navController.navigate(PlayerFragmentDirections.actionPlayerToCamera(args.cameraId, args.width, args.height, args.fps))
        }
    }

    private fun createMediaPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(args.filename)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDisplay(fragmentPlayerBinding.playerScreen.holder)
            prepare()
            seekTo(0)
        }
    }

    companion object {
        private val TAG = PlayerFragment::class.java.simpleName
    }
}
