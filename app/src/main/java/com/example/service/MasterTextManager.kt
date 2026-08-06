package com.example.service

import android.content.Context
import android.content.SharedPreferences

object MasterTextManager {

    private const val PREF_NAME = "adviser_master_text_prefs"

    // Default Pure Telugu & English Master Text Body Templates
    const val DEFAULT_TELUGU_BIRTHDAY = "గౌరవనీయులైన {client_name} గారికి,\n\nమీకు నా హృదయపూర్వక జన్మదిన శుభాకాంక్షలు! 🎂 శ్రీ శ్రీనివాస ప్రభువు ఆశీస్సులతో మీరు నిండు నూరేళ్లు పూర్ణ ఆయురారోగ్యాలు, అష్టైశ్వర్యాలు మరియు సకల సుఖసంతోషాలతో వర్ధిల్లాలని మనస్ఫూర్తిగా ప్రార్థిస్తున్నాను.\n\nభవదీయుడు,\nమీ ఫైనాన్షియల్ అడ్వైజర్"

    const val DEFAULT_ENGLISH_BIRTHDAY = "Dear {client_name},\n\nWishing you a very Happy Birthday! 🎂 May this special year bring you abundant good health, great joy, and financial success.\n\nWarm regards,\nYour Financial Advisor"

    const val DEFAULT_TELUGU_ANNIVERSARY = "గౌరవనీయులైన {client_name} గారికి,\n\nమీకు మరియు మీ శ్రీమతి/శ్రీవారికి వైవాహిక జీవిత వార్షికోత్సవ దివ్య శుభాకాంక్షలు! 💍 మీ దాంపత్య బంధం అనురాగం, అన్యోన్యతలతో నిరంతరం పచ్చగా వర్ధిల్లాలని ఆకాంక్షిస్తున్నాము.\n\nభవదీయుడు,\nమీ ఫైనాన్షియల్ అడ్వైజర్"

    const val DEFAULT_ENGLISH_ANNIVERSARY = "Dear {client_name},\n\nWishing you both a very Happy Wedding Anniversary! 💍 May your bond grow stronger each day with love and happiness.\n\nWarm regards,\nYour Financial Advisor"

    const val DEFAULT_TELUGU_RENEWAL = "గౌరవనీయులైన {client_name} గారికి నమస్కారములు.\n\nమీ జీవిత/ఆరోగ్య రక్షణ ఇన్సూరెన్స్ పాలసీ (No: {policy_no}) యొక్క రెన్యూవల్ గడువు తేదీ: {due_date}. చెల్లించవలసిన ప్రీమియం మొత్తం: ₹{premium}.\n\nసమయానికి ప్రీమియం చెల్లించి మీ కుటుంబ ఆర్థిక సురక్షితతను నిరంతరాయంగా కాపాడుకోగలరు. సహాయం కోసం మమ్మల్ని సంప్రదించండి.\n\nధన్యవాదాలు,\nమీ ఫైనాన్షియల్ అడ్వైజర్"

    const val DEFAULT_ENGLISH_RENEWAL = "Dear {client_name},\n\nFriendly reminder that your Insurance Policy (No: {policy_no}) renewal is due on {due_date}. Premium Amount: ₹{premium}.\n\nPlease renew on time to ensure uninterrupted financial protection for your family.\n\nWarm regards,\nYour Financial Advisor"

    const val DEFAULT_TELUGU_GENERAL = "గౌరవనీయులైన {client_name} గారికి శ్రేయోభిలాషి నమస్కారములు.\n\nఈ సంతోషకరమైన సందర్భంలో మీకు మరియు మీ కుటుంబ సభ్యులకు నా హృదయపూర్వక మంగళ శుభాకాంక్షలు! శ్రీ లక్ష్మీ నారాయణుల కృపాకటాక్షాలతో మీకు అన్ని వేళలా జయము, శాంతి కలుగాలని కోరుకుంటున్నాము.\n\nభవదీయుడు,\nమీ ఫైనాన్షియల్ అడ్వైజర్"

    const val DEFAULT_ENGLISH_GENERAL = "Dear {client_name},\n\nWarm greetings on this joyful occasion! Wishing you and your loved ones peace, good health, and prosperity always.\n\nWarm regards,\nYour Financial Advisor"

    // SharedPreferences Keys
    const val KEY_TELUGU_BIRTHDAY = "master_text_telugu_birthday"
    const val KEY_ENGLISH_BIRTHDAY = "master_text_english_birthday"
    const val KEY_TELUGU_ANNIVERSARY = "master_text_telugu_anniversary"
    const val KEY_ENGLISH_ANNIVERSARY = "master_text_english_anniversary"
    const val KEY_TELUGU_RENEWAL = "master_text_telugu_renewal"
    const val KEY_ENGLISH_RENEWAL = "master_text_english_renewal"
    const val KEY_TELUGU_GENERAL = "master_text_telugu_general"
    const val KEY_ENGLISH_GENERAL = "master_text_english_general"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getMasterText(context: Context, key: String, defaultVal: String): String {
        return getPrefs(context).getString(key, defaultVal) ?: defaultVal
    }

    fun saveMasterText(context: Context, key: String, text: String) {
        getPrefs(context).edit().putString(key, text).apply()
    }

    fun resetToDefaults(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
