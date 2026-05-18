import os
import firebase_admin
from firebase_admin import credentials, firestore

class FirestoreWriter:
    def __init__(self):
        cred_path = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
        if not cred_path:
            print("WARNING: No GOOGLE_APPLICATION_CREDENTIALS provided. Firestore writes will be mocked.")
            self.db = None
            return
            
        try:
            if not firebase_admin._apps:
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
            self.db = firestore.client()
        except Exception as e:
            print(f"Failed to initialize Firestore: {e}. Writes will be mocked.")
            self.db = None

    def write_crisis(self, crisis_id, classification, status, allocated=None, messages=None, simulated_actions=None):
        if not self.db:
            return
            
        doc_ref = self.db.collection('crises').document(crisis_id)
        
        lat, lon = 24.8607, 67.0011 # default Karachi
        
        c_type_raw = str(classification.get("crisisType", "")).upper()
        if "FLOOD" in c_type_raw:
            mapped_type = "FLOOD"
        elif "ACCIDENT" in c_type_raw:
            mapped_type = "TRAFFIC_ACCIDENT"
        elif "HEATWAVE" in c_type_raw:
            mapped_type = "HEATWAVE"
        elif "FIRE" in c_type_raw or "COLLAPSE" in c_type_raw or "POWER" in c_type_raw or "WATER" in c_type_raw:
            mapped_type = "INFRASTRUCTURE_FAILURE"
        else:
            mapped_type = "UNKNOWN"

        sev_raw = str(classification.get("severity", "MONITORING")).upper()
        if sev_raw not in ["CRITICAL", "HIGH", "MEDIUM", "LOW", "MONITORING"]:
            sev_raw = "MONITORING"
            
        try:
            conf_val = int(float(classification.get("confidence", 0.0)) * 100)
        except:
            conf_val = 0
            
        neighborhood = classification.get("neighborhood", "Unknown")
        
        data = {
            "crisis_type": mapped_type,
            "severity": sev_raw,
            "confidence": conf_val,
            "centroid": firestore.GeoPoint(lat, lon),
            "impact_radius_km": 5.0,
            "title_en": f"{mapped_type.replace('_', ' ')} Alert in {neighborhood}",
            "title_ur": f"الرٹ: {neighborhood} میں ہنگامی صورتحال",
            "description_en": messages.get("PUBLIC", classification.get("reasoning", "")) if messages else classification.get("reasoning", ""),
            "description_ur": "",
            "assigned_agencies": list(allocated.keys()) if allocated else [],
            "missing_persons_count": 0,
            "analyst_reasoning": classification.get("reasoning", ""),
            "status": status.upper() if status else "MONITORING",
            "created_at": firestore.SERVER_TIMESTAMP,
            "updated_at": firestore.SERVER_TIMESTAMP,
            
            # Legacy fields for debugging/traceability
            "type": classification.get("crisisType"),
            "neighborhood": neighborhood,
            "affectedPopulation": classification.get("affectedPopulation"),
            "expectedDurationHours": classification.get("expectedDurationHours"),
            "spreadRisk": classification.get("spreadRisk"),
            "conflictingSignals": classification.get("conflictingSignals", False)
        }
        
        if allocated:
            data["resourcesAllocated"] = allocated
        if messages:
            data["stakeholderMessages"] = messages
        if simulated_actions:
            data["simulatedActions"] = simulated_actions
            
        try:
            doc_ref.set(data, merge=True)
        except Exception as e:
            print(f"Error writing crisis to Firestore: {e}")

    def write_agent_trace(self, trace_data):
        if not self.db:
            return
            
        trace_data["timestamp"] = firestore.SERVER_TIMESTAMP
        
        try:
            self.db.collection('agent_traces').add(trace_data)
        except Exception as e:
            print(f"Error writing trace to Firestore: {e}")

    def write_alerts(self, crisis_id, classification, messages):
        if not self.db:
            return
            
        alert_data = {
            "crisisId": crisis_id,
            "severity": classification.get("severity"),
            "type": classification.get("crisisType"),
            "title": f"{classification.get('severity')} Alert: {classification.get('crisisType')}",
            "description": messages.get("PUBLIC", ""),
            "neighborhood": classification.get("neighborhood"),
            "isRead": False,
            "timestamp": firestore.SERVER_TIMESTAMP
        }
        
        try:
            self.db.collection('alerts').add(alert_data)
        except Exception as e:
            print(f"Error writing alert to Firestore: {e}")
