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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import cz.zegkljan.videoreferee.R
import cz.zegkljan.videoreferee.utils.LAST_IGNORED_VERSION_KEY
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import org.json.JSONArray
import org.json.JSONTokener

/**
 * This [Fragment] checks for the availability of a newer version and offers a download if there is one.
 */
class VersionCheckFragment : Fragment() {
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            leave()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity()
        val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES)

        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val lastVersionStr = prefs.getString(LAST_IGNORED_VERSION_KEY, null)
        val lastIgnoredVersion: SemVer? = if (lastVersionStr == null) {
            null
        } else {
            SemVer.parse(lastVersionStr)
        }
        val version = SemVer.parse(info.versionName)

        lifecycleScope.launch {
            val latest = getLatestRelease()
            if (latest == null) {
                leave()
                return@launch
            }

            if (version < latest.version && (lastIgnoredVersion == null || latest.version > lastIgnoredVersion)) {
                AlertDialog.Builder(requireActivity()).apply {
                    setTitle(R.string.new_version_title)
                    setMessage(getString(R.string.new_version_message, latest.version, version))
                    setPositiveButton(R.string.yes) { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latest.url))
                        launcher.launch(intent)
                    }
                    setNeutralButton(R.string.no) { _, _ ->
                        leave()
                    }
                    setNegativeButton(R.string.no_dont_ask) { _, _ ->
                        prefs.edit {
                            putString(LAST_IGNORED_VERSION_KEY, latest.version.toString())
                            apply()
                        }
                        leave()
                    }
                }.show()
            } else {
                leave()
            }
        }

        return null
    }

    private fun leave() {
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(VersionCheckFragmentDirections.actionVersionCheckToPermissions())
    }

    private suspend fun getLatestRelease(): Release? {
        val client = HttpClient(Android) {
            engine {
                connectTimeout = 500
                socketTimeout = 500
            }
        }

        val res: String
        try {
            val response: HttpResponse = client.request(GIT_RELEASES) {
                method = HttpMethod.Get
                header("Accept", "application/vnd.github.v3+json")
            }
            res = response.receive()
        } catch (e: Exception) {
            return null
        }
        val jsonReleases = JSONTokener(res).nextValue() as JSONArray
        val releases = mutableListOf<Release>()
        for (i in 0 until jsonReleases.length()) {
            val jsonRelease = jsonReleases.getJSONObject(i)
            val ver = jsonRelease.getString("tag_name").removePrefix("v")
            val url = jsonRelease.getString("html_url")
            val isDraft = jsonRelease.getBoolean("draft")
            val isPrerelease = jsonRelease.getBoolean("prerelease")
            if (isDraft || isPrerelease) {
                continue
            }
            releases.add(Release(ver, url))
        }
        releases.sort()
        return releases.last()
    }

    companion object {
        const val GIT_RELEASES = "https://api.github.com/repos/zegkljan/videoreferee/releases"

        data class Release(val version: SemVer, val url: String) : Comparable<Release> {
            constructor(versionString: String, url: String) : this(SemVer.parse(versionString), url)

            override fun compareTo(other: Release): Int {
                return version.compareTo(other.version)
            }
        }
    }
}
