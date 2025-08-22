package com.multisensor.recording.ui.privacy

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.multisensor.recording.R
import com.multisensor.recording.security.PrivacyManager
import com.multisensor.recording.util.Logger

class ConsentDialog(
    private val context: Context,
    private val privacyManager: PrivacyManager,
    private val logger: Logger,
    private val onConsentResult: (Boolean) -> Unit
) {

    fun show(studyId: String? = null) {

        if (privacyManager.hasValidConsent()) {
            onConsentResult(true)
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_consent, null)

        val titleText = dialogView.findViewById<TextView>(R.id.consent_title)
        val contentText = dialogView.findViewById<TextView>(R.id.consent_content)
        val dataTypesText = dialogView.findViewById<TextView>(R.id.data_types)
        val rightsText = dialogView.findViewById<TextView>(R.id.user_rights)
        val anonymizationCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_anonymization)
        val faceBlurringCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_face_blurring)
        val understandCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_understand)

        setupConsentContent(titleText, contentText, dataTypesText, rightsText, studyId)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("I Consent") { _, _ ->
                handleConsent(true, anonymizationCheckbox.isChecked, faceBlurringCheckbox.isChecked, studyId)
            }
            .setNegativeButton("Decline") { _, _ ->
                handleConsent(false, false, false, studyId)
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.isEnabled = false

            understandCheckbox.setOnCheckedChangeListener { _, isChecked ->
                positiveButton.isEnabled = isChecked
            }
        }

        dialog.show()

        logger.info("Consent dialog displayed for study: ${studyId ?: "unknown"}")
    }

    private fun setupConsentContent(
        titleText: TextView,
        contentText: TextView,
        dataTypesText: TextView,
        rightsText: TextView,
        studyId: String?
    ) {
        titleText.text = "Research Data Collection Consent"

        val consentContent = """
            <h3>Purpose of Data Collection</h3>
            <p>This application collects multi-sensor physiological data for research purposes${if (studyId != null) " as part of study: <b>$studyId</b>" else ""}.</p>

            <h3>Data Processing and Storage</h3>
            <p>Your data will be:</p>
            <ul>
            <li>Stored locally on this device with encryption</li>
            <li>Used solely for research purposes</li>
            <li>Not shared with third parties without your explicit consent</li>
            <li>Anonymized when possible to protect your privacy</li>
            </ul>

            <h3>Voluntary Participation</h3>
            <p>Your participation is voluntary. You may withdraw consent at any time, and your data will be deleted upon request.</p>
        """.trimIndent()

        contentText.text = Html.fromHtml(consentContent, Html.FROM_HTML_MODE_LEGACY)
        contentText.movementMethod = LinkMovementMethod.getInstance()

        val dataTypesContent = """
            <h3>Types of Data Collected</h3>
            <ul>
            <li><b>Video recordings:</b> RGB camera footage for hand gesture and movement analysis (hands-only)</li>
            <li><b>Thermal imagery:</b> Thermal camera data for physiological monitoring</li>
            <li><b>Sensor data:</b> Shimmer device measurements (GSR, accelerometer, etc.)</li>
            <li><b>Device metadata:</b> Technical information about recording sessions</li>
            <li><b>Timestamps:</b> Recording session timing information</li>
            </ul>
        """.trimIndent()

        dataTypesText.text = Html.fromHtml(dataTypesContent, Html.FROM_HTML_MODE_LEGACY)

        val rightsContent = """
            <h3>Your Rights (GDPR Compliance)</h3>
            <ul>
            <li><b>Right to withdraw:</b> You can withdraw consent at any time</li>
            <li><b>Right to deletion:</b> You can request deletion of your data</li>
            <li><b>Right to access:</b> You can request a copy of your data</li>
            <li><b>Right to rectification:</b> You can request correction of inaccurate data</li>
            <li><b>Data portability:</b> You can request transfer of your data</li>
            </ul>

            <p><small>Data retention period: ${privacyManager.getDataRetentionDays()} days from collection date.</small></p>
        """.trimIndent()

        rightsText.text = Html.fromHtml(rightsContent, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun handleConsent(
        consentGiven: Boolean,
        enableAnonymization: Boolean,
        enableFaceBlurring: Boolean,
        studyId: String?
    ) {
        if (consentGiven) {

            val participantId = privacyManager.generateAnonymousParticipantId()

            privacyManager.recordConsent(participantId, studyId)

            privacyManager.configureAnonymization(
                enableDataAnonymization = enableAnonymization,
                enableFaceBlurring = enableFaceBlurring,
                enableMetadataStripping = true
            )

            logger.info("User consent granted with participant ID: $participantId")
        } else {
            logger.info("User consent declined")
        }

        onConsentResult(consentGiven)
    }

    fun showPrivacySettings() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_privacy_settings, null)

        val currentSettings = privacyManager.getAnonymizationSettings()
        val consentInfo = privacyManager.getConsentInfo()

        val anonymizationCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_anonymization)
        val faceBlurringCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_face_blurring)
        val metadataStrippingCheckbox = dialogView.findViewById<CheckBox>(R.id.checkbox_metadata_stripping)
        val participantIdText = dialogView.findViewById<TextView>(R.id.participant_id_text)
        val consentDateText = dialogView.findViewById<TextView>(R.id.consent_date_text)

        anonymizationCheckbox.isChecked = currentSettings.dataAnonymizationEnabled
        faceBlurringCheckbox.isChecked = currentSettings.faceBlurringEnabled
        metadataStrippingCheckbox.isChecked = currentSettings.metadataStrippingEnabled

        participantIdText.text = "Participant ID: ${consentInfo.participantId ?: "Not set"}"

        if (consentInfo.consentDate > 0) {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(consentInfo.consentDate))
            consentDateText.text = "Consent given: $date"
        } else {
            consentDateText.text = "Consent not given"
        }

        AlertDialog.Builder(context)
            .setTitle("Privacy Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                privacyManager.configureAnonymization(
                    enableDataAnonymization = anonymizationCheckbox.isChecked,
                    enableFaceBlurring = faceBlurringCheckbox.isChecked,
                    enableMetadataStripping = metadataStrippingCheckbox.isChecked
                )
                logger.info("Privacy settings updated")
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Withdraw Consent") { _, _ ->
                showWithdrawConsentDialog()
            }
            .show()
    }

    private fun showWithdrawConsentDialog() {
        AlertDialog.Builder(context)
            .setTitle("Withdraw Consent")
            .setMessage("Are you sure you want to withdraw your consent? This will delete all collected data and you will no longer be able to participate in the study.")
            .setPositiveButton("Withdraw") { _, _ ->
                if (privacyManager.withdrawConsent()) {
                    logger.info("User consent withdrawn successfully")

                    onConsentResult(false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
