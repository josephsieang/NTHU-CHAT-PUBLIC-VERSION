package com.google.firebase.codelab.nthuchat

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

import com.google.firebase.database.Query

@Entity(tableName = "user")
class User {

    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    @ColumnInfo(name = "div")
    var Div: String = ""

    @ColumnInfo(name = "classes")
    var Classes: String = ""
}