package com.example.service

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class KramaTelecomConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.i(TAG, "Creating incoming telecom VoIP connection...")
        return KramaCallConnection().apply {
            setInitializing()
            setAddress(request?.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName("Krama VoIP Contact", android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            setRinging()
            setActive()
        }
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.i(TAG, "Creating outgoing telecom VoIP connection...")
        return KramaCallConnection().apply {
            setInitializing()
            setAddress(request?.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            setDialing()
            setActive()
        }
    }

    private class KramaCallConnection : Connection() {
        override fun onAnswer() {
            super.onAnswer()
            Log.i(TAG, "Telecom call answered")
            setActive()
        }

        override fun onReject() {
            super.onReject()
            Log.i(TAG, "Telecom call rejected")
            setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REJECTED))
            destroy()
        }

        override fun onDisconnect() {
            super.onDisconnect()
            Log.i(TAG, "Telecom call disconnected")
            setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.LOCAL))
            destroy()
        }
    }

    companion object {
        private const val TAG = "KramaTelecomService"
    }
}
