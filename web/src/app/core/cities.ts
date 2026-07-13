/**
 * Bundled place database for the birth-place picker.
 *
 * Typical users don't know latitude/longitude or IANA timezone names, so the
 * form lets them pick a city and we fill in coordinates + timezone. Coverage:
 * every Indian state/UT capital, all million-plus cities, the major district
 * towns of the Hindi belt, and the overseas cities where the Indian diaspora
 * is concentrated. City-level coordinates are plenty for Moon-chart accuracy.
 *
 * Each city carries a Hindi name so search works in both scripts.
 */

export interface City {
  name: string;      // English name
  hi: string;        // Hindi (Devanagari) name
  region: string;    // State (India) or country, English
  regionHi: string;  // Same, Hindi
  lat: number;
  lon: number;
  tz: string;        // IANA timezone
  search: string;    // Pre-normalized haystack: name + aliases + hi + region
}

const STATE_HI: Record<string, string> = {
  'Andhra Pradesh': 'आंध्र प्रदेश', 'Arunachal Pradesh': 'अरुणाचल प्रदेश', 'Assam': 'असम',
  'Bihar': 'बिहार', 'Chhattisgarh': 'छत्तीसगढ़', 'Goa': 'गोवा', 'Gujarat': 'गुजरात',
  'Haryana': 'हरियाणा', 'Himachal Pradesh': 'हिमाचल प्रदेश', 'Jharkhand': 'झारखंड',
  'Karnataka': 'कर्नाटक', 'Kerala': 'केरल', 'Madhya Pradesh': 'मध्य प्रदेश',
  'Maharashtra': 'महाराष्ट्र', 'Manipur': 'मणिपुर', 'Meghalaya': 'मेघालय',
  'Mizoram': 'मिज़ोरम', 'Nagaland': 'नागालैंड', 'Odisha': 'ओडिशा', 'Punjab': 'पंजाब',
  'Rajasthan': 'राजस्थान', 'Sikkim': 'सिक्किम', 'Tamil Nadu': 'तमिलनाडु',
  'Telangana': 'तेलंगाना', 'Tripura': 'त्रिपुरा', 'Uttar Pradesh': 'उत्तर प्रदेश',
  'Uttarakhand': 'उत्तराखंड', 'West Bengal': 'पश्चिम बंगाल', 'Delhi': 'दिल्ली',
  'Jammu & Kashmir': 'जम्मू और कश्मीर', 'Ladakh': 'लद्दाख', 'Chandigarh': 'चंडीगढ़',
  'Puducherry': 'पुदुच्चेरी', 'Andaman & Nicobar': 'अंडमान और निकोबार',
};

/** [name, hindi, state, lat, lon, aliases?] — timezone is always Asia/Kolkata. */
type IndiaRow = [string, string, string, number, number, string?];

