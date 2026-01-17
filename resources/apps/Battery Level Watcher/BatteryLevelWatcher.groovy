#include kurtsanders.SanderSoft-Library

import groovy.transform.Field

@Field static String APP_NAME                      = "Battery Level Watcher"
@Field static String NAMESPACE                     = "kurtsanders"
@Field static String AUTHOR_NAME                   = "Kurt Sanders"
@Field static final String VERSION                 = "1.0.0"

definition(
    name              : APP_NAME,
    namespace         : NAMESPACE,
    author            : AUTHOR_NAME,
    description       : "Active Battery Level Watcher.",
    importUrl		  : "https://raw.githubusercontent.com/KurtSanders/HubitatPackages/refs/heads/master/resources/apps/Battery%20Level%20Watcher/BatteryLevelWatcher.groovy",
    category          : "",
    iconUrl           : "",
    iconX2Url         : "",
    singleInstance    : true
) {
}

preferences {
    page name:"pageStatus"
    page name:"pageConfigure"
}

// Show Status page
def pageStatus() {
    def pageProperties = [
        name:       "pageStatus",
        title:      getFormat("title","${APP_NAME} Status - Version ${VERSION}"),
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]    
    def deviceList = makeDeviceList()
    return dynamicPage(pageProperties) {
        for (int i = 0; i < 5; i++) {
	        def lineout = ''
            def tempList = deviceList.findAll { it.group == i }
            if (tempList) {
                def sectionTitile
                switch(i) {
                    case 0:
                    	sectionTitle = getFormat('header-red',"Batteries with errors or no status")
                    break
                    case 1:
                    	sectionTitle = getFormat('header-red',"Batteries with low charge (less than $settings.level1)")
                    break
                    case 2:
                    	sectionTitle = getFormat('header-orange',"Batteries with medium charge (between $settings.level1 and $settings.level3)")
                    break
                    case 3:
                    	sectionTitle = getFormat('header-orange',"Batteries with high charge (more than $settings.level3)")
                    break
                    case 4:
                    	sectionTitle = getFormat('header-green',"Batteries with full charge")
                    break
                }                    
                section("${sectionTitle}") {
                    tempList.sort { it.battery }.each { userMap ->
                        lineout += '<tr>' + 
                            "<td>${userMap.battery}%</td>" +
                            "<td><a href=http://${location.hub.localIP}/device/edit/${userMap.id}>${userMap?.name}</a></td>" +
                            "<td>${userMap.type}</td>" +
                            '</tr>'
                    }
                def htmlStyle 	= '<html><head><style>' + 
                    			  'table,th,td{border: 1px solid black;border-collapse:collapse;padding:8px;text-align: center;}' +
                    			  'table{margin-left:auto;margin-right:auto;width: 50%;}'+
                    			  '</style></head>'
                def htmlHead 	= '<table><thead><tr>' + 
                    '<th>Battery Level</th>' +
                    '<th>Device Name</th>' +
                    '<th>Device Type</th>' +
                    '</thead></tr><tbody>'
                def htmlFoot 	= '</table></tbody></html>'
                    paragraph "${htmlStyle}${htmlHead}${lineout}${htmlFoot}"
                }
            }
        }
        section(sectionHeader("Setup")) {
            href "pageStatus", title:"Refresh", description:""
            href "pageConfigure", title:"Configure", description:""
            displayPaypal()
        }
        section(sectionHeader("Logging Options")) {
            if (logLevel == null && logLevelTime == null) {
                logInfo "${app.name}: Setting Innital logLevel and LogLevelTime defaults"
                app.updateSetting(logLevel, [type: "enum", value: [5]])
                app.updateSetting(logLevelTime, [type: "enum", value: [30]])
            }
            //Logging Options
            input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"), submitOnChange: true,
                description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
            input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"), submitOnChange: true,
                description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 10, options: LOG_TIMES
        }
    }
}

