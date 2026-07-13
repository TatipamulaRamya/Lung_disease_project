package com.respiratoryai.mobile.recording

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WavAudioRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recording = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var writerJob: Job? = null
    private var outputFile: File? = null
    private var sampleRate: Int = DEFAULT_SAMPLE_RATE

    fun start(targetFile: File): Boolean {
        if (recording.get()) {
            return false
        }

        val resolvedSampleRate = supportedSampleRate()
        val minBufferSize = AudioRecord.getMinBufferSize(
            resolvedSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        if (minBufferSize <= 0) {
            return false
        }

        val candidate = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            resolvedSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
        )

        if (candidate.state != AudioRecord.STATE_INITIALIZED) {
            candidate.release()
            return false
        }

        sampleRate = resolvedSampleRate
        audioRecord = candidate
        outputFile = targetFile
        recording.set(true)

        candidate.startRecording()
        writerJob = scope.launch {
            writePcmAsWave(
                record = candidate,
                file = targetFile,
                bufferSize = minBufferSize,
                sampleRate = resolvedSampleRate,
            )
        }

        return true
    }

    suspend fun stop(): File? {
        if (!recording.get()) {
            return outputFile
        }

        recording.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        writerJob?.join()
        writerJob = null

        return outputFile
    }

    fun release() {
        recording.set(false)
        audioRecord?.release()
        audioRecord = null
        writerJob?.cancel()
        writerJob = null
    }

    private fun writePcmAsWave(
        record: AudioRecord,
        file: File,
        bufferSize: Int,
        sampleRate: Int,
    ) {
        RandomAccessFile(file, "rw").use { raf ->
            writeWaveHeader(
                raf = raf,
                sampleRate = sampleRate,
                channelCount = 1,
                bitsPerSample = 16,
                dataLength = 0L,
            )

            val buffer = ByteArray(bufferSize)
            var totalBytes = 0L

            while (recording.get() && scope.isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    raf.write(buffer, 0, read)
                    totalBytes += read
                }
            }

            raf.seek(0)
            writeWaveHeader(
                raf = raf,
                sampleRate = sampleRate,
                channelCount = 1,
                bitsPerSample = 16,
                dataLength = totalBytes,
            )
        }
    }

    private fun writeWaveHeader(
        raf: RandomAccessFile,
        sampleRate: Int,
        channelCount: Int,
        bitsPerSample: Int,
        dataLength: Long,
    ) {
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val chunkSize = 36 + dataLength

        raf.writeBytes("RIFF")
        raf.writeInt(Integer.reverseBytes(chunkSize.toInt()))
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeInt(Integer.reverseBytes(16))
        raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(channelCount.toShort()).toInt())
        raf.writeInt(Integer.reverseBytes(sampleRate))
        raf.writeInt(Integer.reverseBytes(byteRate))
        raf.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
        raf.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
        raf.writeBytes("data")
        raf.writeInt(Integer.reverseBytes(dataLength.toInt()))
    }

    private fun supportedSampleRate(): Int {
        val candidates = listOf(16_000, 22_050, 44_100)
        return candidates.firstOrNull { rate ->
            AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ) > 0
        } ?: DEFAULT_SAMPLE_RATE
    }

    companion object {
        private const val DEFAULT_SAMPLE_RATE = 16_000
    }
}
