#include kurtsanders.SanderSoft-Library

import groovy.transform.Field

@Field static String APP_NAME                      = "Battery Level Watcher"
@Field static String NAMESPACE                     = "kurtsanders"
@Field static String AUTHOR_NAME                   = "Kurt Sanders"
@Field static final String VERSION                 = "1.0.0"
@Field static final String defaultDateTimeFormat 	= 'MMM d, yyyy, h:mm a'

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
    def js = """
		var t = document.getElementById('sales-info');
		if(t) {
			Array.from(t.rows).forEach((tr, rowIdx) => {
				Array.from(tr.cells).forEach((cell, cellIdx) => {
					if (cell.innerText == '') {
						cell.style.backgroundColor = 'lightblue';
					}else if (cell.innerText >=0 && cell.innerText <= $settings.level1) {
						cell.style.backgroundColor = 'yellow';
					}else if (cell.innerText > $settings.level1 && cell.innerText <= $settings.level3) {
						cell.style.backgroundColor = '#e6ffe6';
					}else if (cell.innerText > $settings.level3 && cell.innerText < 100) {
						cell.style.backgroundColor = 'lightgreen';
					}else if (cell.innerText >= 100) {
						cell.style.backgroundColor = 'green';
					}
				});
			});
		}
		
		/*var table = document.getElementById("sales-info");
		for (var i = 0, row; row = table.rows[i]; i++) {
			for (var j = 0, cell; cell = row.cells[j]; j++) {
				if (cell.innerText == '') {
					cell.style.backgroundColor = 'lightblue';
				}else if (cell.innerText >=0 && cell.innerText <=$settings.level1) {
					cell.style.backgroundColor = 'yellow';
				}else if (cell.innerText > $settings.level1 && cell.innerText <=$settings.level3) {
					cell.style.backgroundColor = '#e6ffe6';
				}else if (cell.innerText > $settings.level3 && cell.innerText <=100) {
					cell.style.backgroundColor = 'lightgreen';
				}else if (cell.innerText >= 100) {
					cell.style.backgroundColor = 'green';
				}
			}  
		}*/
	"""
    def htmlStyle 	= 
        	'<html><head><style>' + 
            'table th, td {text-align: center;padding: 8px;}' +
            'tr:nth-child(even) td:nth-child(n+2),tr:nth-child(even) th:nth-child(n+2) {background-color: #D6EEEE; /* Light gray for odd rows */}' +        
//            'tr:nth-child(even) {background-color: #D6EEEE;}' +
//            'table td:nth-child(1){background-color:white;}' +
            'table {border-collapse: collapse;width: 100%;}' +
            '</style></head>'
    def htmlHead 	= 
        	"<table id='sales-info'><thead>" + 
            '<tr><th>Battery Level</th>' +
            '<th>Device Name</th>' +
            '<th>Device Type</th>' +
            '<th>Last Battery Report Duration</th>' +
            '<th>Last Activity</th>' +
            '</thead></tr><tbody>'
	def htmlFoot 	= "</table><script>${js}</script></tbody></html>"
    def lineout = ''

    return dynamicPage(pageProperties) {
        // Iterate four loops
        for (int i = 0; i < 5; i++) {
            def tempList = deviceList.findAll { it.group == i }
            if (tempList) {
                def sectionTitile
                switch(i) {
                    case 0:
                    	sectionTitle = getFormat('header-red',"Batteries with Errors or No Status")
                    break
                    case 1:
                    	sectionTitle = getFormat('header-red',"Batteries with LOW charge (less than $settings.level1)%")
                    break
                    case 2:
                    	sectionTitle = getFormat('header-blue',"Batteries with Medium Charge (between ${settings.level1}% and ${settings.level3}%)")
                    break
                    case 3:
                    sectionTitle = getFormat('header-red',"Batteries with High Charge (more than ${settings.level3}%)")
                    break
                    case 4:
                    	sectionTitle = getFormat('header-green',"Batteries with Full Charge")
                    break
                }
                lineout += "<th colspan='5'>${sectionTitle}</th>"
                tempList.sort { it.battery }.each { userMap ->
                    lineout += '<tr>' + 
                        "<td>${userMap.battery}</td>" +
                        "<td><a href=http://${location.hub.localIP}/device/edit/${userMap.id}>${userMap?.name}</a></td>" +
                        "<td>${userMap.type}</td>" +
                        "<td>${userMap.lastBatteryReportDuration}</td>" +
                        "<td>${userMap?.lastActivity}</td>" +
                        '</tr>'
                }
            }
        }
        section() {
                paragraph "${htmlStyle}${htmlHead}${lineout}${htmlFoot}"            
        }        
        section(sectionHeader("Application Setup")) {
            href "pageStatus", title:"Refresh", description:""
            href "pageConfigure", title:"Application Configure", description:""
            displayPaypal()
        }
        section(sectionHeader("Logging Options")) {
            if (logLevel == null && logLevelTime == null) {
                logInfo "${app.name}: Setting Inital logLevel and logLevelTime defaults"
                app.updateSetting(logLevel, [type: "enum", value: [5]])
                app.updateSetting(logLevelTime, [type: "enum", value: [30]])
            }
            //Logging Options
            input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"), submitOnChange: true,
                description: fmtDesc("Logs selected level and above"), defaultValue: 3, options: LOG_LEVELS
            input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"), submitOnChange: true,
                description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 10, options: LOG_TIMES
        }
        displayPaypal()
    }
}

