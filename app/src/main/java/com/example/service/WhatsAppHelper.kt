package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    fun sendWhatsAppMessage(
        context: Context,
        phone: String,
        message: String,
        attachmentUriStr: String? = null,
        mimeTypeStr: String? = null
    ) {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

        val attachmentUri = if (!attachmentUriStr.isNullOrBlank()) {
            try { Uri.parse(attachmentUriStr) } catch (e: Exception) { null }
        } else null

        if (attachmentUri != null) {
            val mimeType = mimeTypeStr?.ifBlank { null } ?: "*/*"
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, attachmentUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
            } catch (e: Exception) {
                // System share chooser fallback
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, attachmentUri)
                        putExtra(Intent.EXTRA_TEXT, "$message\n(To: $formattedPhone)")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share Wish / Message with File").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (ex: Exception) {
                    Toast.makeText(context, "Sharing failed: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            try {
                val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val webUrl = "https://wa.me/$formattedPhone?text=${Uri.encode(message)}"
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                } catch (ex: Exception) {
                    Toast.makeText(context, "Could not launch WhatsApp: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun generateCloudApiPayloadJson(
        clientPhone: String,
        templateName: String,
        parameters: List<String>
    ): String {
        val cleanPhone = clientPhone.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

        val paramsJson = parameters.joinToString(",") { "\"$it\"" }
        return """
            {
              "messaging_product": "whatsapp",
              "to": "$formattedPhone",
              "type": "template",
              "template": {
                "name": "$templateName",
                "language": { "code": "en_US" },
                "components": [
                  {
                    "type": "body",
                    "parameters": [
                       ${parameters.joinToString(",") { """{ "type": "text", "text": "$it" }""" }}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
    }
}
