// NISHAAN Firestore Seed Data
// Run this in the Firebase Console > Firestore > Import, or use the Firebase Admin SDK
//
// HOW TO USE:
// 1. Go to Firebase Console > Firestore
// 2. Create each collection manually and add these documents
// 3. OR use the Node.js script below to seed automatically
//
// ============================================================
// To seed via Node.js, run:
//   node seed_firestore.js
// (Requires: npm install firebase-admin)
// ============================================================

const admin = require('firebase-admin');

// Initialize with your service account key
// Download from: Firebase Console > Project Settings > Service Accounts > Generate New Private Key
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const { GeoPoint, Timestamp } = admin.firestore;

async function seed() {
  console.log('🌱 Seeding NISHAAN Firestore...');

  // ── CRISES ──────────────────────────────────────────────────
  const crises = [
    {
      crisis_type: 'FLOOD',
      severity: 'CRITICAL',
      confidence: 91,
      status: 'CONFIRMED',
      centroid: new GeoPoint(24.8607, 67.0011), // Karachi - Gulshan
      impact_radius_km: 5.0,
      title_en: 'Flash Flood — Gulshan-e-Iqbal',
      title_ur: 'اچانک سیلاب — گلشن اقبال',
      description_en: 'Heavy rainfall has caused severe urban flooding in Gulshan-e-Iqbal area. Multiple streets submerged, vehicles stranded. Water level rising rapidly.',
      description_ur: 'شدید بارش نے گلشن اقبال علاقے میں شدید شہری سیلاب کا باعث بنا ہے۔ متعدد سڑکیں زیر آب، گاڑیاں پھنسی ہوئی ہیں۔',
      signal_ids: ['sig_001', 'sig_002', 'sig_003', 'sig_004'],
      assigned_agencies: ['rescue_1122', 'ndma', 'pakistan_army'],
      missing_persons_count: 3,
      analyst_reasoning: 'Cross-referenced 4 independent signals: 2 Twitter reports (Urdu), 1 PMD severe weather warning, 1 eyewitness report. Geographic clustering within 2km radius. High confidence classification.',
      created_at: Timestamp.now(),
      updated_at: Timestamp.now()
    },
    {
      crisis_type: 'HEATWAVE',
      severity: 'HIGH',
      confidence: 78,
      status: 'ACTIVE',
      centroid: new GeoPoint(25.3960, 68.3578), // Hyderabad
      impact_radius_km: 15.0,
      title_en: 'Extreme Heatwave — Hyderabad District',
      title_ur: 'شدید گرمی کی لہر — حیدرآباد ضلع',
      description_en: 'Temperature exceeding 48°C for 3rd consecutive day. Multiple heatstroke cases reported at hospitals. PMD advisory issued.',
      description_ur: 'مسلسل تیسرے دن درجہ حرارت 48 ڈگری سے تجاوز کر گیا۔ ہسپتالوں میں ہیٹ اسٹروک کے متعدد کیسز رپورٹ۔',
      signal_ids: ['sig_005', 'sig_006', 'sig_007'],
      assigned_agencies: ['health_department', 'ndma'],
      missing_persons_count: 1,
      analyst_reasoning: 'PMD official warning confirmed. 3 signals from health facilities and social media corroborate. High confidence due to official source.',
      created_at: Timestamp.fromDate(new Date(Date.now() - 3600000)),
      updated_at: Timestamp.now()
    },
    {
      crisis_type: 'TRAFFIC_ACCIDENT',
      severity: 'MEDIUM',
      confidence: 65,
      status: 'ACTIVE',
      centroid: new GeoPoint(33.6844, 73.0479), // Islamabad
      impact_radius_km: 1.0,
      title_en: 'Multi-Vehicle Collision — Islamabad Expressway',
      title_ur: 'کئی گاڑیوں کا تصادم — اسلام آباد ایکسپریس وے',
      description_en: 'Major collision involving 5+ vehicles on Islamabad Expressway near Faizabad. Traffic completely blocked. Emergency services dispatched.',
      description_ur: 'فیض آباد کے قریب اسلام آباد ایکسپریس وے پر 5 سے زائد گاڑیوں کا بڑا تصادم۔',
      signal_ids: ['sig_008', 'sig_009'],
      assigned_agencies: ['rescue_1122', 'police', 'edhi_foundation'],
      missing_persons_count: 0,
      analyst_reasoning: 'Google Traffic API detected anomalous stoppage. 2 social media reports confirm multi-vehicle accident. Medium confidence — awaiting official source.',
      created_at: Timestamp.fromDate(new Date(Date.now() - 1800000)),
      updated_at: Timestamp.now()
    },
    {
      crisis_type: 'EARTHQUAKE',
      severity: 'MONITORING',
      confidence: 42,
      status: 'MONITORING',
      centroid: new GeoPoint(35.9200, 74.3100), // Gilgit
      impact_radius_km: 30.0,
      title_en: 'Seismic Activity — Gilgit-Baltistan',
      title_ur: 'زلزلے کی سرگرمی — گلگت بلتستان',
      description_en: 'Minor tremors reported in Gilgit region. Magnitude estimated 3.2. Monitoring for aftershocks.',
      description_ur: 'گلگت علاقے میں معمولی زلزلے کے جھٹکے محسوس کیے گئے۔ شدت 3.2 تخمینہ۔',
      signal_ids: ['sig_010'],
      assigned_agencies: [],
      missing_persons_count: 0,
      analyst_reasoning: 'Single signal from NDMA seismic feed. Low magnitude, monitoring status. Will escalate if aftershocks detected.',
      created_at: Timestamp.fromDate(new Date(Date.now() - 7200000)),
      updated_at: Timestamp.now()
    }
  ];

  for (let i = 0; i < crises.length; i++) {
    const ref = await db.collection('crises').add(crises[i]);
    console.log(`  ✅ Crisis: ${crises[i].title_en} (${ref.id})`);

    // Add agent traces for this crisis
    const traces = generateTraces(ref.id, crises[i]);
    for (const trace of traces) {
      await db.collection('agent_traces').add(trace);
    }
    console.log(`  ✅ Added ${traces.length} agent traces for crisis`);
  }

  console.log('\n🎉 Seeding complete!');
  process.exit(0);
}

