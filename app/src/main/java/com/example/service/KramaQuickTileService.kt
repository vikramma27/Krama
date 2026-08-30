package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class KramaQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(isEncryptedModeActive = true)
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val newState = tile.state != Tile.STATE_ACTIVE
        updateTileState(newState)
        Log.d(TAG, "Quick Settings Security Mute Tile toggled: active=$newState")
    }

    private fun updateTileState(isEncryptedModeActive: Boolean) {
        val tile = qsTile ?: return
        if (isEncryptedModeActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Krama E2EE: ON"
            tile.subtitle = "Encrypted Vault Active"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Krama E2EE: OFF"
            tile.subtitle = "Standard Mode"
        }
        tile.updateTile()
    }

    companion object {
        private const val TAG = "KramaQuickTile"
    }
}
