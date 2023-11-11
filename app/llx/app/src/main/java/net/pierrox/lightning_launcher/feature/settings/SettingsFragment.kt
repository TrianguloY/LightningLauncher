package net.pierrox.lightning_launcher.feature.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import net.pierrox.lightning_launcher.LLApp
import net.pierrox.lightning_launcher.Version
import net.pierrox.lightning_launcher.activities.ApplyTemplate
import net.pierrox.lightning_launcher.activities.BackupRestore
import net.pierrox.lightning_launcher.activities.Customize
import net.pierrox.lightning_launcher.activities.ScreenManager
import net.pierrox.lightning_launcher.data.ContainerPath
import net.pierrox.lightning_launcher.data.Page
import net.pierrox.lightning_launcher.template.LLTemplateAPI
import net.pierrox.lightning_launcher.util.PhoneUtils
import net.pierrox.lightning_launcher_extreme.R

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {
    companion object {
        private const val KEY_GLOBAL_CONFIG = "g"
        private const val KEY_CURRENT_PAGE = "d"
        private const val KEY_APP_DRAWER = "a"
        private const val KEY_BACKUP_RESTORE = "br"
        private const val KEY_CAT_SETTINGS = "t"
        private const val KEY_CAT_INFOS = "i"
        private const val KEY_COMMUNITY = "f"
        private const val KEY_RATE = "r"
        private const val KEY_SELECT_LAUNCHER = "s"
        private const val KEY_CONFIGURE_PAGES = "p"
        private const val KEY_CAT_TEMPLATES = "tc"
        private const val KEY_TEMPLATES_BROWSE = "tb"
        private const val KEY_TEMPLATES_APPLY = "ta"
        private const val KEY_UPGRADE = "u"
    }

    private val templateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val componentName = result.data?.component
                if (componentName != null) {
                    val intent = Intent(activity, ApplyTemplate::class.java)
                    intent.putExtra(LLTemplateAPI.INTENT_TEMPLATE_COMPONENT_NAME, componentName)
                    startActivity(intent)
                }
            }
        }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_root)

        setupPreference(KEY_CAT_SETTINGS, R.string.settings, 0)
        setupPreference(KEY_GLOBAL_CONFIG, R.string.general_t, R.string.general_s)
        setupPreference(KEY_CURRENT_PAGE, R.string.dashboard_t, R.string.dashboard_s)
        setupPreference(KEY_APP_DRAWER, R.string.app_drawer_t, R.string.app_drawer_s)

        val app = LLApp.get()
        setupPreference(
            KEY_CONFIGURE_PAGES,
            R.string.configure_pages_t,
            if (app.isFreeVersion()) R.string.tr_br_s else R.string.configure_pages_s
        )
        setupPreference(
            KEY_BACKUP_RESTORE,
            R.string.backup_restore_t,
            if (app.isTrialVersion()) R.string.tr_br_s else 0
        )

        setupPreference(KEY_CAT_INFOS, R.string.app_name, 0)
        setupPreference(KEY_SELECT_LAUNCHER, R.string.select_launcher_title, 0)
        setupPreference(KEY_COMMUNITY, R.string.facebook_t, R.string.facebook_s)
        setupPreference(KEY_RATE, R.string.rate_t, R.string.rate_s)

        setupPreference(KEY_UPGRADE, R.string.tr_rs_t, R.string.tr_rs_s)
        if (app.isTrialVersion()) {
            val left = app.getTrialLeft()
            val d = if (left == 0L) 0 else 1 + left / 86400000L
            findPreference<Preference>(KEY_UPGRADE)?.setSummary(getString(R.string.tr_l, d))
        }

        setupPreference(KEY_CAT_TEMPLATES, R.string.tmpl_t, R.string.tmpl_s)
        setupPreference(KEY_TEMPLATES_BROWSE, R.string.tmpl_b_t, R.string.tmpl_b_s)
        setupPreference(KEY_TEMPLATES_APPLY, R.string.tmpl_a_t, R.string.tmpl_a_s)

        val pc = preferenceScreen.findPreference(KEY_CAT_INFOS) as PreferenceCategory?
        val ratePreference = findPreference<Preference>(KEY_RATE)
        val upgradePreference = findPreference<Preference>(KEY_UPGRADE)

        if (!Version.HAS_RATE_LINK || app.isFreeVersion() || app.isTrialVersion()) {
            ratePreference?.let { pc?.removePreference(it) }
        }
        if (!app.isFreeVersion() && !app.isTrialVersion()) {
            upgradePreference?.let { pc?.removePreference(it) }
        }
    }

    private fun setupPreference(key: String, title: Int, summary: Int) {
        val p: Preference? = findPreference(key)
        if (key == KEY_CAT_INFOS) {
            try {
                val pi: PackageInfo =
                    requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                p?.title = getString(title) + "  v" + pi.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                // pass, cannot fail
            }
        } else {
            p?.setTitle(title)
        }
        if (summary != 0) p?.setSummary(summary)
        p?.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key: String = preference.key

        if (KEY_CONFIGURE_PAGES == key) {
            val intent = Intent(activity, ScreenManager::class.java)
            intent.setAction(Intent.ACTION_EDIT)
            startActivity(intent)
        } else if (KEY_BACKUP_RESTORE == key) {
            startActivity(Intent(activity, BackupRestore::class.java))
        } else if (KEY_SELECT_LAUNCHER == key) {
            PhoneUtils.selectLauncher(activity, true)
        } else if (KEY_UPGRADE == key) {
            LLApp.get().startUnlockProcess(activity)
        } else if (KEY_TEMPLATES_BROWSE == key) {
            startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_VIEW,
                        Version.BROWSE_TEMPLATES_URI
                    ), ""
                )
            )
        } else if (KEY_TEMPLATES_APPLY == key) {
            launchApplyTemplates()
        } else if (KEY_RATE != key && KEY_COMMUNITY != key) {
            val intent = Intent(activity, Customize::class.java)
            var path: ContainerPath? = null
            if (KEY_CURRENT_PAGE == key) {
                val current_page = LLApp.get().appEngine.readCurrentPage(Page.FIRST_DASHBOARD_PAGE)
                val s: String? =
                    requireActivity().intent?.getStringExtra(Customize.INTENT_EXTRA_PAGE_PATH)
                if (s == null) {
                    path = ContainerPath(
                        requireActivity().intent?.getIntExtra(
                            Customize.INTENT_EXTRA_PAGE_ID,
                            current_page
                        ) ?: current_page
                    )
                } else {
                    path = ContainerPath(s)
                    if (path.last == Page.APP_DRAWER_PAGE) {
                        path = ContainerPath(current_page)
                    }
                }
            }
            if (KEY_APP_DRAWER == key) {
                path = ContainerPath(Page.APP_DRAWER_PAGE)
            }
            if (path != null) {
                intent.putExtra(Customize.INTENT_EXTRA_PAGE_PATH, path.toString())
            }
            startActivity(intent)
        }
        return false
    }

    private fun launchApplyTemplates() {
        val filter = Intent(LLTemplateAPI.INTENT_QUERY_TEMPLATE)
        val i = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
            putExtra(Intent.EXTRA_TITLE, getString(R.string.tmpl_c))
            putExtra(Intent.EXTRA_INTENT, filter)
        }
        templateLauncher.launch(i)
    }

}