function generateTraces(crisisId, crisis) {
  const baseTime = crisis.created_at.toDate().getTime();
  return [
    {
      agent_name: 'SENTINEL',
      crisis_id: crisisId,
      action: 'SIGNALS_DETECTED',
      reasoning_summary: `Detected ${crisis.signal_ids.length} signals from multiple sources indicating potential ${crisis.crisis_type.toLowerCase()} event.`,
      confidence: null,
      metadata: { signals_processed: crisis.signal_ids.length },
      timestamp: Timestamp.fromDate(new Date(baseTime - 120000))
    },
    {
      agent_name: 'ANALYST',
      crisis_id: crisisId,
      action: 'CRISIS_CLASSIFIED',
      reasoning_summary: crisis.analyst_reasoning,
      confidence: crisis.confidence,
      metadata: { crisis_type: crisis.crisis_type, severity: crisis.severity },
      timestamp: Timestamp.fromDate(new Date(baseTime - 60000))
    },
    {
      agent_name: 'COMMANDER',
      crisis_id: crisisId,
      action: 'AGENCIES_DISPATCHED',
      reasoning_summary: `Dispatched ${crisis.assigned_agencies.join(', ')} based on ${crisis.crisis_type} protocol. ${crisis.confidence >= 70 ? 'FCM alerts sent to citizens within 3km.' : 'Confidence below 70 — citizens not yet alerted.'}`,
      confidence: crisis.confidence,
      metadata: { agencies_notified: crisis.assigned_agencies },
      timestamp: Timestamp.fromDate(new Date(baseTime))
    }
  ];
}

seed().catch(console.error);
