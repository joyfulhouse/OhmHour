/*
 * OhmConnect Driver
 *  
 *  Copyright 2021 Bryan Li
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
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2021-08-25  Bryan Li       Initial Release
 * 
 */

metadata {
    definition (name: "OhmConnect", namespace: "joyfulhouse", author: "Bryan Li", importUrl: "https://github.com/joyfulhouse/OhmHour/blob/master/devicetypes/OhmConnect.groovy") {
        capability "Switch"
        capability "Polling"
        
        attribute "start_dttm", "date"
        attribute "end_dttm", "date"
        attribute "scheduled", "bool"
        
        command "refresh"
	}
    
	preferences {
        input name: "cookie", type: "string", title: "Cookie", defaultValue: false, required: true
        input name: "debug", type: "bool", title: "Debug", defaultValue: true, required: true
    }
}

def on(){
    sendEvent(name: "switch", value: "on")
}

def off(){
    // Update switch state
    sendEvent(name: "switch", value: "off")
    
    // Reset the scheduling data
    sendEvent(name: "start_dttm", value: null)
    sendEvent(name: "end_dttm", value: null)
    sendEvent(name: "scheduled", value: false)
    
    // Reuse updated method to unschedule and reschedule the refresh
    updated()
}

def installed(){
	initialize()
}

def initialize(){
	off()
    updated()
}

def updated(){
    unschedule()
    schedule('0 0 0 ? * *', doRefresh)
}

def poll(){
    doRefresh()   
}

def refresh(){
    doRefresh()
}

def doRefresh() {
	def currentState = device.currentValue("switch")
    
    def headers = [:] 
    headers.put("cookie", cookie)
    
	def params = [
	  uri:  'https://login.ohmconnect.com/api/v2/',
	  path: 'upcoming_events',
      headers: headers
	]
    
	try {
		httpGet(params) { resp ->
            // Set Schedule
            if(resp.data[0]){
                setSchedule(resp.data[0].start_dttm, resp.data[0].end_dttm)
            }
        }
	} catch (e) {
		log.error "something went wrong: $e"
	}
}

def setSchedule(start_dttm, end_dttm){
    try {
        Date startDate = toDateTime(start_dttm)
        Date endDate = toDateTime(end_dttm)

        if(startDate && endDate && !scheduled){
            if(debug)
                log.debug "Next OhmHour: " + startDate.toString() + " to " + endDate.toString()

            sendEvent(name: "start_dttm", value: startDate)
            sendEvent(name: "end_dttm", value: endDate)
            sendEvent(name: "scheduled", value: true)
            
            schedule(startDate, on)
            schedule(endDate, off)
        }
    } catch (Exception e) {
        log.error e.getMessage()
    }
}