const INDIA: IndiaRow[] = [
  // — Metros & Delhi NCR —
  ['Delhi', 'दिल्ली', 'Delhi', 28.6139, 77.2090, 'new delhi नई दिल्ली'],
  ['Mumbai', 'मुंबई', 'Maharashtra', 19.0760, 72.8777, 'bombay बम्बई'],
  ['Kolkata', 'कोलकाता', 'West Bengal', 22.5726, 88.3639, 'calcutta कलकत्ता'],
  ['Chennai', 'चेन्नई', 'Tamil Nadu', 13.0827, 80.2707, 'madras मद्रास'],
  ['Bengaluru', 'बेंगलुरु', 'Karnataka', 12.9716, 77.5946, 'bangalore बैंगलोर'],
  ['Hyderabad', 'हैदराबाद', 'Telangana', 17.3850, 78.4867],
  ['Gurugram', 'गुरुग्राम', 'Haryana', 28.4595, 77.0266, 'gurgaon गुड़गांव'],
  ['Noida', 'नोएडा', 'Uttar Pradesh', 28.5355, 77.3910],
  ['Ghaziabad', 'गाज़ियाबाद', 'Uttar Pradesh', 28.6692, 77.4538],
  ['Faridabad', 'फ़रीदाबाद', 'Haryana', 28.4089, 77.3178],

  // — Uttar Pradesh —
  ['Lucknow', 'लखनऊ', 'Uttar Pradesh', 26.8467, 80.9462],
  ['Kanpur', 'कानपुर', 'Uttar Pradesh', 26.4499, 80.3319],
  ['Varanasi', 'वाराणसी', 'Uttar Pradesh', 25.3176, 82.9739, 'banaras बनारस kashi काशी'],
  ['Prayagraj', 'प्रयागराज', 'Uttar Pradesh', 25.4358, 81.8463, 'allahabad इलाहाबाद'],
  ['Agra', 'आगरा', 'Uttar Pradesh', 27.1767, 78.0081],
  ['Meerut', 'मेरठ', 'Uttar Pradesh', 28.9845, 77.7064],
  ['Bareilly', 'बरेली', 'Uttar Pradesh', 28.3670, 79.4304],
  ['Aligarh', 'अलीगढ़', 'Uttar Pradesh', 27.8974, 78.0880],
  ['Moradabad', 'मुरादाबाद', 'Uttar Pradesh', 28.8386, 78.7733],
  ['Saharanpur', 'सहारनपुर', 'Uttar Pradesh', 29.9680, 77.5552],
  ['Gorakhpur', 'गोरखपुर', 'Uttar Pradesh', 26.7606, 83.3732],
  ['Firozabad', 'फ़िरोज़ाबाद', 'Uttar Pradesh', 27.1592, 78.3957],
  ['Jhansi', 'झाँसी', 'Uttar Pradesh', 25.4484, 78.5685],
  ['Muzaffarnagar', 'मुज़फ़्फ़रनगर', 'Uttar Pradesh', 29.4727, 77.7085],
  ['Mathura', 'मथुरा', 'Uttar Pradesh', 27.4924, 77.6737],
  ['Ayodhya', 'अयोध्या', 'Uttar Pradesh', 26.7922, 82.1998, 'faizabad फ़ैज़ाबाद'],
  ['Shahjahanpur', 'शाहजहाँपुर', 'Uttar Pradesh', 27.8815, 79.9099],
  ['Mirzapur', 'मिर्ज़ापुर', 'Uttar Pradesh', 25.1460, 82.5690],
  ['Bulandshahr', 'बुलंदशहर', 'Uttar Pradesh', 28.4069, 77.8498],
  ['Azamgarh', 'आज़मगढ़', 'Uttar Pradesh', 26.0685, 83.1836],
  ['Ballia', 'बलिया', 'Uttar Pradesh', 25.7585, 84.1497],
  ['Deoria', 'देवरिया', 'Uttar Pradesh', 26.5024, 83.7791],
  ['Basti', 'बस्ती', 'Uttar Pradesh', 26.7945, 82.7159],
  ['Sultanpur', 'सुल्तानपुर', 'Uttar Pradesh', 26.2647, 82.0727],
  ['Rae Bareli', 'रायबरेली', 'Uttar Pradesh', 26.2345, 81.2409],
  ['Sitapur', 'सीतापुर', 'Uttar Pradesh', 27.5684, 80.6829],
  ['Hardoi', 'हरदोई', 'Uttar Pradesh', 27.3963, 80.1255],
  ['Etawah', 'इटावा', 'Uttar Pradesh', 26.7855, 79.0150],
  ['Unnao', 'उन्नाव', 'Uttar Pradesh', 26.5471, 80.4878],
  ['Jaunpur', 'जौनपुर', 'Uttar Pradesh', 25.7541, 82.6829],
  ['Gonda', 'गोंडा', 'Uttar Pradesh', 27.1340, 81.9619],

  // — Bihar —
  ['Patna', 'पटना', 'Bihar', 25.5941, 85.1376],
  ['Gaya', 'गया', 'Bihar', 24.7914, 85.0002],
  ['Bhagalpur', 'भागलपुर', 'Bihar', 25.2425, 86.9842],
  ['Muzaffarpur', 'मुज़फ़्फ़रपुर', 'Bihar', 26.1225, 85.3906],
  ['Darbhanga', 'दरभंगा', 'Bihar', 26.1542, 85.8918],
  ['Purnia', 'पूर्णिया', 'Bihar', 25.7771, 87.4753],
  ['Arrah', 'आरा', 'Bihar', 25.5541, 84.6603, 'ara'],
  ['Begusarai', 'बेगूसराय', 'Bihar', 25.4182, 86.1272],
  ['Katihar', 'कटिहार', 'Bihar', 25.5541, 87.5591],
  ['Munger', 'मुंगेर', 'Bihar', 25.3746, 86.4735, 'monghyr'],
  ['Chhapra', 'छपरा', 'Bihar', 25.7815, 84.7477, 'chapra'],
  ['Hajipur', 'हाजीपुर', 'Bihar', 25.6928, 85.2084],
  ['Samastipur', 'समस्तीपुर', 'Bihar', 25.8629, 85.7811],
  ['Sasaram', 'सासाराम', 'Bihar', 24.9538, 84.0288],
  ['Motihari', 'मोतिहारी', 'Bihar', 26.6486, 84.9166],
  ['Siwan', 'सिवान', 'Bihar', 26.2196, 84.3592],
  ['Bettiah', 'बेतिया', 'Bihar', 26.8022, 84.5028],
  ['Kishanganj', 'किशनगंज', 'Bihar', 26.1027, 87.9349],
  ['Buxar', 'बक्सर', 'Bihar', 25.5647, 83.9777],
  ['Bihar Sharif', 'बिहारशरीफ़', 'Bihar', 25.1982, 85.5217, 'nalanda नालंदा'],
  ['Jehanabad', 'जहानाबाद', 'Bihar', 25.2133, 84.9871],
  ['Aurangabad (Bihar)', 'औरंगाबाद (बिहार)', 'Bihar', 24.7521, 84.3742],
  ['Gopalganj', 'गोपालगंज', 'Bihar', 26.4685, 84.4432],
  ['Saharsa', 'सहरसा', 'Bihar', 25.8774, 86.5928],
  ['Madhubani', 'मधुबनी', 'Bihar', 26.3487, 86.0713],
  ['Nawada', 'नवादा', 'Bihar', 24.8827, 85.5389],
  ['Sitamarhi', 'सीतामढ़ी', 'Bihar', 26.5951, 85.4808],
  ['Araria', 'अररिया', 'Bihar', 26.1362, 87.4617],
  ['Jamui', 'जमुई', 'Bihar', 24.9247, 86.2246],
  ['Khagaria', 'खगड़िया', 'Bihar', 25.5022, 86.4671],

  // — Jharkhand —
  ['Ranchi', 'राँची', 'Jharkhand', 23.3441, 85.3096],
  ['Jamshedpur', 'जमशेदपुर', 'Jharkhand', 22.8046, 86.2029, 'tatanagar टाटानगर'],
  ['Dhanbad', 'धनबाद', 'Jharkhand', 23.7957, 86.4304],
  ['Bokaro', 'बोकारो', 'Jharkhand', 23.6693, 86.1511],
  ['Deoghar', 'देवघर', 'Jharkhand', 24.4823, 86.6961],
  ['Hazaribagh', 'हज़ारीबाग़', 'Jharkhand', 23.9925, 85.3637],
  ['Giridih', 'गिरिडीह', 'Jharkhand', 24.1913, 86.3096],
  ['Dumka', 'दुमका', 'Jharkhand', 24.2685, 87.2468],

  // — Madhya Pradesh & Chhattisgarh —
  ['Bhopal', 'भोपाल', 'Madhya Pradesh', 23.2599, 77.4126],
  ['Indore', 'इंदौर', 'Madhya Pradesh', 22.7196, 75.8577],
  ['Jabalpur', 'जबलपुर', 'Madhya Pradesh', 23.1815, 79.9864],
  ['Gwalior', 'ग्वालियर', 'Madhya Pradesh', 26.2183, 78.1828],
  ['Ujjain', 'उज्जैन', 'Madhya Pradesh', 23.1765, 75.7885],
  ['Sagar', 'सागर', 'Madhya Pradesh', 23.8388, 78.7378],
  ['Dewas', 'देवास', 'Madhya Pradesh', 22.9676, 76.0534],
  ['Satna', 'सतना', 'Madhya Pradesh', 24.6005, 80.8322],
  ['Ratlam', 'रतलाम', 'Madhya Pradesh', 23.3315, 75.0367],
  ['Rewa', 'रीवा', 'Madhya Pradesh', 24.5362, 81.3037],
  ['Chhindwara', 'छिंदवाड़ा', 'Madhya Pradesh', 22.0574, 78.9382],
  ['Raipur', 'रायपुर', 'Chhattisgarh', 21.2514, 81.6296],
  ['Bhilai', 'भिलाई', 'Chhattisgarh', 21.1938, 81.3509],
  ['Bilaspur', 'बिलासपुर', 'Chhattisgarh', 22.0797, 82.1409],
  ['Korba', 'कोरबा', 'Chhattisgarh', 22.3595, 82.7501],
  ['Durg', 'दुर्ग', 'Chhattisgarh', 21.1904, 81.2849],

  // — Rajasthan —
  ['Jaipur', 'जयपुर', 'Rajasthan', 26.9124, 75.7873],
  ['Jodhpur', 'जोधपुर', 'Rajasthan', 26.2389, 73.0243],
  ['Kota', 'कोटा', 'Rajasthan', 25.2138, 75.8648],
  ['Udaipur', 'उदयपुर', 'Rajasthan', 24.5854, 73.7125],
  ['Ajmer', 'अजमेर', 'Rajasthan', 26.4499, 74.6399],
  ['Bikaner', 'बीकानेर', 'Rajasthan', 28.0229, 73.3119],
  ['Bhilwara', 'भीलवाड़ा', 'Rajasthan', 25.3407, 74.6313],
  ['Alwar', 'अलवर', 'Rajasthan', 27.5530, 76.6346],
  ['Sikar', 'सीकर', 'Rajasthan', 27.6094, 75.1399],
  ['Pali', 'पाली', 'Rajasthan', 25.7711, 73.3234],
  ['Sri Ganganagar', 'श्रीगंगानगर', 'Rajasthan', 29.9094, 73.8801],
  ['Bharatpur', 'भरतपुर', 'Rajasthan', 27.2152, 77.5030],

  // — Haryana, Punjab, Himachal, Uttarakhand, J&K, Ladakh —
  ['Rohtak', 'रोहतक', 'Haryana', 28.8955, 76.6066],
  ['Panipat', 'पानीपत', 'Haryana', 29.3909, 76.9635],
  ['Karnal', 'करनाल', 'Haryana', 29.6857, 76.9905],
  ['Hisar', 'हिसार', 'Haryana', 29.1492, 75.7217],
  ['Sonipat', 'सोनीपत', 'Haryana', 28.9931, 77.0151],
  ['Ambala', 'अंबाला', 'Haryana', 30.3752, 76.7821],
  ['Panchkula', 'पंचकुला', 'Haryana', 30.6942, 76.8606],
  ['Yamunanagar', 'यमुनानगर', 'Haryana', 30.1290, 77.2674],
  ['Chandigarh', 'चंडीगढ़', 'Chandigarh', 30.7333, 76.7794],
  ['Ludhiana', 'लुधियाना', 'Punjab', 30.9010, 75.8573],
  ['Amritsar', 'अमृतसर', 'Punjab', 31.6340, 74.8723],
  ['Jalandhar', 'जालंधर', 'Punjab', 31.3260, 75.5762],
  ['Patiala', 'पटियाला', 'Punjab', 30.3398, 76.3869],
  ['Bathinda', 'बठिंडा', 'Punjab', 30.2110, 74.9455],
  ['Mohali', 'मोहाली', 'Punjab', 30.7046, 76.7179],
  ['Pathankot', 'पठानकोट', 'Punjab', 32.2643, 75.6421],
  ['Hoshiarpur', 'होशियारपुर', 'Punjab', 31.5320, 75.9115],
  ['Shimla', 'शिमला', 'Himachal Pradesh', 31.1048, 77.1734],
  ['Dharamshala', 'धर्मशाला', 'Himachal Pradesh', 32.2190, 76.3234],
  ['Mandi', 'मंडी', 'Himachal Pradesh', 31.7076, 76.9318],
  ['Solan', 'सोलन', 'Himachal Pradesh', 30.9084, 77.0999],
  ['Dehradun', 'देहरादून', 'Uttarakhand', 30.3165, 78.0322],
  ['Haridwar', 'हरिद्वार', 'Uttarakhand', 29.9457, 78.1642],
  ['Rishikesh', 'ऋषिकेश', 'Uttarakhand', 30.0869, 78.2676],
  ['Haldwani', 'हल्द्वानी', 'Uttarakhand', 29.2183, 79.5126],
  ['Roorkee', 'रुड़की', 'Uttarakhand', 29.8543, 77.8880],
  ['Nainital', 'नैनीताल', 'Uttarakhand', 29.3919, 79.4542],
  ['Srinagar', 'श्रीनगर', 'Jammu & Kashmir', 34.0837, 74.7973],
  ['Jammu', 'जम्मू', 'Jammu & Kashmir', 32.7266, 74.8570],
  ['Leh', 'लेह', 'Ladakh', 34.1526, 77.5771],

  // — Gujarat —
  ['Ahmedabad', 'अहमदाबाद', 'Gujarat', 23.0225, 72.5714],
  ['Surat', 'सूरत', 'Gujarat', 21.1702, 72.8311],
  ['Vadodara', 'वडोदरा', 'Gujarat', 22.3072, 73.1812, 'baroda बड़ौदा'],
  ['Rajkot', 'राजकोट', 'Gujarat', 22.3039, 70.8022],
  ['Bhavnagar', 'भावनगर', 'Gujarat', 21.7645, 72.1519],
  ['Jamnagar', 'जामनगर', 'Gujarat', 22.4707, 70.0577],
  ['Junagadh', 'जूनागढ़', 'Gujarat', 21.5222, 70.4579],
  ['Gandhinagar', 'गांधीनगर', 'Gujarat', 23.2156, 72.6369],
  ['Gandhidham', 'गांधीधाम', 'Gujarat', 23.0753, 70.1337],
  ['Anand', 'आणंद', 'Gujarat', 22.5645, 72.9289],

  // — Maharashtra & Goa —
  ['Pune', 'पुणे', 'Maharashtra', 18.5204, 73.8567],
  ['Nagpur', 'नागपुर', 'Maharashtra', 21.1458, 79.0882],
  ['Thane', 'ठाणे', 'Maharashtra', 19.2183, 72.9781],
  ['Navi Mumbai', 'नवी मुंबई', 'Maharashtra', 19.0330, 73.0297],
  ['Kalyan', 'कल्याण', 'Maharashtra', 19.2403, 73.1305],
  ['Nashik', 'नासिक', 'Maharashtra', 19.9975, 73.7898],
  ['Chh. Sambhajinagar', 'छत्रपति संभाजीनगर', 'Maharashtra', 19.8762, 75.3433, 'aurangabad औरंगाबाद'],
  ['Solapur', 'सोलापुर', 'Maharashtra', 17.6599, 75.9064],
  ['Amravati', 'अमरावती', 'Maharashtra', 20.9374, 77.7796],
  ['Kolhapur', 'कोल्हापुर', 'Maharashtra', 16.7050, 74.2433],
  ['Sangli', 'सांगली', 'Maharashtra', 16.8524, 74.5815],
  ['Akola', 'अकोला', 'Maharashtra', 20.7002, 77.0082],
  ['Nanded', 'नांदेड़', 'Maharashtra', 19.1383, 77.3210],
  ['Latur', 'लातूर', 'Maharashtra', 18.4088, 76.5604],
  ['Dhule', 'धुले', 'Maharashtra', 20.9042, 74.7749],
  ['Ahilyanagar', 'अहिल्यानगर', 'Maharashtra', 19.0948, 74.7480, 'ahmednagar अहमदनगर'],
  ['Chandrapur', 'चंद्रपुर', 'Maharashtra', 19.9615, 79.2961],
  ['Malegaon', 'मालेगांव', 'Maharashtra', 20.5579, 74.5287],
  ['Satara', 'सतारा', 'Maharashtra', 17.6805, 74.0183],
  ['Panaji', 'पणजी', 'Goa', 15.4909, 73.8278, 'panjim'],
  ['Margao', 'मडगांव', 'Goa', 15.2832, 73.9862, 'madgaon'],

  // — Karnataka —
  ['Mysuru', 'मैसूर', 'Karnataka', 12.2958, 76.6394, 'mysore'],
  ['Hubballi', 'हुबली', 'Karnataka', 15.3647, 75.1240, 'hubli dharwad धारवाड़'],
  ['Mangaluru', 'मंगलुरु', 'Karnataka', 12.9141, 74.8560, 'mangalore मैंगलोर'],
  ['Belagavi', 'बेलगावी', 'Karnataka', 15.8497, 74.4977, 'belgaum बेलगाम'],
  ['Kalaburagi', 'कलबुर्गी', 'Karnataka', 17.3297, 76.8343, 'gulbarga गुलबर्गा'],
  ['Davanagere', 'दावणगेरे', 'Karnataka', 14.4644, 75.9218],
  ['Ballari', 'बल्लारी', 'Karnataka', 15.1394, 76.9214, 'bellary बेल्लारी'],
  ['Shivamogga', 'शिवमोग्गा', 'Karnataka', 13.9299, 75.5681, 'shimoga'],
  ['Vijayapura', 'विजयपुरा', 'Karnataka', 16.8302, 75.7100, 'bijapur बीजापुर'],
  ['Tumakuru', 'तुमकुरु', 'Karnataka', 13.3379, 77.1173, 'tumkur'],
  ['Udupi', 'उडुपी', 'Karnataka', 13.3409, 74.7421],

  // — Tamil Nadu, Kerala, Puducherry —
  ['Coimbatore', 'कोयंबटूर', 'Tamil Nadu', 11.0168, 76.9558],
  ['Madurai', 'मदुरै', 'Tamil Nadu', 9.9252, 78.1198],
  ['Tiruchirappalli', 'तिरुचिरापल्ली', 'Tamil Nadu', 10.7905, 78.7047, 'trichy'],
  ['Salem', 'सेलम', 'Tamil Nadu', 11.6643, 78.1460],
  ['Tirunelveli', 'तिरुनेलवेली', 'Tamil Nadu', 8.7139, 77.7567],
  ['Erode', 'इरोड', 'Tamil Nadu', 11.3410, 77.7172],
  ['Vellore', 'वेल्लोर', 'Tamil Nadu', 12.9165, 79.1325],
  ['Thoothukudi', 'तूतीकोरिन', 'Tamil Nadu', 8.7642, 78.1348, 'tuticorin'],
  ['Thanjavur', 'तंजावुर', 'Tamil Nadu', 10.7870, 79.1378, 'tanjore'],
  ['Tiruppur', 'तिरुप्पुर', 'Tamil Nadu', 11.1085, 77.3411],
  ['Nagercoil', 'नागरकोइल', 'Tamil Nadu', 8.1833, 77.4119],
  ['Puducherry', 'पुदुच्चेरी', 'Puducherry', 11.9416, 79.8083, 'pondicherry पांडिचेरी'],
  ['Thiruvananthapuram', 'तिरुवनंतपुरम', 'Kerala', 8.5241, 76.9366, 'trivandrum'],
  ['Kochi', 'कोच्चि', 'Kerala', 9.9312, 76.2673, 'cochin ernakulam'],
  ['Kozhikode', 'कोझिकोड', 'Kerala', 11.2588, 75.7804, 'calicut'],
  ['Thrissur', 'त्रिशूर', 'Kerala', 10.5276, 76.2144],
  ['Kollam', 'कोल्लम', 'Kerala', 8.8932, 76.6141, 'quilon'],
  ['Kannur', 'कण्णूर', 'Kerala', 11.8745, 75.3704],
  ['Alappuzha', 'अलप्पुझा', 'Kerala', 9.4981, 76.3388, 'alleppey'],
  ['Palakkad', 'पलक्कड़', 'Kerala', 10.7867, 76.6548],

  // — Andhra Pradesh & Telangana —
  ['Visakhapatnam', 'विशाखापत्तनम', 'Andhra Pradesh', 17.6868, 83.2185, 'vizag'],
  ['Vijayawada', 'विजयवाड़ा', 'Andhra Pradesh', 16.5062, 80.6480],
  ['Guntur', 'गुंटूर', 'Andhra Pradesh', 16.3067, 80.4365],
  ['Nellore', 'नेल्लोर', 'Andhra Pradesh', 14.4426, 79.9865],
  ['Kurnool', 'कुरनूल', 'Andhra Pradesh', 15.8281, 78.0373],
  ['Rajahmundry', 'राजमुंदरी', 'Andhra Pradesh', 17.0005, 81.8040],
  ['Kakinada', 'काकीनाडा', 'Andhra Pradesh', 16.9891, 82.2475],
  ['Tirupati', 'तिरुपति', 'Andhra Pradesh', 13.6288, 79.4192],
  ['Anantapur', 'अनंतपुर', 'Andhra Pradesh', 14.6819, 77.6006],
  ['Kadapa', 'कडपा', 'Andhra Pradesh', 14.4674, 78.8241],
  ['Warangal', 'वारंगल', 'Telangana', 17.9689, 79.5941],
  ['Nizamabad', 'निज़ामाबाद', 'Telangana', 18.6725, 78.0941],
  ['Karimnagar', 'करीमनगर', 'Telangana', 18.4386, 79.1288],
  ['Khammam', 'खम्मम', 'Telangana', 17.2473, 80.1514],
  ['Secunderabad', 'सिकंदराबाद', 'Telangana', 17.4399, 78.4983],

  // — East & Northeast —
  ['Howrah', 'हावड़ा', 'West Bengal', 22.5958, 88.2636],
  ['Durgapur', 'दुर्गापुर', 'West Bengal', 23.5204, 87.3119],
  ['Asansol', 'आसनसोल', 'West Bengal', 23.6739, 86.9524],
  ['Siliguri', 'सिलीगुड़ी', 'West Bengal', 26.7271, 88.3953],
  ['Kharagpur', 'खड़गपुर', 'West Bengal', 22.3460, 87.2313],
  ['Bardhaman', 'बर्धमान', 'West Bengal', 23.2324, 87.8615, 'burdwan'],
  ['Malda', 'मालदा', 'West Bengal', 25.0108, 88.1411],
  ['Bhubaneswar', 'भुवनेश्वर', 'Odisha', 20.2961, 85.8245],
  ['Cuttack', 'कटक', 'Odisha', 20.4625, 85.8828],
  ['Rourkela', 'राउरकेला', 'Odisha', 22.2604, 84.8536],
  ['Berhampur', 'ब्रह्मपुर', 'Odisha', 19.3149, 84.7941, 'brahmapur'],
  ['Sambalpur', 'संबलपुर', 'Odisha', 21.4669, 83.9812],
  ['Puri', 'पुरी', 'Odisha', 19.8135, 85.8312],
  ['Guwahati', 'गुवाहाटी', 'Assam', 26.1445, 91.7362],
  ['Dibrugarh', 'डिब्रूगढ़', 'Assam', 27.4728, 94.9120],
  ['Silchar', 'सिलचर', 'Assam', 24.8333, 92.7789],
  ['Imphal', 'इंफाल', 'Manipur', 24.8170, 93.9368],
  ['Aizawl', 'आइज़ोल', 'Mizoram', 23.7271, 92.7176],
  ['Agartala', 'अगरतला', 'Tripura', 23.8315, 91.2868],
  ['Shillong', 'शिलांग', 'Meghalaya', 25.5788, 91.8933],
  ['Kohima', 'कोहिमा', 'Nagaland', 25.6751, 94.1086],
  ['Itanagar', 'ईटानगर', 'Arunachal Pradesh', 27.0844, 93.6053],
  ['Gangtok', 'गंगटोक', 'Sikkim', 27.3389, 88.6065],
  ['Port Blair', 'पोर्ट ब्लेयर', 'Andaman & Nicobar', 11.6234, 92.7265],
];

