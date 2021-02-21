package com.ouluuni21.assistedreminder

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.widget.ImageView
import android.widget.TextView

class SpeechListener (private val editText: TextView, private val micButton: ImageView) : RecognitionListener {
    override fun onReadyForSpeech(bundle: Bundle) { }

    override fun onBeginningOfSpeech() {
        //editText.setText("");
        editText.hint = "Listening..."
    }

    override fun onRmsChanged(v: Float) { }

    override fun onBufferReceived(bytes: ByteArray) { }

    override fun onEndOfSpeech() { }

    override fun onError(i: Int) { }

    override fun onResults(bundle: Bundle) {
        val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val builder = StringBuilder()
        micButton.setImageResource(R.drawable.ic_mic_off)
        if (editText.text.isNotEmpty()) {
            editText.text = builder.append(editText.text)
                    .append(" ")
                    .append(data?.get(0).toString())
        }
        else
            editText.text = data?.get(0).toString()
    }

    override fun onPartialResults(bundle: Bundle) { }

    override fun onEvent(i: Int, bundle: Bundle) { }
}