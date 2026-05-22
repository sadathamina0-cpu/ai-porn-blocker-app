package com.example

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentHashMap

class PurityAccessibilityService : AccessibilityService() {

    // A rolling cache of recently processed and replaced texts to completely prevent infinite cycles.
    private val processedCleanTexts = ConcurrentHashMap<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!PreferencesManager.isProtectionEnabled(this)) {
            return
        }

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            // Direct source scanning
            val source = event.source
            if (source != null) {
                try {
                    scanAndProcess(source)
                } catch (e: Exception) {
                    Log.e("PurityService", "Error scanning source node", e)
                } finally {
                    try {
                        source.recycle()
                    } catch (e: Exception) {
                        // Already recycled or released
                    }
                }
            }

            // High-reliability fallback: scan the currently focused input field across any app window
            val focusedNode = try {
                findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            } catch (e: Exception) {
                null
            }
            if (focusedNode != null) {
                try {
                    scanAndProcess(focusedNode)
                } catch (e: Exception) {
                    Log.e("PurityService", "Error scanning focused input node", e)
                } finally {
                    try {
                        focusedNode.recycle()
                    } catch (e: Exception) {
                        // Already recycled
                    }
                }
            }
        }
    }

    private fun scanAndProcess(node: AccessibilityNodeInfo?) {
        if (node == null) return

        if (node.isEditable) {
            processEditableNode(node)
        } else {
            // Recurse children to find deep embedded input fields (such as inside Google Chrome and custom frameworks)
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = try {
                    node.getChild(i)
                } catch (e: Exception) {
                    null
                }
                if (child != null) {
                    scanAndProcess(child)
                    try {
                        child.recycle()
                    } catch (e: Exception) {
                        // Prevent crash if already recycled
                    }
                }
            }
        }
    }

    private fun processEditableNode(source: AccessibilityNodeInfo) {
        val rawText = source.text ?: return
        val currentText = rawText.toString()
        if (currentText.isEmpty()) return

        // 1. Check loop prevention cache
        if (shouldSkipProcessing(currentText)) {
            return
        }

        val blockedWords = PreferencesManager.getBlockedWords(this)
            .filter { it.isNotEmpty() }
            .sortedByDescending { it.length } // Longest first to cleanly eliminate compound adult phrases first!

        if (blockedWords.isEmpty()) return

        val caseInsensitive = PreferencesManager.isCaseInsensitive(this)
        val replacement = PreferencesManager.getReplacementText(this)

        var hasMatch = false
        var cleanText = currentText

        for (word in blockedWords) {
            // Fast checklist before running replacement routines
            if (caseInsensitive) {
                if (cleanText.contains(word, ignoreCase = true)) {
                    hasMatch = true
                    cleanText = cleanText.replace(word, replacement, ignoreCase = true)
                }
            } else {
                if (cleanText.contains(word)) {
                    hasMatch = true
                    cleanText = cleanText.replace(word, replacement)
                }
            }
        }

        // Only command updates if text contains blocked phrases and differs (fully cycle robust)
        if (hasMatch && cleanText != currentText) {
            // Cache clean output text to instantly skip subsequent echo callbacks
            markTextAsProcessed(cleanText)

            synchronized(this) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, cleanText)
                
                val setSuccess = source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                if (setSuccess) {
                    // Instantly snap the cursor position back to the end so input flows with absolutely zero lag
                    val selectionArgs = Bundle()
                    selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cleanText.length)
                    selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cleanText.length)
                    source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
                }
            }
        }
    }

    private fun shouldSkipProcessing(text: String): Boolean {
        val lastCommittedTime = processedCleanTexts[text] ?: return false
        // If exact text was set by us within the last 2000ms, skip processing to avoid echo loops
        if (System.currentTimeMillis() - lastCommittedTime < 2000) {
            return true
        } else {
            processedCleanTexts.remove(text)
        }
        return false
    }

    private fun markTextAsProcessed(text: String) {
        // Enforce max footprint cache size to keep battery and RAM usage practically zero
        if (processedCleanTexts.size > 20) {
            val iterator = processedCleanTexts.keys.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        processedCleanTexts[text] = System.currentTimeMillis()
    }

    override fun onInterrupt() {
        Log.w("PurityService", "Purity Lock service interrupted by operating system")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("PurityService", "Purity Lock active shielding fully integrated and operational")
    }
}
