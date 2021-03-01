package info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.message

import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.Id
import info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.exceptions.CouldNotParseMessageException
import java.nio.ByteBuffer

/***
 * MessagePacket contains header and raw payload for a message
 */
data class MessagePacket(
    val type: MessageType,
    val source: Id,
    val destination: Id,
    val payload: ByteArray,
    val sequenceNumber: Byte,
    val ack: Boolean = false,
    val ackNumber: Byte = 0.toByte(),
    val eqos: Short = 0.toShort(), // TODO: understand
    val priority: Boolean = false,
    val lastMessage: Boolean = false,
    val gateway: Boolean = false,
    val sas: Boolean = false, // TODO: understand
    val tfs: Boolean = false, // TODO: understand
    val version: Short = 0.toShort()
) {

    fun asByteArray(): ByteArray {
        val bb = ByteBuffer.allocate(16 + payload.size)
        bb.put(MAGIC_PATTERN.toByteArray())

        val f1 = Flag()
        f1.set(0, this.version.toInt() and 4 != 0)
        f1.set(1, this.version.toInt() and 2 != 0)
        f1.set(2, this.version.toInt() and 1 != 0)
        f1.set(3, this.sas)
        f1.set(4, this.tfs)
        f1.set(5, this.eqos.toInt() and 4 != 0)
        f1.set(6, this.eqos.toInt() and 2 != 0)
        f1.set(7, this.eqos.toInt() and 1 != 0)

        val f2 = Flag()
        f2.set(0, this.ack)
        f2.set(1, this.priority)
        f2.set(2, this.lastMessage)
        f2.set(3, this.gateway)
        f2.set(4, this.type.value.toInt() and 8 != 0)
        f2.set(5, this.type.value.toInt() and 4 != 0)
        f2.set(6, this.type.value.toInt() and 2 != 0)
        f2.set(7, this.type.value.toInt() and 1 != 0)

        bb.put(f1.value.toByte())
        bb.put(f2.value.toByte())
        bb.put(this.sequenceNumber)
        bb.put(this.ackNumber)

        bb.put((this.payload.size ushr 3).toByte())
        bb.put((this.payload.size shl 5).toByte())

        bb.put(this.source.address)
        bb.put(this.destination.address)

        bb.put(this.payload)

        val ret = ByteArray(bb.position())
        bb.flip()
        bb.get(ret)

        return ret
    }

    companion object {

        private val MAGIC_PATTERN = "TW" // all messages start with this string
        private val HEADER_SIZE = 16

        fun parse(payload: ByteArray): MessagePacket {
            if (payload.size < HEADER_SIZE) {
                throw CouldNotParseMessageException(payload)
            }
            if (payload.copyOfRange(0, 2).decodeToString() != MAGIC_PATTERN) {
                throw CouldNotParseMessageException(payload)
            }
            val f1 = Flag(payload[2].toInt())
            val sas = f1.get(3) != 0
            val tfs = f1.get(4) != 0
            val version = ((f1.get(0) shl 2) or (f1.get(1) shl 1) or (f1.get(2) shl 0)).toShort()
            val eqos = (f1.get(7) or (f1.get(6) shl 1) or (f1.get(5) shl 2)).toShort()

            val f2 = Flag(payload[3].toInt())
            val ack = f2.get(0) != 0
            val priority = f2.get(1) != 0
            val lastMessage = f2.get(2) != 0
            val gateway = f2.get(3) != 0
            val type =
                MessageType.byValue((f1.get(7) or (f1.get(6) shl 1) or (f1.get(5) shl 2) or (f1.get(4) shl 3)).toByte())
            if (version.toInt() != 0) {
                throw CouldNotParseMessageException(payload)
            }
            val sequenceNumber = payload[4]
            val ackNumber = payload[5]
            val size = (payload[6].toInt() shl 3) or (payload[7].toUnsignedInt() ushr 5)
            if (size + HEADER_SIZE > payload.size) {
                throw CouldNotParseMessageException(payload)
            }
            val payloadEnd = 16 + size +
                if (type == MessageType.ENCRYPTED) 8
                else 0

            return MessagePacket(
                type = type,
                ack = ack,
                eqos = eqos,
                priority = priority,
                lastMessage = lastMessage,
                gateway = gateway,
                sas = sas,
                tfs = tfs,
                version = version,
                sequenceNumber = sequenceNumber,
                ackNumber = ackNumber,
                source = Id(payload.copyOfRange(8, 12)),
                destination = Id(payload.copyOfRange(12, 16)),
                payload = payload.copyOfRange(16, payloadEnd),
            )
        }
    }
}

private class Flag(var value: Int = 0) {

    fun set(idx: Byte, set: Boolean) {
        val mask = 1 shl (7 - idx)
        if (!set)
            return
        value = value or mask
    }

    fun get(idx: Byte): Int {
        val mask = 1 shl (7 - idx)
        if (value and mask == 0) {
            return 0
        }
        return 1
    }
}

internal fun Byte.toUnsignedInt() = this.toInt() and 0xff
