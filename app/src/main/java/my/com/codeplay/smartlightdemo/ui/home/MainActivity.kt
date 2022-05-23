package my.com.codeplay.smartlightdemo.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.home.sdk.bean.HomeBean
import com.tuya.smart.home.sdk.builder.ActivatorBuilder
import com.tuya.smart.home.sdk.callback.ITuyaHomeResultCallback
import com.tuya.smart.sdk.api.IResultCallback
import com.tuya.smart.sdk.api.ITuyaActivator
import com.tuya.smart.sdk.api.ITuyaActivatorGetToken
import com.tuya.smart.sdk.api.ITuyaSmartActivatorListener
import com.tuya.smart.sdk.bean.DeviceBean
import com.tuya.smart.sdk.enums.ActivatorEZStepCode
import com.tuya.smart.sdk.enums.ActivatorModelEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.com.codeplay.smartlightdemo.BuildConfig
import my.com.codeplay.smartlightdemo.PrefsHandler
import my.com.codeplay.smartlightdemo.R
import my.com.codeplay.smartlightdemo.databinding.ActivityMainBinding
import my.com.codeplay.smartlightdemo.databinding.ItemHomePartBinding
import my.com.codeplay.smartlightdemo.db.DatabaseManager
import my.com.codeplay.smartlightdemo.db.Device
import my.com.codeplay.smartlightdemo.ui.control.EXTRA_DEVICE_ID
import my.com.codeplay.smartlightdemo.ui.control.EXTRA_DEVICE_NAME
import my.com.codeplay.smartlightdemo.ui.control.SmartLightControlActivity
import my.com.codeplay.smartlightdemo.utils.isWiFiConnected
import kotlin.math.hypot
import kotlin.properties.Delegates

const val EXTRA_USERNAME = "${BuildConfig.APPLICATION_ID}.extra.EXTRA_USERNAME"

