package com.example.coinclicker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.logging.Handler
import kotlin.collections.*
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    lateinit var coinButton: ImageButton
    lateinit var shopButton: ImageButton
    lateinit var counterText: TextView
    lateinit var screenHeight: Number
    lateinit var screenWidth: Number
    lateinit var coinLayout : ConstraintLayout
    lateinit var shopResultLauncher: ActivityResultLauncher<Intent>
    lateinit var autoClickerLabel : TextView
    lateinit var goldMinerLabel : TextView
    lateinit var bankLabel : TextView

    var coinNumber = 0 // Initialise a counter to keep track of the number of coins
    var boughtItems = IntArray(3) // Initialise an array to track purchases from shop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Get the screen height and width for later uses
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Checks for API level 30
            val display = this.display
            display?.apply { getRealMetrics(displayMetrics) }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels


        // Set up a shopResultLauncher to enable us to get the updated coinNumber from ShopActivity
        shopResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Handle the result from the ShopActivity here
                val data = result.data
                coinNumber = data?.getIntExtra("coinNumber", coinNumber) ?: coinNumber
                boughtItems = data?.getIntArrayExtra("purchaseArray") ?: boughtItems
                counterText.text = coinNumber.toString()
                autoClickerLabel.text = getString(R.string.autoClickerLabel, boughtItems[0])
                goldMinerLabel.text = getString(R.string.goldMinerLabel, boughtItems[1])
                bankLabel.text = getString(R.string.bankLabel, boughtItems[2])
            }
        }


        counterText = findViewById(R.id.coin_count) // Get the counter object
        coinButton = findViewById(R.id.coin_button) // Get the coin
        shopButton = findViewById(R.id.shopButton) // Get the shop button
        coinLayout = findViewById(R.id.fallingCoinConstraint) // Get the constraint for the background coins
        autoClickerLabel = findViewById(R.id.autoClickerLabel) // Get the display for auto clicker amount
        goldMinerLabel = findViewById(R.id.goldMinerLabel) // Get the display for gold miner amount
        bankLabel = findViewById(R.id.bankLabel) // Get the display for bank amount


        // Set the values for the coins and counterText from the saved coin value
        val sharedPref = getSharedPreferences("coinValue", Context.MODE_PRIVATE)
        coinNumber = sharedPref.getInt("coins", 0)
        counterText.text = sharedPref.getInt("coins", 0).toString()
        // Populate the index array of purchased items
        for (i in boughtItems.indices) {
            boughtItems[i] = sharedPref.getInt("boughtItems_$i", 0)
        }

        // Set the counters for the purchases to the correct values
        autoClickerLabel.text = getString(R.string.autoClickerLabel, boughtItems[0])
        goldMinerLabel.text = getString(R.string.goldMinerLabel, boughtItems[1])
        bankLabel.text = getString(R.string.bankLabel, boughtItems[2])

        // Update the coins per second total
        addCoinsPerSecond()

        // Initialise a gold color for the background change
        val colorFilter = Color.parseColor("#80897800")


        // Make sure the coin counter is not editable
        counterText.isFocusable = false
        counterText.isCursorVisible = false
        autoClickerLabel.isFocusable = false
        autoClickerLabel.isCursorVisible = false
        goldMinerLabel.isFocusable = false
        goldMinerLabel.isCursorVisible = false
        bankLabel.isFocusable = false
        bankLabel.isCursorVisible = false


        // Set up an idle animation for the central coin
        val idleAnim = ScaleAnimation(
            1f, 0.7f,
            1f, 0.7f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        idleAnim.duration = 1500
        idleAnim.repeatCount = ValueAnimator.INFINITE
        idleAnim.repeatMode = Animation.REVERSE
        coinButton.startAnimation(idleAnim)

        // Call the functions required to generate and animate the background coins falling
        startCoinGenerator()
        startCoinAnimation()


        // Runs when the middle coin is clicked
        coinButton.setOnClickListener {

            // Play an animation that makes the coin bounce on touch
            val bounceAnim = ScaleAnimation(
                1f, 0.5f,
                1f, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            bounceAnim.duration = 75
            bounceAnim.repeatCount = 1
            bounceAnim.repeatMode = Animation.REVERSE
            coinButton.startAnimation(bounceAnim)
            // Event Listener for the bounce animation on click
            bounceAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    coinButton.startAnimation(idleAnim) // Start the idle animation again
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })


            // Play an animation that changes the background of the app when the button is pressed
            val fadeIn = ObjectAnimator.ofArgb(window.decorView, "backgroundColor", Color.BLACK, colorFilter)
            fadeIn.duration = 100

            val fadeOut = ObjectAnimator.ofArgb(window.decorView, "backgroundColor", colorFilter, Color.BLACK)
            fadeOut.duration = 100

            val screenHueChanger = AnimatorSet()
            screenHueChanger.playSequentially(fadeIn, fadeOut)
            screenHueChanger.start()


            coinNumber++ // Update our coin counter value when the coin is clicked
            counterText.text = coinNumber.toString() // Display the new value at the top of the screen
        }

        // Start the shopPage activity when the shop button is clicked
        shopButton.setOnClickListener {
            val shopPage = Intent(this, ShopActivity::class.java)
            shopPage.putExtra("coinValue", coinNumber) // Send the current coin number to the activity
            shopPage.putExtra("purchaseArray", boughtItems)
            shopResultLauncher.launch(shopPage)
        }
    }

    // Save all important data in this override function
    override fun onPause() {
        super.onPause()

        // Save the coin value before the app closes for use in later sessions
        val sharedPref = getSharedPreferences("coinValue", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt("coins", coinNumber)
        for (i in boughtItems.indices) {
            editor.putInt("boughtItems_$i", boughtItems[i])
        }
        // "coins" = coinNumber
        // "boughtItems_0" = Number of auto clickers
        // "boughtItems_1" = Number of gold miners
        // "boughItems_2" = Number of banks
        editor.apply()
    }

    val coinImages = mutableListOf<ImageView>() // Create a list of the coin images so we can apply the animation to them all

    // Function to generate the necessary number of coins
    fun startCoinGenerator() {
        val coinInterval = 1000 // Add a new coin every x coins

        var backgroundCoins = (coinNumber / coinInterval) // Get the whole number needed
            if (coinImages.size > backgroundCoins){ // If there are too many coins in the list
                // Remove images until its the right size
                coinImages.subList(0, coinImages.size - backgroundCoins).clear()
            } else if (coinImages.size < backgroundCoins) { // If there arent enough coins in the list
                // Determine how many to add
                backgroundCoins = backgroundCoins - coinImages.size
                // Add them to the mutable list
                while (backgroundCoins > 0){
                    val coinImage = ImageView(this@MainActivity)
                    coinImage.setImageResource(R.mipmap.coin_sprite_foreground)
                    coinImage.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    coinLayout.addView(coinImage)

                    // Set the starting position of the new coin
                    val coinX = Random.nextInt(0, screenWidth.toInt())
                    coinImage.translationX = coinX.toFloat()
                    coinImage.translationY = -coinImage.height.toFloat() - 500f
                    coinImage.scaleX = 0.4f
                    coinImage.scaleY = 0.4f

                    coinImages.add(coinImage)
                    backgroundCoins -= 1
                }
            }

    }

    // Function to give the background coins the necessary animation
    fun startCoinAnimation() {
        var delay = 0L
        coinImages.forEach { coinImage ->
            coinImage.alpha = 0.6f // Change the alpha value

            // Generate a random duration for the coin fall animation between 3 and 7 seconds
            val duration = (3 + Random.nextFloat() * 4) * 1000

            // Create an animation that moves the coin from the top to the bottom of the screen
            val coinFallAnimation = ObjectAnimator.ofFloat(
                coinImage, "translationY", -100f, screenHeight.toFloat()
            )
            coinFallAnimation.duration = duration.toLong()
            coinFallAnimation.repeatCount = ValueAnimator.INFINITE
            coinFallAnimation.repeatMode = ValueAnimator.RESTART
            coinFallAnimation.interpolator = LinearInterpolator()

            // Start the animation with a delay
            coinFallAnimation.startDelay = delay
            coinFallAnimation.start()

            // Increase the delay for the next coin's animation
            delay += duration.toLong() / 2
        }
    }


    // Define a function to add the coins per second every second
    val scope = MainScope()
    var job : Job? = null

    fun addCoinsPerSecond() {
        job = scope.launch {
            while(true) {
                if (boughtItems[0] > 0) {
                    coinNumber += (boughtItems[0] * 0.2).roundToInt()
                }
                if (boughtItems[1] > 0) {
                    coinNumber += boughtItems[1] * 5
                }
                if (boughtItems[2] > 0) {
                    coinNumber += boughtItems[2] * 20
                }
                counterText.text = coinNumber.toString()
                delay(1000)
            }
        }
    }
}