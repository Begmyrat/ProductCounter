package com.example.elegantcount.fragment

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elegantcount.R
import com.example.elegantcount.adapter.ProductListAdapter
import com.example.elegantcount.databinding.FragmentProductListBinding
import com.example.elegantcount.db.ProductDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.FileOutputStream
import java.io.IOException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

import org.apache.poi.hssf.usermodel.HeaderFooter.file
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.provider.Settings
import android.widget.ArrayAdapter
import androidx.core.content.FileProvider
import com.example.elegantcount.MainActivity
import com.example.elegantcount.model.Product
import java.io.FileNotFoundException

class FragmentProductList : Fragment(), View.OnClickListener{

    private lateinit var binding: FragmentProductListBinding
    lateinit var productAdapter: ProductListAdapter
    private lateinit var db: ProductDatabase
    private lateinit var requestPermission: ActivityResultLauncher<Array<String>>
    var path = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductListBinding.inflate(layoutInflater, container, false)

        requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            permissions.forEach { actionMap ->
                when(actionMap.key){
                    Manifest.permission.READ_EXTERNAL_STORAGE ->{
                        if(actionMap.value){
//                            shareFile(path)
                        }else{
                            Toast.makeText(context, "Sorry, Unable to take location without permission", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        prepReadWritePermissions()

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = ProductDatabase(requireContext())
        initRecyclerView()

        getDataFromDB()
        addListener()
    }

    private fun prepReadWritePermissions() {
        if(ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ){
//            shareFile(path)
        }else{
            val permissionRequest = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestPermission.launch(permissionRequest)
        }
    }

    lateinit var boxAdapter: ArrayAdapter<String>
    var boxGroup = mutableListOf<String>()
    lateinit var allData: List<Product>

    private fun getDataFromDB() {
        GlobalScope.launch(Dispatchers.IO) {
            allData = db.getProductDao().getAllProducts()
            boxGroup = allData.groupBy { it.boxNumber }.map { it.key } as MutableList<String>
            boxGroup.add(0,"All")

                GlobalScope.launch(Dispatchers.Main){

                boxAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, boxGroup)
                binding.autoCompleteBox.setAdapter(boxAdapter)
                productAdapter.differ.submitList(allData)
                productAdapter.notifyDataSetChanged()


            }
        }
    }

    private fun addListener() {

        binding.bConvertToExcelAll.setOnClickListener(this)
        binding.bConvertToExcelGroup.setOnClickListener(this)

        productAdapter.setOnItemClickListener { product ->
            GlobalScope.launch(Dispatchers.IO) {
                db.getProductDao().deleteProduct(product)
                getDataFromDB()
            }
        }

        binding.autoCompleteBox.setOnItemClickListener { adapterView, view, i, l ->
            binding.autoCompleteBox.text
            updateData(i)
        }
    }

    private fun updateData(i: Int) {
        val s = boxGroup[i]
        val a = allData.filter { it.boxNumber.equals(s) }
        if(i>0)
            productAdapter.differ.submitList(a)
        else
            productAdapter.differ.submitList(allData)
        productAdapter.notifyDataSetChanged()
    }

    private fun initRecyclerView() {
        productAdapter = ProductListAdapter()
        binding.recyclerview.apply{
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.b_convertToExcelAll -> {
                convertToExcel()
            }
            R.id.b_convertToExcelGroup -> {
                convertToExcelGroup()
            }
        }
    }

    private fun convertToExcelGroup() {
        val hssfWorkbook = HSSFWorkbook()
        val hssfSheet = hssfWorkbook.createSheet("Group - ${(activity as MainActivity)?.boxNumber}")

        val hssfRowTitleUser = hssfSheet.createRow(0)
        hssfRowTitleUser.createCell(0).setCellValue((activity as MainActivity)?.username)

        val hssfRowTitle = hssfSheet.createRow(1)
        hssfRowTitle.createCell(0).setCellValue("Name")
        hssfRowTitle.createCell(1).setCellValue("PartyNo")
        hssfRowTitle.createCell(2).setCellValue("ExpireDate")
        hssfRowTitle.createCell(3).setCellValue("Count (PartyNo)")
        hssfRowTitle.createCell(4).setCellValue("Count (Name)")
        //--------------
        //--------------
        var row = 2
        var srNo = 1

        val group = productAdapter.differ.currentList.groupBy { it.periodNo }

        for (a in group) {
            val list = a.value
            val hssfRow = hssfSheet.createRow(row)
            hssfRow.createCell(0).setCellValue(list?.get(0)?.name)
            hssfRow.createCell(1).setCellValue(list?.get(0)?.periodNo)
            hssfRow.createCell(2).setCellValue(list?.get(0)?.expireDate)
            hssfRow.createCell(3).setCellValue(list?.size.toString())
            val size = productAdapter.differ.currentList.filter { it.name == list[0].name }.size
            hssfRow.createCell(4).setCellValue(size.toString())
            row++
            srNo++
        }
        //---------
        //---------
//        val path =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + File.separator)
//        val file = File(path.toString())
//        file.mkdirs()
//        val fileName = path.toString() + "/" + "transaction_" + System.currentTimeMillis() + ".xls"
//        try {
//            val fileOutputStream = FileOutputStream(fileName)
//            hssfWorkbook.write(fileOutputStream)
//            fileOutputStream.flush()
//            fileOutputStream.close()
//            Toast.makeText(context, "File downloaded successfully \n $path", Toast.LENGTH_LONG).show()
//            Log.e("EXCELSUCCESS: ", path.toString())
//            this.path = fileName
////            prepReadWritePermissions()
//            shareFile(fileName)
//        } catch (e: IOException) {
//            Log.e("EXCELFAILED: ", e.message.toString())
//            e.printStackTrace()
//        }

        //Get App Director, APP_DIRECTORY_NAME is a string
        val appDirectory = requireContext().getExternalFilesDir("Download")

        //Check App Directory whether it exists or not, create if not.
        if (appDirectory != null && !appDirectory.exists()) {
            appDirectory.mkdirs()
        }

        //Create excel file with extension .xlsx
        val excelFile = File(appDirectory,"${(activity as MainActivity).boxNumber}.xls")

        //Write workbook to file using FileOutputStream
        try {
            val fileOut = FileOutputStream(excelFile)
            hssfWorkbook.write(fileOut)
            fileOut.close()
            shareFile(excelFile.absolutePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun convertToExcel() {

        val hssfWorkbook = HSSFWorkbook()
        val hssfSheet = hssfWorkbook.createSheet("All - ${(productAdapter.differ.currentList?.get(0))?.boxNumber}")

        val hssfRowTitleUser = hssfSheet.createRow(0)
        hssfRowTitleUser.createCell(0).setCellValue((activity as MainActivity)?.username)

        val hssfRowTitle = hssfSheet.createRow(1)
        hssfRowTitle.createCell(0).setCellValue("id")
        hssfRowTitle.createCell(1).setCellValue("name")
        hssfRowTitle.createCell(2).setCellValue("partyNo")
        hssfRowTitle.createCell(3).setCellValue("expireDate")
        hssfRowTitle.createCell(4).setCellValue("boxNumber")
        //--------------
        //--------------
        var row = 2
        var srNo = 1
        for (a in productAdapter.differ.currentList) {
            val hssfRow = hssfSheet.createRow(row)
            hssfRow.createCell(0).setCellValue(a.pId)
            hssfRow.createCell(1).setCellValue(a.name)
            hssfRow.createCell(2).setCellValue(a.periodNo)
            hssfRow.createCell(3).setCellValue(a.expireDate)
            hssfRow.createCell(4).setCellValue(a.boxNumber)
            row++
            srNo++
        }
        //---------
        //---------
//        val path =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + File.separator)
//
//        val fileName = path.toString() + "/" + "transaction_" + System.currentTimeMillis() + ".xls"
//
//        val file = File(fileName.toString())
//        file.mkdirs()
//
//        try {
//            val fileOutputStream = FileOutputStream(fileName)
//            hssfWorkbook.write(fileOutputStream)
//            fileOutputStream.flush()
//            fileOutputStream.close()
//            Toast.makeText(context, "File downloaded successfully \n $path", Toast.LENGTH_LONG).show()
//            Log.e("EXCELSUCCESS: ", path.toString())
//            this.path = fileName
////            prepReadWritePermissions()
//            shareFile(fileName)
//        } catch (e: IOException) {
//            Log.e("EXCELFAILED: ", e.message.toString())
//            e.printStackTrace()
//        }

        //Get App Director, APP_DIRECTORY_NAME is a string
        val appDirectory = requireContext().getExternalFilesDir("Download")

        //Check App Directory whether it exists or not, create if not.
        if (appDirectory != null && !appDirectory.exists()) {
            appDirectory.mkdirs()
        }

        //Create excel file with extension .xlsx
        val excelFile = File(appDirectory,"${(activity as MainActivity).boxNumber}.xls")

        //Write workbook to file using FileOutputStream
        try {
            val fileOut = FileOutputStream(excelFile)
            hssfWorkbook.write(fileOut)
            fileOut.close()
            shareFile(excelFile.absolutePath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun shareFile(path: String){

        var file = File(path)

        Log.d("hehehe: ", path)

        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                activity?.packageName + ".provider",
                file
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/xls"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Purchase Bill..."
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Sharing Bill purchase items..."
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share Via"))

        }
    }
}