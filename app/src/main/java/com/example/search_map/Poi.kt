package com.example.search_map

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

class Poi private constructor(parcel: Parcel) : Parcelable {

    var id: String
    var location: Location
    var title: String
    var address: String = ""

    override fun describeContents(): Int { //описание содержания
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) { //передача в Parsel
        dest.writeString(this.id)
        dest.writeParcelable(this.location, flags)
        dest.writeString(this.title)
        dest.writeString(this.address)
    }

    init {
        this.id = parcel.readString()!!
        this.location = parcel.readParcelable(Location::class.java.classLoader)!!
        this.title = parcel.readString()!!
        this.address = parcel.readString()!!
    }

    override fun toString(): String {
        return "Poi{" + "id='" + id + '\''.toString() + ", location=" + location + ", title='" +
                title + '\''.toString() + ", address='" + address + '\''.toString() + '}'.toString()
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Poi> = object : Parcelable.Creator<Poi> {
            override fun createFromParcel(source: Parcel): Poi {
                return Poi(source)
            }

            override fun newArray(size: Int): Array<Poi?> {
                return arrayOfNulls(size)
            }
        }
    }
}
