package com.example.elegantcount.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.elegantcount.MainActivity
import com.example.elegantcount.model.Product
import com.example.elegantcount.R
import com.example.elegantcount.databinding.FragmentQrReaderBinding
import com.example.elegantcount.db.ProductDatabase
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import android.widget.AdapterView.OnItemClickListener
import java.io.FileInputStream
import org.apache.poi.ss.usermodel.*
import java.io.File

class FragmentQrReader : Fragment(), View.OnClickListener {

    private var barcodeDetector: BarcodeDetector? = null
    private var cameraSource: CameraSource? = null
    val REQUEST_CAMERA_PERMISSION = 1234
    var productList = mutableListOf<Product>()
    var isReadyToScan = true
    var drugNames = mutableListOf<String>()
    lateinit var binding: FragmentQrReaderBinding
    lateinit var db: ProductDatabase
    var selectedDrugName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQrReaderBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, drugNames)
        binding.autoCompleteIlac.setAdapter(adapter)
        db = ProductDatabase(requireContext())

        initQrReader()
        addListeners()
//        addDrugs()

        barcodeDetector?.setProcessor(object : Detector.Processor<Barcode?> {
            override fun release() {
                Toast.makeText(
                    context,
                    "To prevent memory leaks barcode scanner has been stopped",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode?>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() != 0) {
                    binding.tData.post(Runnable {
                        if (isReadyToScan && barcodes.valueAt(0) != null && barcodes.valueAt(0)!!.displayValue != null) {
                            binding.tData.removeCallbacks(null)
//                            binding.tData.text = barcodes.valueAt(0)!!.displayValue
                            val response = barcodes.valueAt(0)!!.displayValue
                            binding.tData.text = response
                            response.replace("","#")
                            var temp = ""
                            response.forEachIndexed { i, char ->
                                if(!char.isDigit() && !char.isLetter()){
                                    temp += "#"
                                }
                                else{
                                    temp += char
                                }
                            }
                            showText()

                            temp = temp.substring(temp.indexOf("#")+1)
                            var id = temp.substring(0, temp.indexOf("#"))
                            temp = temp.substring(temp.indexOf("#")+1)
                            var expire = temp.substring(2, 8)
                            var partyNo = temp.substring(11)

//                            Log.d("data", partyNo)
//                            val i17 = response.indexOf("17")
//                            val id = response.substring(4, i17)
//                            val expire = response.substring(i17+2, i17+8)
//                            val partyNo = response.substring(i17+10)
//
//                            val data = response.split("\n")
//
//                            Log.d("gelendata:", data.toString())
                            vibratePhone()
//
                            GlobalScope.launch(Dispatchers.IO) {
                                db.getProductDao().updateOrInsert(Product(pId = id, periodNo = partyNo, expireDate = expire, boxNumber = binding.tiEczane.text.toString(), name = ""))
                            }

                            isReadyToScan = false
                            try {
                                val jsonObject = JSONObject(response)
                                Log.d("CLIENT: ", jsonObject.toString())

                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    })
                }
            }
        })
    }
    private fun showText() {
        Log.d("secretData:",binding.tData.text.toString())
//        binding.tData.text.forEach {
//            Log.d("char:", ".${it.toString()}.")
//        }
        Log.d("split: ", binding.tData.text.split("").toString())
    }

    private fun addDrugs() {
        drugNames.add("Analgin")
        drugNames.add("Dimedrol")
        drugNames.add("Amadey At")
        drugNames.add("Trimol")
        drugNames.add("Trimodol")
        drugNames.add("Trivega")

        val filepath = "src/main/assets/drugs.xlsx"
//        val inputStream = FileInputStream(filepath)
        val inputStream = requireContext().assets.open("drugs.xlsx")
        //Instantiate Excel workbook using existing file:
        var xlWb = WorkbookFactory.create(inputStream)
        //Row index specifies the row in the worksheet (starting at 0):
        val rowNumber = 0
        //Cell index specifies the column within the chosen row (starting at 0):
        val columnNumber = 0
        //Get reference to first sheet:
        val xlWs = xlWb.getSheetAt(0)
        println(xlWs.getRow(rowNumber).getCell(columnNumber))

    }

    private fun getExcelFile(): File? {
//        return getFileFromAssets(requireContext(), "drugs.xlsx")
        return File("src/main/assets/drugs.xlsx")
    }

    @Throws(IOException::class)
    fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

    private fun readExcelAsWorkbook(): Workbook?{

        //Reading excel file
        getExcelFile()?.let {
            try {

                //Reading excel file as stream
                val inputStream = FileInputStream(it)

                //Return workbook
                return WorkbookFactory.create(inputStream)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        return null
    }

    private fun getSheet(): Sheet? {

        //Getting workbook
        readExcelAsWorkbook()?.let { workbook ->

            //Checking that sheet exist
            //This function will also tell you total number of sheets
            if (workbook.numberOfSheets > 0) {

                //Return first sheet of excel. You can get all existing sheets
                return workbook.getSheetAt(0)
            }
        }

        return null
    }

    private fun getRow(){
        //get sheet
        getSheet()?.let{ sheet ->

            //To find total number of rows
            val totalRows = sheet.physicalNumberOfRows

            //Total number of cells of a row
            val totalColumns = sheet.getRow(0).physicalNumberOfCells

            //Get value of first cell from row
            sheet.getRow(0).getCell(0)
        }
    }

    private fun initQrReader() {
        barcodeDetector = BarcodeDetector.Builder(requireContext())
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        cameraSource = CameraSource.Builder(
            requireContext(),
            barcodeDetector
        ).setAutoFocusEnabled(true).build()

        val surfaceHeight: Int = 100
        val surfaceWidth: Int = 100
        val lp: ViewGroup.LayoutParams = binding.surfaceView.layoutParams

        binding.surfaceView.layoutParams = lp

        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraSource?.start(binding.surfaceView.getHolder())
                    } else {
                        activity?.let {
                            ActivityCompat.requestPermissions(
                                it,
                                arrayOf(Manifest.permission.CAMERA),
                                REQUEST_CAMERA_PERMISSION
                            )
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource?.stop()
            }
        })
    }

    private fun addListeners() {
        binding.bSave.setOnClickListener(this)
        binding.bScan.setOnClickListener(this)

        binding.autoCompleteIlac.onItemClickListener =
            OnItemClickListener { parent, arg1, position, arg3 ->
                selectedDrugName = drugNames[position]
            }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.b_save -> {
                (activity as MainActivity)?.username = binding.tiName.text.toString()
                (activity as MainActivity)?.boxNumber = binding.tiEczane.text.toString()
                findNavController().navigate(R.id.action_fragmentQrReader_to_fragmentProductList)
            }
            R.id.b_scan -> {
                isReadyToScan = true
//                getRow()
            }
        }
    }

    fun Fragment.vibratePhone() {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(400)
        }
    }
}