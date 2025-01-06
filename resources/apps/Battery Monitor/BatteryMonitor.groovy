#include kurtsanders.SanderSoft-Library

import groovy.transform.Field

@Field static String APP_NAME                      = "Battery Monitor"
@Field static String NAMESPACE                     = "kurtsanders"
@Field static String AUTHOR_NAME                   = "Kurt Sanders"
@Field static final String VERSION                 = "1.0.0"

definition(
    name              : APP_NAME,
    namespace         : NAMESPACE,
    author            : AUTHOR_NAME,
    description       : "Access and control a BWA Cloud Control Spa.",
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

def makeDeviceHtmlLink(device) {
    return "<a href=http://${location.hub.localIP}/device/edit/${device.id}>${device?.currentBattery?:'?'}% ${device?.displayName}</a><br>"
}

// Show Status page
def pageStatus() {
    def pageProperties = [
        name:       "pageStatus",
        title:      getFormat("title","${APP_NAME} Status"),
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    def listLevel0 = ""
    def listLevel1 = ""
    def listLevel2 = ""
    def listLevel3 = ""
    def listLevel4 = ""

    if (settings.level1 == null) { settings.level1 = 33 }
    if (settings.level3 == null) { settings.level3 = 67 }
    if (settings.pushMessage) { settings.pushMessage = true }
    return dynamicPage(pageProperties) {
        settings.devices.each() {
            try {
                if (it.currentBattery == null) {
                    listLevel0 += makeDeviceHtmlLink(it)
                } else if (it.currentBattery >= 0 && it.currentBattery <  settings.level1.toInteger()) {
                    listLevel1 += makeDeviceHtmlLink(it)
                } else if (it.currentBattery >= settings.level1.toInteger() && it.currentBattery <= settings.level3.toInteger()) {
                    listLevel2 += makeDeviceHtmlLink(it)
                } else if (it.currentBattery >  settings.level3.toInteger() && it.currentBattery < 100) {
                    listLevel3 += makeDeviceHtmlLink(it)
                } else if (it.currentBattery == 100) {
                    listLevel4 += makeDeviceHtmlLink(it)
                } else {
                    listLevel0 += makeDeviceHtmlLink(it)
                }
            } catch (e) {
                log.trace "Caught error checking battery status for '${it.displayName}'."
                log.trace e
                listLevel0 += "$it.displayName\r\n\r\n"
            }
        }

        if (listLevel0) {
            section(getFormat('header-red',"Batteries with errors or no status")) {
                paragraph listLevel0.trim()
            }
        }

        if (listLevel1) {
            section(getFormat('header-red',"Batteries with low charge (less than $settings.level1)")) {
                paragraph listLevel1.trim()
            }
        }

        if (listLevel2) {
            section(getFormat('header-orange',"Batteries with medium charge (between $settings.level1 and $settings.level3)")) {
                paragraph listLevel2.trim()
            }
        }

        if (listLevel3) {
            section(getFormat('header-orange',"Batteries with high charge (more than $settings.level3)")) {
                paragraph listLevel3.trim()
            }
        }

        if (listLevel4) {
            section(getFormat('header-green',"Batteries with full charge")) {
                paragraph listLevel4.trim()
            }
        }

        section("Menu") {
            href "pageStatus", title:"Refresh", description:""
            href "pageConfigure", title:"Configure", description:""
            href "pageAbout", title:"About", description: ""
        }
    }
}

// Show Configure Page
def pageConfigure() {
    def helpPage =
        "Select devices with batteries that you wish to monitor."

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

      def nightlyStatusMsg = [
        name:           "nightlyStatus",
        type:           "bool",
        title:          "Send scheduled status message for all devices?",
        defaultValue:   true
    ]



    def inputPush      = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Send push notifications?",
        defaultValue:   true
    ]

    def pageProperties = [
        name:           "pageConfigure",
        title:          "${app.name} Configuration",
        nextPage:       "pageStatus",
        uninstall:      true
    ]

    return dynamicPage(pageProperties) {
        section("About") {
            paragraph helpPage
        }

        section("Devices") {
            input inputBattery
        }

        section("Settings") {
            input inputLevel1
            input inputLevel3
        }

        section("Notification") {
            input inputTime
            input inputPush
            input "sendPushMessage", "capability.notification", title: "Notification Devices: Hubitat PhoneApp or Pushover", multiple: true, required: false


            input nightlyStatusMsg
           // input inputSMS
        }

        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def installed() {
    log.debug "Initialized with settings: ${settings}"

    initialize()
}

def updated() {
    unschedule()
    unsubscribe()
    initialize()
    //nightlyStatus() for testing fx
  //  log.debug "in updated pushmessage = $settings.pushMessage"
}

def initialize() {
    schedule(settings.time, updateStatus)
}

def send(msg) {
    log.debug msg

    if (settings.pushMessage) {

        sendPushMessage.deviceNotification(msg)
    }
   /* } else {
        sendNotificationEvent(msg)
    }*/

  /*  if (settings.phoneNumber != null) {
        sendSms(phoneNumber, msg)
    }*/
}

// lgk now fx to prepare nightly message and send to get complete status not just outstanding devices.
def nightlyStatus()

{
    def listLevel0 = ""
    def listLevel1 = ""
    def listLevel2 = ""
    def listLevel3 = ""
    def listLevel4 = ""
    def myhub =  location.hubs[0].name
    def outgoingMsg = "For Hub: $myhub\r\n"

    if (settings.level1 == null) { settings.level1 = 33 }
    if (settings.level3 == null) { settings.level3 = 67 }

    if (settings.nightlyStatus == true)
     {
        settings.devices.each() {
            try {
                if (it.currentBattery == null) {
                    listLevel0 += "$it.displayName\r\n"
                } else if (it.currentBattery >= 0 && it.currentBattery <  settings.level1.toInteger()) {
                    listLevel1 += "$it.currentBattery  $it.displayName\r\n"
                } else if (it.currentBattery >= settings.level1.toInteger() && it.currentBattery <= settings.level3.toInteger()) {
                    listLevel2 += "$it.currentBattery  $it.displayName\r\n"
                } else if (it.currentBattery >  settings.level3.toInteger() && it.currentBattery < 100) {
                    listLevel3 += "$it.currentBattery  $it.displayName\r\n"
                } else if (it.currentBattery == 100) {
                    listLevel4 += "$it.displayName\r\n"
                } else {
                    listLevel0 += "$it.currentBattery  $it.displayName\r\n"
                }
            } catch (e) {
                log.trace "Caught error checking battery status."
                log.trace e
                listLevel0 += "$it.displayName\r\n"
            }
        }

        if (listLevel0) {
      	 	    outgoingMsg = outgoingMsg + "Batteries with errors or no status\r\n\r\n" +
                listLevel0.trim()
            }

      // prepare message

        if (listLevel1) {
     	  	    outgoingMsg = outgoingMsg + "\r\n\r\nBatteries with low charge (less than $settings.level1)\r\n\r\n" +
         		listLevel1.trim()
            }

        if (listLevel2) {
       			 outgoingMsg = outgoingMsg + "\r\n\r\nBatteries with medium charge (between $settings.level1 and $settings.level3)\r\n\r\n" +
                 listLevel2.trim()
        }

        if (listLevel3) {
                outgoingMsg = outgoingMsg + "\r\n\r\nBatteries with high charge (more than $settings.level3)\r\n\r\n" +
                listLevel3.trim()
            }


        if (listLevel4) {
              outgoingMsg = outgoingMsg + "\r\n\r\nBatteries with full charge\r\n\r\n" +
                 listLevel4.trim()
            }

  // lgk now send message
  //log.debug "nightly message is $outgoingMsg"

 send(outgoingMsg)
 }
}


def updateStatus() {
    settings.devices.each() {
        try {
            if (it.currentBattery == null) {
                send("${it.displayName} battery is not reporting.")
            } else if (it.currentBattery > 100) {
                send("${it.displayName} battery is ${it.currentBattery}, which is over 100.")
            } else if (it.currentBattery < settings.level1) {
                send("${it.displayName} battery is ${it.currentBattery} (threshold ${settings.level1}.)")
            }
        } catch (e) {
            log.trace "Caught error checking battery status."
            log.trace e
            send("${it.displayName} battery reported a non-integer level.")
        }
    }
   nightlyStatus()
}