// Show Configure Page
def pageConfigure() {
    def pageProperties = [
        name:           "pageConfigure",
        title:           getFormat("title","${APP_NAME} Application Configuration"),
        nextPage:       "pageStatus",
        uninstall:      true
    ]
    return dynamicPage(pageProperties) {
        section(getFormat('header-green',"Select Devices with a Battery attribute")) {
            input name: "devices", 				type: "capability.battery", title: "Which devices with batteries?", multiple: true, required: true
        }        
        section(getFormat('header-green',"Application Settings")) {
            input name: "level1", 				type: "number", title: "Low battery threshold?", defaultValue: "20", required: true
            input name: "level3",				type: "number", title: "Medium battery threshold?", defaultValue: "70", required: true
        }        
        section(getFormat('header-green',"Low Battery Notification")) {
            input name: "time", 				type: "time", title: "Notify at what time daily?", required: true
            input name: "pushMessage", 			type: "bool", title: "Send push notifications?", defaultValue: true
            input name: "sendPushMessage", 		type: "capability.notification", title: "Select Notification Devices", 	multiple: true, required: false
            input name: "includeHubName", 		type: "bool", title: "Include hub name in notifications (${location.name})", defaultValue: true
            input name: "pushMessageBattery", 	type: "bool", title: "Send push notifications for all devices?", 	defaultValue: false
            input name: "modes", 				type: "mode", title: "Only send notifications if mode is", multiple: true, required: false
        }
        section(getFormat('header-green',"Options")) {
            label title:"Assign a name", required:false
        }
    }
}

def installed() {
    logInfo "Initialized with settings: ${settings}"
    app.updateSetting(level1,[value: 33, type:"number"])
    app.updateSetting(level3,[value: 67, type:"number"])
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
    logDebug "batteryWatcher() called: ${evt.device.displayName}: ${evt.name} ${evt.value}%"
    if (pushMessageBattery) send("${evt.device.displayName}: ${evt.name} is ${evt.value}%")
    if (evt.value.toInteger() < settings.level1) {
        send("Warning: ${evt.device.displayName} battery is ${evt.value}% is below your low threshold of ${settings.level1}%.")
    }
}

def updateStatus() {
    settings.devices.each() {
        try {
            switch(it.currentBattery.toInteger()) {
                case {it > 100}:
	                send("${it.displayName} battery is ${it.currentBattery}%, which is over 100.")
    	            break
                case {it >= settings.level1}:
                	break
                case {it < settings.level1}:
                	send("${it.displayName} battery is ${it.currentBattery}% which is lesss than threshold level: ${settings.level1}%.)")
                	break
                default:
                    send("${it.displayName} battery '${it.currentBattery}'is not reporting an integer battery level.")
    	            break
        	}
        } catch (e) {
            logErr "Caught error checking battery status."
            logErr e
            send("Error: ${it.displayName} battery '${it.currentBattery}' reported a non-integer level.")
        }
    }
}

def makeDeviceList() {
    updateStatus()
    def deviceList = []
    int group 
    settings.devices.each() {
        try {
            switch(it.currentBattery.toInteger()) {
                case {it <= settings.level1}:
                group = 1
                break
                case {it <= settings.level3}:
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
        def lastBatteryReportDuration
        def lastActivity = it.getLastActivity()
        def lastBatteryEvent = it.events(max:200).find{it.name == 'battery'}
        // Create deviceList of devices with group numbers
        def map = [:] // Create an empty map
        map.group 						= group
        map.id  						= it.id 
        map.name						= it.label?:it.displayName
        map.battery						= it.currentBattery
        map.type						= it.typeName
        map.lastActivity 				= durationCalc(lastActivity)
        map.lastBatteryReportDuration 	= durationCalc(lastBatteryEvent?.date)
        deviceList << map // Add map to the list
    }
    logDebug "deviceList = ${deviceList}"
    return deviceList
}

def durationCalc(endDate) {
    def durationOutput=''
    if (endDate) {
        def today = new Date()
        use(groovy.time.TimeCategory) {
            def duration = today - endDate
            if (duration.days>0) {
                if (duration.days==1) {
                    durationOutput += "${duration.days} day, "
                } else {
                    durationOutput += "${duration.days} days, "                    
                }
            }
            if (duration.hours>0) {
                if (duration.hours==1) {
                    durationOutput += "${duration.hours} hour, "
                } else {
                    durationOutput += "${duration.hours} hours, "                    
                }
            }
            if (duration.minutes==1) {
                durationOutput += duration.minutes + ' min.'
            } else {
                durationOutput += duration.minutes + ' mins.'
            }
        }
    }
    return durationOutput?:'unknown'
}

Boolean isModeOK() {
   Boolean isOK = !settings["modes"] || settings["modes"].contains(location.mode)
   logDebug "Checking if mode is OK; reutrning: ${isOK}"
   return isOK
}

def send(msg) {
    if (settings.pushMessage && isModeOK()) {
        if (settings["includeHubName"]) {
            msg = "${location.name}: ${msg}"
        }
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

