/*
 * EZsalt Driver Hubitat Integration by Kurt Sanders 2025 
 *
 *  Copyright 2025 Kurt Sanders
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

#include kurtsanders.SanderSoft-Library
#include kurtsanders.EZsalt-Library

import groovy.transform.Field

@Field static final String APP_NAME      			= "EZsalt Driver"
@Field static final String VERSION                 	= "0.0.1"

metadata {
    definition ('name': "EZsalt Driver", 'namespace': NAMESPACE, 'author': AUTHOR_NAME) {
        capability "Initialize"
        capability "Actuator"
        capability "Consumable" // consumableStatus - ENUM ["missing", "order", "maintenance_required", "good", "replace"]
        capability "Refresh"
        
        command "disconnect"

        attribute "distance", "number"
        attribute "SaltLevelPct", "number"
        attribute "SaltTile", "string"
        attribute "NotificationPCT", "number"
        attribute "LastMessage", "string"
    }

    preferences {
    	//  Display Help Link
		input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
        //  User Preferences & Settings
        input name: "macaddress", type: "text", title: "EZSalt Mac Address (xx:xx:xx:xx:xx:xx):", required: true, displayDuringSetup: true
        input name: "salttankheightin", type: "number", title: "Salt Tank Height (inches):", required: true, displayDuringSetup: true, defaultValue: "30"
        input name: "saltlevellowin", type: "number", title: "Salt Level Notification Height (inches):", required: true, displayDuringSetup: true, defaultValue: "15"
        input name: "RunTime", type: "time", title: "When to Run (Time) each day?", required: true, displayDuringSetup: true, defaultValue: "18:00"
        input name: "QOS", type: "text", title: "QOS Value:", required: false, defaultValue: "1", displayDuringSetup: true
        input name: "retained", type: "bool", title: "Retain message:", required: false, defaultValue: false, displayDuringSetup: true
        //	Logging Levels & Help
		input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
    		description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
		input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
    		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
    }
}

def installed() {
    setLogLevel("Debug", "30 Minutes")
    checkLogLevel()  // Set Logging Objects    
	log.info "Setting Inital logging level to 'Debug' for 30 minutes"
    log.info "Installed EZSalt Tank Device..."
}

def updated() {
	logInfo "updated..."
    checkLogLevel()  // Set Logging Objects    
    logDebug "Debug logging is: ${logEnable == true}"      
    state.NotificationPCT = Math.round(saltlevellowin / salttankheightin * 100)
    state.clientID = device.deviceNetworkId[-8..-1].trim()
    cronSchedule()
    initialize()
}

def disconnect() {
    logInfo "Disconnecting from EZsalt mqtt broker"
    interfaces.mqtt.disconnect()
}

def cronSchedule() {
    def hour = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", RunTime).format("HH")
    def min = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", RunTime).format("mm")
    logInfo "Schedule set for reading salt level at ${hour}:${min} every day"
    unschedule() 
    schedule("0 ${min} ${hour} * * ?", "refresh")
}

def initialize() {
    String[] macaddressParts = macaddress.split(":")
    state.topicSub = "tele/EZsalt_${macaddressParts[3]+macaddressParts[4]+macaddressParts[5]}/SENSOR" 
    logTrace "==> state.topicSub= ${state.topicSub}"
    sendEvent(name: "NotificationPCT", value: state.NotificationPCT)
    mqttConnect()
}

void refresh() {
	 initialize()   
}

def mqttConnect() {
    try {
        if(settings?.retained==null) settings?.retained=false
        if(settings?.QOS==null) setting?.QOS="1"       
        interfaces.mqtt.connect(MQTT_URL,"client-hubitat",null,null)
        pauseExecution(1000)
        if (interfaces.mqtt.isConnected()) {
            logDebug "Successfully connected to ${MQTT_URL}"
            interfaces.mqtt.subscribe(state.topicSub)
            logDebug "Subscribed to: ${state.topicSub}"
        } else {
            logDebug "mqttConnect(): Initialize error connecting to ${MQTT_URL}"
            disconnect()
        }
    } catch(e) {
        logErr "mqttConnect(): Initialize error: ${e.message}"
        disconnect()
    }
    pauseExecution(1000)
    interfaces.mqtt.disconnect()
}

def parse(String description) {
    logTrace "==> parse()"
    Date date = new Date(); 
    topic = interfaces.mqtt.parseMessage(description).topic
    topic = topic.substring(topic.lastIndexOf("/") + 1)
    logDebug "==> topic= ${topic}"
    payload = interfaces.mqtt.parseMessage(description).payload
    logDebug "==> payload= ${payload}"
    int distance = Math.round(new groovy.json.JsonSlurper().parseText(payload).VL53L0X.Distance / 25.4)    
    logDebug "==> distance= ${distance}"
    int SaltLevelPct = ((salttankheightin - distance) / salttankheightin * 100).toInteger()
    logDebug "==> SaltLevelPct= ${SaltLevelPct}"

    switch(SaltLevelPct) {
        case 0..25: 
        img = "salt-empty.png"
        break
        case 26..50: 
        img = "salt-half.png"
        break
        case 51..100: 
        img = "salt-full.png"
        break
        default: 
            img = "Attention.svg"
        break
    }

    if (distance > saltlevellowin) {
        consumableStatusValue = "good"
        sendEvent(name: "switch", value: "off", displayed: true)
    } else {
        consumableStatusValue = "order"
        sendEvent(name: "switch", value: "on", displayed: true)
    }
    
    state."${topic}" = "${payload}"
    state.lastpayloadreceived = "Topic: ${topic} : ${payload} @ ${date.toString()}"        

    sendEvent(name: "LastMessage", 			value : "${payload} @ ${date.toString()}"	, displayed: true)
    sendEvent(name: "distance", 			value : distance							, displayed: true)
    sendEvent(name: "consumableStatus", 	value : consumableStatusValue				, displayed: true)
    sendEvent(name: "SaltLevelPct", 		value : SaltLevelPct						, displayed: true)
            
    img = "${IMAGES_LINK}${img}"
    html = "<style>img.salttankImage { max-width:80%;height:auto;}div#salttankImgWrapper {width=100%}div#salttankWrapper {font-size:13px;margin: 30px auto; text-align:center;}</style><div id='salttankWrapper'>"
    html += "<div id='salttankImgWrapper'><center><img src='${img}' class='saltankImage'></center></div>"
    html += "Salt Level: ${SaltLevelPct}%</div>"
    sendEvent(name: "SaltTile", 			value: html									, displayed: true)
    
    logDebug "topic: ${topic}"
    logDebug "payload: ${payload}"
    logDebug "distance = ${distance} in"
    logDebug "Consumables: ${consumableStatusValue}"
    logDebug "Tank Fixed Height: ${salttankheightin} Salt Level Pct: ${SaltLevelPct} Notify: ${state.NotificationPCT}%"
}


def mqttClientStatus(String status) {
    if(!status.contains("succeeded")) {
        logErr "mqttClientStatus: ${status}"
        try { interfaces.mqtt.disconnect() }
        catch (e) { logErr "interfaces.mqtt.disconnect(): ${e}" }

        logWarn "MQTT Broker: ${status} Will restart in 5 seconds"
        runIn (5,mqttConnect)  
    }
} 