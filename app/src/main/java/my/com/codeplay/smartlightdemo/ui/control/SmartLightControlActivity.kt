package my.com.codeplay.smartlightdemo.ui.control

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tuya.smart.centralcontrol.TuyaLightDevice
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.sdk.api.IDeviceListener
import com.tuya.smart.sdk.api.IResultCallback
import com.tuya.smart.sdk.api.ITuyaDevice
import com.tuya.smart.sdk.centralcontrol.api.constants.LightScene
import com.tuya.smart.sdk.centralcontrol.parser.bean.LightColourData
import my.com.codeplay.smartlightdemo.BuildConfig
import my.com.codeplay.smartlightdemo.R
import my.com.codeplay.smartlightdemo.databinding.ActivitySmartLightControlBinding

const val EXTRA_DEVICE_NAME = "${BuildConfig.APPLICATION_ID}.extra.DEVICE_NAME"
const val EXTRA_DEVICE_ID = "${BuildConfig.APPLICATION_ID}.extra.DEVICE_ID"

class SmartLightControlActivity : AppCompatActivity() {
    private val tag = SmartLightControlActivity::class.java.simpleName
    private val workModes = listOf(
        "White",
        "Colour",
        "Scene"
        // "Music"
    )
    private val scenes = listOf(
        "Goodnight",
        "Work",
        "Read",
        "Casual"
    )
    private var device: ITuyaDevice? = null
    private var workMode: String? = null
    private var scene: String? = null
    //private lateinit var lightDevice: TuyaLightDevice
    //private lateinit var workMode: LightMode
    //private lateinit var scene: LightScene
    private var brightness: Int = -1
    private var colorTemperature: Int = -1
    private var colorHSV: String? = null
    //private lateinit var colorHSV: LightColourData

    private lateinit var binding: ActivitySmartLightControlBinding
    private val offlineSnackbar by lazy {
        Snackbar.make(binding.root, R.string.device_offline, Snackbar.LENGTH_INDEFINITE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmartLightControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.powerButton.isActivated = false
        binding.workModeSpinner.adapter =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, workModes)
        binding.sceneSpinner.adapter =
            ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, scenes)

