package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

class ToolExecutionEngine(private val context: Context) {

    // Common app aliases mapping to package names
    private val appAliases = mapOf(
        "youtube" to "com.google.android.youtube",
        "yt" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "insta" to "com.instagram.android",
        "calculator" to "com.google.android.calculator",
        "calc" to "com.google.android.calculator",
        "camera" to "com.google.android.GoogleCamera",
        "gmail" to "com.google.android.gm",
        "mail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "clock" to "com.google.android.deskclock",
        "alarm" to "com.google.android.deskclock",
        "settings" to "com.android.settings",
        "photos" to "com.google.android.apps.photos",
        "gallery" to "com.google.android.apps.photos"
    )

    fun getToolsDeclarationJson(): JSONArray {
        val toolsArray = JSONArray()
        val toolsObj = JSONObject()
        val functionDeclarations = JSONArray()

        // 1. openApp
        val openApp = JSONObject().apply {
            put("name", "openApp")
            put("description", "Launch any application on the user's phone, such as YouTube, Instagram, WhatsApp, Calculator, Spotify, Camera, or Settings.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("packageName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The package name or simple name of the app (e.g., 'youtube', 'whatsapp', 'calculator', 'com.instagram.android')")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply { put("packageName") })
            }
            put("parameters", params)
        }
        functionDeclarations.put(openApp)

        // 2. searchAndCallContact
        val callContact = JSONObject().apply {
            put("name", "searchAndCallContact")
            put("description", "Look up a contact from the user's contacts list by name and trigger a phone call.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The name of the contact to search and call (e.g., 'Mom', 'Alex', 'John')")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply { put("contactName") })
            }
            put("parameters", params)
        }
        functionDeclarations.put(callContact)

        // 3. sendWhatsAppMessage
        val whatsApp = JSONObject().apply {
            put("name", "sendWhatsAppMessage")
            put("description", "Locate a contact and deep-link directly into WhatsApp with a pre-filled chat message.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Contact name or phone number of the recipient")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The text message content to send via WhatsApp")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply {
                    put("contactName")
                    put("message")
                })
            }
            put("parameters", params)
        }
        functionDeclarations.put(whatsApp)

        // 4. sendGmail
        val gmail = JSONObject().apply {
            put("name", "sendGmail")
            put("description", "Compose or send an email via Gmail with recipient, subject, and body.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("recipientEmail", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The recipient email address (e.g. boss@company.com)")
                    })
                    put("subject", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Subject line of the email")
                    })
                    put("body", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Body content of the email message")
                    })
                }
                put("properties", props)
                put("required", JSONArray().apply {
                    put("recipientEmail")
                    put("subject")
                    put("body")
                })
            }
            put("parameters", params)
        }
        functionDeclarations.put(gmail)

        toolsObj.put("functionDeclarations", functionDeclarations)
        toolsArray.put(toolsObj)
        return toolsArray
    }

    suspend fun executeTool(name: String, args: JSONObject): ToolResult {
        return try {
            when (name) {
                "openApp" -> {
                    val packageName = args.optString("packageName", "")
                    executeOpenApp(packageName)
                }
                "searchAndCallContact" -> {
                    val contactName = args.optString("contactName", "")
                    executeSearchAndCallContact(contactName)
                }
                "sendWhatsAppMessage" -> {
                    val contactName = args.optString("contactName", "")
                    val message = args.optString("message", "")
                    executeSendWhatsAppMessage(contactName, message)
                }
                "sendGmail" -> {
                    val recipientEmail = args.optString("recipientEmail", "")
                    val subject = args.optString("subject", "")
                    val body = args.optString("body", "")
                    executeSendGmail(recipientEmail, subject, body)
                }
                else -> ToolResult(
                    isSuccess = false,
                    toolName = name,
                    summary = "Unknown tool requested: $name",
                    responsePayload = JSONObject().apply {
                        put("status", "error")
                        put("message", "Tool $name is not supported.")
                    }
                )
            }
        } catch (e: Exception) {
            ToolResult(
                isSuccess = false,
                toolName = name,
                summary = "Failed to execute $name: ${e.localizedMessage}",
                responsePayload = JSONObject().apply {
                    put("status", "error")
                    put("message", "Error executing $name: ${e.localizedMessage}")
                }
            )
        }
    }

    private fun executeOpenApp(rawName: String): ToolResult {
        val trimmed = rawName.trim().lowercase()
        val pm = context.packageManager

        // Check alias mapping first
        val targetPkg = appAliases[trimmed] ?: rawName.trim()

        var launchIntent = pm.getLaunchIntentForPackage(targetPkg)

        // If not found directly, try finding installed packages matching label
        if (launchIntent == null) {
            try {
                val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in installedPackages) {
                    val label = pm.getApplicationLabel(app).toString().lowercase()
                    if (label.contains(trimmed) || trimmed.contains(label)) {
                        launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) break
                    }
                }
            } catch (_: Exception) {}
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ToolResult(
                isSuccess = true,
                toolName = "openApp",
                summary = "Launched $rawName",
                responsePayload = JSONObject().apply {
                    put("status", "success")
                    put("message", "Application $rawName was launched on the device.")
                }
            )
        } else {
            // Check settings special intent
            if (trimmed.contains("setting")) {
                val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                ToolResult(
                    isSuccess = true,
                    toolName = "openApp",
                    summary = "Opened Settings",
                    responsePayload = JSONObject().apply {
                        put("status", "success")
                        put("message", "Opened device Settings.")
                    }
                )
            } else {
                ToolResult(
                    isSuccess = false,
                    toolName = "openApp",
                    summary = "App '$rawName' not found on device",
                    responsePayload = JSONObject().apply {
                        put("status", "not_found")
                        put("message", "App '$rawName' does not appear to be installed on this device.")
                    }
                )
            }
        }
    }

    private fun executeSearchAndCallContact(contactName: String): ToolResult {
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPerm) {
            return ToolResult(
                isSuccess = false,
                toolName = "searchAndCallContact",
                summary = "Contacts permission needed to call $contactName",
                responsePayload = JSONObject().apply {
                    put("status", "permission_denied")
                    put("message", "Tell the user sassily that they haven't granted Contacts permission yet so you cannot read their phone book.")
                }
            )
        }

        // Search contact
        val phoneNumber = lookupPhoneNumber(contactName)
        if (phoneNumber == null) {
            return ToolResult(
                isSuccess = false,
                toolName = "searchAndCallContact",
                summary = "Contact '$contactName' not found",
                responsePayload = JSONObject().apply {
                    put("status", "contact_not_found")
                    put("message", "No phone number found for '$contactName' in the contacts book.")
                }
            )
        }

        // If CALL_PHONE granted, trigger ACTION_CALL; otherwise gracefully trigger ACTION_DIAL
        val callIntent = if (hasCallPerm) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        context.startActivity(callIntent)

        val actionDesc = if (hasCallPerm) "Calling $contactName ($phoneNumber)" else "Opening dialer for $contactName ($phoneNumber)"
        return ToolResult(
            isSuccess = true,
            toolName = "searchAndCallContact",
            summary = actionDesc,
            responsePayload = JSONObject().apply {
                put("status", "success")
                put("contactName", contactName)
                put("phoneNumber", phoneNumber)
                put("action", if (hasCallPerm) "placed_direct_call" else "opened_dialer")
                put("message", "Successfully started call to $contactName.")
            }
        )
    }

    private fun executeSendWhatsAppMessage(contactName: String, message: String): ToolResult {
        // Try looking up contact phone if it is a name
        val phoneFromContacts = if (contactName.any { it.isLetter() }) {
            lookupPhoneNumber(contactName)
        } else {
            contactName
        }

        val cleanedPhone = phoneFromContacts?.replace(Regex("[^0-9+]"), "")

        val intent = if (!cleanedPhone.isNullOrBlank()) {
            val url = "https://api.whatsapp.com/send?phone=${Uri.encode(cleanedPhone)}&text=${Uri.encode(message)}"
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // General WhatsApp share intent
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                isSuccess = true,
                toolName = "sendWhatsAppMessage",
                summary = "Opening WhatsApp for $contactName",
                responsePayload = JSONObject().apply {
                    put("status", "success")
                    put("message", "WhatsApp was opened with pre-filled message for $contactName: '$message'")
                }
            )
        } catch (_: Exception) {
            // Fallback: try opening via browser or generic text share
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(genericIntent)
                ToolResult(
                    isSuccess = true,
                    toolName = "sendWhatsAppMessage",
                    summary = "Shared message (WhatsApp fallback)",
                    responsePayload = JSONObject().apply {
                        put("status", "fallback_share")
                        put("message", "WhatsApp app was not found, opened system share for the message.")
                    }
                )
            } catch (ex: Exception) {
                ToolResult(
                    isSuccess = false,
                    toolName = "sendWhatsAppMessage",
                    summary = "WhatsApp messaging failed",
                    responsePayload = JSONObject().apply {
                        put("status", "error")
                        put("message", "Could not launch WhatsApp or share intent: ${ex.localizedMessage}")
                    }
                )
            }
        }
    }

    private fun executeSendGmail(recipientEmail: String, subject: String, body: String): ToolResult {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipientEmail")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Try targeting Gmail package if available
        try {
            val pm = context.packageManager
            val gmailIntent = Intent(emailIntent).apply { setPackage("com.google.android.gm") }
            if (gmailIntent.resolveActivity(pm) != null) {
                context.startActivity(gmailIntent)
            } else {
                context.startActivity(emailIntent)
            }
        } catch (_: Exception) {
            context.startActivity(emailIntent)
        }

        return ToolResult(
            isSuccess = true,
            toolName = "sendGmail",
            summary = "Drafted email to $recipientEmail",
            responsePayload = JSONObject().apply {
                put("status", "success")
                put("recipient", recipientEmail)
                put("subject", subject)
                put("message", "Opened email composer with recipient $recipientEmail and subject '$subject'.")
            }
        )
    }

    private fun lookupPhoneNumber(nameQuery: String): String? {
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasContactsPerm) return null

        var cursor: Cursor? = null
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$nameQuery%")

            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    return cursor.getString(numberIndex)
                }
            }
        } catch (_: Exception) {
            // Ignore cursor exceptions
        } finally {
            cursor?.close()
        }
        return null
    }
}

data class ToolResult(
    val isSuccess: Boolean,
    val toolName: String,
    val summary: String,
    val responsePayload: JSONObject
)
