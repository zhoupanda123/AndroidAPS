package info.nightscout.androidaps.plugins.pump.omnipod.dash.driver.comm.exceptions

open class ScanFailException : Exception {
    constructor(message: String) : super(message)
    constructor(errorCode: Int) : super("errorCode$errorCode")
}
