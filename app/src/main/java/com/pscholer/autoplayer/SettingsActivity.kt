package com.pscholer.autoplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.pscholer.autoplayer.data.remote.jellyfin.CrossDomainRedirectException
import com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository
import com.pscholer.autoplayer.databinding.ActivitySettingsBinding
import com.pscholer.autoplayer.databinding.ItemSafFolderBinding
import com.pscholer.autoplayer.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val jellyfin: JellyfinRepository
) : ViewModel() {

    sealed class JellyfinSaveState {
        data object Idle : JellyfinSaveState()
        data object Checking : JellyfinSaveState()
        data class Success(val displayName: String) : JellyfinSaveState()
        data class Error(val message: String) : JellyfinSaveState()
    }

    sealed class PlexSaveState {
        data object Idle : PlexSaveState()
        data object Saved : PlexSaveState()
    }

    private val _jellyfinSaveState = MutableStateFlow<JellyfinSaveState>(JellyfinSaveState.Idle)
    val jellyfinSaveState: StateFlow<JellyfinSaveState> = _jellyfinSaveState.asStateFlow()

    private val _plexSaveState = MutableStateFlow<PlexSaveState>(PlexSaveState.Idle)
    val plexSaveState: StateFlow<PlexSaveState> = _plexSaveState.asStateFlow()

    val plexConfigFlow = prefs.plexConfigFlow
    val jellyfinConfigFlow = prefs.jellyfinConfigFlow
    val safUrisFlow = prefs.safUrisFlow
    val connectionInfoFlow = prefs.lastConnectionFlow

    fun savePlex(serverUrl: String, token: String) = viewModelScope.launch {
        prefs.savePlexConfig(serverUrl.trim(), token.trim())
        _plexSaveState.value = PlexSaveState.Saved
    }

    fun consumePlexSaved() {
        _plexSaveState.value = PlexSaveState.Idle
    }

    fun saveJellyfin(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _jellyfinSaveState.value = JellyfinSaveState.Checking
            _jellyfinSaveState.value = runCatching {
                val name = jellyfin.testAndSave(serverUrl.trim(), username.trim(), password.trim())
                JellyfinSaveState.Success(name)
            }.getOrElse { e ->
                val msg = when {
                    e is CrossDomainRedirectException ->
                        "Server URL routes through an auth proxy (${e.originalHost} → ${e.redirectedHost}). " +
                        "Enable 'Extended compatibility' in your Pangolin dashboard, or enter the " +
                        "direct Jellyfin URL (e.g. jellyfin.yourdomain.com) instead."
                    e.message?.contains("CLEARTEXT") == true ->
                        "Cleartext HTTP blocked — use https:// in the server URL"
                    e.message?.contains("Unable to resolve host") == true ->
                        "Server not found — check the URL"
                    e.message?.contains("auth proxy", ignoreCase = true) == true ||
                    e.message?.contains("HTML page", ignoreCase = true) == true ->
                        "Server URL routes through an auth proxy — enter the direct Jellyfin URL instead"
                    e.message?.contains("401") == true ||
                    e.message?.contains("Unauthorized") == true ->
                        "Wrong username or password"
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out — is the server reachable?"
                    else -> e.message ?: "Connection failed"
                }
                JellyfinSaveState.Error(msg)
            }
        }
    }

    fun consumeJellyfinTransient() {
        // Reset Success/Error back to Idle once UI has handled the one-shot Snackbar.
        val s = _jellyfinSaveState.value
        if (s is JellyfinSaveState.Success || s is JellyfinSaveState.Error) {
            _jellyfinSaveState.value = JellyfinSaveState.Idle
        }
    }

    fun addSafUri(uri: Uri) = viewModelScope.launch { prefs.addSafUri(uri) }
    fun removeSafUri(uri: Uri) = viewModelScope.launch { prefs.removeSafUri(uri) }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Activity
