package com.example.mp3playerondb

import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.example.mp3playerondb.databinding.ActivityPlayBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class PlayActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityPlayBinding
    val ALBUM_IMAGE_SIZE = 90
    var mediaPlayer: MediaPlayer? = null
    private var music : MusicData? = null
    lateinit var musicData: MusicData
    private var bitmap: Bitmap? = null
    private var playList: MutableList<Parcelable>? = null
    private var currentposition: Int = 0
    var mp3playerJob: Job? = null
    var pauseFlag = false
    private val PREVIOUS = 0
    private val NEXT = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 전달해온 intent 값을 가져옴
        playList = intent.getParcelableArrayListExtra("parcelableList")
        currentposition = intent.getIntExtra("position", 0)
        musicData = playList?.get(currentposition) as MusicData

        // 화면에 바인딩 진행
        binding.albumTitle.text = musicData.title
        binding.albumArtist.text = musicData.artist
        binding.totalDuration.text = SimpleDateFormat("mm:ss").format(musicData.duration)
        binding.playDuration.text = "00:00"
        val bitmap = musicData.getAlbumBitmap(this, ALBUM_IMAGE_SIZE)
        if (bitmap != null) {
            binding.albumImage.setImageBitmap(bitmap)
        } else {
            binding.albumImage.setImageResource(R.drawable.music)
        }
        // 음악 파일객체 가져옴
        mediaPlayer = MediaPlayer.create(this, musicData.getMusicUri())
        // 이벤트 처리(일시정지, 실행, 돌아가기, 정지, 시크바 조절)
        binding.listButton.setOnClickListener(this)
        binding.playButton.setOnClickListener(this)
        binding.nextSongButton.setOnClickListener(this)
        binding.backSongButton.setOnClickListener(this)
        binding.seekBar.max = mediaPlayer!!.duration
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.listButton -> {
                mp3playerJob?.cancel()
                mediaPlayer?.stop()
//                mediaPlayer?.release()
//                mediaPlayer = null
                finish()
            }
            R.id.playButton -> {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()
                    binding.playButton.setImageResource(R.drawable.play_circle_24)
                    pauseFlag = true
                } else {
                    mediaPlayer?.start()
                    binding.playButton.setImageResource(R.drawable.pause_circle_24)
                    pauseFlag = false

                    // 코루틴으로 음악을 재생
                    val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
                    mp3playerJob = backgroundScope.launch {
                        while (mediaPlayer!!.isPlaying) {
                            var currentPosition = mediaPlayer?.currentPosition!!
                            // 코루틴속에서 화면의 값을 변동시키고자 할 때 runOnUiThread 사용
                            var strCurrentPosition = SimpleDateFormat("mm:ss").format(mediaPlayer?.currentPosition)
                            runOnUiThread {
                                binding.seekBar.progress = currentPosition
                                binding.playDuration.text = strCurrentPosition
                            }
                            try {
                                delay(1000)
                                binding.seekBar.incrementProgressBy(1000)
                            } catch (e: java.lang.Exception) {
                                Log.e("PlayActivity", "delay 오류발생 ${e.printStackTrace()}")
                            }
                        }
                        if (pauseFlag == false) {
                            runOnUiThread {
                                binding.seekBar.progress = 0
                                binding.playButton.setImageResource(R.drawable.play_circle_24)
                                binding.playDuration.text = "00:00"
                            }
                        }
                    }
                }
            }

            R.id.nextSongButton ->{
                currentposition = getPosition(NEXT, currentposition)
                setReplay()
            }
            R.id.backSongButton -> {
                currentposition = getPosition(PREVIOUS, currentposition)
                setReplay()
            }
        }
    } fun getPosition(option:Int , position : Int) :Int {
        var newPosition: Int = position
        when (position) {
            0 -> {newPosition =
                if(option == PREVIOUS){ playList!!.size -1 } else  position +1
            }
            in 1 until (playList!!.size -1) -> {
                newPosition =
                    if (option == PREVIOUS){ position -1 } else position +1
            }
            playList!!.size -1 -> {
                newPosition =
                    if (option == PREVIOUS) { position - 1 } else 0
            }
        }
        return newPosition
    }
    fun setReplay(){
        mediaPlayer?.stop()
        mp3playerJob?.cancel()
        music = playList?.get(currentposition) as MusicData

        binding.albumTitle.text = music?.title
        binding.albumArtist.text = music?.artist
        binding.totalDuration.text = SimpleDateFormat("mm:ss").format(music?.duration)
        binding.playDuration.text = "00:00"
        bitmap = music?.getAlbumBitmap(this,ALBUM_IMAGE_SIZE)
        if (bitmap != null) {
            binding.albumImage.setImageBitmap(bitmap)
        } else {
            binding.albumImage.setImageResource(R.drawable.music)
        }
        //음악 등록
        mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())

        //시크바 음악 재생위치 변경
        binding.seekBar.max = mediaPlayer!!.duration
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                Log.d("chap17mp3_dp1", "움직인다")
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                Log.d("chap17mp3_dp1", "안움직인다?")
            }
        })
    }

    override fun onBackPressed() {
        mp3playerJob?.cancel()
        mediaPlayer?.stop()
//        mediaPlayer?.release()
//        mediaPlayer = null
        finish()
    }

}