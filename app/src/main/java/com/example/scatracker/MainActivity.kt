package com.example.scatracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker

import com.example.scatracker.RouteTrackingService.Companion.EXTRA_SECRET_CAT_AGENT_ID
import com.example.scatracker.worker.CatFurGroomingWorker
import com.example.scatracker.worker.CatLitterBoxSittingWorker
import com.example.scatracker.worker.CatStretchingWorker
import com.example.scatracker.worker.CatSuitUpWorker

class MainActivity : AppCompatActivity() {
    private val workManager = WorkManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //define network constraints to require internet connection
        val networkConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        //define cat agent
        val catAgentId = "CatAgent1"

        //define all the tasks that will be requested of the cat agent
        //set their network constraints and input data
        val catStretchingRequest = OneTimeWorkRequest.Builder(CatStretchingWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getCatAgentIdInputData(CatStretchingWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)
            ).build()
        val catFurGroomingRequest = OneTimeWorkRequest.Builder(CatFurGroomingWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getCatAgentIdInputData(CatFurGroomingWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)
            ).build()
        val catLitterBoxRequest = OneTimeWorkRequest.Builder(CatLitterBoxSittingWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getCatAgentIdInputData(CatLitterBoxSittingWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)
            ).build()
        val catSuitUpRequest = OneTimeWorkRequest.Builder(CatSuitUpWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getCatAgentIdInputData(CatSuitUpWorker.INPUT_DATA_CAT_AGENT_ID, catAgentId)
            ).build()

        //set observers for all requests and set success message
        observeRequest(catStretchingRequest, "Agent done stretching")
        observeRequest(catFurGroomingRequest, "Agent done grooming its fur")
        observeRequest(catLitterBoxRequest, "Agent done with the litter box")
        observeRequest(catSuitUpRequest, "Agent done suiting up. Ready to go!")

        //set the order of requests for the agent
        workManager.beginWith(catStretchingRequest)
            .then(catFurGroomingRequest)
            .then(catLitterBoxRequest)
            .then(catSuitUpRequest)
            .enqueue()
    }

    //set up the observers for agents
    private fun observeRequest(request: OneTimeWorkRequest, resultMessage: String) {
        //find the work info for the specified id and set the action for when the request is completed
        workManager.getWorkInfoByIdLiveData(request.id)
            .observe(this, Observer { info ->
                if (info.state.isFinished) {
                    showResult(resultMessage)
                    launchTrackingService()
                }
            })
    }

    //set observer for tracking service;
    private fun launchTrackingService() {
        RouteTrackingService.trackingCompletion.observe(this, Observer { agentId ->
            showResult("Agent $agentId arrived!")
        })
        val serviceIntent = Intent(this, RouteTrackingService::class.java).apply {
            putExtra(EXTRA_SECRET_CAT_AGENT_ID, "007")
        }
        //calls onStartCommand in RouteTrackingService
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    //get the agent's input data to track progress
    private fun getCatAgentIdInputData(catAgentIdKey: String, catAgentIdValue: String) =
        Data.Builder().putString(catAgentIdKey, catAgentIdValue).build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}