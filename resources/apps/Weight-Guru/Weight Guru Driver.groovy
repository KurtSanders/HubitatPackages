/*
 *  Weight Guru Device Driver
 *
 *  Copyright 2025 Kurt Sannders
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
#include kurtsanders.Weight-Guru-Library

import groovy.transform.Field

@Field static final String PARENT_DEVICE_NAME 	= "Weight Guru Driver"
@Field static final String VERSION 				= "0.0.1"
@Field static final String BASE_LOGIN_URL   	= "https://api.weightgurus.com"
@Field static final String PATH_LOGIN_URL   	= "/v3/account/login"
@Field static final String PATH_OPERATION_URL   = "/v3/operation"
@Field static final List   WG_ATTRIBUTES   		= ["weight","bmi","email","entryTimestamp","bodyFat","muscleMass","water","goalType","lastName","gender","goalWeight","firstName","initialWeight","height","weightUnit","expiresAt"]
@Field static final List   WG_ATTRIBUTES_INT   	= ["weight","bmi","bodyFat","muscleMass","water","goalWeight","initialWeight","height"]

metadata {
    definition (name: PARENT_DEVICE_NAME, namespace: NAMESPACE, author: "Kurt Sanders") {
        capability "Refresh"
        capability "Sensor"
        capability "Initialize"
        
        attribute "weight"			, "number"
        attribute "bmi"				, "number"
        attribute "entryTimestamp"	, "string"
        attribute "bodyFat"			, "number"
        attribute "muscleMass"		, "number"
        attribute "water"			, "number"
        attribute "goalType"		, "string"
        attribute "lastName"		, "string"
        attribute "gender"			, "string"
        attribute "goalWeight"		, "number"
        attribute "firstName"		, "string"
        attribute "initialWeight"	, "number"
        attribute "height"			, "number"
        attribute "weightUnit"		, "string"
        attribute "expiresAt"		, "string"
        attribute "email"			, "string"
    }
}

//Additional Preferences
preferences {
    // Weight Guru Account Information
    input("email"	, "string"	, title: "Email"	, description: "Weight Guru Email"	, required: true)
    input("password", "password", title: "Password"	, description: "Password"			, required: true)
    input("poll_hr"	, "number"	, title: "Set device daily polling hour", defaultValue: '8', description: "Poll Weight Guru for data.", required: true)
    
	//Logging Options
	input name: "logLevel", type: "enum", title: fmtTitle("Logging Level"),
		description: fmtDesc("Logs selected level and above"), defaultValue: 0, options: LOG_LEVELS
	input name: "logLevelTime", type: "enum", title: fmtTitle("Logging Level Time"),
		description: fmtDesc("Time to enable Debug/Trace logging"),defaultValue: 0, options: LOG_TIMES
	//Help Link
	input name: "helpInfo", type: "hidden", title: fmtHelpInfo("Community Link")
}

void initialize() {
	do_login()
}

void installed() {
    setLogLevel("Debug", "30 Minutes")
    logInfo "New Install: Inital logging level set at 'Debug' for 30 Minutes"
    device.updateSetting('poll_hr', [type: "number", value: 8])
}

def updated() {
	logInfo "Preferences Updated..."
	checkLogLevel()
    unschedule('refresh')
    logInfo "Updated daily refresh schedule: Every day at ${poll_hr} ${(poll_hr > 12?' pm':' am')}"
    schedule("0 0 ${poll_hr} * * ?", "refresh")
}

def refresh() {
    def today = new Date().format( 'yyyy-MM-dd' )
    get_weight_history(today)
}

void do_login() {
    logInfo 'Starting API authentication to Weight Guru'
    def auth_data = '{"email": "'+settings.email+'", "password": "'+settings.password+'", "web" : "true"}'
	def params = [
        uri   	: BASE_LOGIN_URL,
        path	: PATH_LOGIN_URL,
        body	: auth_data,
    ]
    logTrace "==> httpPost params= ${params}"
    try {
        httpPostJson(params) { response ->
            logDebug "==> web login status= ${response.status}"
            if (response?.status == 200) {
                logTrace "Login response is ${response?.data}"
                logInfo "Saving authentication data"
                state.expiresAt		= response.data['expiresAt']
                state.refreshToken	= response.data['refreshToken']
                state.accessToken	= response.data['accessToken']
                iterateEventMap(response.data['account'])
                scheduleTokenInitialize(response.data['expiresAt'])
            } else {
                logErr "Login failed 'rc=${response?.status}': Check any errors above and correct"
            }
        }
     } catch (Exception e) {
    	log.debug "** Exception: $e"
    }
}

def scheduleTokenInitialize(expiresAt=state.expiresAt) {
	def expiresDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", expiresAt)
    unschedule('initialze')
    String param = expiresDate.format( "'0 0 0 'dd MM '?' yyyy" )
    logTrace "==> schedule param= ${param}"
	schedule(param, "initialize")
}


def get_weight_history(start_date=null) {
    logInfo 'Starting Data Gathering Operation API to Weight Guru'
    if (start_date==null) start_date = new Date().format( 'yyyy-MM-dd' )
    else startDate = start_date
	def params = [
        uri   				: BASE_LOGIN_URL,
        path				: PATH_OPERATION_URL,
        headers				: ["authorization": "Bearer ${state.accessToken}"],
        contentType			: "application/json",
        query				: [
            "start"			: start_date
        ]
    ]
    logTrace "==> httpGet params= ${params}"
    
    try {
        httpGet(params) { response ->
            if (response.status == 200) {
                logDebug "${response.data}"
                iterateEventMap(response.data.operations)
            } else if (response.status == 401) { 
                logWarn "Unauthorized/Expired Token"                
            	do_login()
                runIn(5,'refresh')
            } else {
            	logErr "Unexpected return code from API: ${response?.status}, Exiting"
            }
        }
     } catch (Exception e) {
		logErr "** Exception: $e"
    }
}

//    def start_date = (datetime.today().strftime("%Y-%m-%d"))
/* 
def isTokenValid()
    if parse(str(datetime.now())) >= parse(self.data.get('expiresAt')).replace(tzinfo=None):
        logInfo('Refreshing access token...')
        do_login()

def __getAllEntries(self):
    # grab all your data
    req = requests.get(
        "https://api.weightgurus.com/v3/operation/",
        headers={
            "Authorization": f"Bearer {self.data.get('accessToken')}",
            "Accept": "application/json, text/plain, * /*",
        },
    )
    for entry in req.json()["operations"]:
        print(entry)
*/

def iterateEventMap(Map map) {
    logDebug "==> iterateMap(${map})"
    map.each { key, value ->
        if (value instanceof Map) {
            iterateMap(value)
        } else {
            if (value && WG_ATTRIBUTES.contains(key)) {
                value = (WG_ATTRIBUTES_INT.contains(key))?(float) value/10:value
                sendEvent(name: key, value: value)
	            logInfo "$key → $value"
            }
        }
    }
}

