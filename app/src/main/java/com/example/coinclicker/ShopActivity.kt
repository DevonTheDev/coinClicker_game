package com.example.coinclicker

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class ShopActivity : AppCompatActivity() {

    private var coinNumber: Int = 0
    lateinit var coinAmountLabel : TextView

    lateinit var autoClickerButton : Button
    lateinit var goldMinerButton: Button
    lateinit var bankButton: Button

    private var autoClickerPrice = 100
    private var goldMinerPrice = 1000
    private var bankPrice = 10000

    private var itemIntArray = IntArray(3)
    // [0] = Auto Clicker Quantity
    // [1] = Gold Miner Quantity
    // [2] = Bank Quantity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        coinAmountLabel = findViewById(R.id.coinTotal)
        coinNumber = intent.getIntExtra("coinValue", 0) // Get the coin value from the IntExtra passed to the Intent
        coinAmountLabel.text = coinNumber.toString()
        itemIntArray = intent.getIntArrayExtra("purchaseArray") ?: itemIntArray // Populate the itemIntArray with the array passed from MainActivity

        autoClickerButton = findViewById(R.id.autoClicker)
        goldMinerButton = findViewById(R.id.goldMiner)
        bankButton = findViewById(R.id.bank)

        autoClickerButton.text = getString(R.string.autoClicker, autoClickerPrice)
        goldMinerButton.text = getString(R.string.goldMiner, goldMinerPrice)
        bankButton.text = getString(R.string.bank, bankPrice)

        updateButtons() // Call to make sure the buttons have proper functionality from the initialisation of the menu

        // Buttons to spend the coins
        autoClickerButton.setOnClickListener{
            coinNumber -= autoClickerPrice
            coinAmountLabel.text = coinNumber.toString()
            itemIntArray[0] += 1
            updateButtons(autoClickerButton)
        }

        goldMinerButton.setOnClickListener{
            coinNumber -= goldMinerPrice
            coinAmountLabel.text = coinNumber.toString()
            itemIntArray[1] += 1
            updateButtons(goldMinerButton)
        }

        bankButton.setOnClickListener{
            coinNumber -= bankPrice
            coinAmountLabel.text = coinNumber.toString()
            itemIntArray[2] += 1
            updateButtons(bankButton)
        }

    }

    fun updateButtons(buttonToUpdate: Button? = null) {
        // Update the looks and functionality of the shop buttons if there aren't enough coins
        autoClickerButton.isEnabled = coinNumber >= autoClickerPrice
        autoClickerButton.isClickable = autoClickerButton.isEnabled
        autoClickerButton.alpha = if (autoClickerButton.isEnabled) 1.0f else 0.3f

        goldMinerButton.isEnabled = coinNumber >= goldMinerPrice
        goldMinerButton.isClickable = goldMinerButton.isEnabled
        goldMinerButton.alpha = if (goldMinerButton.isEnabled) 1.0f else 0.3f

        bankButton.isEnabled = coinNumber >= bankPrice
        bankButton.isClickable = bankButton.isEnabled
        bankButton.alpha = if (bankButton.isEnabled) 1.0f else 0.3f

        // Update the button text if a buttonToUpdate parameter is provided
        buttonToUpdate?.let {
            val buttonName = it.text.toString().substringBeforeLast(" ")
            val resourceId = resources.getIdentifier(buttonName, "string", packageName)
            if (resourceId != 0) {
                it.text = getString(resourceId, getButtonPrice(buttonName))
            }
        } ?: run {
            // Update all button text if no buttonToUpdate parameter is provided
            autoClickerButton.text = getString(R.string.autoClicker, autoClickerPrice)
            goldMinerButton.text = getString(R.string.goldMiner, goldMinerPrice)
            bankButton.text = getString(R.string.bank, bankPrice)
        }

        // Send the updated coinNumber back to MainActivity
        val resultIntent = Intent()
        resultIntent.putExtra("coinNumber", coinNumber)
        resultIntent.putExtra("purchaseArray", itemIntArray)
        setResult(Activity.RESULT_OK, resultIntent)
    }

    private fun getButtonPrice(buttonName: String): Int {
        return when (buttonName) {
            getString(R.string.autoClicker) -> autoClickerPrice
            getString(R.string.goldMiner) -> goldMinerPrice
            getString(R.string.bank) -> bankPrice
            else -> 0
        }
    }

    override fun onStop() {
        super.onStop()
        finish() // Finish the activity and send back to MainActivity
    }
}