package com.example.service

import android.content.Context

object TeluguTranslator {

    fun getBirthdayWish(clientName: String, isTelugu: Boolean, context: Context? = null): String {
        val raw = if (context != null) {
            val key = if (isTelugu) MasterTextManager.KEY_TELUGU_BIRTHDAY else MasterTextManager.KEY_ENGLISH_BIRTHDAY
            val defaultVal = if (isTelugu) MasterTextManager.DEFAULT_TELUGU_BIRTHDAY else MasterTextManager.DEFAULT_ENGLISH_BIRTHDAY
            MasterTextManager.getMasterText(context, key, defaultVal)
        } else {
            if (isTelugu) MasterTextManager.DEFAULT_TELUGU_BIRTHDAY else MasterTextManager.DEFAULT_ENGLISH_BIRTHDAY
        }
        return raw.replace("{client_name}", clientName)
    }

    fun getAnniversaryWish(clientName: String, isTelugu: Boolean, context: Context? = null): String {
        val raw = if (context != null) {
            val key = if (isTelugu) MasterTextManager.KEY_TELUGU_ANNIVERSARY else MasterTextManager.KEY_ENGLISH_ANNIVERSARY
            val defaultVal = if (isTelugu) MasterTextManager.DEFAULT_TELUGU_ANNIVERSARY else MasterTextManager.DEFAULT_ENGLISH_ANNIVERSARY
            MasterTextManager.getMasterText(context, key, defaultVal)
        } else {
            if (isTelugu) MasterTextManager.DEFAULT_TELUGU_ANNIVERSARY else MasterTextManager.DEFAULT_ENGLISH_ANNIVERSARY
        }
        return raw.replace("{client_name}", clientName)
    }

    fun getRenewalReminder(
        clientName: String,
        policyNo: String,
        dueDate: String,
        premium: String,
        isTelugu: Boolean,
        context: Context? = null
    ): String {
        val raw = if (context != null) {
            val key = if (isTelugu) MasterTextManager.KEY_TELUGU_RENEWAL else MasterTextManager.KEY_ENGLISH_RENEWAL
            val defaultVal = if (isTelugu) MasterTextManager.DEFAULT_TELUGU_RENEWAL else MasterTextManager.DEFAULT_ENGLISH_RENEWAL
            MasterTextManager.getMasterText(context, key, defaultVal)
        } else {
            if (isTelugu) MasterTextManager.DEFAULT_TELUGU_RENEWAL else MasterTextManager.DEFAULT_ENGLISH_RENEWAL
        }
        return raw.replace("{client_name}", clientName)
            .replace("{policy_no}", policyNo)
            .replace("{due_date}", dueDate)
            .replace("{premium}", premium)
    }

    fun getCampaignMessage(clientName: String, isTelugu: Boolean, context: Context? = null): String {
        val raw = if (context != null) {
            val key = if (isTelugu) MasterTextManager.KEY_TELUGU_GENERAL else MasterTextManager.KEY_ENGLISH_GENERAL
            val defaultVal = if (isTelugu) MasterTextManager.DEFAULT_TELUGU_GENERAL else MasterTextManager.DEFAULT_ENGLISH_GENERAL
            MasterTextManager.getMasterText(context, key, defaultVal)
        } else {
            if (isTelugu) MasterTextManager.DEFAULT_TELUGU_GENERAL else MasterTextManager.DEFAULT_ENGLISH_GENERAL
        }
        return raw.replace("{client_name}", clientName)
    }

    fun convertTextLanguage(text: String, toTelugu: Boolean): String {
        if (text.isBlank()) return ""

        if (toTelugu) {
            // Pure Telugu transformation mapping
            var converted = text
            if (converted.contains("Happy Birthday", ignoreCase = true) || converted.contains("Birthday", ignoreCase = true)) {
                converted = converted.replace(Regex("(?i)Dear (.*?)[,\\n]"), "గౌరవనీయులైన $1 గారికి,\n")
                    .replace(Regex("(?i)Wishing you a very Happy Birthday!.*? prosperity\\."), "మీకు నా హృదయపూర్వక జన్మదిన శుభాకాంక్షలు! 🎂 శ్రీ వారి ఆశీస్సులతో పూర్ణ ఆయురారోగ్యాలు ప్రసాదించాలని కోరుకుంటున్నాము.")
                    .replace(Regex("(?i)Happy Birthday!"), "హృదయపూర్వక జన్మదిన శుభాకాంక్షలు! 🎂")
            }
            if (converted.contains("Anniversary", ignoreCase = true)) {
                converted = converted.replace(Regex("(?i)Dear (.*?)[,\\n]"), "గౌరవనీయులైన $1 గారికి,\n")
                    .replace(Regex("(?i)Wishing you a very Happy Wedding Anniversary!.*"), "మీకు వైవాహిక జీవిత వార్షికోత్సవ దివ్య శుభాకాంక్షలు! 💍 మీ దాంపత్యం వర్ధిల్లాలని ఆశిస్తున్నాము.")
                    .replace(Regex("(?i)Happy Wedding Anniversary!"), "వైవాహిక జీవిత వార్షికోత్సవ శుభాకాంక్షలు! 💍")
            }
            converted = converted
                .replace("Dear ", "గౌరవనీయులైన ")
                .replace("Reminder that your", "మీ జ్ఞాపకార్థం: మీ")
                .replace("Insurance Policy", "ఇన్సూరెన్స్ పాలసీ")
                .replace("Policy No", "పాలసీ సంఖ్య")
                .replace("due on", "చివరి గడువు తేదీ")
                .replace("Premium Amount", "ప్రీమియం మొత్తం")
                .replace("Please connect", "దయచేసి సంప్రదించండి")
                .replace("Warm regards", "భవదీయుడు")
                .replace("Your Financial Advisor", "మీ ఫైనాన్షియల్ అడ్వైజర్")
                .replace("Thank you", "ధన్యవాదాలు")

            return converted
        } else {
            // Smart translation mapping for Telugu -> English
            var converted = text
                .replace("గౌరవనీయులైన ", "Dear ")
                .replace(" గారికీ,", ",")
                .replace(" గారికి,", ",")
                .replace(" నమస్కారములు.", "")
                .replace(" నమస్కారం.", "")
                .replace("జన్మదిన శుభాకాంక్షలు!", "Happy Birthday! 🎂")
                .replace("వైవాహిక జీవిత వార్షికోత్సవ శుభాకాంక్షలు!", "Happy Wedding Anniversary! 💍")
                .replace("ఇన్సూరెన్స్ పాలసీ", "Insurance Policy")
                .replace("చివరి గడువు తేదీ", "Due Date")
                .replace("ప్రీమియం మొత్తం", "Premium Amount")
                .replace("భవదీయుడు", "Warm regards")
                .replace("మీ ఫైనాన్షియల్ అడ్వైజర్", "Your Financial Advisor")
                .replace("ధన్యవాదాలు", "Thank you")
            return converted
        }
    }
}
