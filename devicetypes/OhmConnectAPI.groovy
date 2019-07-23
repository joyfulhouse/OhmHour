/**
*  Copyright 2015 SmartThings
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
*  OhmConnect Device Type
*
*  Author: Bryan Li (based on work by markewest@gmail.com)
*
*  Date: 2018-02-19
*/

preferences {
	input name: "ohmidCode", type: "text", title: "Ohm Id", 
	description: "Configure your OhmConnect 8 digit ID", 
	required: true, displayDuringSetup: true
}

metadata {
	definition (name: "OhmConnect API", namespace: "joyfulhouse", author: "Bryan Li") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
		capability "Polling"
		capability "Refresh"
        
		command "refresh"
	}

	// simulator metadata
	simulator {

	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"status", type: "thermostat", width: 6, height: 4, canChangeIcon: false, decoration: "flat") {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'Ohm Hour', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
				attributeState "off", label: 'Stable Grid', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"polling.poll", icon:"st.secondary.refresh"
		}

		main "status"
		details(["status","refresh"])
	}
}

def installed() {
	log.debug "Installing OhmConnect Device"
	//runEvery1Minute(doRefresh)
    schedule("0 0-4,30-34,59 * * * ?", doRefresh)
}

def updated() {
	log.debug "Updated OhmConnect Device"
    //runEvery1Minute(doRefresh)
	schedule("0 0-4,30-34,59 * * * ?", doRefresh)
}

def parse(String description) {

}

def on() {
	sendEvent(name: "switch", value: "on")
}

def fanOn() {
	sendEvent(name: "switch", value: "on")
}

def off() {
	sendEvent(name: "switch", value: "off")
}

def fanOff() {
	sendEvent(name: "switch", value: "off")
}

def poll() {
	log.debug "OhmConnect Polling"

	refresh()
}

def refresh() {
	doRefresh()
}

def doRefresh() {
	def currentState = device.currentValue("switch")
	def params = [
	uri:  'https://login.ohmconnect.com/verify-ohm-hour/',
	path: ohmidCode,
	]
	try {
		httpGet(params) { resp ->
            if(resp.data.active == "True"){
                log.debug "Ohm Hour"
                if(currentState == "off"){
                    sendEvent(name: "switch", value: "on")
                }

            } else {
                log.debug "Not currently an Ohm Hour"
                if(currentState == "on"){
                    sendEvent(name: "switch", value: "off")
                }
            }
		}
	} catch (e) {
		log.error "something went wrong: $e"
	}
}