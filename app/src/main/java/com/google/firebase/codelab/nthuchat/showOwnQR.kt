package com.google.firebase.codelab.nthuchat


import android.Manifest.permission.CAMERA
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import com.github.sumimakito.awesomeqr.AwesomeQrRenderer
import com.github.sumimakito.awesomeqr.RenderResult
import com.github.sumimakito.awesomeqr.option.RenderOption
import com.github.sumimakito.awesomeqr.option.background.BlendBackground
import com.github.sumimakito.awesomeqr.option.color.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.fragment_show_own_qr.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

private const val TAG = "showOwnQR"
private const val REQUEST_CODE_CAMERA = 2

class showOwnQR : Fragment() {

    private var qrImageView: ImageView? = null
    private var mUid: String? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mFirebaseUser: FirebaseUser? = null
    private var cameraBtn: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //you can set the title for your toolbar here for different fragments different titles
        activity!!.title = getString(R.string.addFriend)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val contentView = inflater.inflate(R.layout.fragment_show_own_qr, container, false)

        qrImageView = contentView.findViewById(R.id.qrImageView)

        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseUser = mFirebaseAuth!!.getCurrentUser()
        mUid = mFirebaseUser!!.uid
        Log.d(TAG, "mUid: $mUid")

        val color = Color()
        color.light = 0xFFFFFFFF.toInt() // for blank spaces
        color.dark = 0xFF000000.toInt() // for non-blank spaces
        color.background = 0xFFFFFFFF.toInt()

        val renderOption = RenderOption()
        renderOption.content = mUid.toString() // content to encode
        renderOption.size = 800 // size of the final QR code image
        renderOption.borderWidth = 20 // width of the empty space around the QR code
        renderOption.clearBorder = true // if set to true, the background will NOT be drawn on the border area
        renderOption.color = color



        try {
            val result = AwesomeQrRenderer.render(renderOption)
            if (result.bitmap != null) {
                qrImageView!!.setImageBitmap(result.bitmap)
            } else if (result.type == RenderResult.OutputType.GIF) {
                // If your Background is a GifBackground, the image
                // will be saved to the output file set in GifBackground
                // instead of being returned here. As a result, the
                // result.bitmap will be null.
            } else {
                // Oops, something gone wrong.
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Oops, something gone wrong.
        }

        return contentView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        cameraBtn = menu!!.findItem(R.id.cameraBtn)
        cameraBtn!!.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId) {
            R.id.cameraBtn -> {
                val hasCameraPermission = ContextCompat.checkSelfPermission(activity!!, CAMERA)
                Log.d(TAG, "hasCameraPermission: $hasCameraPermission")
                if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {

                    if(ActivityCompat.shouldShowRequestPermissionRationale(activity!!, CAMERA)) {
                        ActivityCompat.requestPermissions(activity!!, arrayOf(CAMERA), REQUEST_CODE_CAMERA)
                    } else {
                        //The user has permanently denied the permission, directly bring them to the setting
                        Log.d(TAG, " Launching setting")
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        var uri = Uri.fromParts("package", this.toString(), null)
                        Log.d(TAG, "Uri is$uri")
                        intent.data = uri
                        this.startActivity(intent)
                    }
                } else {
                    //QR addFriend
                    val transaction = fragmentManager!!.beginTransaction()
                    transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                    transaction.replace(R.id.content_frame, QR_scanner())
                    transaction.commit()
                    Log.d(TAG, "has camera permission")
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
