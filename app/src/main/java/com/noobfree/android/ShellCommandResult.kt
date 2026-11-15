package com.noobfree.android

import android.os.Parcel
import android.os.Parcelable

data class ShellCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false
) : Parcelable {

    val isSuccess: Boolean
        get() = exitCode == 0 && !timedOut

    constructor(parcel: Parcel) : this(
        exitCode = parcel.readInt(),
        stdout = parcel.readString().orEmpty(),
        stderr = parcel.readString().orEmpty(),
        timedOut = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(exitCode)
        parcel.writeString(stdout)
        parcel.writeString(stderr)
        parcel.writeByte(if (timedOut) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ShellCommandResult> {
        override fun createFromParcel(parcel: Parcel): ShellCommandResult = ShellCommandResult(parcel)
        override fun newArray(size: Int): Array<ShellCommandResult?> = arrayOfNulls(size)
    }
}
