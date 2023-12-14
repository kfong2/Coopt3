package com.example.coopt3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Message
import android.provider.MediaStore
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    //UI Views
    private lateinit var inputImageBtn: MaterialButton
    private lateinit var recognizeTextBtn: MaterialButton
    private lateinit var imageIv: ImageView
    private lateinit var recognizedTextEt: EditText

    private companion object{
        //To handle the result of Camera/Gallery permissions in onRequestPermissonResults
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    //Uri of the image that we will take from Camera/Gallery
    private var imageUri: Uri? = null

    //Arrays of permission required to pick image from Camera/Gallery
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>

    //Progress dialog
    private lateinit var progressDialog: ProgressDialog

    //TextRecognizer
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Init UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn)
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn)
        imageIv = findViewById(R.id.imageIv)
        recognizedTextEt = findViewById(R.id.recognizedTextEt)

        //Init arrays of permissions required for Camera, Gallery
        cameraPermission =  arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //Init setup the permission required for Camera, Gallery
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //Init TextRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        //Handle click, show input image dialog
        inputImageBtn.setOnClickListener {
            showInputImageDialog()

            // Clear the content of the recognizedTextEt
            recognizedTextEt.text.clear()
        }

        //Handle click, start recognizing text from image we took from Camera/Gallery
        recognizeTextBtn.setOnClickListener {
            //Check if image is picked or not, picked if imageUri is not null
            if (imageUri == null) {
                //ImageUri is null, which means we haven't picked image yet, can't recognize text
                showToast("Pick Image First ...")
            }
            else {
                //imageUri is not null, which means we have picked image, we can recognize text
                recognizeTextFromImage()

                // Clear the content of the recognizedTextEt
                recognizedTextEt.text.clear()
            }
        }
    }

    private fun recognizeTextFromImage() {
        //Set message and show progress dialog
        progressDialog.setMessage("Preparing Image ...")
        progressDialog.show()

        // Replace coding to get preloaded picture under gallery for demo in virtural mobile use
        try {
            //Prepare InputImage from image uri
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            //Image prepared, we are about to start text recognition process, change progress message
            progressDialog.setMessage("Recognizing text ...")
            //Start text recognition process from image
            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    //Process completed, dismiss dialog
                    progressDialog.dismiss()
                    //Get the recognized text
                    val recognizedText = text.text
                    //Set the recognized text to edit text
                    recognizedTextEt.setText(recognizedText)
                }
                .addOnFailureListener{e->
                    //Failed recognizing text from image, dismiss dialog, show reason in Toast
                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to ${e.message}")
                }
        }
        catch (e:Exception){
            //Exception occurred while preparing InputImage, dismiss dialog, show reason in Toast
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }
    }

    private fun showInputImageDialog() {
        //Init PopupMenu param 1 is context, param 2 is UI View where you want to show PopupMenu
        val popupMenu = PopupMenu(this, inputImageBtn)

        //Add items Camera, Gallery to PopupMenu, parm 2 is menu id, param 3 is position of this menu item in menu items list,
        //params 4 is title of the menu
        //popupMenu.menu.add(Menu.NONE, 1, 1, "CAMERA")
        //popupMenu.menu.add(Menu.NONE, 2, 2, "GALLERY")
        popupMenu.menu.add(Menu.NONE, 1, 1, getString(R.string.button_camera))
        popupMenu.menu.add(Menu.NONE, 2, 2, getString(R.string.button_gallery))

        //Show PopupMenu
        popupMenu.show()

        //Handle PopupMenu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            //Get item id that is clicked from PopupMenu
            val id = menuItem.itemId
            if (id == 1){
                //Camera is clicked, check if camera permissions are granted or not
                if (checkCameraPermission()){
                    //Camera permissions granted, we can launch camera intent
                    pickImageCamera()
                }
                else{
                    //Camera permissions not granted, request the camera permissions
                    requestCameraPermission()
                }
            }
            else if(id == 2){
                //Gallery is clicked, check if storage permission is granted or not
                if (checkStoragePermission()){
                    //Storage permission granted, we can launch the gallery intent
                    pickImageGallery()
                }
                else{
                    //Storage permission not granted, request the storage permission
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun pickImageGallery(){
        //Intent to pick image from gallery will show all resources from where we can pick the image
        val intent = Intent(Intent.ACTION_PICK)
        //Set type of file we want to pick i.e. image
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            //Here we will receive the image, if picked
            if (result.resultCode == Activity.RESULT_OK){
                //Image picked
                val data = result.data
                imageUri = data!!.data
                //Set to imageView i.e. imageIv
                imageIv.setImageURI(imageUri)
            }
            else{
                //Cancelled
                showToast("Cancelled...!")
            }
        }

    private fun pickImageCamera(){
        //Get ready the image data to store in MediaStore
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")
        //Image Uri
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //Intent to launch camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            //Here we will receive the image, if taken from camera
            if (result.resultCode == Activity.RESULT_OK){

                //Image is taken- from camera
                //We already have the image in imageUri using function pickImageCamera
                imageIv.setImageURI(imageUri)
            }
            else{
                //Cancelled
                showToast("Cancelled....")
            }
        }

    private fun checkStoragePermission() : Boolean{
        /*Check if storage permission is allowed or not
        return true if allowed, false id not allowed*/
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() : Boolean{
        /*Check if camera & storage permissions are allowed or not
        return true if allowed, false id not allowed*/
        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return cameraResult && storageResult
    }

    private fun requestStoragePermission(){
        //Request storage permission (for gallery image pick)
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermission(){
        //Request camera permissions (for camera intent)
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permission: Array<out String>,
        grantResults: IntArray
    ){
        super.onRequestPermissionsResult(requestCode, permission, grantResults)
        //Handle permission(s) results
        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                //Check if some action from permission dialog performed or not Allow/Deny
                if (grantResults.isNotEmpty()){
                    //Check if Camera, Storage permission granted, contains boolean results either true or false
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    //Check if both permission are granted or not
                    if (cameraAccepted && storageAccepted){
                        //Both permissions (Camera & Gallery) are granted, we can launch camera intent
                        pickImageCamera()
                    }
                    else{
                        //One or both permissions are denied, can't launch camera intent
                        showToast("Camera & Storage permission are required ...")
                    }
                }
            }

            STORAGE_REQUEST_CODE ->{
                //Check if some action from permission dialog performed or not Allow/Deny
                if (grantResults.isNotEmpty()){
                    //Check if Storage permission granted, contains boolean resutls either true or false
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    //Check if storage permission is granted or not
                    if (storageAccepted){
                        //Storage permission granted, we can launch gallery intent
                        pickImageGallery()
                    }
                    else{
                        //Storage permission denied, can't launch gallery intent
                        showToast("Storage permission is required ...")
                    }
                }
            }
        }
    }

    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}


