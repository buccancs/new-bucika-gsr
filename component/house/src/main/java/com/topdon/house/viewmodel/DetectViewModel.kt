package com.topdon.house.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.DirDetect
import com.topdon.lib.core.db.entity.HouseDetect
import com.topdon.lib.core.db.entity.ItemDetect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 房屋检测 ViewModel.
 *
 * Created by LCG on 2024/8/22.
 */
class DetectViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * 所有房屋检测列表，调用 [queryAll] 会触发更改.
     */
    val detectListLD =  MutableLiveData<List<HouseDetect>>()
    /**
     * 查询所有房屋检测列表，结果通过 [detectListLD] 返回.
     */
    fun queryAll() {
        viewModelScope.launch(Dispatchers.IO) {
            detectListLD.postValue(AppDatabase.getInstance().houseDetectDao().queryAll())
        }
    }


    /**
     * 一项房屋检测，调用 [queryById]、[insertDefaultDirs] 会触发更改.
     */
    val detectLD = MutableLiveData<HouseDetect?>()
    /**
     * 查询指定 id 的房屋检测数据，结果通过 [detectLD] 返回.
     */
    fun queryById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            detectLD.postValue(AppDatabase.getInstance().houseDetectDao().queryById(id))
        }
    }
    /**
     * 为指定检测插入默认的目录列表，结果通过 [detectLD] 返回.
     */
    fun insertDefaultDirs(houseDetect: HouseDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().houseDetectDao().insertDefaultDirs(houseDetect)
            detectLD.postValue(houseDetect)
        }
    }


    /**
     * 某个房屋检测下的某个目录信息，调用 [queryDirById] 会触发更改，注意目录所属检测信息未加载.
     */
    val dirLD = MutableLiveData<DirDetect>()
    /**
     * 查询指定 id 的目录信息，结果通过 [dirLD] 返回.
     */
    fun queryDirById(dirId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dirLD.postValue(AppDatabase.getInstance().houseDetectDao().queryDir(dirId))
        }
    }


    /**
     * 复制房屋检测结果，调用 [copyDetect] 会触发更改.
     */
    val copyDetectLD = MutableLiveData<Pair<Int, HouseDetect>>()
    /**
     * 复制指定的检测，结果通过 [copyDetectLD] 返回.
     * @param position 不使用，透传
     * @param houseDetect 要复制的检测
     */
    fun copyDetect(position: Int, houseDetect: HouseDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            copyDetectLD.postValue(Pair(position, AppDatabase.getInstance().houseDetectDao().copyDetect(houseDetect)))
        }
    }

    /**
     * 复制房屋检测目录结果，调用 [copyDir] 会触发更改.
     */
    val copyDirLD = MutableLiveData<Pair<Int, DirDetect>>()
    /**
     * 在指定的检测中，复制指定的目录，结果通过 [copyDirLD] 返回.
     * @param layoutIndex 不使用，透传
     * @param dirDetect 要复制的目录
     */
    fun copyDir(layoutIndex: Int, dirDetect: DirDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            val dirList: ArrayList<DirDetect> = dirDetect.houseDetect.dirList
            val position = dirList.indexOf(dirDetect)
            if (position >= 0) {
                val newDir: DirDetect = AppDatabase.getInstance().houseDetectDao().copyDir(dirList, position)
                dirList.add(position + 1, newDir)
                copyDirLD.postValue(Pair(layoutIndex, newDir))
            }
        }
    }

    /**
     * 复制房屋检测项目结果，调用 [copyItem] 会触发更改.
     */
    val copyItemLD = MutableLiveData<Pair<Int, ItemDetect>>()
    /**
     * 在指定的检测中，复制指定的项目，结果通过 [copyItemLD] 返回.
     * @param layoutIndex 不使用，透传
     * @param itemDetect 要复制的项目
     */
    fun copyItem(layoutIndex: Int, itemDetect: ItemDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            val itemList: ArrayList<ItemDetect> = itemDetect.dirDetect.itemList
            val position = itemList.indexOf(itemDetect)
            if (position >= 0) {
                val newItem = AppDatabase.getInstance().houseDetectDao().copyItem(itemList, position)
                itemList.add(position + 1, newItem)
                copyItemLD.postValue(Pair(layoutIndex, newItem))
            }
        }
    }

    /**
     * 删除房屋检测项目结果，调用 [delItem] 会触发更改.
     */
    val delItemLD = MutableLiveData<Pair<Int, ItemDetect>>()
    /**
     * 在指定的检测中，删除指定的项目，结果通过 [delItemLD] 返回.
     * @param layoutIndex 不使用，透传
     * @param itemDetect 要删除的项目
     */
    fun delItem(layoutIndex: Int, itemDetect: ItemDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            val dirDetect: DirDetect = itemDetect.dirDetect
            val itemList: ArrayList<ItemDetect> = dirDetect.itemList
            val position = itemList.indexOf(itemDetect)
            if (position >= 0) {
                AppDatabase.getInstance().houseDetectDao().deleteItem(itemDetect)
                itemList.removeAt(position)

                if (itemList.isEmpty()) {//全部项目都删光了，目录也要删掉
                    val dirList: ArrayList<DirDetect> = dirDetect.houseDetect.dirList
                    val dirPosition = dirList.indexOf(dirDetect)
                    if (dirPosition >= 0) {
                        AppDatabase.getInstance().houseDetectDao().deleteDir(dirDetect)
                        dirList.removeAt(dirPosition)
                    }
                    if (dirList.isEmpty()) {//全部目录都删光了
                        detectLD.postValue(dirDetect.houseDetect)
                    } else {
                        delItemLD.postValue(Pair(layoutIndex, itemDetect))
                    }
                } else {
                    //删除后目录里的3个数量可能需要刷新
                    if (itemDetect.state > 0) {
                        val dir = itemDetect.dirDetect
                        when (itemDetect.state) {
                            1 -> dir.goodCount--
                            2 -> dir.warnCount--
                            3 -> dir.dangerCount--
                        }
                        updateDir(dir)
                    }
                    delItemLD.postValue(Pair(layoutIndex, itemDetect))
                }
            }
        }
    }


    /**
     * 更新目录信息.
     */
    fun updateDir(vararg dirDetect: DirDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().houseDetectDao().updateDir(*dirDetect)
        }
    }
    /**
     * 更新项目信息.
     */
    fun updateItem(vararg itemDetect: ItemDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().houseDetectDao().updateItem(*itemDetect)
        }
    }


    /**
     * 删除指定的房屋检测数据.
     */
    fun deleteMore(vararg houseDetect: HouseDetect) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance().houseDetectDao().deleteDetect(*houseDetect)
            queryAll()
        }
    }
}