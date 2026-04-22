package jp.uhimania.qrreader

import android.app.Application
import jp.uhimania.qrreader.data.source.LocalDataSource
import jp.uhimania.qrreader.data.source.LocalDatabase

class QRReaderApplication : Application() {
    lateinit var source: LocalDataSource

    override fun onCreate() {
        super.onCreate()

        source = LocalDatabase.getDatabase(this).dataSource()
    }
}