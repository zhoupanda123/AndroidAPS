package info.nightscout.androidaps.plugins.pump.danaR.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.logging.StacktraceLoggerWrapper;

/**
 * Created by mike on 20.07.2016.
 */
public class MsgHistoryNewDone extends MessageBase {
    private static Logger log = StacktraceLoggerWrapper.getLogger(L.PUMPCOMM);
    public static boolean received = false;

    public MsgHistoryNewDone() {
        SetCommand(0x42F1);
        received = false;
        if (L.isEnabled(L.PUMPCOMM))
            log.debug("New message");
    }

    @Override
    public void handleMessage(byte[] bytes) {
        received = true;
        if (L.isEnabled(L.PUMPCOMM))
            log.debug("History new done received");
    }
}
