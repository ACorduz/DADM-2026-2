package com.example.reto0
import androidx.activity.ComponentActivity
import android.widget.TextView
import android.os.Bundle


class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Hola Mundo!"
            textSize=24f})
    }

}