// ─────────────────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val vm: SettingsViewModel by viewModels()
    private val folderAdapter = SafFolderAdapter(::confirmRemoveFolder)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Snackbar.make(binding.root, R.string.permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    private val dirPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        vm.addSafUri(uri)
        Snackbar.make(binding.root, R.string.folder_added, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.rvFolders.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = folderAdapter
        }

        binding.btnSavePlex.setOnClickListener { onSavePlexClicked() }
        binding.btnSaveJellyfin.setOnClickListener { onSaveJellyfinClicked() }
        binding.btnAddFolder.setOnClickListener { dirPicker.launch(null) }

        // Clear inline errors when the user starts typing
        binding.etPlexUrl.doOnInput { binding.tilPlexUrl.error = null }
        binding.etPlexToken.doOnInput { binding.tilPlexToken.error = null }
        binding.etJfUrl.doOnInput { binding.tilJfUrl.error = null }
        binding.etJfUsername.doOnInput { binding.tilJfUsername.error = null }
        binding.etJfPassword.doOnInput { binding.tilJfPassword.error = null }

        observeState()
        maybeRequestPermission(savedInstanceState == null)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.plexConfigFlow.collect(::renderPlex) }
                launch { vm.jellyfinConfigFlow.collect(::renderJellyfin) }
                launch { vm.safUrisFlow.collect(::renderFolders) }
                launch { vm.plexSaveState.collect(::renderPlexSaveState) }
                launch { vm.jellyfinSaveState.collect(::renderJellyfinSaveState) }
                launch { vm.connectionInfoFlow.collect(::renderConnectionInfo) }
            }
        }
    }

    // ── Renderers ────────────────────────────────────────────────────────────

    private fun renderPlex(cfg: PreferencesManager.PlexConfig) {
        // Only fill if field is empty (don't clobber in-progress edits)
        if (binding.etPlexUrl.text.isNullOrEmpty()) binding.etPlexUrl.setText(cfg.serverUrl)
        if (binding.etPlexToken.text.isNullOrEmpty()) binding.etPlexToken.setText(cfg.token)
        binding.tvPlexStatus.setText(
            if (cfg.serverUrl.isNotBlank() && cfg.token.isNotBlank()) R.string.status_configured
            else R.string.status_not_configured
        )
        binding.tvPlexStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (cfg.serverUrl.isNotBlank() && cfg.token.isNotBlank()) R.color.status_success
                else R.color.md_on_surface_variant
            )
        )
    }

    private var jellyfinDisplayName: String? = null

    private fun renderJellyfin(cfg: PreferencesManager.JellyfinConfig) {
        if (binding.etJfUrl.text.isNullOrEmpty()) binding.etJfUrl.setText(cfg.serverUrl)
        if (binding.etJfUsername.text.isNullOrEmpty()) binding.etJfUsername.setText(cfg.username)
        if (binding.etJfPassword.text.isNullOrEmpty()) binding.etJfPassword.setText(cfg.password)

        val configured = cfg.serverUrl.isNotBlank() && cfg.username.isNotBlank()
        binding.tvJellyfinStatus.text = when {
            jellyfinDisplayName != null -> getString(R.string.status_connected, jellyfinDisplayName)
            configured -> getString(R.string.status_configured)
            else -> getString(R.string.status_not_configured)
        }
        binding.tvJellyfinStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (configured) R.color.status_success else R.color.md_on_surface_variant
            )
        )
    }

    private fun renderFolders(uris: Set<Uri>) {
        val list = uris.toList()
        folderAdapter.submit(list.map { uri ->
            val name = DocumentFile.fromTreeUri(this, uri)?.name ?: uri.lastPathSegment ?: uri.toString()
            SafFolderRow(uri, name)
        })
        binding.tvFolderStatus.text = when (list.size) {
            0 -> getString(R.string.status_no_folders)
            1 -> getString(R.string.status_folders_count, 1)
            else -> getString(R.string.status_folders_count_plural, list.size)
        }
        binding.tvFolderStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (list.isEmpty()) R.color.md_on_surface_variant else R.color.status_success
            )
        )
    }

    private fun renderPlexSaveState(state: SettingsViewModel.PlexSaveState) {
        if (state is SettingsViewModel.PlexSaveState.Saved) {
            Snackbar.make(binding.root, R.string.saved, Snackbar.LENGTH_SHORT).show()
            vm.consumePlexSaved()
        }
    }

    private fun renderJellyfinSaveState(state: SettingsViewModel.JellyfinSaveState) {
        when (state) {
            is SettingsViewModel.JellyfinSaveState.Idle -> {
                binding.btnSaveJellyfin.isEnabled = true
                binding.btnSaveJellyfin.text = getString(R.string.save)
            }
            is SettingsViewModel.JellyfinSaveState.Checking -> {
                binding.btnSaveJellyfin.isEnabled = false
                binding.btnSaveJellyfin.text = getString(R.string.checking)
            }
            is SettingsViewModel.JellyfinSaveState.Success -> {
                binding.btnSaveJellyfin.isEnabled = true
                binding.btnSaveJellyfin.text = getString(R.string.save)
                jellyfinDisplayName = state.displayName
                binding.tvJellyfinStatus.text = getString(R.string.status_connected, state.displayName)
                binding.tvJellyfinStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.status_success)
                )
                Snackbar.make(
                    binding.root,
                    getString(R.string.status_connected, state.displayName),
                    Snackbar.LENGTH_SHORT
                ).show()
                vm.consumeJellyfinTransient()
            }
            is SettingsViewModel.JellyfinSaveState.Error -> {
                binding.btnSaveJellyfin.isEnabled = true
                binding.btnSaveJellyfin.text = getString(R.string.save)
                // Inline error on URL field — most errors stem from URL/connectivity
                binding.tilJfUrl.error = state.message
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                vm.consumeJellyfinTransient()
            }
        }
    }

    private fun renderConnectionInfo(info: PreferencesManager.ConnectionInfo) {
        if (info.hostPackage.isEmpty()) {
            binding.tvConnectionNever.visibility = android.view.View.VISIBLE
            binding.layoutConnectionInfo.visibility = android.view.View.GONE
            return
        }
        binding.tvConnectionNever.visibility = android.view.View.GONE
        binding.layoutConnectionInfo.visibility = android.view.View.VISIBLE
        val label = if (info.hostVersion != "unknown") "${info.hostPackage}  (${info.hostVersion})"
                    else info.hostPackage
        binding.tvHostPackage.text = label
        binding.tvCarApiLevel.text = info.carApiLevel.toString()
        binding.tvConnectedAt.text = info.connectedAt
    }

    // ── Click handlers ───────────────────────────────────────────────────────

    private fun onSavePlexClicked() {
        val url = binding.etPlexUrl.text()
        val token = binding.etPlexToken.text()
        var ok = true
        if (url.isBlank()) { binding.tilPlexUrl.error = getString(R.string.error_url_required); ok = false }
        if (token.isBlank()) { binding.tilPlexToken.error = getString(R.string.error_token_required); ok = false }
        if (!ok) return
        vm.savePlex(url, token)
    }

    private fun onSaveJellyfinClicked() {
        val url = binding.etJfUrl.text()
        val user = binding.etJfUsername.text()
        val pass = binding.etJfPassword.text()
        var ok = true
        if (url.isBlank()) { binding.tilJfUrl.error = getString(R.string.error_url_required); ok = false }
        if (user.isBlank()) { binding.tilJfUsername.error = getString(R.string.error_username_required); ok = false }
        if (pass.isBlank()) { binding.tilJfPassword.error = getString(R.string.error_password_required); ok = false }
        if (!ok) return
        jellyfinDisplayName = null
        vm.saveJellyfin(url, user, pass)
    }

    private fun confirmRemoveFolder(row: SafFolderRow) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_remove_folder_title)
            .setMessage(R.string.confirm_remove_folder_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ ->
                runCatching {
                    contentResolver.releasePersistableUriPermission(
                        row.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                vm.removeSafUri(row.uri)
                Snackbar.make(binding.root, R.string.folder_removed, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    // ── Permission ───────────────────────────────────────────────────────────

    private fun maybeRequestPermission(isFreshLaunch: Boolean) {
        if (!isFreshLaunch) return
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return

        if (shouldShowRequestPermissionRationale(permission)) {
            showRationaleDialog(permission)
        } else {
            // First ask: show educational dialog before the system prompt
            showRationaleDialog(permission)
        }
    }

    private fun showRationaleDialog(permission: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_title)
            .setMessage(R.string.permission_message)
            .setNegativeButton(R.string.permission_skip, null)
            .setPositiveButton(R.string.permission_grant) { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .show()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun TextInputEditText.text() = text?.toString().orEmpty()

    private inline fun TextInputEditText.doOnInput(crossinline action: () -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { action() }
        })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SAF folder list adapter
// ─────────────────────────────────────────────────────────────────────────────

private data class SafFolderRow(val uri: Uri, val displayName: String)

private class SafFolderAdapter(
    private val onRemove: (SafFolderRow) -> Unit
) : RecyclerView.Adapter<SafFolderAdapter.VH>() {

    private val items = mutableListOf<SafFolderRow>()

    fun submit(newItems: List<SafFolderRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSafFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.binding.tvFolderName.text = row.displayName
        holder.binding.btnRemove.setOnClickListener { onRemove(row) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemSafFolderBinding) : RecyclerView.ViewHolder(binding.root)
}
