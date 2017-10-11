package com.jiaozhu.accelerider.panel

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.jiaozhu.accelerider.R
import com.jiaozhu.accelerider.commonTools.HttpResponse
import com.jiaozhu.accelerider.commonTools.SelectorRecyclerAdapter
import com.jiaozhu.accelerider.commonTools.SingerPicker
import com.jiaozhu.accelerider.model.FileModel
import com.jiaozhu.accelerider.model.UserModel
import com.jiaozhu.accelerider.panel.adapter.FileAdapter
import com.jiaozhu.accelerider.panel.fragment.CommRecycleFragment
import com.jiaozhu.accelerider.support.HttpClient
import com.jiaozhu.accelerider.support.Preferences
import com.jiaozhu.accelerider.support.Tools
import kotlinx.android.synthetic.main.view_toolbar.*
import kotlinx.android.synthetic.main.view_toolbar_comm.*
import toast
import java.util.*

class MainActivity : BaseActivity(), SelectorRecyclerAdapter.OnItemClickListener {

    private lateinit var fragment: CommRecycleFragment
    private val fileList: ArrayList<FileModel> = arrayListOf()
    private lateinit var adapter: FileAdapter
    private val stack = Stack<FileModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mToolbar)
        adapter = FileAdapter(this, fileList)
        fragment = CommRecycleFragment.newInstance(fileList, adapter)
        supportFragmentManager.beginTransaction().add(R.id.layout, fragment).commit()
        adapter.itemClickListener = this
        adapter.onDownloadClickListener = SelectorRecyclerAdapter.OnItemClickListener { view, position ->
            addDownload(fileList[position])
        }
        init()
    }

    private fun init() {
        if (Preferences.uk.isEmpty())
            getUserList()
        else {
            stack.clear()
            stack.push(FileModel())
            fresh()
        }
    }

    /**
     * 加入下载列表
     */
    private fun addDownload(vararg fileModels: FileModel) {
        spinnerDialog.show()
        spinnerDialog.setTitle("正在获取下载地址")
        HttpClient.getDownloadUrl(fileModels.toList(), object : HttpResponse() {

            override fun onSuccess(statusCode: Int, result: JSONObject) {
                println(result.toJSONString())
                result.getJSONObject("links").entries.forEach {
                    val name = it.key
                    val url = (it.value as List<String>)[0]
                }
            }

            override fun onFailure(statusCode: Int, msg: String?, error: Throwable?) {
                toast(msg)
            }

            override fun onFinish() {
                spinnerDialog.dismiss()
            }

        })
//        DLManager.getInstance(this).dlStart(fileModel)
    }


    /**
     * 获取用户列表
     */
    fun getUserList() {
        spinnerDialog.show()
        spinnerDialog.setTitle("正在获取账户列表")
        HttpClient.getUserList(object : HttpResponse() {
            override fun onSuccess(statusCode: Int, result: JSONObject) {
                if (result.getInteger("errno") == 0) {
                    Preferences.userList = result.getString("userlist")
                    showUserListDialog(JSON.parseArray(Preferences.userList, UserModel::class.java))
                }
            }

            override fun onFailure(statusCode: Int, msg: String, error: Throwable) {
                toast(msg)
            }

            override fun onFinish() {
                spinnerDialog.dismiss()
            }

        })
    }

    /**
     * 弹出账户选择列表
     */
    private fun showUserListDialog(userList: List<UserModel>) {
        SingerPicker.showDialog(this, "账户列表", userList, { position, description ->
            Preferences.uk = userList[position].Uk
            Preferences.name = userList[position].Name
            init()
            fresh()
        }, userList.indexOfFirst { it.Name == Preferences.uk }, true, true)
    }

    /**
     * 刷新文件列表
     * @param   addStack    是否需要进栈
     */
    private fun fresh(file: FileModel? = null, addStack: Boolean = true) {
        spinnerDialog.show()
        spinnerDialog.setTitle("正在获取文件列表")
        HttpClient.getFileList(file?.path ?: "/", object : HttpResponse() {
            override fun onSuccess(statusCode: Int, result: JSONObject?) {
                if (addStack)
                    stack.push(file)
                mTitle.text = Preferences.name + (file?.path ?: "/")
                fileList.clear()
                fileList.addAll(result?.getJSONArray("list")?.toJavaList(FileModel::class.java) as List<FileModel>)
                fragment.adapter?.notifyDataSetChanged()
            }

            override fun onFailure(statusCode: Int, msg: String, error: Throwable) {
                error.printStackTrace()
            }

            override fun onFinish() {
                spinnerDialog.dismiss()
            }

        })
    }


    /**
     * 开始同步
     */
    private fun syn() {
        if (!Tools.isConnect(this)) {
            toast("无可用网络！")
            return
        }
        if (!Tools.isWifi(this)) {
            showNetWarnDialog()
        } else {
        }
    }

    /**
     * 弹出蜂窝网络警告对话框
     */
    private fun showNetWarnDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("同步数据")
        builder.setMessage("您现在处于非WIFI环境，是否继续进行同步？")
        builder.setPositiveButton("继续") { _, _ -> }
        builder.setNegativeButton("取消", null)
        builder.create().show()
    }

    override fun onItemClick(view: View?, position: Int) {
        val model = fileList[position]
        if (model.isdir != 0)
            fresh(model)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_change_user -> getUserList()
            R.id.action_setting -> toSettingActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 进入设定界面
     */
    private fun toSettingActivity() {
        val i = Intent(this@MainActivity, SettingActivity::class.java)
        startActivity(i)
    }


    private var backPressTime: Long = 0
    override fun onBackPressed() {
        if (stack.size > 1) {
            stack.pop()
            fresh(stack.lastElement(), false)
            return
        }
        if (System.currentTimeMillis() - backPressTime > 2000) {
            backPressTime = System.currentTimeMillis()
            Toast.makeText(this, "双击返回键退出应用", Toast.LENGTH_SHORT).show()
        } else {
            finish()
            System.exit(0)
        }
    }
}


