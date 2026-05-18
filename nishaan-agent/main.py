import time
import schedule
import logging
import math
from datetime import datetime, timezone, timedelta
from dotenv import load_dotenv

from signals.weather_signal import fetch_weather_signal
from signals.social_signal import fetch_social_signals
from signals.traffic_signal import fetch_traffic_signals
from agent.signal_fuser import fuse_signals
from agent.crisis_classifier import classify_crisis
from agent.resource_allocator import ResourceAllocator
from agent.action_simulator import simulate_action
from agent.stakeholder_notifier import generate_messages
from firestore.writer import FirestoreWriter

logging.basicConfig(level=logging.INFO, format='%(message)s')
load_dotenv()

writer = FirestoreWriter()
allocator = ResourceAllocator()
cycle_count = 0
pk_time = timezone(timedelta(hours=5))

def log_trace(step, action, reasoning, confidence=None, crisis_id=None, input_data=None, output_data=None):
    timestamp = datetime.now(pk_time).isoformat()
    conf_str = f" (confidence: {confidence:.2f})" if confidence is not None else ""
    log_msg = f"[NISHAAN AGENT] {timestamp} [STEP {step}] {action} — {reasoning}{conf_str}"
    logging.info(log_msg)
    
    if crisis_id:
        writer.write_agent_trace({
            "crisisId": crisis_id,
            "step": step,
            "action": action,
            "reasoning": reasoning,
            "confidence": confidence,
            "inputData": input_data or {},
            "outputData": output_data or {}
        })

def haversine_distance(lat1, lon1, lat2, lon2):
    R = 6371.0 # Earth radius in km
    
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    
    a = (math.sin(dlat / 2)**2) + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * (math.sin(dlon / 2)**2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    
    return R * c

def process_unlinked_missing_persons():
    active_crises = writer.get_active_crises()
    if not active_crises:
        return
        
    unlinked_persons = writer.get_unlinked_missing_persons()
    if not unlinked_persons:
        return
        
    for person in unlinked_persons:
        loc = person.get('last_seen_location')
        if not loc:
            continue
            
        person_lat = loc.latitude
        person_lon = loc.longitude
        
        # Find closest crisis within impact radius
        closest_crisis = None
        min_distance = float('inf')
        
        for crisis in active_crises:
            c_loc = crisis.get('centroid')
            if not c_loc:
                continue
                
            c_lat = c_loc.latitude
            c_lon = c_loc.longitude
            radius = crisis.get('impact_radius_km', 5.0)
            
            dist = haversine_distance(person_lat, person_lon, c_lat, c_lon)
            if dist <= radius and dist < min_distance:
                min_distance = dist
                closest_crisis = crisis
                
        if closest_crisis:
            crisis_id = closest_crisis['id']
            person_id = person['id']
            writer.link_missing_person(person_id, crisis_id)
            
            log_trace(
                step=7,
                action="MISSING_PERSON_LINKED",
                reasoning=f"Linked missing person {person.get('person_name', 'Unknown')} to crisis {closest_crisis.get('title_en', crisis_id)} (distance: {min_distance:.2f}km).",
                crisis_id=crisis_id,
                input_data={"person_id": person_id, "distance": min_distance}
            )

def agent_loop():
    global cycle_count
    cycle_count += 1
    
    multi_crisis_injection = (cycle_count % 5 == 0)
    weather = fetch_weather_signal()
    social = fetch_social_signals(force_multi=multi_crisis_injection)
    traffic = fetch_traffic_signals()
    
    fused_contexts = fuse_signals(weather, social, traffic)
    
    for idx, context in enumerate(fused_contexts):
        crisis_id = f"crisis_{int(time.time())}_{idx}"
        
        classification = classify_crisis(context)
        if not classification.get("crisisDetected", False):
            continue
        
        weather_desc = f"weather: {weather.get('conditionCode', 'unknown')} (cred: {weather.get('credibility', 0):.2f})"
        social_desc = f"social: {len(social)} reports"
        
        log_trace(
            step=1, 
            action="SIGNAL FUSION", 
            reasoning=f"Weather: {weather_desc}, Social: {social_desc}, Traffic: data loaded",
            crisis_id=crisis_id,
            input_data={"weather": weather, "social": social, "traffic": traffic},
            output_data=context
        )
        
        log_trace(
            step=2,
            action="CRISIS CLASSIFIED",
            reasoning=f"{classification.get('crisisType')} / {classification.get('severity')} / {classification.get('neighborhood')}",
            confidence=classification.get("confidence", 0.0),
            crisis_id=crisis_id,
            input_data=context,
            output_data=classification
        )
        
        false_alarm_prob = classification.get("falseAlarmProbability", 0.0)
        conflicting = classification.get("conflictingSignals", False)
        
        if false_alarm_prob > 0.6 or conflicting:
            log_trace(
                step=2.5,
                action="FALSE ALARM VERIFICATION",
                reasoning=f"High false alarm prob ({false_alarm_prob}) or conflicting signals. Dispatching field inspector.",
                crisis_id=crisis_id
            )
            import random
            if random.random() < false_alarm_prob:
                log_trace(
                    step=3,
                    action="ALERT RETRACTED",
                    reasoning="Signal was confirmed as false alarm by field verification.",
                    crisis_id=crisis_id
                )
                writer.write_crisis(crisis_id, classification, "FALSE_ALARM")
                continue
            else:
                log_trace(
                    step=3,
                    action="SIGNAL VERIFIED",
                    reasoning="Signal verified by field inspector — escalating",
                    crisis_id=crisis_id
                )
        
        if classification.get("confidence", 0) > 0.4:
            alloc_result = allocator.allocate(crisis_id, classification)
            allocated = alloc_result["allocated"]
            tradeoffs = alloc_result["tradeoff_reasoning"]
            
            res_str = ", ".join([f"{count} {res}" for res, count in allocated.items()])
            reasoning_str = f"Allocated: {res_str}. Trade-offs: {tradeoffs}" if tradeoffs else f"Allocated: {res_str}."
            
            log_trace(
                step=4,
                action="RESOURCES ALLOCATED",
                reasoning=reasoning_str,
                crisis_id=crisis_id,
                input_data=classification,
                output_data=alloc_result
            )
            
            simulated_actions = []
            for res_type, count in allocated.items():
                sim_act = simulate_action(res_type, count, classification)
                simulated_actions.append(sim_act)
            
            actions_summary = " ".join([s["action"] for s in simulated_actions])
            log_trace(
                step=5,
                action="ACTIONS SIMULATED",
                reasoning=actions_summary,
                crisis_id=crisis_id,
                output_data={"simulated_actions": simulated_actions}
            )
            
            messages = generate_messages(classification, allocated, simulated_actions)
            
            writer.write_crisis(crisis_id, classification, "ACTIVE", allocated, messages, simulated_actions)
            writer.write_alerts(crisis_id, classification, messages)
            
            log_trace(
                step=6,
                action="FIRESTORE UPDATED",
                reasoning=f"crises/{crisis_id} written. Alerts generated.",
                crisis_id=crisis_id
            )
            
    # After processing all signals and new crises, link unlinked missing persons to active crises
    process_unlinked_missing_persons()

if __name__ == "__main__":
    print("Starting NISHAAN Autonomous Agent Loop...")
    agent_loop()
    schedule.every(60).seconds.do(agent_loop)
    while True:
        schedule.run_pending()
        time.sleep(1)
