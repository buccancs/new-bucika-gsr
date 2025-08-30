package com.guide.zm04c.matrix

class RingBuffer {

    private lateinit var byteArray: ByteArray

    //读取byte数组的位置
    private var mReadPositon = 0

    //未被读取数据的长度
    private var mUnReadLength = 0

    /**
     * Create a new RingBuffer of the specified size.
     * @param size The size in bytes of the RingBuffer.
     */
    constructor(size: Int) {
        byteArray = ByteArray(size)
    }

    /**
     * Turn an existing byte array into a RingBuffer.
     * @param buffer A byte array to be used as a RingBuffer.
     */
    constructor(buffer: ByteArray) {
        byteArray = buffer
    }

    /**
     * Turn a byte array which already contains data into a RingBuffer.
     * @param buffer A byte array to be used as a RingBuffer.
     * @param tail The pointer to the beginning of the data in the array.
     * @param length The length of the data in the array.
     */
    constructor(buffer: ByteArray, tail: Int, length: Int) {
        byteArray = buffer
        mReadPositon = tail
        mUnReadLength = length
    }

    /**
     * Write to the RingBuffer from a byte array.
     * If the write exceeds the free space in the RingBuffer, only part of the
     * data will be written.
     *
     * @param buffer A byte array from which the data will be copied.
     * @param offset The offset in the byte array where the data begins.
     * @param length The number of bytes to be written.
     * @return The number of bytes successfully written to the RingBuffer.
     * This may be less than the requested length if there is insufficient free
     * space in the RingBuffer, or zero if the RingBuffer is full.
     */
    fun write(buffer: ByteArray?, offset: Int, length: Int): Int {
        var head: Int
        var toEnd: Int
        var toWrite: Int
        synchronized(this) {
            head = (mReadPositon + mUnReadLength) % byteArray.size
            toEnd = byteArray.size - head
            // if the request exceeds the free space, write as much as possible
            toWrite = Math.min(length, byteArray.size - mUnReadLength)
        }
        if (toWrite > 0) {
            if (toWrite > toEnd) {
                // write from the head to the end
                System.arraycopy(buffer!!, offset, byteArray, head, toEnd)
                // write the remainder from the beginning
                System.arraycopy(buffer!!, offset + toEnd, byteArray, 0, toWrite - toEnd)
            } else {
                // write the whole thing at once
                System.arraycopy(buffer!!, offset, byteArray, head, toWrite)
            }

            // writing increases the length
            synchronized(this) { mUnReadLength += toWrite }
        }
        return toWrite
    }

    /**
     * Read from the RingBuffer into a byte array.
     *
     * @param buffer A byte array in which the read data will be placed.
     * @param offset The offset in the byte array where the read data should be placed.
     * @param length The number of bytes to be read.
     * @return The number of bytes successfully read from the RingBuffer.
     * This may be less than the requested length if there were fewer bytes in
     * the buffer, or zero if the buffer was empty.
     */
    fun read(buffer: ByteArray?, offset: Int, length: Int): Int {
        var toEnd: Int
        var toRead: Int
        synchronized(this) {
            toEnd = byteArray.size - mReadPositon
            // if the request exceeds the available data, read as much as is available
            toRead = Math.min(length, mUnReadLength)
        }
        if (toRead > toEnd) {
            // read from the tail to the end
            System.arraycopy(byteArray, mReadPositon, buffer, offset, toEnd)
            // read the requested remainder from the beginning
            System.arraycopy(byteArray, 0, buffer, offset + toEnd, toRead - toEnd)
        } else {
            // read the whole requested thing at once
            System.arraycopy(byteArray, mReadPositon, buffer, offset, toRead)
        }

        // reading moves the tail and decreases the length
        synchronized(this) {
            mReadPositon = (mReadPositon + toRead) % byteArray.size
            mUnReadLength -= toRead
        }
        return toRead
    }


    //向前移动length个字节
    fun moveForward(length: Int): Int {
        synchronized(this) {
            mReadPositon = (mReadPositon + length) % byteArray.size
            mUnReadLength -= length
        }
        return length
    }

    //向后移动length个字节
    fun moveBack(length: Int): Int {
        synchronized(this) {
            if (mReadPositon > length) {
                mReadPositon -= length
            } else {
                mReadPositon = mReadPositon - length + byteArray.size
            }
            mUnReadLength += length
        }
        return length
    }

    /**
     * Get the length of the data contained in the RingBuffer.
     * @return The length of the data in bytes.
     */
    fun getUnReadLength(): Int {
        return mUnReadLength
    }

    /**
     * Get the maximum capacity of the RingBuffer.
     * @return The maximum capacity in bytes.
     */
    fun getMaxLength(): Int {
        return byteArray.size
    }

    /**
     * Get the size of the unused space in the RingBuffer.
     * @return The unused capacity in bytes.
     */
    fun getFreeSpace(): Int {
        return byteArray.size - mUnReadLength
    }

    /**
     * Get the underlying byte array.
     * @return The underlying byte array.
     */
    fun getByteArray(): ByteArray? {
        return byteArray
    }

    /**
     * Get the tail pointer for the underlying byte array.
     * @return The tail pointer.
     */
    fun getReadPositon(): Int {
        return mReadPositon
    }

    override fun toString(): String {
        return "RingBuffer(byteArray=${byteArray.contentToString()}, mReadPositon=$mReadPositon, mUnReadLength=$mUnReadLength)"
    }

}