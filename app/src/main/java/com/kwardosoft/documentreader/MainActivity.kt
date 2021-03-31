package com.kwardosoft.documentreader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudDocumentTextDetector
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    val REQUEST_IMAGE_CAPTURE = 1

    var imageBitmap: Bitmap? = null

    var tts: TextToSpeech? = null

    private var mScannedImage: ImageView? = null
    private var mScannedText: TextView? = null
    private var mScanButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mScannedImage = findViewById(R.id.scanned_image)
        mScannedText = findViewById(R.id.scanned_text)
        mScanButton = findViewById(R.id.scan_button)

        tts = TextToSpeech(this, this)

        mScanButton?.setOnClickListener(View.OnClickListener {
            dispatchTakePictureIntent()
        })

    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageBitmap = data?.extras?.get("data") as Bitmap
            mScannedImage?.setImageBitmap(imageBitmap)
            detectTextFromImage()
        }
    }

    private fun detectTextFromImage() {
        val firebaseVisionImage: FirebaseVisionImage? = FirebaseVisionImage.fromBitmap(imageBitmap!!)
        val firebaseVisionTextDetector: FirebaseVisionTextDetector? = FirebaseVision.getInstance().visionTextDetector
        firebaseVisionTextDetector?.detectInImage(firebaseVisionImage!!)?.addOnSuccessListener(OnSuccessListener<FirebaseVisionText> {
            displayTextFromImage(it)
        })?.addOnFailureListener(OnFailureListener {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
        })
    }

    private fun displayTextFromImage(firebaseVisionText: FirebaseVisionText) {
        val blockList: MutableList<FirebaseVisionText.Block>? = firebaseVisionText.blocks
        if (blockList?.size!! == 0) {
            Toast.makeText(this, "No Text Found", Toast.LENGTH_SHORT).show()
        } else {
            var string = ""
            blockList.forEach{
                string += it.text
            }
            mScannedText?.text = string
            speakOut()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.UK)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "This language is not supported", Toast.LENGTH_SHORT).show()
            } else {

            }

        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakOut() {
        val text = mScannedText?.text
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}