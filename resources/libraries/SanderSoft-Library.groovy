/*******************************************************************
*** SanderSoft - Core App/Device Helpers                        ***
/*******************************************************************/

library (
    base: "app",
    author: "Kurt Sanders",
    category: "Apps",
    description: "Core functions for SanderSoft applications.",
    name: "SanderSoft-Library",
    namespace: "kurtsanders",
    documentationLink: "https://github.com/KurtSanders/",
    version: "0.0.7",
    disclaimer: "This core library is only for use with SanderSoft Apps and Drivers."
)

import groovy.json.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import groovy.transform.Field
import hubitat.helper.RMUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import groovyx.net.http.HttpResponseException

@Field static final String GITHUB_IMAGES_LINK      = "https://raw.githubusercontent.com/kurtsanders/HubitatPackages/master/resources/images/"

def uninstalled() {
    unschedule()
    removeChildDevices(getChildDevices())
}

def returnVar(var) {
    def dataType = "String"
    def returnValue
    if (!(settings."${var}" == null)) { returnValue = settings."${var}" }
    if (!(state."${var}" == null)) { returnValue = state."${var}" }
	if (!(atomicState."${var}" == null)) { returnValue = atomicState."${var}" }
    def dateTest = returnValue =~ /\d\d\d\d-\d\d-\d\dT\d\d:/
    if (dateTest) { dataType = "Date" }
    if (returnValue == "true") { dataType = "Boolean" }
    if (returnValue == "false") { dataType = "Boolean" }
    if (returnValue == true) { dataType = "Boolean" }
    if (returnValue == false) { dataType = "Boolean" }
    if (dataType == "Date") {returnValue = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", returnValue)}
    logDebug ("returnVar(${var}), DataType:${dataType}, Value: ${returnValue}")
    if (returnValue == null || returnValue == "") {}
    return returnValue
}

private removeChildDevices(delete) {
    delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def displayPaypal() {
    section() {
        paragraph("<a href='https://www.paypal.com/donate/?hosted_button_id=E4WXT86RTPXDC'>Please consider making a small donation to support the developers application via PayPal™.</a><br>" +
                  "<small><i>Copyright \u00a9 2012-${currentYear} SandersSoft™ - All rights reserved.</i></small><br>")
    }
}

public makeWebLink (link, label, hoverTitle="Click Me") {
    // Check if a local path on the hub
    if (link.startsWith('/')) {
        link = "http://${location.hub.localIP}${link}"
    }
    hoverTitle="title='${hoverTitle}'"
    def boxGraphic = "<a href=${link} target='_blank' ${hoverTitle} > ${BOX_ARROW} </a>"
    def line  = "<span><a target='_blank' ${hoverTitle} rel='noopener noreferrer' href=${link}><strong>${label}</strong></a>${boxGraphic}</span>"
    return line
}

public String convertToCurrentTimeZone(String dateStr) {

    TimeZone utc = TimeZone.getTimeZone("UTC");
    SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    SimpleDateFormat destFormat = new SimpleDateFormat('EEE MMM d, h:mm a');
    sourceFormat.setTimeZone(utc);
    Date convertedDate = sourceFormat.parse(dateStr);
    return destFormat.format(convertedDate);

}
//get the current time zone

public String getCurrentTimeZone(){
    TimeZone tz = Calendar.getInstance().getTimeZone();
    return tz.getID();
}

String nowFormatted(dtFormat=null) {
    dtFormat = dtFormat?dtFormat:'yyyy-MMM-dd h:mm:ss a'
    if(location.timeZone) return new Date().format(dtFormat, location.timeZone)
    else                  return new Date().format(dtFormat)
}

String fmtTitle(String str) {
    return "<strong>${str}</strong>"
}

String fmtDesc(String str) {
    return "<div style='font-size: 85%; font-style: italic; padding: 1px 0px 4px 2px;'>${str}</div>"
}

def getImage(type) {
    def loc = "<img src=" + GITHUB_IMAGES_LINK
    if(type == "Blank")          return "${loc}blank.png height=40 width=5}>"
    if(type == "checkMarkGreen") return "${loc}checkMarkGreen2.png height=20 width=20>"
    if(type == "optionsGreen")   return "${loc}options-green.png height=30 width=30>"
    if(type == "optionsRed")     return "${loc}options-red.png height=30 width=30>"
    if(type == "instructions")   return "${loc}instructions.png height=30 width=30>"
    if(type == "logo")           return "${loc}logo.png height=40>"
    if(type == "qmark")          return "${loc}question-mark-icon.png height=16>"
    if(type == "qmark2")         return "${loc}question-mark-icon-2.jpg height=16>"
    if(type == "button-red")     return "${loc}/button-red.png height=30 width=30>"
    if(type == "qmark")          return "${loc}question-mark-icon.png height=16>"
    if(type == "qmark2")         return "${loc}question-mark-icon-2.jpg height=16>"
}

def getFormat(type, myText="") {
    if(type == "line")        return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title")       return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
    if (type.contains('-')) {
        switch (type.split('-')[0]) {
            case "button":
            return "<a style='color:white;text-align:center;font-size:20px;font-weight:bold;background-color:${type.split('-')[1]};border:1px solid #000000;box-shadow:3px 4px #8B8F8F;border-radius:10px' href='${page}'>${myText}</a>"
            break
            case "text":
            return "<span style=color:${type.split('-')[1]}>${myText}</span>"
            break
            case "header":
            return "<div style='color:#ffffff;font-weight: bold;background-color:${type.split('-')[1]};border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
            default:
                def error = "getFormat() contained a '-' but invalid/missing type"
                log.error error
            return error
            break
        }
    }
}

def sectionHeader(title){
    return getFormat("header-blue", "${getImage("Blank")}"+" ${title}")
}

public static String formatSeconds(int timeInSeconds)
{
    int hours = timeInSeconds / 3600;
    int secondsLeft = timeInSeconds - hours * 3600;
    int minutes = secondsLeft / 60;
    int seconds = secondsLeft - minutes * 60;

    String formattedTime = "";
    if (hours < 10)
        formattedTime += "0";
    formattedTime += hours + ":";

    if (minutes < 10)
        formattedTime += "0";
    formattedTime += minutes + ":";

    if (seconds < 10)
        formattedTime += "0";
    formattedTime += seconds ;

    return formattedTime;
}

//Logging Level Options
@Field static final Map LOG_LEVELS = [0:"Off", 1:"Error", 2:"Warn", 3:"Info", 4:"Debug", 5:"Trace"]
@Field static final Map LOG_TIMES  = [0:"Indefinitely", 1:"1 Minute", 5:"5 Minutes", 10:"10 Minutes", 15:"15 Minutes", 30:"30 Minutes", 60:"1 Hour", 120:"2 Hours", 180:"3 Hours", 360:"6 Hours", 720:"12 Hours", 1440:"24 Hours"]
@Field static final String LOG_DEFAULT_LEVEL = 0

//Call this function from within updated() and configure() with no parameters: checkLogLevel()
void checkLogLevel(Map levelInfo = [level:null, time:null]) {
    //Set Defaults
    unschedule(logsOff)
    if (app) {
        if (settings.logLevel == null) app.updateSetting("logLevel",[value:LOG_DEFAULT_LEVEL, type:"enum"])
        if (settings.logLevelTime == null) app.updateSetting("logLevelTime",[value:"0", type:"enum"])
    } else {
        if (settings.logLevel == null) device.updateSetting("logLevel",[value:LOG_DEFAULT_LEVEL, type:"enum"])
        if (settings.logLevelTime == null) device.updateSetting("logLevelTime",[value:"0", type:"enum"])
    }
     //Schedule turn off and log as needed
    if (levelInfo.level == null) levelInfo = getLogLevelInfo()
    String logMsg = "Logging Level is: ${LOG_LEVELS[levelInfo.level]}"
    if (levelInfo.level >= 1 && levelInfo.time > 0) {
        logMsg += " for ${LOG_TIMES[levelInfo.time]}"
        logInfo "A 'logsOff' cron job has been scheduled in ${formatSeconds(60*levelInfo.time)}"
        runIn(60*levelInfo.time, logsOff, [overwrite: true])
    }
    if (levelInfo.time == 0) logMsg += " (${LOG_TIMES[levelInfo.time]})"
    logInfo "${logMsg}"
}

void syncLogLevel(level, time) {
    logTrace "syncLogLevel(${level}, ${time})"
    logTrace "0) UpdateSetting ${device.displayName} logLevel:${level} and logLevelTime:${time}"
    device.updateSetting("logLevel",[value:"${level}", type:"enum"])
    device.updateSetting("logLevelTime",[value:"${time}", type:"enum"])
    checkLogLevel()
    getChildDevices().eachWithIndex { childDev, index ->
        logTrace "${index+1}) UpdateSetting ${childDev.displayName} logLevel:${level} and logLevelTime:${time}"
        childDev.updateSetting("logLevel",[value:"${level}", type:"enum"])
        childDev.updateSetting("logLevelTime",[value:"${time}", type:"enum"])
        logTrace "${index+1}) Calling ${childDev.displayName}.checkLogLevel()"
        childDev.checkLogLevel()
    }
}

//Function for optional command
void setLogLevel(String levelName, String timeName=null) {
    Integer level = LOG_LEVELS.find{ levelName.equalsIgnoreCase(it.value) }.key
    Integer time = LOG_TIMES.find{ timeName.equalsIgnoreCase(it.value) }.key
    if (app) {
        app.updateSetting("logLevel",[value:"${level}", type:"enum"])
        app.updateSetting("logLevelTime",[value:"${time}", type:"enum"])
    } else {
        device.updateSetting("logLevel",[value:"${level}", type:"enum"])
        device.updateSetting("logLevelTime",[value:"${time}", type:"enum"])
    }
    checkLogLevel(level: level, time: time)
}

Map getLogLevelInfo() {
    Integer level = settings.logLevel as Integer ?: 0
    Integer time = settings.logLevelTime as Integer ?: 0
    return [level: level, time: time]
}

void logsOff() {
    logInfo "Logging auto disabled"
    setLogLevel("Off","Indefinitely")
}