/** [name, hindi, country, countryHindi, lat, lon, tz, aliases?] */
type WorldRow = [string, string, string, string, number, number, string, string?];

const WORLD: WorldRow[] = [
  ['Dubai', 'दुबई', 'UAE', 'संयुक्त अरब अमीरात', 25.2048, 55.2708, 'Asia/Dubai'],
  ['Abu Dhabi', 'आबू धाबी', 'UAE', 'संयुक्त अरब अमीरात', 24.4539, 54.3773, 'Asia/Dubai'],
  ['Sharjah', 'शारजाह', 'UAE', 'संयुक्त अरब अमीरात', 25.3463, 55.4209, 'Asia/Dubai'],
  ['Doha', 'दोहा', 'Qatar', 'क़तर', 25.2854, 51.5310, 'Asia/Qatar'],
  ['Riyadh', 'रियाद', 'Saudi Arabia', 'सऊदी अरब', 24.7136, 46.6753, 'Asia/Riyadh'],
  ['Jeddah', 'जेद्दा', 'Saudi Arabia', 'सऊदी अरब', 21.4858, 39.1925, 'Asia/Riyadh'],
  ['Muscat', 'मस्कट', 'Oman', 'ओमान', 23.5880, 58.3829, 'Asia/Muscat'],
  ['Kuwait City', 'कुवैत सिटी', 'Kuwait', 'कुवैत', 29.3759, 47.9774, 'Asia/Kuwait'],
  ['Manama', 'मनामा', 'Bahrain', 'बहरीन', 26.2285, 50.5860, 'Asia/Bahrain'],
  ['Kathmandu', 'काठमांडू', 'Nepal', 'नेपाल', 27.7172, 85.3240, 'Asia/Kathmandu'],
  ['Colombo', 'कोलंबो', 'Sri Lanka', 'श्रीलंका', 6.9271, 79.8612, 'Asia/Colombo'],
  ['Dhaka', 'ढाका', 'Bangladesh', 'बांग्लादेश', 23.8103, 90.4125, 'Asia/Dhaka'],
  ['Karachi', 'कराची', 'Pakistan', 'पाकिस्तान', 24.8607, 67.0011, 'Asia/Karachi'],
  ['Lahore', 'लाहौर', 'Pakistan', 'पाकिस्तान', 31.5204, 74.3587, 'Asia/Karachi'],
  ['Singapore', 'सिंगापुर', 'Singapore', 'सिंगापुर', 1.3521, 103.8198, 'Asia/Singapore'],
  ['Kuala Lumpur', 'कुआलालंपुर', 'Malaysia', 'मलेशिया', 3.1390, 101.6869, 'Asia/Kuala_Lumpur'],
  ['Bangkok', 'बैंकॉक', 'Thailand', 'थाईलैंड', 13.7563, 100.5018, 'Asia/Bangkok'],
  ['Hong Kong', 'हांगकांग', 'Hong Kong', 'हांगकांग', 22.3193, 114.1694, 'Asia/Hong_Kong'],
  ['Tokyo', 'टोक्यो', 'Japan', 'जापान', 35.6762, 139.6503, 'Asia/Tokyo'],
  ['Sydney', 'सिडनी', 'Australia', 'ऑस्ट्रेलिया', -33.8688, 151.2093, 'Australia/Sydney'],
  ['Melbourne', 'मेलबर्न', 'Australia', 'ऑस्ट्रेलिया', -37.8136, 144.9631, 'Australia/Melbourne'],
  ['Auckland', 'ऑकलैंड', 'New Zealand', 'न्यूज़ीलैंड', -36.8509, 174.7645, 'Pacific/Auckland'],
  ['London', 'लंदन', 'UK', 'यूनाइटेड किंगडम', 51.5074, -0.1278, 'Europe/London'],
  ['Birmingham', 'बर्मिंघम', 'UK', 'यूनाइटेड किंगडम', 52.4862, -1.8904, 'Europe/London'],
  ['Paris', 'पेरिस', 'France', 'फ्रांस', 48.8566, 2.3522, 'Europe/Paris'],
  ['Frankfurt', 'फ्रैंकफर्ट', 'Germany', 'जर्मनी', 50.1109, 8.6821, 'Europe/Berlin'],
  ['Amsterdam', 'एम्स्टर्डम', 'Netherlands', 'नीदरलैंड', 52.3676, 4.9041, 'Europe/Amsterdam'],
  ['Toronto', 'टोरंटो', 'Canada', 'कनाडा', 43.6532, -79.3832, 'America/Toronto'],
  ['Vancouver', 'वैंकूवर', 'Canada', 'कनाडा', 49.2827, -123.1207, 'America/Vancouver'],
  ['New York', 'न्यूयॉर्क', 'USA', 'अमेरिका', 40.7128, -74.0060, 'America/New_York'],
  ['Boston', 'बोस्टन', 'USA', 'अमेरिका', 42.3601, -71.0589, 'America/New_York'],
  ['Washington DC', 'वाशिंगटन डीसी', 'USA', 'अमेरिका', 38.9072, -77.0369, 'America/New_York'],
  ['Atlanta', 'अटलांटा', 'USA', 'अमेरिका', 33.7490, -84.3880, 'America/New_York'],
  ['Chicago', 'शिकागो', 'USA', 'अमेरिका', 41.8781, -87.6298, 'America/Chicago'],
  ['Houston', 'ह्यूस्टन', 'USA', 'अमेरिका', 29.7604, -95.3698, 'America/Chicago'],
  ['Dallas', 'डलास', 'USA', 'अमेरिका', 32.7767, -96.7970, 'America/Chicago'],
  ['San Francisco', 'सैन फ्रांसिस्को', 'USA', 'अमेरिका', 37.7749, -122.4194, 'America/Los_Angeles'],
  ['Los Angeles', 'लॉस एंजिलिस', 'USA', 'अमेरिका', 34.0522, -118.2437, 'America/Los_Angeles'],
  ['Seattle', 'सिएटल', 'USA', 'अमेरिका', 47.6062, -122.3321, 'America/Los_Angeles'],
  ['Nairobi', 'नैरोबी', 'Kenya', 'केन्या', -1.2921, 36.8219, 'Africa/Nairobi'],
  ['Johannesburg', 'जोहान्सबर्ग', 'South Africa', 'दक्षिण अफ़्रीका', -26.2041, 28.0473, 'Africa/Johannesburg'],
  ['Port Louis', 'पोर्ट लुइस', 'Mauritius', 'मॉरीशस', -20.1609, 57.5012, 'Indian/Mauritius'],
];

function haystack(...parts: Array<string | undefined>): string {
  return parts.filter(Boolean).join(' ').toLowerCase();
}

export const CITIES: City[] = [
  ...INDIA.map(([name, hi, state, lat, lon, aliases]): City => ({
    name, hi, region: state, regionHi: STATE_HI[state] ?? state, lat, lon,
    tz: 'Asia/Kolkata',
    search: haystack(name, hi, aliases, state, STATE_HI[state]),
  })),
  ...WORLD.map(([name, hi, country, countryHi, lat, lon, tz, aliases]): City => ({
    name, hi, region: country, regionHi: countryHi, lat, lon, tz,
    search: haystack(name, hi, aliases, country, countryHi),
  })),
];

/** Prefix-first ranked search over English names, Hindi names, and aliases. */
export function searchCities(query: string, limit = 8): City[] {
  const q = query.trim().toLowerCase();
  if (q.length < 2) return [];
  const starts: City[] = [];
  const contains: City[] = [];
  for (const c of CITIES) {
    if (c.name.toLowerCase().startsWith(q) || c.hi.startsWith(q)) starts.push(c);
    else if (c.search.includes(q)) contains.push(c);
    if (starts.length >= limit) break;
  }
  return [...starts, ...contains].slice(0, limit);
}