        if (intent.hasExtra(EXTRA_DEVICE_NAME) && intent.hasExtra(EXTRA_DEVICE_ID)) {
            binding.deviceNameText.text = intent.getStringExtra(EXTRA_DEVICE_NAME)
            val devId = intent.getStringExtra(EXTRA_DEVICE_ID)

            // Initialise a device to issue standard commands
            device = TuyaHomeSdk.newDeviceInstance(devId)
            // Todo: get device data points and update controls.
            device?.registerDeviceListener(object : IDeviceListener {
                override fun onDpUpdate(devId: String?, dpStr: MutableMap<String, Any>?) {
                    Log.i(tag, "onDpUpdate: $dpStr")
                    dpStr?.let {
                        if (it.containsKey("switch_led")) {
                            binding.powerButton.isActivated = it["switch_led"] as Boolean
                        }
                        if (it.containsKey("work_mode")) {
                            val value = it["work_mode"] as String
                            workMode = value
                        }
                        if (it.containsKey("scene_data")) {
                            scene = it["scene_data"] as String
                        }
                        if (it.containsKey("bright_value")) {
                            brightness = it["bright_value"] as Int
                        }
                        if (it.containsKey("temp_value")) {
                            colorTemperature = it["temp_value"] as Int
                        }
                        if (it.containsKey("colour_data")) {
                            colorHSV = it["colour_data"] as String
                        }
                    }
                }

                override fun onRemoved(devId: String?) {

                }

                override fun onStatusChanged(devId: String?, status: Boolean) {

                }

                override fun onNetworkStatusChanged(devId: String?, online: Boolean) {
                    if (online) {
                        if (offlineSnackbar.isShownOrQueued)
                            offlineSnackbar.dismiss()
                    } else {
                        offlineSnackbar.show()
                    }
                }

                override fun onDevInfoUpdate(devId: String?) {}
            })

            binding.powerButton.setOnClickListener {
                val localIsActivated = !binding.powerButton.isActivated

                val dpCodeMap: HashMap<String, Any> = HashMap()
                dpCodeMap["switch_led"] = localIsActivated
                sendCommands(dpCodeMap)
            }

            binding.workModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val localWorkMode = workModes[position].lowercase()

                    if (localWorkMode.equals(workMode, true))
                        return

                    val dpCodeMap: HashMap<String, Any> = HashMap()
                    dpCodeMap["work_mode"] = localWorkMode
                    sendCommands(dpCodeMap)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            binding.sceneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val localScene = when(position) {
                        0 -> "000e0d0000000000000000c80000"
                        1 -> "010e0d0000000000000003e801f4"
                        2 -> "020e0d0000000000000003e803e8"
                        else -> "030e0d0000000000000001f401f4"
                    }

                    if (localScene.equals(scene, true))
                        return

                    val dpCodeMap: HashMap<String, Any> = HashMap()
                    dpCodeMap["scene_data"] = localScene
                    sendCommands(dpCodeMap)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            binding.brightnessSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {}

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    var localBrightness = seekBar.progress * 10
                    if (localBrightness <= 25)
                        localBrightness = 25

                    if (localBrightness == brightness)
                        return

                    val dpCodeMap: HashMap<String, Any> = HashMap()
                    dpCodeMap["bright_value"] = localBrightness
                    sendCommands(dpCodeMap)
                }
            })

            binding.colourTemperatureSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {}

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    var localColorTemperature = seekBar.progress * 10
                    if (localColorTemperature <= 25)
                        localColorTemperature = 25

                    if (localColorTemperature == colorTemperature)
                        return

                    val dpCodeMap: HashMap<String, Any> = HashMap()
                    dpCodeMap["temp_value"] = localColorTemperature
                    sendCommands(dpCodeMap)
                }
            })

            binding.colorPicker.setOnColorChangedListener {
                val hsv = FloatArray(3)
                Color.colorToHSV(it, hsv)
                val hue = String.format("%04x", hsv[0].toInt())
                val saturation = String.format("%04x", (hsv[1]*1000).toInt())
                val value = String.format("%04x", (hsv[2]*1000).toInt())
                Log.d(tag, "Hue (0-360): ${hsv[0]}|$hue Saturation (0-1000): ${hsv[1]}|$saturation Value (0-1000): ${hsv[2]}|$value")

                val dpCodeMap: HashMap<String, Any> = HashMap()
                dpCodeMap["colour_data"] = "$hue$saturation$value"
                sendCommands(dpCodeMap)
            }

            /* Light firmware has two versions (v1 and v2). An app must develop two sets of
             *  control logic even though standard commands are used. Below are the APIs from
             *  the Tuya lighting SDK.
            val lightDevice = TuyaLightDevice(devId)
            Log.d(tag, "Light type: ${lightDevice.lightType()}")
            lightDevice.registerLightListener(object : ILightListener {
                override fun onDpUpdate(lightDataPoint: LightDataPoint?) {
                    lightDataPoint?.let {
                        Log.i(tag, "onDpUpdate: ${it}")

                        binding.powerButton.isActivated = it.powerSwitch
                        workMode = it.workMode
                        binding.workModeSpinner.setSelection(workMode.ordinal)

                        scene = it.scene

                        brightness = it.brightness
                        binding.brightnessSeekbar.progress = brightness

                        *//*colorTemperature = it.colorTemperature

                        colorHSV = it.colorHSV*//*
                    } ?: Log.i(tag, "onDpUpdate: lightDataPoint is null!")
                }

                override fun onDevInfoUpdate() {}

                override fun onStatusChanged(status: Boolean) {
                    Log.i(tag, "onDeviceStatusChanged: $status")
                }

                override fun onNetworkStatusChanged(online: Boolean) {
                    Log.i(tag, "onNetworkStatusChanged: $online")

                    if (online) {
                        if (offlineSnackbar.isShownOrQueued)
                            offlineSnackbar.dismiss()
                    } else {
                        offlineSnackbar.show()
                    }
                }

                override fun onRemoved() {}
            })

            binding.powerButton.setOnClickListener {
                lightDevice.powerSwitch(!binding.powerButton.isActivated, object : IResultCallback {
                    override fun onSuccess() {
                        binding.powerButton.isActivated = !binding.powerButton.isActivated
                    }

                    override fun onError(code: String?, errorMsg: String?) {
                        Snackbar.make(
                            binding.root,
                            errorMsg ?: getString(R.string.operation_failed),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                })
            }

            binding.workModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val localLightMode = when (position) {
                        0 -> LightMode.MODE_WHITE
                        1 -> LightMode.MODE_COLOUR
                        2 -> LightMode.MODE_SCENE
                        else -> throw IllegalArgumentException("Invalid light mode")
                    }

                    if (localLightMode == workMode)
                        return

                    lightDevice.workMode(localLightMode, object : IResultCallback {
                        override fun onSuccess() {
                            workMode = localLightMode
                        }

                        override fun onError(code: String?, error: String?) {
                            Snackbar.make(binding.root, R.string.operation_failed, Snackbar.LENGTH_LONG)
                                .show()
                        }
                    })
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            binding.brightnessSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val localBrightness = seekBar.progress

                    if (localBrightness == brightness)
                        return
                    lightDevice.brightness(localBrightness, object : IResultCallback {
                        override fun onSuccess() {
                            Log.d(tag, "on brightness change succeed")
                            brightness = localBrightness
                        }

                        override fun onError(code: String?, error: String?) {
                            Log.d(tag, "on brightness change error: $code, $error")
                            Snackbar.make(binding.root, R.string.operation_failed, Snackbar.LENGTH_LONG)
                                .show()
                        }
                    })
                }
            })*/
        }
    }

    override fun onDestroy() {
        device?.onDestroy()
        //lightDevice.unRegisterDevListener()
        super.onDestroy()
    }

    /**
     * Traditional method that uses non-standard commands to control a device.
     */
    private fun sendCommands(dps: String) {
        device?.publishDps(dps, object : IResultCallback {
            override fun onError(code: String?, error: String?) {
                Snackbar.make(binding.root, R.string.operation_failed, Snackbar.LENGTH_LONG)
                    .show()
            }

            override fun onSuccess() {
                Log.d(tag, "Operation succeeded")
            }
        })
    }

    /**
     * Method that uses standard commands to control a device. Developer can determine whether
     * a product can be controlled by standard commands based on its product ID:
     *
     * boolean isStandard = TuyaHomeSdk.getDataInstance().isStandardProduct("product_id")
     *
     */
    private fun sendCommands(dpCodeMap: HashMap<String, Any>) {
        device?.publishCommands(dpCodeMap, object : IResultCallback {
            override fun onError(code: String?, error: String?) {
                Snackbar.make(binding.root, R.string.operation_failed, Snackbar.LENGTH_LONG)
                    .show()
            }

            override fun onSuccess() {
                Log.d(tag, "Operation succeeded")
            }
        })
    }
}