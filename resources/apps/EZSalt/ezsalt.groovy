/*******************************************************************
*** SanderSoft - Core App/Device Helpers                        ***
/*******************************************************************/

import groovy.transform.Field

@Field static final String 	AUTHOR_NAME          	= "Kurt Sanders"
@Field static final String 	NAMESPACE            	= "kurtsanders"
@Field static final String  PARENT_DEVICE_NAME      = "EZsalt"
@Field static final String  PARENT_DEVICE_TYPE_NAME = "EZsalt Driver"
@Field static final String 	COMM_LINK            	= "https://community.hubitat.com/"
@Field static final String 	IMAGES_LINK      		= "https://raw.githubusercontent.com/KurtSanders/HubitatPackages/b9313438404a99e92bd49322cf0a8c42d892c5c5/resources/apps/EZSalt/images/"
@Field static final String 	GITHUB_LINK          	= "https://github.com/KurtSanders/HubitatPackages/tree/master/resources/apps/EZSalt#readme"
@Field static final String 	MQTT_URL 				= "tcp://mqtt.ezsalt.xyz:1883"

library (
    base: "app",
    author: AUTHOR_NAME,
    category: "Apps",
    description: "Core functions for EZsalt Device Driver.",
    name: "EZsalt-Library",
    namespace: "kurtsanders",
    documentationLink: "https://github.com/KurtSanders/",
    version: "0.0.1",
    disclaimer: "This library is only for use with SanderSoft Apps and Drivers."
)

def help() {
    section("${getImage('instructions')} <b>${app.name} Online Documentation</b>", hideable: true, hidden: true) {
        paragraph "<a href='${GITHUB_LINK}#readme' target='_blank'><h4 style='color:DodgerBlue;'>Click this link to view Online Documentation for ${app.name}</h4></a>"
    }
}

String fmtHelpInfo(String str) {
    String info = "${PARENT_DEVICE_NAME} v${VERSION}"
    String prefLink = "<a href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 70%;'>${info}</div></a>"
    String topStyle = "style='font-size: 18px; padding: 1px 12px; border: 2px solid Crimson; border-radius: 6px;'" //SlateGray
    String topLink = "<a ${topStyle} href='${COMM_LINK}' target='_blank'>${str}<br><div style='font-size: 14px;'>${info}</div></a>"
    if (device) {   
        return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>" +
            "<div style='text-align: center; position: absolute; top: 30px; right: 60px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"
    } else {
        return "<div style='text-align: center; position: absolue; top: 0px; right: 80px; padding: 0px;'><ul class='nav'><li>${topLink}</ul></li></div>"

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