class MainActivity : AppCompatActivity(), FragmentResultListener {
    private val tag = MainActivity::class.java.simpleName
    private val homeName = "MyHome"
    private val rooms = mapOf(
        //"Attir" to R.drawable.ic_twotone_hotel_48,
        "Bathroom 1" to R.drawable.ic_twotone_bathtub_48,
        "Bathroom 2" to R.drawable.ic_twotone_bathtub_48,
        "Balcony" to R.drawable.ic_twotone_balcony_48,
        "Bedroom 1" to R.drawable.ic_twotone_hotel_48,
        "Bedroom 2" to R.drawable.ic_twotone_hotel_48,
        "Bedroom 3" to R.drawable.ic_twotone_hotel_48,
        //"Basement" to R.drawable.ic_twotone_hotel_48,
        "Dining Room" to R.drawable.ic_twotone_dining_48,
        "Garden" to R.drawable.ic_twotone_local_florist_48,
        "Kitchen" to R.drawable.ic_twotone_kitchen_48,
        "Laundry" to R.drawable.ic_twotone_local_laundry_service_48,
        "Living" to R.drawable.ic_twotone_weekend_48,
        "Store Room" to R.drawable.ic_twotone_widgets_48
    )
    private lateinit var binding: ActivityMainBinding
    private var homeId by Delegates.notNull<Long>()
    private var roomId by Delegates.notNull<Long>()
    private var tuyaActivator: ITuyaActivator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainTitleText.text = getString(R.string.title_activity_main, PrefsHandler(this).username)
        homeId = PrefsHandler(this).homeId
        if (homeId > 0) {
            binding.loading.visibility = View.VISIBLE

            TuyaHomeSdk.newHomeInstance(homeId).getHomeDetail(object : ITuyaHomeResultCallback {
                override fun onSuccess(homeBean: HomeBean?) {
                    binding.loading.visibility = View.GONE

                    homeBean?.let {
                        Log.d(tag, it.deviceList?.joinToString { it.devId } ?: "No device added to Home ID: $homeId")
                        // Todo: display list of rooms to select
                        roomId = it.rooms[0].roomId
                        initMainUI()
                    } ?: showInitHomeError(getString(R.string.something_went_wrong))
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    binding.loading.visibility = View.GONE

                    showInitHomeError(errorMsg)
                }
            })
        } else {
            binding.retryButton.setOnClickListener { createHome() }
            createHome()
        }
    }

    override fun onDestroy() {
        //tuyaActivator?.onDestroy()
        super.onDestroy()
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            AddNetworkBottomSheet.tag -> {
                if (result.containsKey(AddNetworkBottomSheet.EXTRA_NETWORK_NAME)
                    && result.containsKey(AddNetworkBottomSheet.EXTRA_NETWORK_PASSWORD)) {
                    getActivationToken(
                        result.getString(AddNetworkBottomSheet.EXTRA_NETWORK_NAME)!!,
                        result.getString(AddNetworkBottomSheet.EXTRA_NETWORK_PASSWORD)!!
                    )
                } else {
                    val cx = binding.addDeviceFab.width / 2
                    val cy = binding.addDeviceFab.height / 2
                    val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
                    val circularRevealAnim = ViewAnimationUtils.createCircularReveal(binding.addDeviceFab, cx, cy, 0f, radius)
                    binding.addDeviceFab.isVisible = true
                    circularRevealAnim.start()
                }
                supportFragmentManager.clearFragmentResultListener(AddNetworkBottomSheet.tag)
            }
        }
    }

    private fun createHome() {
        binding.loading.visibility = View.VISIBLE
        // Todo: Get latitude and longitude from the integration of Google maps
        TuyaHomeSdk.getHomeManagerInstance().createHome(
            homeName,
            0.0,
            0.0,
            "-",
            rooms.keys.toList(),
            object : ITuyaHomeResultCallback {
                override fun onSuccess(homeBean: HomeBean?) {
                    binding.loading.visibility = View.GONE

                    homeBean?.let {
                        PrefsHandler(this@MainActivity).homeId = it.homeId
                        homeId = it.homeId
                        // Todo: display list of rooms to select
                        roomId = it.rooms[0].roomId
                        initMainUI()
                    } ?: showInitHomeError(getString(R.string.something_went_wrong))
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    binding.loading.visibility = View.GONE

                    showInitHomeError(errorMsg)
                }
            })
    }

    private fun showInitHomeError(msg: String? = null) {
        msg?.let {
            binding.createHomeMsg.text = it
        }
        binding.createHomeMsgGroup.visibility = View.VISIBLE
    }

    private fun initMainUI() {
        Log.d(tag, "Home ID: ${PrefsHandler(this).homeId}")
        binding.createHomeMsgGroup.visibility = View.GONE
        binding.deviceUiGroup.visibility = View.VISIBLE

        /*
        binding.homePartList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).also {
                it.orientation = LinearLayoutManager.HORIZONTAL
            }
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            adapter = HomePartAdapter()
        }
        // */

        binding.deviceList.adapter = DeviceAdapter (
            { id, name ->
                if (!binding.addDeviceProgressPanel.isVisible)
                    startActivity(Intent(this@MainActivity, SmartLightControlActivity::class.java).apply {
                        putExtra(EXTRA_DEVICE_ID, id)
                        putExtra(EXTRA_DEVICE_NAME, name)
                    })
            },
            { position, device ->
                AlertDialog.Builder(this)
                    .setTitle(device.name)
                    .setMessage(R.string.remove_this_device)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok
                    ) { dialog, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            DatabaseManager.getInstance(this@MainActivity).deviceDao().delete(device)
                        }
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        )
        //binding.addDeviceButton.setOnClickListener {
        binding.addDeviceFab.setOnClickListener {
            if (isWiFiConnected().not()) {
                Snackbar.make(binding.root, "Connect to a WiFi network and try again", Snackbar.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }

            val cx = binding.addDeviceFab.width / 2
            val cy = binding.addDeviceFab.height / 2
            val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
            val circularHideAnim = ViewAnimationUtils.createCircularReveal(binding.addDeviceFab, cx, cy, radius, 0f)
            circularHideAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    binding.addDeviceFab.visibility = View.INVISIBLE

                    if (supportFragmentManager.findFragmentByTag(AddNetworkBottomSheet.tag) == null) {
                        supportFragmentManager.setFragmentResultListener(
                            AddNetworkBottomSheet.tag,
                            this@MainActivity,
                            this@MainActivity
                        )
                        AddNetworkBottomSheet.newInstance().apply {
                            isCancelable = false
                            show(supportFragmentManager, AddNetworkBottomSheet.tag)
                        }
                    }
                }
            })
            circularHideAnim.start()
        }

        lifecycleScope.launch {
            DatabaseManager.getInstance(this@MainActivity).deviceDao().getAllDevice().collect {
                (binding.deviceList.adapter as DeviceAdapter).submitList(it)
                if (it.isNotEmpty()) {
                    binding.noDeviceMsgGroup.visibility = View.GONE
                } else {
                    binding.noDeviceMsgGroup.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getActivationToken(networkName: String, password: String) {
        binding.addDeviceProgressMsg.text = getString(R.string.get_device_token)
        binding.addDeviceProgressPanel.isVisible = true

        TuyaHomeSdk.getActivatorInstance().getActivatorToken(homeId, object : ITuyaActivatorGetToken {
        //TuyaHomeSdk.getActivatorInstance().getActivatorToken(object : ITuyaActivatorCreateToken {
            override fun onSuccess(token: String?) {
                token?.let {
                    Log.d(tag, "Token ($it) received")
                    tuyaActivator = TuyaHomeSdk.getActivatorInstance().newMultiActivator(buildActivator(networkName, password, it))
                    tuyaActivator?.start()

                    binding.addDeviceProgressMsg.text = getString(R.string.find_device)
                } ?: onAddDeviceEnded(getString(R.string.get_token_failed))
            }

            override fun onFailure(errorCode: String?, errorMsg: String?) {
                onAddDeviceEnded(errorMsg)
            }
        })
    }

    private fun buildActivator(ssid: String, password: String, token: String) =
        ActivatorBuilder()
            .setContext(this)
            .setActivatorModel(ActivatorModelEnum.TY_EZ)
            //.setTimeOut(300) // default is 100 seconds
            .setSsid(ssid)
            .setPassword(password)
            .setToken(token)
            .setListener(object : ITuyaSmartActivatorListener {
                override fun onStep(step: String?, data: Any?) {
                    when (step) {
                        ActivatorEZStepCode.DEVICE_FIND -> {
                            Log.d(tag, "Device found")
                            binding.addDeviceProgressMsg.text = getString(R.string.pair_device)
                        }
                        ActivatorEZStepCode.DEVICE_BIND_SUCCESS -> {
                            Log.d(tag, "Device bind success")
                        }
                    }
                }

                override fun onActiveSuccess(deviceBean: DeviceBean?) {
                    tuyaActivator?.stop()

                    deviceBean?.let {
                        Log.d(tag, "Device info: ${it.devId} ${it.productId} ${it.name} Support Standard Instruction Set: ${TuyaHomeSdk.getDataInstance().isStandardProduct(it.productId)}")

                        TuyaHomeSdk.newRoomInstance(roomId).addDevice(it.devId,
                            object : IResultCallback {
                                override fun onError(code: String?, error: String?) {
                                    Log.d(tag, "Error add device to room: $error")
                                }

                                override fun onSuccess() {
                                    Log.d(tag, "Success add device to room")
                                }
                            })

                        lifecycleScope.launch(Dispatchers.IO) {
                            val results = DatabaseManager.getInstance(this@MainActivity).deviceDao()
                                .insert(Device(it.devId, it.productId, it.name))
                            withContext(Dispatchers.Main) {
                                onAddDeviceEnded(if (results.isEmpty()) getString(R.string.add_device_failed) else null)
                            }
                        }
                    } ?: onAddDeviceEnded(getString(R.string.something_went_wrong))
                }

                override fun onError(errorCode: String?, errorMsg: String?) {
                    onAddDeviceEnded(errorMsg)
                }
            })

    private fun onAddDeviceEnded(errorMsg: String? = null) {
        binding.addDeviceProgressPanel.isVisible = false
        binding.addDeviceFab.isVisible = true

        val cx = binding.addDeviceFab.width / 2
        val cy = binding.addDeviceFab.height / 2
        val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val circularRevealAnim = ViewAnimationUtils.createCircularReveal(binding.addDeviceFab, cx, cy, 0f, radius)
        circularRevealAnim.start()

        errorMsg?.let {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
        }
    }

    private class DeviceAdapter(
        val onDeviceClick : (String, String) -> Unit,
        val onDeviceLongClick : (Int, Device) -> Unit
    ) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.deviceName.text = getItem(position).name
        }

        inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val deviceName: TextView = itemView.findViewById(R.id.textView)
            init {
                itemView.setOnClickListener {
                    val (deviceId, _, name) = getItem(adapterPosition)
                    onDeviceClick(deviceId, name)
                }

                itemView.setOnLongClickListener {
                    onDeviceLongClick(adapterPosition, getItem(adapterPosition))
                    true
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.did == newItem.did
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }

    private inner class HomePartAdapter : RecyclerView.Adapter<HomePartAdapter.ViewHolder>() {
        private var selectedHomePart = 0

        override fun getItemCount(): Int = rooms.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_home_part, parent,false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder) {
                binding.card.setCardBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        if (selectedHomePart == position) R.color.selected_home_part else android.R.color.white
                    )
                )
                binding.icon.setImageResource(rooms.values.elementAt(position))
                binding.roomNameText.text = rooms.keys.elementAt(position)
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding = ItemHomePartBinding.bind(view)
            init {
                binding.root.setOnClickListener {
                    val prevSelection = selectedHomePart
                    selectedHomePart = adapterPosition

                    notifyItemChanged(prevSelection)
                    notifyItemChanged(selectedHomePart)
                }
            }
        }
    }


}