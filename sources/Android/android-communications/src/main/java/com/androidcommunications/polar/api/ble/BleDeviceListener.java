package com.androidcommunications.polar.api.ble;

import android.bluetooth.le.ScanFilter;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.androidcommunications.polar.api.ble.model.BleDeviceSession;
import com.androidcommunications.polar.api.ble.model.advertisement.BleAdvertisementContent;
import com.androidcommunications.polar.api.ble.model.gatt.BleGattBase;
import com.androidcommunications.polar.api.ble.model.gatt.BleGattFactory;
import com.androidcommunications.polar.api.ble.model.gatt.client.BleHrClient;
import com.androidcommunications.polar.api.ble.model.gatt.client.BlePMDClient;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public abstract class BleDeviceListener {

    /**
     * Pre filter interface for search, to improve memory usage
     */
    public interface BleSearchPreFilter {
        boolean process(BleAdvertisementContent content);
    }

    protected BleGattFactory factory;
    protected BleSearchPreFilter preFilter;
    /**
     * @param clients e.g. how to set the clients
     *
     * Set<Class<? extends BleGattBase> > clients = new HashSet<>(Arrays.asList(
     * BleHrClient.class,
     * BleBattClient.class,
     * BleDisClient.class,
     * BleGapClient.class,
     * BlePfcClient.class,
     * BlePsdClient.class,
     * BlePsFtpClient.class,
     * BleH7SettingsClient.class,
     * BlePMDClient.class,
     * BleRscClient.class));
     */
    protected BleDeviceListener(Set<Class<? extends BleGattBase> > clients) {
        factory = new BleGattFactory(clients);
    }

    /**
     * @return true if bluetooth is active
     */
    abstract public boolean bleActive();

    /**
     * @return flowable stream of ble state
     */
    @Deprecated
    abstract public Flowable<Boolean> monitorBleState();

    /**
     * @param cb callback
     */
    abstract public void setBlePowerStateCallback(@Nullable BlePowerStateChangedCallback cb);

    public interface BlePowerStateChangedCallback {
        /**
         * @param power bt state
         */
        void stateChanged(Boolean power);
    }

    /**
     * @param filters scan filter list, android specific
     */
    abstract public void setScanFilters(@Nullable final List<ScanFilter> filters);

    /**
     * enable to optimize memory usage or disable scan pre filter
     * @param filter policy
     */
    abstract public void setScanPreFilter(@Nullable final BleSearchPreFilter filter);

    /**
     * @param enable true enables timer to avoid opportunistic scan, false disables. Default true.
     */
    abstract public void setOpportunisticScan(boolean enable);

    /**
     * Produces: onNext:      When a advertisement has been detected <BR>
     *           onError:     if scan start fails propagates BleStartScanError with error code <BR>
     *           onCompleted: Non produced <BR>
     * @param fetchKnownDevices, fetch known devices means bonded, already connected and already found devices <BR>
     * @return Observable stream <BR>
     */
    abstract public Flowable<BleDeviceSession> search(boolean fetchKnownDevices);

    /**
     * As java does not support destructor/RAII, Client/App should call this whenever the application is being destroyed
     */
    abstract public void shutDown();

    /**
     * aquire connection establishment
     * @param session device
     */
    abstract public void openSessionDirect(BleDeviceSession session);

    /**
     * aquire connection establishment
     * @param session device
     * @param uuids needed uuids to be found from advertisement data, when reconnecting
     */
    abstract public void openSessionDirect(BleDeviceSession session, List<String> uuids);

    /**
     * Deprecated use setDeviceSessionStateChangedCallback instead
     *
     * Produces: onNext: When a device session state has changed, Note use pair.second to check the state (see BleDeviceSession.DeviceSessionState)<BR>
     *           onError: should not be produced<BR>
     *           onCompleted: This is never propagated, NOTE to get completed event configure observable with some end rule e.g. take(1), takeUntil() etc...<BR>
     * @param session, a specific session or null = monitor all sessions
     * @return Observable stream
     */
    @Deprecated
    abstract public Observable<Pair<BleDeviceSession,BleDeviceSession.DeviceSessionState>> monitorDeviceSessionState(BleDeviceSession session);

    public interface BleDeviceSessionStateChangedCallback {
        /**
         * Invoked for all sessions and all state changes
         *
         * @param session check sessionState or session.getPreviousState() for actions
         */
        void stateChanged(BleDeviceSession session, BleDeviceSession.DeviceSessionState sessionState);
    }

    /**
     * set or null state observer
     *
     * @param changedCallback @see BleDeviceSessionStateChangedCallback
     */
    abstract public void setDeviceSessionStateChangedCallback(@Nullable BleDeviceSessionStateChangedCallback changedCallback);

    /**
     * aquires disconnection establishment directly without Observable returned
     * @param session device
     */
    abstract public void closeSessionDirect(BleDeviceSession session);

    /**
     * @return List of current device sessions known
     */
    abstract public Set<BleDeviceSession> deviceSessions();

    /**
     * @param address bt address
     * @return BleDeviceSession
     */
    abstract public BleDeviceSession sessionByAddress(final String address);

    /**
     * Client app/lib can request to remove device from the list,
     * @param deviceSession @see BleDeviceSession
     * @return true device was removed, false no( means device is considered to be alive )
     */
    abstract public boolean removeSession(BleDeviceSession deviceSession);

    /**
     * @return count of sessions removed
     */
    abstract public int removeAllSessions();
    abstract public int removeAllSessions(Set<BleDeviceSession.DeviceSessionState> inStates);

    /**
     * enable or disable automatic reconnection, by default true.
     * @param automaticReconnection
     */
    abstract public void setAutomaticReconnection(boolean automaticReconnection);

    public static final int POWER_MODE_NORMAL = 0;
    public static final int POWER_MODE_LOW = 1;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({POWER_MODE_NORMAL,POWER_MODE_LOW})
    public @interface PowerMode {
    }
    abstract public void setPowerMode(@PowerMode int mode);
}
