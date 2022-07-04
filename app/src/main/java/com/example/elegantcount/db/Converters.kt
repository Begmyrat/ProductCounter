package com.example.elegantcount.db

import androidx.room.TypeConverter
import com.example.elegantcount.model.Source

class Converters {

    @TypeConverter
    fun fromSource(source: Source): String{
        return source.periodNo.toString()
    }

    @TypeConverter
    fun toSource(name: String): Source {
        return Source(name,name)
    }
}