// Show Configure Page
def pageConfigure() {
    def inputBattery   = [
        name:           "devices",
        type:           "capability.battery",
        title:          "Which devices with batteries?",
        multiple:       true,
        required:       true
    ]
    def inputLevel1    = [
        name:           "level1",
        type:           "number",
        title:          "Low battery threshold?",
        defaultValue:   "20",
        required:       true
    ]
    def inputLevel3    = [
        name:           "level3",
        type:           "number",
        title:          "Medium battery threshold?",
        defaultValue:   "70",
        required:       true
    ]
    def inputTime      = [
        name:           "time",
        type:           "time",
        title:          "Notify at what time daily?",
        required:       true
    ]
    def inputPush      = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Send push notifications?",
        defaultValue:   true
    ]
    def pageProperties = [
        name:           "pageConfigure",
        title:           getFormat("title","${APP_NAME} Configuration"),
        nextPage:       "pageStatus",
        uninstall:      true
    ]
    return dynamicPage(pageProperties) {
        section(getFormat('header-green',"Select Devices")) {
            input inputBattery
        }        
        section(getFormat('header-green',"Settings")) {
            input inputLevel1
            input inputLevel3
        }        
        section(getFormat('header-green',"Low Battery Notification")) {
            input inputTime
            input inputPush   
            input "sendPushMessage", "capability.notification", title: "Select Notification Devices", multiple: true, required: false
            input name: "modes", type: "mode", title: "Only send notifications if mode is", multiple: true, required: false
        }
        section(getFormat('header-green',"Options")) {
            label title:"Assign a name", required:false
        }
    }
}

def installed() {
    logInfo "Initialized with settings: ${settings}"
    initialize()
}

def updated() {
    if (settings.level1 == null) 	{ settings.level1 = 33 }
    if (settings.level3 == null) 	{ settings.level3 = 67 }
    if (settings.pushMessage) 		{ settings.pushMessage = true }
    unschedule()
    unsubscribe()
    initialize()
    //nightlyStatus() for testing fx
}

def initialize() {
    schedule(settings.time, updateStatus)
    subscribe(devices,'battery','batteryWatcher')
    checkLogLevel()
}

def batteryWatcher(evt) {
    logDebug "batteryWatcher() called: ${evt.device.displayName}: ${evt.name} ${evt.value}"
    if (evt.value.toInteger() < settings.level1) {
        send("Warning: ${evt.device.displayName} battery is ${evt.value}% is below your low threshold of ${settings.level1}%.")
    }
}

def updateStatus() {
    settings.devices.each() {
        try {
            switch(it.currentBattery.toInteger()) {
                case {it > 100}:
	                send("${it.displayName} battery is ${it.currentBattery}, which is over 100.")
    	            break
                case {it < settings.level1}:
                	send("${it.displayName} battery is ${it.currentBattery} (threshold ${settings.level1}.)")
                	break
                default:
	                send("${it.displayName} battery is not reporting.")
    	            break
        	}
        } catch (e) {
            logErr "Caught error checking battery status."
            logErr e
            send("${it.displayName} battery reported a non-integer level.")
        }
    }
}

def makeDeviceList() {
    def deviceList = []
    int group 
    settings.devices.each() {
        try {
            switch(it.currentBattery.toInteger()) {
                case {it <= settings.level1}:
                group = 1
                break
                case {it <= settings.level2}:
                group = 2
                break
                case {it < 100}:
                group = 3
                break
                case {it == 100}:
                group = 4
                break
                default:
                group = 0
                break
            }
        } catch (e) {
            group = '0'
            logErr "Caught error checking battery status for '${it.displayName}'."
            logErr e
        }
        // Create deviceList of devices with group numbers
        def map = [:] // Create an empty map
        map.group 	= group
        map.id  	= it.id 
        map.name	= it.label?:it.displayName
        map.battery	= it.currentBattery
        map.type	= it.typeName
        deviceList << map // Add map to the list
    }
    return deviceList
}

Boolean isModeOK() {
   Boolean isOK = !settings["modes"] || settings["modes"].contains(location.mode)
   logDebug "Checking if mode is OK; reutrning: ${isOK}"
   return isOK
}

def send(msg) {
    if (settings.pushMessage && isModeOK()) {     
        sendPushMessage.deviceNotification(msg)
    } 
}

//Logging Functions
def logMessage(String msg) {
    if (app) {
        return "<span style='color: blue'>${app.name}</span>: ${msg}"   
    } else {
        return "<span style='color: green'>${device.name}</span>: ${msg}"           
    }
}

void logErr(String msg) {
    if (logLevelInfo.level>=1) log.error "${logMessage(msg)}"
}
void logWarn(String msg) {
    if (logLevelInfo.level>=2) log.warn "${logMessage(msg)}"
}
void logInfo(String msg) {
    if (logLevelInfo.level>=3) log.info "${logMessage(msg)}"
}

void logDebug(String msg) {
        if (logLevelInfo.level>=4) log.debug "${logMessage(msg)}"
}

void logTrace(String msg) {
        if (logLevelInfo.level>=5) log.trace "${logMessage(msg)}"
}

